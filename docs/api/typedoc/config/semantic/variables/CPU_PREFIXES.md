[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Variable: CPU\_PREFIXES

> `const` **CPU\_PREFIXES**: readonly `string`[] = `[]`

Defined in: [config/semantic.ts:115](https://github.com/wontlost-ltd/aster-lang/blob/fe93f11b4481d694b312ddca8e1933258bd7f0df/src/config/semantic.ts#L115)

已知的 CPU 密集型操作前缀（用于 effect 推断）。

当函数调用以这些前缀开头时，推断为 `@cpu` effect。
