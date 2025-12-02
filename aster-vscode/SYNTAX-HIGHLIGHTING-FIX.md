# Syntax Highlighting Improvements

## Problem Summary

The VSCode extension's syntax highlighting was outdated and didn't properly highlight modern Aster code. Key issues:

1. **Data type declarations**: Only recognized old `A ... is a record of` syntax, not modern `Define ... with ...`
2. **Annotations**: Only highlighted effect markers (`@io`, `@cpu`, `@pure`), missing validation annotations (`@NotEmpty`, `@Range`, etc.)
3. **Operators**: Missing `!`, `!=`, `&&`, `||`, `%`, and other common operators
4. **Keywords**: Missing `Define`, `Else`, and other keywords outside specific contexts
5. **Field names**: Not highlighted in `Define` statements

## Solution Implemented

### Files Modified
- `aster-vscode/syntaxes/aster.tmLanguage.json`

### Changes Made

#### 1. Updated Data Type Declaration Pattern

**Before** (only old syntax):
```json
{
  "match": "^(A|An)\\s+([A-Z][A-Za-z0-9_]*)\\s+(is a record of)",
  "captures": {
    "1": { "name": "keyword.control.data.aster" },
    "2": { "name": "entity.name.type.aster" },
    "3": { "name": "keyword.control.data.aster" }
  }
}
```

**After** (supports modern syntax):
```json
{
  "begin": "^(Define)\\s+([A-Z][A-Za-z0-9_]*)\\s+(with)\\s+",
  "beginCaptures": {
    "1": { "name": "keyword.control.data.aster" },
    "2": { "name": "entity.name.type.aster" },
    "3": { "name": "keyword.control.data.aster" }
  },
  "end": "\\.",
  "patterns": [
    { "include": "#annotations" },
    {
      "name": "meta.field.definition.aster",
      "match": "([a-z][A-Za-z0-9_]*)\\s*(:)\\s*([A-Z][A-Za-z0-9_]*)",
      "captures": {
        "1": { "name": "variable.other.field.aster" },
        "2": { "name": "punctuation.separator.colon.aster" },
        "3": { "name": "entity.name.type.aster" }
      }
    }
  ]
}
```

This now properly highlights:
- `Define` keyword
- Type name (`ApplicantInfo`)
- `with` keyword
- Field names (`applicantId`, `age`)
- Field types (`Text`, `Int`)
- Annotations within the definition

#### 2. Added General Annotation Support

**New pattern**:
```json
"annotations": {
  "patterns": [
    {
      "name": "storage.modifier.annotation.aster",
      "match": "@[A-Za-z_][A-Za-z0-9_]*(?:\\([^\\)]*\\))?"
    }
  ]
}
```

This matches any annotation like:
- `@NotEmpty`
- `@Range(min: 18, max: 120)`
- `@PiiRedacted`
- `@io`, `@cpu`, `@pure` (still supported)

#### 3. Updated Enum Declaration Pattern

**Added**:
```json
{
  "match": "^(Define)\\s+([A-Z][A-Za-z0-9_]*)\\s+(as)\\s+(enum)\\s+(with)",
  "captures": {
    "1": { "name": "keyword.control.data.aster" },
    "2": { "name": "entity.name.type.aster" },
    "3": { "name": "keyword.control.enum.aster" },
    "4": { "name": "keyword.control.enum.aster" },
    "5": { "name": "keyword.control.enum.aster" }
  }
}
```

Properly highlights:
```aster
Define Status as enum with Pending, Approved, Rejected.
```

#### 4. Expanded Keyword List

**Before**:
```json
"match": "\\b(Return|Match|When|If|Otherwise|Let|Set|Start|Wait|Await)\\b"
```

**After**:
```json
"match": "\\b(Return|Match|When|If|Else|Otherwise|Let|Set|Start|Wait|Await|Define|To)\\b"
```

Added:
- `Define` - for data type and enum declarations
- `To` - for function declarations (in any position)
- `Else` - alternative to `Otherwise`

#### 5. Enhanced Operator Support

**Before** (single character only):
```json
{
  "name": "keyword.operator.arithmetic.aster",
  "match": "[+\\-*/<>=]"
}
```

**After** (comprehensive operator support):
```json
"operators": {
  "patterns": [
    {
      "name": "keyword.operator.logical.aster",
      "match": "&&|\\|\\|"
    },
    {
      "name": "keyword.operator.comparison.aster",
      "match": "==|!=|<=|>=|<|>"
    },
    {
      "name": "keyword.operator.assignment.aster",
      "match": "="
    },
    {
      "name": "keyword.operator.arithmetic.aster",
      "match": "[+\\-*/%]"
    },
    {
      "name": "keyword.operator.logical.not.aster",
      "match": "!"
    },
    {
      "name": "keyword.operator.question.aster",
      "match": "\\?"
    }
  ]
}
```

Now properly highlights:
- Logical: `&&`, `||`, `!`
- Comparison: `==`, `!=`, `<=`, `>=`, `<`, `>`
- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Assignment: `=`
- Question mark: `?`

## Example: Before and After

### Code Sample
```aster
Define ApplicantInfo with @NotEmpty applicantId: Text, @Range(min: 18, max: 120) age: Int.

Define Status as enum with Pending, Approved, Rejected.

To evaluateCreditCardApplication with applicant: ApplicantInfo, produce Text:
  If <(applicant.creditScore, 550),:
    Return "Rejected".
  Else,:
    Return "Approved".
```

