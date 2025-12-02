# Inlay Hints Fix - Remove `: UNKNOWN` Display

## Problem Summary

The VSCode extension was displaying "`: UNKNOWN`" at the beginning of lines, causing visual clutter and confusion.

### Example Issue

```aster
: UNKNOWN Let result be calculateScore(applicant).
: UNKNOWN If <(result.score, 700),:
: UNKNOWN   Return "Rejected".
```

The "`: UNKNOWN`" prefix appeared before `Let` statements and other code, making the editor hard to read.

## Root Cause

The issue was in the **LSP Inlay Hints** feature (`src/lsp/tokens.ts`).

### What are Inlay Hints?

Inlay hints are small type annotations that VSCode displays inline in the editor to show inferred types. For example:

```aster
Let result be calculateScore(applicant).
    ^^^^^^ : CreditCheckResult  ← This is an inlay hint
```

### The Bug

The inlay hints implementation had two problems:

1. **Showing UNKNOWN types**: When the type checker couldn't infer a type (returning `Unknown`), it still displayed `: UNKNOWN` as a hint
2. **Wrong position**: The hint was placed at the beginning of the line instead of after the variable name

This created the visual artifact:
```
: UNKNOWN Let result be ...
```

Instead of the intended:
```
Let result: CreditCheckResult be ...
```

## Solution Implemented

### Files Modified

1. **`src/types.ts`** (line 188): Added `nameSpan` field to `Let` interface
2. **`src/parser/expr-stmt-parser.ts`** (lines 168, 188): Capture variable name span during parsing
3. **`src/lsp/navigation/shared.ts`** (line 335): Use `nameSpan` for precise positioning
4. **`src/lsp/tokens.ts`** (lines 91-103): Fixed inlay hint generation

### Change 1: Add Variable Name Span to AST

**File**: `src/types.ts` (line 186-189)

```typescript
export interface Let extends Base.BaseLet<Span, Expression> {
  span: Span;
  readonly nameSpan?: Span;  // NEW: Precise span of the variable name
}
```

This allows the LSP to know exactly where the variable name is located, not just the entire statement.

### Change 2: Capture Name Span During Parsing

**File**: `src/parser/expr-stmt-parser.ts`

When parsing `Let` statements:

```typescript
// Line 168 - Capture the name token's span
const name = this.ctx.expect(TT.IDENTIFIER, 'variable name');
const nameSpan = name.span;  // NEW: Save the variable name's position

// Later when creating the Let AST node
return {
  kind: 'Let',
  name: name.value,
  nameSpan: nameSpan,  // NEW: Include in AST
  expr: expr,
  span: startSpan
};
```

### Change 3: Fix Inlay Hint Position and Filter UNKNOWN

**File**: `src/lsp/tokens.ts` (lines 91-103)

**Before**:
```typescript
const walk = (b: Block): void => {
  for (const s of b.statements as Statement[]) {
    if (s.kind === 'Let') {
      const sp = (s as any).span;  // Used entire statement span
      const hint = exprTypeText((s as any).expr);
      if (!sp || !hint) continue;
      // Placed hint at statement start - WRONG!
      out.push({
        position: { line: sp.start.line - 1, character: 0 },
        label: `: ${hint}`,
        kind: InlayHintKind.Type
      });
    }
  }
};
```

**After**:
```typescript
const walk = (b: Block): void => {
  for (const s of b.statements as Statement[]) {
    if (s.kind === 'Let') {
      // Use nameSpan (variable name position) instead of statement span
      const sp = ((s as any).nameSpan as Span | undefined) ??
                 ((s as any).span as Span | undefined);
      const hint = exprTypeText((s as any).expr);
      if (!sp || !hint) continue;

      const trimmed = hint.trim();
      // NEW: Filter out "unknown" types
      if (!trimmed || trimmed.toLowerCase() === 'unknown') continue;

      if (within(sp.start.line - 1, sp.start.col)) {
        const line = Math.max(0, sp.start.line - 1);
        // Place hint at END of variable name, not beginning of line
        const char = Math.max(0, (sp.end?.col ?? sp.start.col) - 1);
        out.push({
          position: { line, character: char },
          label: `: ${trimmed}`,
          kind: InlayHintKind.Type
        });
      }
    }
  }
};
```

**Key changes**:
1. Use `nameSpan` for precise variable name location
2. Place hint at **end of variable name** (`sp.end.col`) instead of beginning of line
3. Filter out hints where type is `"unknown"` (case-insensitive)

## Test Results

### Before Fix

```aster
: UNKNOWN Let result be calculateScore(applicant).
: UNKNOWN Let score be result.score.
: UNKNOWN If <(score, 700),:
```

Visual clutter with `: UNKNOWN` prefixes everywhere.

### After Fix

