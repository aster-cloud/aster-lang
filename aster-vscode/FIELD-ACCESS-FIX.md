# Field Access Type Resolution Fix

## Problem Summary

The TypeScript type checker was reporting "Undefined variable" errors for field access on custom types.

### Example
```aster
Define ApplicantInfo with creditScore: Int, age: Int.

To processApplicant with applicant: ApplicantInfo, produce Int:
  Return applicant.creditScore.  // ❌ ERROR: Undefined variable: applicant.creditScore
```

The type checker treated `applicant.creditScore` as a single variable name lookup instead of recognizing it as field access.

## Root Cause

In `src/typecheck.ts`, the `TypeOfExprVisitor.visitExpression()` method handles the `Name` expression case (line 798-818). When encountering a name like `applicant.creditScore`:

1. It looked up `"applicant.creditScore"` as a single symbol name
2. When not found in the symbol table, it checked enum variants
3. If still not found, it reported "Undefined variable"

**It never parsed the dot notation or resolved field types.**

## Solution Implemented

### File Modified
- `src/typecheck.ts` (lines 798-885)

### Changes Made

Added field access resolution logic that:

1. **Detects dotted names**: Checks if `expression.name.includes('.')`
2. **Splits into parts**: `"applicant.creditScore"` → `["applicant", "creditScore"]`
3. **Resolves base**: Looks up `"applicant"` in symbol table
4. **Walks the field path**: For each field name in the path:
   - Expands type aliases using `TypeSystem.expand()`
   - Checks if current type is a custom `Data` type
   - Looks up the field in the `Data` declaration
   - Updates `currentType` to the field's type
5. **Returns field type**: Final type is the resolved field type

### Code Structure

```typescript
case 'Name': {
  // NEW: Field access resolution
  if (expression.name.includes('.')) {
    const parts = expression.name.split('.');
    const baseName = parts[0]!;
    const fieldPath = parts.slice(1);

    // Look up base variable
    const baseSymbol = symbols.lookup(baseName);
    // ... error handling ...

    // Resolve field access through type chain
    let currentType = baseSymbol.type;
    for (const fieldName of fieldPath) {
      const expanded = TypeSystem.expand(currentType, symbols.getTypeAliases());

      if (expanded.kind === 'TypeName') {
        const dataDecl = module.datas.get(expanded.name);
        const field = dataDecl.fields.find(f => f.name === fieldName);
        currentType = field.type as Core.Type;
      }
    }

    this.result = currentType;
    return;
  }

  // EXISTING: Regular variable lookup
  const symbol = symbols.lookup(expression.name);
  // ...
}
```

## Test Results

### Before Fix
```bash
$ node dist/scripts/typecheck-cli.js creditcard.aster
ERROR: Undefined variable: applicant.creditScore
ERROR: Undefined variable: history.bankruptcyCount
ERROR: Undefined variable: offer.requestedLimit
... (33 errors total)
```

### After Fix
```bash
$ node dist/scripts/typecheck-cli.js creditcard.aster
ERROR: Unknown field 'sufficient' for Unknown
ERROR: Unknown field 'recommendation' for Unknown
ERROR: Unknown field 'score' for Unknown
... (5 errors total) ✅
```

**Improvement**: 85% reduction in false positive errors (33 → 5)

## UPDATE: Complete Fix Implemented

**The remaining 5 errors have been fixed!** See `FUNCTION-RETURN-TYPE-FIX.md` for details.

After implementing function return type inference:
- **Before field access fix**: 33 errors
- **After field access fix**: 5 errors
- **After return type fix**: **0 errors** ✅

### What Was Fixed

The remaining 5 errors were caused by function calls returning `Unknown` type. For example:

```aster
Let result be calculateScore(applicant).  // result had type Unknown
If result.score > 700,:  // ❌ ERROR: Unknown field 'score' for Unknown
```

**Solution**: Added function signature environment that pre-scans function declarations and stores return types. Now `result` correctly has the declared return type, and field access works.

```aster
Let result be calculateScore(applicant).  // result has correct type ✅
If result.score > 700,:  // ✅ Field access works!
```

**See**: `FUNCTION-RETURN-TYPE-FIX.md` and `TYPE-SYSTEM-IMPROVEMENTS.md` for complete details.

## Remaining Limitations (Not Critical)

### 1. Direct Field Access on Function Call Results (Without Variable)

**Not supported** (requires AST changes):
```aster
If calculateScore(applicant).score > 700,:  // Would need FieldAccess AST node
```

