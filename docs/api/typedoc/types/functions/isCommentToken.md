[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: isCommentToken()

> **isCommentToken**(`token`): `token is Token & { kind: COMMENT; value: CommentValue }`

Defined in: [types.ts:116](https://github.com/wontlost-ltd/aster-lang/blob/0b0406c33523b608f58b6400b072157cb0880387/src/types.ts#L116)

判断指定 Token 是否为注释 Token，便于在遍历过程中筛选注释。

## Parameters

### token

[`Token`](../interfaces/Token.md)

## Returns

`token is Token & { kind: COMMENT; value: CommentValue }`