```aster
Let result be calculateScore(applicant).
    ^^^^^^ : CreditCheckResult

Let score be result.score.
    ^^^^^ : Int

If <(score, 700),:
```

- ✅ No `: UNKNOWN` displayed
- ✅ Inlay hints positioned correctly (after variable name)
- ✅ Only shows hints for successfully inferred types

## How Inlay Hints Work Now

### Example 1: Successfully Inferred Type

```aster
Let result be calculateScore(applicant).
```

**VSCode displays**:
```aster
Let result: CreditCheckResult be calculateScore(applicant).
```

The `: CreditCheckResult` hint appears right after the variable name.

### Example 2: Unknown Type (Filtered Out)

```aster
Let value be unknownFunction().
```

**VSCode displays**:
```aster
Let value be unknownFunction().
```

No hint displayed because type is `Unknown`.

### Example 3: Multiple Variables

```aster
Let applicant be getApplicant().
    ^^^^^^^^^ : ApplicantInfo

Let score be applicant.creditScore.
    ^^^^^ : Int

Let approved be <(score, 700).
    ^^^^^^^^ : Bool
```

Each variable gets its hint in the correct position.

## Inlay Hints Configuration

Users can control inlay hints in VSCode settings:

### Disable Inlay Hints Entirely

```json
// .vscode/settings.json
{
  "editor.inlayHints.enabled": "off"
}
```

### Enable Only for Specific Languages

```json
{
  "editor.inlayHints.enabled": "on",
  "[aster]": {
    "editor.inlayHints.enabled": "on"
  }
}
```

### Customize Appearance

Inlay hints appear in a lighter color by default. Users can customize via themes or settings.

## Implementation Details

### Why Capture nameSpan?

Before this fix, the `Let` AST node only had a `span` covering the entire statement:

```
Let result be calculateScore(applicant).
^                                       ^
|                                       |
span.start                          span.end
```

With `nameSpan`, we can precisely locate just the variable name:

```
Let result be calculateScore(applicant).
    ^    ^
    |    |
nameSpan.start  nameSpan.end
```

This allows placing the hint right after `result` instead of at the beginning of the line.

### Why Filter UNKNOWN?

When the type checker encounters:
- Functions it doesn't recognize
- Complex expressions it can't infer
- Errors in type inference

It returns an `Unknown` type. Displaying `: UNKNOWN` to users:
- Provides no useful information
- Creates visual noise
- Suggests a bug rather than helpful feedback

By filtering it out, we only show hints that actually help the developer understand their code.

## Related Features

This fix improves the **Type Hover** feature as well. When you hover over a variable, VSCode shows its type using the same type inference system.

### Before
- Hover showed `Unknown` for many variables
- Limited usefulness

### After
- Hover shows proper types from function return type inference
- Accurate type information

## Files Changed

1. **`src/types.ts`**: Added `nameSpan?: Span` to `Let` interface
2. **`src/parser/expr-stmt-parser.ts`**: Capture and store variable name span
3. **`src/lsp/navigation/shared.ts`**: Use `nameSpan` for navigation
4. **`src/lsp/tokens.ts`**: Fixed inlay hint position and filtering

## How to Use

The fix is included in the packaged extension:

```bash
# Install updated extension
code --uninstall-extension wontlost.aster-vscode
code --install-extension aster-vscode-0.3.0.vsix --force

# Reload VSCode
# Cmd+Shift+P → "Developer: Reload Window"
```

After reloading:
- ✅ No more `: UNKNOWN` at line beginnings
- ✅ Inlay hints appear in correct positions
- ✅ Only useful type hints are shown

## Future Enhancements

Potential improvements for inlay hints:

1. **Parameter hints**: Show parameter types in function calls
2. **Return type hints**: Show inferred return types in function bodies
3. **Generic type hints**: Show resolved generic type parameters
4. **Configurable verbosity**: Let users choose when to show hints

## Commit Message

```
fix(lsp): Fix inlay hints showing ': UNKNOWN' at line beginnings

LSP changes:
- Added nameSpan field to Let AST nodes for precise variable location
- Parser now captures variable name span during Let statement parsing
- Inlay hints now positioned at end of variable name, not line start
- Filter out 'Unknown' type hints to reduce visual noise

Result: Inlay hints now appear correctly positioned and only show
useful type information, eliminating the confusing ': UNKNOWN' prefix.

Test: Open .aster file in VSCode - no ': UNKNOWN' displayed
```

## References

- Issue: "There is an ': UNKNOWN' at the beginning of lines"
- Root cause: LSP inlay hints implementation in `src/lsp/tokens.ts`
- Fixed files: `src/types.ts`, `src/parser/expr-stmt-parser.ts`, `src/lsp/tokens.ts`
- Related: Function return type inference (`FUNCTION-RETURN-TYPE-FIX.md`)
