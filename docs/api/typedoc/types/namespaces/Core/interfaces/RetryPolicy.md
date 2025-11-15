[**@wontlost-ltd/aster-lang**](../../../../README.md)

***

# Interface: RetryPolicy

Defined in: [types.ts:481](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types.ts#L481)

RetryPolicy 基础接口。

## Extends

- [`BaseRetryPolicy`](../../../base/interfaces/BaseRetryPolicy.md)

## Properties

### maxAttempts

> `readonly` **maxAttempts**: `number`

Defined in: [types/base.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L242)

#### Inherited from

[`BaseRetryPolicy`](../../../base/interfaces/BaseRetryPolicy.md).[`maxAttempts`](../../../base/interfaces/BaseRetryPolicy.md#maxattempts)

***

### backoff

> `readonly` **backoff**: `"exponential"` \| `"linear"`

Defined in: [types/base.ts:243](https://github.com/wontlost-ltd/aster-lang/blob/1bf231501298c8356da6c07e4bf89f297ebdc5b7/src/types/base.ts#L243)

#### Inherited from

[`BaseRetryPolicy`](../../../base/interfaces/BaseRetryPolicy.md).[`backoff`](../../../base/interfaces/BaseRetryPolicy.md#backoff)
