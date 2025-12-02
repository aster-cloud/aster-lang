# Selection and Highlighting Improvements

## Problem Summary

When testing creditcard.aster, several highlighting and selection issues were identified:

1. **Function calls not highlighted**: `calculateScore(...)`, `max(...)` appeared as plain text
2. **Property access not highlighted**: `applicant.creditScore`, `riskScore.score` - the field part was plain text
3. **Operator functions not highlighted**: `<+(...)`, `=(...)`, `>=(...)` - prefix operators before `(` were plain text
4. **Poor selection behavior**: Double-clicking `>=` only selected one character, not the whole operator
5. **Missing `be` keyword**: The `be` in `Let x be ...` was not highlighted

## Root Causes

### Issue 1: No Function Call Pattern

The TextMate grammar had no pattern to match function calls. It only highlighted:
- Function **declarations** (after `To` keyword)
- Type names (capitalized words)
- Keywords

Regular function calls like `calculateComprehensiveRiskScore(...)` were treated as plain text.

### Issue 2: No Property Access Pattern

Field access like `applicant.creditScore` had two parts:
- `applicant` - highlighted as a variable (if it matched a pattern)
- `.` - highlighted as punctuation
- `creditScore` - **not highlighted** (just plain text)

There was no pattern to recognize the identifier after a dot as a property/field.

### Issue 3: No Operator Function Pattern

Aster uses prefix notation for operators:
```aster
<+(x, y)      // Addition
=(a, b)       // Equality check
>=(score, 700) // Greater than or equal
```

The grammar only highlighted standalone operators (`+`, `=`, `>=`), not operators used as function names.

### Issue 4: Default Word Pattern

VSCode uses a default `wordPattern` that treats `.`, `<`, `>`, `=` as word separators. This caused:
- Double-click on `applicant.creditScore` → only selects `applicant` or `creditScore`
- Double-click on `>=` → only selects `>` or `=`
- Poor editing experience

### Issue 5: Missing Keyword

The `be` keyword (used in `Let x be expr`) was not in the keyword list.

## Solutions Implemented

### Fix 1: Add Function Call Highlighting

**File**: `aster-vscode/syntaxes/aster.tmLanguage.json`

**Added pattern** (line 303-310):
```json
"function-calls": {
  "patterns": [
    {
      "name": "entity.name.function.call.aster",
      "match": "\\b[a-z][A-Za-z0-9_]*\\b(?=\\s*\\()"
    }
  ]
}
```

**Pattern explanation**:
- `\\b[a-z][A-Za-z0-9_]*\\b` - Lowercase identifier (function names start with lowercase)
- `(?=\\s*\\()` - Lookahead for optional whitespace followed by `(`
- Excludes keywords because they're matched earlier in the pattern list

**What it highlights**:
```aster
calculateScore(applicant)
max(a, b, c)
min(values)
determineFinalCreditLimit(...)
```

### Fix 2: Add Property Access Highlighting

**Added pattern** (line 311-318):
```json
"property-access": {
  "patterns": [
    {
      "name": "variable.other.property.aster",
      "match": "(?<=\\.)[A-Za-z_][A-Za-z0-9_]*"
    }
  ]
}
```

**Pattern explanation**:
- `(?<=\\.)` - Positive lookbehind for a dot
- `[A-Za-z_][A-Za-z0-9_]*` - Identifier following the dot

**What it highlights**:
```aster
applicant.creditScore
         ^^^^^^^^^^^ ← highlighted as property

riskScore.score
         ^^^^^ ← highlighted as property

history.bankruptcyCount
        ^^^^^^^^^^^^^^^ ← highlighted as property
```

### Fix 3: Add Operator Function Highlighting

**Added pattern** (line 319-326):
```json
"operator-functions": {
  "patterns": [
    {
      "name": "keyword.operator.function.aster",
      "match": "(?<![<>=!+\\-*/%])[<>=!+\\-*/%]+(?=\\s*\\()"
    }
  ]
}
```