**Current workaround** (works perfectly):
```aster
Let result be calculateScore(applicant).
If result.score > 700,:  // ✅ Works with current implementation
```

This limitation requires parser and AST changes to support `FieldAccess` expressions. See Codex analysis in `docs/workstreams/function-return-type-inference/operations-log.md`.

### 2. Module-Scoped Field Access

**Example**: `Text.concat()`, `List.map()` etc.

**Status**: Already handled separately (not affected by this fix). The type checker has special handling for module method calls.

### 3. Nested Field Access on Inline Constructs

**Example**:
```aster
Return (User with name = "Alice", age = 30).name.  // May not work
```

**Reason**: Needs expression type inference for `Construct` expressions, then field resolution on the result.

## Impact on LSP Features

### ✅ Improved

1. **Diagnostics**: 85% fewer false positive type errors
2. **Type Hover**: Can now show correct types for field expressions like `applicant.creditScore`
3. **Code Completion**: Can suggest fields when typing `applicant.`

### 🔄 Unchanged (Already Working)

1. **Go to Definition**: LSP navigation features work independently of type checking
2. **Find References**: Works based on AST structure, not type resolution
3. **Syntax Highlighting**: Grammar-based, unaffected

## How to Enable the Fix

### For VSCode Extension Users

1. **Rebuild the project**:
```bash
cd /Users/rpang/IdeaProjects/aster-lang
npm run build
```

2. **Rebuild VSCode extension**:
```bash
cd aster-vscode
npm run package
```

3. **Reinstall extension**:
```bash
code --uninstall-extension wontlost.aster-vscode
code --install-extension aster-vscode-0.3.0.vsix --force
```

4. **Reload VSCode**: `Cmd+Shift+P` → "Developer: Reload Window"

### For CLI Users

After rebuilding (`npm run build`), the fix is automatically available:

```bash
node dist/scripts/typecheck-cli.js your-file.aster
```

## Future Enhancements

To fix the remaining 5 errors, we would need to:

### Enhancement 1: Function Return Type Inference

When encountering `functionCall().field`:

1. **Infer return type** of the function call
2. **Apply field resolution** on that return type

Example:
```typescript
// In visitExpression for 'Call'
if (expression.target.kind === 'Name') {
  // Look up function declaration
  const funcDecl = findFunctionDeclaration(expression.target.name);
  if (funcDecl) {
    this.result = funcDecl.ret as Core.Type;  // Return type
  }
}

// Then when we see field access on a Call expression:
case 'Name': {
  if (expression.name.includes('.')) {
    // Check if base is actually a Call expression result
    // Resolve its type, then apply field resolution
  }
}
```

### Enhancement 2: Expression-Level Field Access

Support field access on any expression, not just names:

```typescript
interface FieldAccess {
  kind: 'FieldAccess';
  object: Expression;  // Any expression
  field: string;
  span?: Span;
}
```

This would require parser changes to create `FieldAccess` AST nodes.

## Related Files

- **Fixed**: `src/typecheck.ts` (TypeOfExprVisitor)
- **Supporting**: `src/typecheck/type_system.ts` (TypeSystem.expand)
- **Supporting**: `src/typecheck/symbol_table.ts` (SymbolTable.getTypeAliases)
- **Test case**: `quarkus-policy-api/src/main/resources/policies/finance/creditcard.aster`

## Verification

To verify the fix works:

```bash
# 1. Build
npm run build

# 2. Test with example file
node dist/scripts/typecheck-cli.js quarkus-policy-api/src/main/resources/policies/finance/creditcard.aster

# Expected: 5 errors (down from 33)
# Errors should only be for function call result field access
```

## Commit Message

```
fix(typecheck): Add field access type resolution for custom Data types

The TypeScript type checker now properly resolves field access on custom types
defined with `Define`. When encountering dotted names like `applicant.creditScore`:

- Splits the name into base variable and field path
- Looks up the base variable's type in the symbol table
- Walks through the field path, resolving each field's type from Data declarations
- Returns the final field's type instead of reporting "Undefined variable"

This reduces false positive type errors by 85% in realistic code that uses
custom data types with field access.

Limitation: Field access on function call results still requires storing the
result in a variable first.

Test: creditcard.aster (33 errors → 5 errors)
```

## References

- Original issue: "Type check 和跳转定义还有问题"
- Test file: `quarkus-policy-api/.../creditcard.aster`
- Error before: `ERROR: Undefined variable: applicant.creditScore` (×33)
- Error after: `ERROR: Unknown field 'score' for Unknown` (×5)
