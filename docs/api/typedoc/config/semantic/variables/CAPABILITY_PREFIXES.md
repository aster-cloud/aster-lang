[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Variable: CAPABILITY\_PREFIXES

> `const` **CAPABILITY\_PREFIXES**: `Record`\<`string`, readonly `string`[]\>

Defined in: [config/semantic.ts:144](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/config/semantic.ts#L144)

Capability 到调用前缀的映射表。

用于在 ASTER_CAP_EFFECTS_ENFORCE=1 模式下检查 capability 子集规则。
例如：声明了 [Http] capability 的函数只能调用以 'Http.' 开头的函数。
