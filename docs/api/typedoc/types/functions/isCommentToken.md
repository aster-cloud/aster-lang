[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: isCommentToken()

> **isCommentToken**(`token`): `token is Token & { kind: COMMENT; value: CommentValue }`

Defined in: [types.ts:119](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types.ts#L119)

判断指定 Token 是否为注释 Token，便于在遍历过程中筛选注释。

## Parameters

### token

[`Token`](../interfaces/Token.md)

## Returns

`token is Token & { kind: COMMENT; value: CommentValue }`
