[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Variable: CPU\_PREFIXES

> `const` **CPU\_PREFIXES**: readonly `string`[] = `[]`

Defined in: [config/semantic.ts:115](https://github.com/wontlost-ltd/aster-lang/blob/f1b05539f0a45448fa7c96fcf895807e73b4c6f5/src/config/semantic.ts#L115)

已知的 CPU 密集型操作前缀（用于 effect 推断）。

当函数调用以这些前缀开头时，推断为 `@cpu` effect。