**Pattern explanation**:
- `(?<![<>=!+\\-*/%])` - Negative lookbehind to avoid matching middle of operators
- `[<>=!+\\-*/%]+` - One or more operator characters
- `(?=\\s*\\()` - Lookahead for optional whitespace then `(`

**What it highlights**:
```aster
<+(x, y)        // + highlighted as operator function
^^
=(a, b)         // = highlighted as operator function
^
>=(score, 700)  // >= highlighted as operator function
^^
-(x, y)         // - highlighted as operator function
^
```

### Fix 4: Add Word Pattern for Better Selection

**File**: `aster-vscode/language-configuration.json`

**Added** (line 5):
```json
"wordPattern": "([a-zA-Z_][a-zA-Z0-9_]*\\.)*[a-zA-Z_][a-zA-Z0-9_]*|[<>=!+\\-*/%]+|[0-9]+"
```

**Pattern explanation**:
- `([a-zA-Z_][a-zA-Z0-9_]*\\.)*` - Optional prefix identifiers with dots
- `[a-zA-Z_][a-zA-Z0-9_]*` - Main identifier
- `|[<>=!+\\-*/%]+` - OR multi-character operators
- `|[0-9]+` - OR numbers

**What it enables**:

| Double-click on... | Selects... |
|-------------------|------------|
| `applicant.creditScore` | Whole expression (optional) |
| `>=` | Both `>` and `=` |
| `<=` | Both `<` and `=` |
| `!=` | Both `!` and `=` |
| `<+` | Both `<` and `+` |

### Fix 5: Add `be` Keyword

**Updated** (line 240):
```json
"match": "\\b(Return|Match|When|If|Else|Otherwise|Let|Set|Start|Wait|Await|Define|To|be)\\b"
```

Added `be` to the keyword list.

**What it highlights**:
```aster
Let result be calculateScore(applicant).
           ^^ ← highlighted as keyword
```

## Pattern Order in Grammar

The pattern order is critical. After all fixes, the order is:

```json
"patterns": [
  { "include": "#comments" },           // 1. Comments first
  { "include": "#module-declaration" },
  { "include": "#import-statement" },
  { "include": "#function-declaration" },
  { "include": "#data-type-declaration" },
  { "include": "#enum-declaration" },
  { "include": "#keywords" },           // 2. Keywords
  { "include": "#effects" },
  { "include": "#annotations" },
  { "include": "#strings" },
  { "include": "#numbers" },
  { "include": "#function-calls" },     // 3. NEW: Function calls
  { "include": "#property-access" },    // 4. NEW: Property access
  { "include": "#operator-functions" }, // 5. NEW: Operator functions
  { "include": "#types" },              // 6. Types (fallback)
  { "include": "#operators" },
  { "include": "#punctuation" }
]
```

Function calls, property access, and operator functions come **before** the generic `#types` pattern to ensure they match first.

## Visual Impact

### Before Fixes

```aster
Let riskScore be calculateComprehensiveRiskScore(
  applicant.creditScore,
  history.bankruptcyCount
).

If <(riskScore.score, 500),:
  Return "High Risk".
```

**Highlighting**:
- `calculateComprehensiveRiskScore` - plain text (no color)
- `creditScore`, `bankruptcyCount` - plain text
- `score` (after `riskScore.`) - plain text
- `<` - operator color
- `(` - just opens, no context

### After Fixes

```aster
Let riskScore be calculateComprehensiveRiskScore(
    ^^^^^^^^^                                      keyword
              ^^                                   keyword
                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^    function call
  applicant.creditScore,
           ^^^^^^^^^^                              property
  history.bankruptcyCount
         ^^^^^^^^^^^^^^^                           property
).

If <(riskScore.score, 500),:
   ^                                               operator function
      ^^^^^^^^^^^^                                 keyword
              ^^^^^                                property
  Return "High Risk".
```

**Colors** (depend on your VSCode theme):
- Keywords (`Let`, `be`, `If`, `Return`) - Purple/Blue
- Function calls (`calculateComprehensiveRiskScore`) - Yellow/Cyan
- Properties (`creditScore`, `bankruptcyCount`, `score`) - Light Blue/White
- Operator functions (`<`) - Red/Orange
- Types (`ApplicantInfo`, `Int`) - Green/Teal

