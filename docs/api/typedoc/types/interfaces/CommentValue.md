[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: CommentValue

Defined in: [types.ts:113](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L113)

注释 Token 的取值结构

保存原始文本、整理后的主体文本以及注释分类，使词法分析阶段的注释处理更加可控。

## Properties

### raw

> `readonly` **raw**: `string`

Defined in: [types.ts:114](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L114)

***

### text

> `readonly` **text**: `string`

Defined in: [types.ts:115](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L115)

***

### trivia

> `readonly` **trivia**: `"inline"` \| `"standalone"`

Defined in: [types.ts:116](https://github.com/wontlost-ltd/aster-lang/blob/40a20d5de10b2ccdcc6d4d9cf102a12c54b7db94/src/types.ts#L116)
