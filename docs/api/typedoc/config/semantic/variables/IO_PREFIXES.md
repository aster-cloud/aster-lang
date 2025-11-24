[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Variable: IO\_PREFIXES

> `const` **IO\_PREFIXES**: readonly `string`[]

Defined in: [config/semantic.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/0c51a366f9525ababae2dac9f0cf7c0dee59fc16/src/config/semantic.ts#L100)

已知的 IO 操作前缀（用于 effect 推断）。

当函数调用以这些前缀开头时，推断为 `@io` effect。
例如：`Http.get()`, `Db.query()`