### Before Fix
- `Define` - not highlighted (treated as plain text)
- `ApplicantInfo` - highlighted (type name)
- `with` - not highlighted in this context
- `@NotEmpty`, `@Range(...)` - not highlighted (plain text)
- `applicantId`, `age` - not highlighted (plain text)
- `Text`, `Int` - highlighted (type names)
- `!=`, `&&`, `||` - partially highlighted (only first character)
- `Else` - not highlighted

### After Fix
- `Define` - ✅ highlighted as keyword
- `ApplicantInfo` - ✅ highlighted as type name
- `with` - ✅ highlighted as keyword
- `@NotEmpty`, `@Range(...)` - ✅ highlighted as annotations
- `applicantId`, `age` - ✅ highlighted as field names
- `Text`, `Int` - ✅ highlighted as type names
- `!=`, `&&`, `||` - ✅ fully highlighted as operators
- `Else` - ✅ highlighted as keyword

## TextMate Scope Names Used

Following VSCode/TextMate conventions:

| Element | Scope Name | Color Theme Mapping |
|---------|-----------|---------------------|
| `Define`, `To`, `If` | `keyword.control.aster` | Keywords (purple/blue) |
| `ApplicantInfo`, `Int` | `entity.name.type.aster` | Types (green/teal) |
| `applicantId`, `age` | `variable.other.field.aster` | Variables (white/light) |
| `@NotEmpty`, `@Range` | `storage.modifier.annotation.aster` | Annotations (yellow/gold) |
| `+`, `-`, `*`, `%` | `keyword.operator.arithmetic.aster` | Operators (white/red) |
| `&&`, `\|\|`, `!` | `keyword.operator.logical.aster` | Operators (white/red) |
| `==`, `!=`, `<`, `>` | `keyword.operator.comparison.aster` | Operators (white/red) |
| Strings | `string.quoted.double.aster` | Strings (orange/red) |
| Numbers | `constant.numeric.integer.aster` | Numbers (green) |
| Comments | `comment.line.aster` | Comments (gray) |

## Testing

The grammar file was validated for:
1. **JSON validity**: `python3 -m json.tool` - ✅ passes
2. **Pattern matching**: Regex patterns tested against sample code - ✅ matches correctly
3. **No regressions**: Old syntax still supported alongside new syntax - ✅ backward compatible

## How to Use

The updated grammar is automatically included in the packaged extension.

### For Extension Users

1. **Install the updated extension**:
   ```bash
   code --uninstall-extension wontlost.aster-vscode
   code --install-extension aster-vscode-0.3.0.vsix --force
   ```

2. **Reload VSCode**:
   - `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows/Linux)
   - Type "Developer: Reload Window"
   - Press Enter

3. **Verify highlighting**:
   - Open any `.aster` file
   - Check that `Define`, annotations, operators, etc. are properly colored
   - Colors depend on your VS Code theme

### For Extension Developers

After modifying `syntaxes/aster.tmLanguage.json`:

1. Validate JSON:
   ```bash
   python3 -m json.tool aster-vscode/syntaxes/aster.tmLanguage.json > /dev/null
   ```

2. Rebuild extension:
   ```bash
   cd aster-vscode
   npm run package
   ```

3. Test in VSCode:
   - Press F5 to launch Extension Development Host
   - Open sample `.aster` files
   - Verify highlighting

## Related Files

- **Fixed**: `aster-vscode/syntaxes/aster.tmLanguage.json`
- **Test files**:
  - `quarkus-policy-api/.../creditcard.aster` - Complex real-world example
  - `aster-vscode/examples/bad-types.aster` - Simple test file
- **Documentation**: `aster-vscode/readme.md` (mentions syntax highlighting feature)

## Future Enhancements

Potential improvements for syntax highlighting:

1. **Semantic Highlighting**: Use LSP semantic tokens for context-aware highlighting (e.g., distinguish local variables from parameters)
2. **Generic Type Parameters**: Better highlighting for `List<T>`, `Map<K, V>` patterns
3. **String Interpolation**: If Aster adds string interpolation, add syntax for it
4. **Multiline Comments**: If Aster adds `/* ... */` comments, add pattern
5. **Doc Comments**: Special highlighting for documentation comments (if convention is established)

## Backward Compatibility

The updated grammar maintains support for old Aster syntax:

| Old Syntax | Still Supported? |
|------------|------------------|
| `A User is a record of` | ✅ Yes |
| `It has name: Text` | ✅ Yes |
| `An Status is one of` | ✅ Yes |
| Function declarations with `To` | ✅ Yes |
| `@io`, `@cpu`, `@pure` | ✅ Yes |

## Commit Message

```
fix(vscode): Update syntax highlighting for modern Aster syntax

The TextMate grammar now properly highlights modern Aster code:

- Data declarations: Supports `Define ... with ...` (not just old syntax)
- Annotations: Highlights all annotations (@NotEmpty, @Range, etc.)
- Operators: Full support for !=, &&, ||, %, ! and other operators
- Keywords: Added Define, To, Else to general keyword list
- Field names: Properly highlighted in Define statements

The grammar maintains backward compatibility with old Aster syntax while
providing better visual clarity for modern code.

Test: creditcard.aster and bad-types.aster display correct highlighting
```

## References

- Issue: "The highlight is not proper"
- TextMate Language Grammars: https://macromates.com/manual/en/language_grammars
- VSCode Syntax Highlight Guide: https://code.visualstudio.com/api/language-extensions/syntax-highlight-guide
- Updated grammar: `aster-vscode/syntaxes/aster.tmLanguage.json`