## Selection Behavior

### Before Fix

```
Double-click "applicant.creditScore"
→ Selects only "applicant" or "creditScore"

Double-click ">="
→ Selects only ">" or "="

Double-click "<+("
→ Selects only "<" or "+" or "("
```

### After Fix

```
Double-click "applicant.creditScore"
→ Can select whole expression (theme-dependent)

Double-click ">="
→ Selects both ">="

Double-click "<+"
→ Selects both "<+"

Double-click function name before "("
→ Selects whole function name
```

## Test Case: creditcard.aster

The fixes were specifically tested against creditcard.aster which contains:

**Line 17-40**: Complex function calls
```aster
Let riskScore be calculateComprehensiveRiskScore(...)
Let creditLimit be determineFinalCreditLimit(...)
Let apr be calculateAPR(...)
```
✅ All function names now highlighted

**Line 44-121**: Property access chains
```aster
applicant.creditScore
history.bankruptcyCount
riskScore.score
incomeCheck.recommendation
```
✅ All properties now highlighted

**Line 139-187**: Operator functions
```aster
<+(x, y)
=(value, threshold)
>=(score, minimum)
-(total, penalty)
```
✅ All operator functions now highlighted

## Files Modified

1. **`aster-vscode/syntaxes/aster.tmLanguage.json`**:
   - Added `#function-calls` pattern (line 303-310)
   - Added `#property-access` pattern (line 311-318)
   - Added `#operator-functions` pattern (line 319-326)
   - Added `be` to keywords (line 240)
   - Updated pattern order (line 40-47)

2. **`aster-vscode/language-configuration.json`**:
   - Added `wordPattern` (line 5)

## How to Use

The fixes are included in the rebuilt extension:

```bash
# Install updated extension
code --uninstall-extension wontlost.aster-vscode
code --install-extension aster-vscode-0.3.0.vsix --force

# Reload VSCode
# Cmd+Shift+P → "Developer: Reload Window"

# Open creditcard.aster
# Verify:
# - Function calls are colored
# - Properties after dots are colored
# - Operator functions (like >=(...)) are colored
# - Double-clicking >= selects both characters
```

## Theme Compatibility

The scope names used follow TextMate conventions and work with all standard VSCode themes:

| Scope Name | Theme Mapping | Typical Color |
|-----------|---------------|---------------|
| `entity.name.function.call.aster` | Function calls | Yellow/Cyan |
| `variable.other.property.aster` | Object properties | Light Blue/White |
| `keyword.operator.function.aster` | Operators | Red/Orange |
| `keyword.control.aster` | Keywords | Purple/Blue |
| `entity.name.type.aster` | Types | Green/Teal |

Different themes may use different colors, but the distinctions will be clear.

## Future Enhancements

Potential improvements:

1. **Semantic Highlighting**: Use LSP semantic tokens for even more precise coloring
2. **Built-in Function Highlighting**: Special color for `Text.concat`, `List.map`, etc.
3. **Parameter Highlighting**: Highlight parameter names differently from local variables
4. **Bracket Pair Colorization**: Enhanced bracket matching for nested operators

## Commit Message

```
fix(vscode): Improve highlighting and selection for function calls and operators

Grammar changes:
- Added function call highlighting (lowercase names before '(')
- Added property access highlighting (identifiers after '.')
- Added operator function highlighting (operators before '(')
- Added 'be' keyword to keyword list

Language configuration:
- Added wordPattern for better operator and dotted identifier selection
- Double-clicking '>=' now selects both characters
- Improved editing experience for prefix operators

Test: creditcard.aster now has proper highlighting for all syntax elements
```

## References

- Issue: "Test select and highlighting on creditcard.aster, looks like it's not working perfectly"
- Test file: `quarkus-policy-api/.../creditcard.aster`
- Grammar file: `aster-vscode/syntaxes/aster.tmLanguage.json`
- Language config: `aster-vscode/language-configuration.json`
- Analysis: Codex identified missing patterns in lines 17-187 of creditcard.aster
