[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: RetryPolicy

Defined in: [types.ts:470](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types.ts#L470)

RetryPolicy 基础接口。

## Extends

- [`BaseRetryPolicy`](../../../base/interfaces/BaseRetryPolicy.md)

## Properties

### maxAttempts

> `readonly` **maxAttempts**: `number`

Defined in: [types/base.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L242)

#### Inherited from

[`BaseRetryPolicy`](../../../base/interfaces/BaseRetryPolicy.md).[`maxAttempts`](../../../base/interfaces/BaseRetryPolicy.md#maxattempts)

***

### backoff

> `readonly` **backoff**: `"exponential"` \| `"linear"`

Defined in: [types/base.ts:243](https://github.com/wontlost-ltd/aster-lang/blob/79dfa19a5124ff223e5e405f5fcfc462e6d1587a/src/types/base.ts#L243)

#### Inherited from

[`BaseRetryPolicy`](../../../base/interfaces/BaseRetryPolicy.md).[`backoff`](../../../base/interfaces/BaseRetryPolicy.md#backoff)
