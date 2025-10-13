[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Variable: IO\_PREFIXES

> `const` **IO\_PREFIXES**: readonly `string`[]

Defined in: [config/semantic.ts:100](https://github.com/wontlost-ltd/aster-lang/blob/a669709a62ce82370b6a92f305d9949cbbada7eb/src/config/semantic.ts#L100)

已知的 IO 操作前缀（用于 effect 推断）。

当函数调用以这些前缀开头时，推断为 `@io` effect。
例如：`Http.get()`, `Db.query()`
