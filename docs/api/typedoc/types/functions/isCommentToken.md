[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: isCommentToken()

> **isCommentToken**(`token`): `token is Token & { kind: COMMENT; value: CommentValue }`

Defined in: [types.ts:119](https://github.com/wontlost-ltd/aster-lang/blob/2b787d0632f54e12e5d73672527a6dad5f0766ce/src/types.ts#L119)

判断指定 Token 是否为注释 Token，便于在遍历过程中筛选注释。

## Parameters

### token

[`Token`](../interfaces/Token.md)

## Returns

`token is Token & { kind: COMMENT; value: CommentValue }`
