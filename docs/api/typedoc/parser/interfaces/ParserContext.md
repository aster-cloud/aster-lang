[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: ParserContext

Defined in: [parser.ts:21](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L21)

## Properties

### tokens

> `readonly` **tokens**: readonly [`Token`](../../types/interfaces/Token.md)[]

Defined in: [parser.ts:22](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L22)

***

### index

> **index**: `number`

Defined in: [parser.ts:23](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L23)

***

### moduleName

> **moduleName**: `null` \| `string`

Defined in: [parser.ts:24](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L24)

***

### declaredTypes

> **declaredTypes**: `Set`\<`string`\>

Defined in: [parser.ts:25](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L25)

***

### currentTypeVars

> **currentTypeVars**: `Set`\<`string`\>

Defined in: [parser.ts:26](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L26)

***

### collectedEffects

> **collectedEffects**: `null` \| `string`[]

Defined in: [parser.ts:27](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L27)

***

### effectSnapshots

> **effectSnapshots**: (`null` \| `string`[])[]

Defined in: [parser.ts:28](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L28)

***

### debug

> **debug**: `object`

Defined in: [parser.ts:29](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L29)

#### enabled

> **enabled**: `boolean`

#### depth

> **depth**: `number`

#### log()

> **log**(`message`): `void`

##### Parameters

###### message

`string`

##### Returns

`void`

## Methods

### peek()

> **peek**(`offset?`): [`Token`](../../types/interfaces/Token.md)

Defined in: [parser.ts:30](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L30)

#### Parameters

##### offset?

`number`

#### Returns

[`Token`](../../types/interfaces/Token.md)

***

### next()

> **next**(): [`Token`](../../types/interfaces/Token.md)

Defined in: [parser.ts:31](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L31)

#### Returns

[`Token`](../../types/interfaces/Token.md)

***

### at()

> **at**(`kind`, `value?`): `boolean`

Defined in: [parser.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L32)

#### Parameters

##### kind

[`TokenKind`](../../types/enumerations/TokenKind.md)

##### value?

`null` | `string` | `number` | `boolean`

#### Returns

`boolean`

***

### expect()

> **expect**(`kind`, `message`): [`Token`](../../types/interfaces/Token.md)

Defined in: [parser.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L33)

#### Parameters

##### kind

[`TokenKind`](../../types/enumerations/TokenKind.md)

##### message

`string`

#### Returns

[`Token`](../../types/interfaces/Token.md)

***

### isKeyword()

> **isKeyword**(`kw`): `boolean`

Defined in: [parser.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L34)

#### Parameters

##### kw

`string`

#### Returns

`boolean`

***

### isKeywordSeq()

> **isKeywordSeq**(`words`): `boolean`

Defined in: [parser.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L35)

#### Parameters

##### words

`string` | `string`[]

#### Returns

`boolean`

***

### nextWord()

> **nextWord**(): [`Token`](../../types/interfaces/Token.md)

Defined in: [parser.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L36)

#### Returns

[`Token`](../../types/interfaces/Token.md)

***

### nextWords()

> **nextWords**(`words`): `void`

Defined in: [parser.ts:37](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L37)

#### Parameters

##### words

`string`[]

#### Returns

`void`

***

### consumeIndent()

> **consumeIndent**(): `void`

Defined in: [parser.ts:38](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L38)

#### Returns

`void`

***

### consumeNewlines()

> **consumeNewlines**(): `void`

Defined in: [parser.ts:39](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L39)

#### Returns

`void`

***

### pushEffect()

> **pushEffect**(`effects`): `void`

Defined in: [parser.ts:40](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L40)

#### Parameters

##### effects

`string`[]

#### Returns

`void`

***

### snapshotEffects()

> **snapshotEffects**(): `null` \| `string`[]

Defined in: [parser.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L41)

#### Returns

`null` \| `string`[]

***

### restoreEffects()

> **restoreEffects**(`snapshot`): `void`

Defined in: [parser.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L42)

#### Parameters

##### snapshot

`null` | `string`[]

#### Returns

`void`

***

### withTypeScope()

> **withTypeScope**\<`T`\>(`names`, `body`): `T`

Defined in: [parser.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/515b722332297ca0ad587cf979b44d7363fdedd5/src/parser.ts#L43)

#### Type Parameters

##### T

`T`

#### Parameters

##### names

`Iterable`\<`string`\>

##### body

() => `T`

#### Returns

`T`
