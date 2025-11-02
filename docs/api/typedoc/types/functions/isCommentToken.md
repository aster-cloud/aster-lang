[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Function: isCommentToken()

> **isCommentToken**(`token`): `token is Token & { kind: COMMENT; value: CommentValue }`

Defined in: [types.ts:116](https://github.com/wontlost-ltd/aster-lang/blob/06d5b68ae3e15dae97e142479397c69ed809146a/src/types.ts#L116)

判断指定 Token 是否为注释 Token，便于在遍历过程中筛选注释。

## Parameters

### token

[`Token`](../interfaces/Token.md)

## Returns

`token is Token & { kind: COMMENT; value: CommentValue }`
