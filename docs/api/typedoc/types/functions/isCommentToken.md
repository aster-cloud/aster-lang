[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: isCommentToken()

> **isCommentToken**(`token`): `token is Token & { kind: COMMENT; value: CommentValue }`

Defined in: [types.ts:116](https://github.com/wontlost-ltd/aster-lang/blob/4b70229c0d095f215a12808b993d8bc2c120bc1e/src/types.ts#L116)

判断指定 Token 是否为注释 Token，便于在遍历过程中筛选注释。

## Parameters

### token

[`Token`](../interfaces/Token.md)

## Returns

`token is Token & { kind: COMMENT; value: CommentValue }`
