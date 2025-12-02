# Stdlib API 参考手册

**版本**: 0.2.0
**状态**: 草案 (Draft)
**最后更新**: 2025-11-08 17:50 NZDT
**维护者**: Claude Code

---

## 概述

Aster 标准库提供以下核心类型及其操作：
- **Text** - 不可变文本字符串
- **List\<T\>** - 泛型列表
- **Map\<K,V\>** - 键值映射
- **Result\<T,E\>** - 错误处理类型
- **Maybe\<T\>** - 可选值类型
- **Int**, **Long**, **Double** - 数值类型

所有 API 都包含效果注解（`with ∅`, `with CPU`, `with IO`），确保编译时效果检查。

---

## Text（文本类型）

### Text.length

#### 签名
```typescript
fn length(text: Text): Int with ∅
```

#### 参数
- `text: Text` - 要计算长度的文本

#### 返回值
- `Int` - 文本的字符数（Unicode 字符计数）

#### 效果
- `∅` (纯计算，无副作用)

#### 示例
```aster
Given a text "Hello"
When I get its length
Then I get 5
```

#### 边界情况
- 空字符串返回 0
- Emoji 计算为 1 个字符（如 "👍" length = 1）
- Unicode 组合字符按单个字符计数

---

### Text.isEmpty

#### 签名
```typescript
fn isEmpty(text: Text): Bool with ∅
```

#### 参数
- `text: Text` - 要检查的文本

#### 返回值
- `Bool` - 如果文本为空返回 `True`，否则返回 `False`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text ""
When I check if it is empty
Then I get True

Given a text "hello"
When I check if it is empty
Then I get False
```

---

### Text.concat

#### 签名
```typescript
fn concat(left: Text, right: Text): Text with ∅
```

#### 参数
- `left: Text` - 左侧文本
- `right: Text` - 右侧文本

#### 返回值
- `Text` - 连接后的新文本

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "Hello"
And a text " World"
When I concat them
Then I get "Hello World"
```

---

### Text.indexOf

#### 签名
```typescript
fn indexOf(text: Text, substring: Text): Maybe<Int> with ∅
```

#### 参数
- `text: Text` - 源文本
- `substring: Text` - 要查找的子字符串

#### 返回值
- `Maybe<Int>` - 第一次出现的索引位置（从 0 开始），如果未找到返回 `None`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "Hello World"
When I find index of "World"
Then I get Some(6)

Given a text "Hello World"
When I find index of "Python"
Then I get None
```

---

### Text.startsWith

#### 签名
```typescript
fn startsWith(text: Text, prefix: Text): Bool with ∅
```

#### 参数
- `text: Text` - 源文本
- `prefix: Text` - 前缀

#### 返回值
- `Bool` - 如果文本以指定前缀开始返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "Hello World"
When I check if it starts with "Hello"
Then I get True
```

---

### Text.endsWith

#### 签名
```typescript
fn endsWith(text: Text, suffix: Text): Bool with ∅
```

#### 参数
- `text: Text` - 源文本
- `suffix: Text` - 后缀

#### 返回值
- `Bool` - 如果文本以指定后缀结束返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "Hello World"
When I check if it ends with "World"
Then I get True
```

---

### Text.substring

#### 签名
```typescript
fn substring(text: Text, start: Int, end: Int): Text with ∅
```

#### 参数
- `text: Text` - 源文本
- `start: Int` - 起始索引（包含，从 0 开始）
- `end: Int` - 结束索引（不包含）

#### 返回值
- `Text` - 提取的子字符串

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "Hello World"
When I substring from 0 to 5
Then I get "Hello"
```

#### 边界情况
- `start` 超出范围返回空字符串
- `end` 超出范围截断到文本末尾
- `start > end` 返回空字符串

---

### Text.toLowerCase

#### 签名
```typescript
fn toLowerCase(text: Text): Text with ∅
```

#### 参数
- `text: Text` - 源文本

#### 返回值
- `Text` - 全部转换为小写的新文本

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "Hello World"
When I convert to lowercase
Then I get "hello world"
```

---

### Text.toUpperCase

#### 签名
```typescript
fn toUpperCase(text: Text): Text with ∅
```

#### 参数
- `text: Text` - 源文本

#### 返回值
- `Text` - 全部转换为大写的新文本

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "Hello World"
When I convert to uppercase
Then I get "HELLO WORLD"
```

---

### Text.trim

#### 签名
```typescript
fn trim(text: Text): Text with ∅
```

#### 参数
- `text: Text` - 源文本

#### 返回值
- `Text` - 去除首尾空白字符后的新文本

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "  Hello World  "
When I trim it
Then I get "Hello World"
```

#### 说明
- 移除的空白字符包括：空格、制表符、换行符

---

### Text.split

#### 签名
```typescript
fn split(text: Text, delimiter: Text): List<Text> with ∅
```

#### 参数
- `text: Text` - 源文本
- `delimiter: Text` - 分隔符

#### 返回值
- `List<Text>` - 分割后的文本列表

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a text "apple,banana,cherry"
When I split by ","
Then I get ["apple", "banana", "cherry"]
```

#### 边界情况
- 分隔符未找到时返回包含原文本的单元素列表
- 空分隔符将文本分割为字符列表

---

### Text.join

#### 签名
```typescript
fn join(parts: List<Text>, separator: Text): Text with ∅
```

#### 参数
- `parts: List<Text>` - 要连接的文本列表
- `separator: Text` - 分隔符

#### 返回值
- `Text` - 连接后的文本

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list ["apple", "banana", "cherry"]
When I join with ", "
Then I get "apple, banana, cherry"
```

---

## List\<T\>（列表类型）

### List.length

#### 签名
```typescript
fn length\<T\>(list: List\<T\>): Int with ∅
```

#### 参数
- `list: List\<T\>` - 源列表

#### 返回值
- `Int` - 列表中元素的数量

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [1, 2, 3, 4, 5]
When I get its length
Then I get 5
```

---

### List.isEmpty

#### 签名
```typescript
fn isEmpty\<T\>(list: List\<T\>): Bool with ∅
```

#### 参数
- `list: List\<T\>` - 源列表

#### 返回值
- `Bool` - 如果列表为空返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given an empty list
When I check if it is empty
Then I get True
```

---

### List.get

#### 签名
```typescript
fn get\<T\>(list: List\<T\>, index: Int): Maybe\<T\> with ∅
```

#### 参数
- `list: List\<T\>` - 源列表
- `index: Int` - 索引位置（从 0 开始）

#### 返回值
- `Maybe\<T\>` - 索引处的元素，如果索引越界返回 `None`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [10, 20, 30]
When I get element at index 1
Then I get Some(20)

Given a list [10, 20, 30]
When I get element at index 5
Then I get None
```

---

### List.append

#### 签名
```typescript
fn append\<T\>(list: List\<T\>, element: T): List\<T\> with ∅
```

#### 参数
- `list: List\<T\>` - 源列表
- `element: T` - 要添加的元素

#### 返回值
- `List\<T\>` - 添加元素后的新列表

#### 效果
- `∅` (纯计算，返回新列表，不修改原列表)

#### 示例
```aster
Given a list [1, 2, 3]
When I append 4
Then I get [1, 2, 3, 4]
```

---

### List.prepend

#### 签名
```typescript
fn prepend\<T\>(list: List\<T\>, element: T): List\<T\> with ∅
```

#### 参数
- `list: List\<T\>` - 源列表
- `element: T` - 要添加的元素

#### 返回值
- `List\<T\>` - 在开头添加元素后的新列表

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [2, 3, 4]
When I prepend 1
Then I get [1, 2, 3, 4]
```

---

### List.concat

#### 签名
```typescript
fn concat\<T\>(left: List\<T\>, right: List\<T\>): List\<T\> with ∅
```

#### 参数
- `left: List\<T\>` - 左侧列表
- `right: List\<T\>` - 右侧列表

#### 返回值
- `List\<T\>` - 连接后的新列表

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [1, 2]
And a list [3, 4]
When I concat them
Then I get [1, 2, 3, 4]
```

---

### List.map

#### 签名
```typescript
fn map\<T, U, E\>(list: List\<T\>, f: T -> U with E): List\<U\> with E
```

#### 参数
- `list: List\<T\>` - 源列表
- `f: T -> U with E` - 映射函数（可能有效果 E）

#### 返回值
- `List\<U\>` - 应用函数后的新列表

#### 效果
- `E` - 继承映射函数的效果（效果多态）

#### 示例
```aster
Given a list [1, 2, 3]
When I map each element to its double
Then I get [2, 4, 6]
```

#### 说明
- 如果 `f` 是 `with IO`，整个 `map` 操作也是 `with IO`
- 空列表返回空列表
- 映射函数按顺序应用于每个元素

---

### List.filter

#### 签名
```typescript
fn filter<T, E>(list: List\<T\>, predicate: T -> Bool with E): List\<T\> with E
```

#### 参数
- `list: List\<T\>` - 源列表
- `predicate: T -> Bool with E` - 谓词函数

#### 返回值
- `List\<T\>` - 满足条件的元素组成的新列表

#### 效果
- `E` - 继承谓词函数的效果

#### 示例
```aster
Given a list [1, 2, 3, 4, 5]
When I filter elements greater than 2
Then I get [3, 4, 5]
```

---

### List.fold

#### 签名
```typescript
fn fold\<T, U, E\>(list: List\<T\>, initial: U, f: (U, T) -> U with E): U with E
```

#### 参数
- `list: List\<T\>` - 源列表
- `initial: U` - 初始累积值
- `f: (U, T) -> U with E` - 折叠函数

#### 返回值
- `U` - 最终累积值

#### 效果
- `E` - 继承折叠函数的效果

#### 示例
```aster
Given a list [1, 2, 3, 4]
When I fold with initial 0 and add function
Then I get 10
```

#### 说明
- 从左到右遍历列表
- 折叠函数接收累积值和当前元素，返回新累积值

---

### List.head

#### 签名
```typescript
fn head\<T\>(list: List\<T\>): Maybe\<T\> with ∅
```

#### 参数
- `list: List\<T\>` - 源列表

#### 返回值
- `Maybe\<T\>` - 第一个元素，如果列表为空返回 `None`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [1, 2, 3]
When I get the head
Then I get Some(1)
```

---

### List.tail

#### 签名
```typescript
fn tail\<T\>(list: List\<T\>): Maybe<List\<T\>> with ∅
```

#### 参数
- `list: List\<T\>` - 源列表

#### 返回值
- `Maybe<List\<T\>>` - 除第一个元素外的剩余列表，如果列表为空返回 `None`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [1, 2, 3, 4]
When I get the tail
Then I get Some([2, 3, 4])
```

---

### List.last

#### 签名
```typescript
fn last\<T\>(list: List\<T\>): Maybe\<T\> with ∅
```

#### 参数
- `list: List\<T\>` - 源列表

#### 返回值
- `Maybe\<T\>` - 最后一个元素，如果列表为空返回 `None`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [1, 2, 3]
When I get the last element
Then I get Some(3)
```

---

### List.contains

#### 签名
```typescript
fn contains\<T\>(list: List\<T\>, element: T): Bool with ∅
```

#### 参数
- `list: List\<T\>` - 源列表
- `element: T` - 要查找的元素

#### 返回值
- `Bool` - 如果列表包含该元素返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [1, 2, 3]
When I check if it contains 2
Then I get True
```

#### 说明
- 使用相等性比较 (`==`)
- 元素类型 T 必须支持相等性比较

---

### List.indexOf

#### 签名
```typescript
fn indexOf\<T\>(list: List\<T\>, element: T): Maybe<Int> with ∅
```

#### 参数
- `list: List\<T\>` - 源列表
- `element: T` - 要查找的元素

#### 返回值
- `Maybe<Int>` - 第一次出现的索引位置，如果未找到返回 `None`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a list [10, 20, 30, 20]
When I find index of 20
Then I get Some(1)
```

---

### List.find

#### 签名
```typescript
fn find<T, E>(list: List\<T\>, predicate: T -> Bool with E): Maybe\<T\> with E
```

#### 参数
- `list: List\<T\>` - 源列表
- `predicate: T -> Bool with E` - 谓词函数

#### 返回值
- `Maybe\<T\>` - 第一个满足条件的元素，如果未找到返回 `None`

#### 效果
- `E` - 继承谓词函数的效果

#### 示例
```aster
Given a list [1, 2, 3, 4, 5]
When I find first element greater than 3
Then I get Some(4)
```

---

## Map\<K,V\>（映射类型）

### Map.get

#### 签名
```typescript
fn get<K, V>(map: Map<K, V>, key: K): Maybe\<V\> with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射
- `key: K` - 键

#### 返回值
- `Maybe\<V\>` - 键对应的值，如果键不存在返回 `None`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a map {"name": "Alice", "age": "30"}
When I get value for key "name"
Then I get Some("Alice")
```

---

### Map.put

#### 签名
```typescript
fn put<K, V>(map: Map<K, V>, key: K, value: V): Map<K, V> with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射
- `key: K` - 键
- `value: V` - 值

#### 返回值
- `Map<K, V>` - 添加/更新键值对后的新映射

#### 效果
- `∅` (纯计算，返回新映射，不修改原映射)

#### 示例
```aster
Given an empty map
When I put key "name" with value "Bob"
Then I get {"name": "Bob"}
```

#### 说明
- 如果键已存在，更新其值
- 如果键不存在，添加新键值对

---

### Map.remove

#### 签名
```typescript
fn remove<K, V>(map: Map<K, V>, key: K): Map<K, V> with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射
- `key: K` - 要移除的键

#### 返回值
- `Map<K, V>` - 移除键值对后的新映射

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a map {"name": "Alice", "age": "30"}
When I remove key "age"
Then I get {"name": "Alice"}
```

---

### Map.containsKey

#### 签名
```typescript
fn containsKey<K, V>(map: Map<K, V>, key: K): Bool with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射
- `key: K` - 键

#### 返回值
- `Bool` - 如果映射包含该键返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a map {"name": "Alice"}
When I check if it contains key "name"
Then I get True
```

---

### Map.keys

#### 签名
```typescript
fn keys<K, V>(map: Map<K, V>): List\<K\> with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射

#### 返回值
- `List\<K\>` - 所有键的列表

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a map {"name": "Alice", "age": "30"}
When I get all keys
Then I get ["name", "age"]
```

#### 说明
- 键的顺序未定义

---

### Map.values

#### 签名
```typescript
fn values<K, V>(map: Map<K, V>): List\<V\> with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射

#### 返回值
- `List\<V\>` - 所有值的列表

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a map {"name": "Alice", "age": "30"}
When I get all values
Then I get ["Alice", "30"]
```

---

### Map.entries

#### 签名
```typescript
fn entries<K, V>(map: Map<K, V>): List<(K, V)> with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射

#### 返回值
- `List<(K, V)>` - 所有键值对的列表

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a map {"name": "Alice", "age": "30"}
When I get all entries
Then I get [("name", "Alice"), ("age", "30")]
```

---

### Map.size

#### 签名
```typescript
fn size<K, V>(map: Map<K, V>): Int with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射

#### 返回值
- `Int` - 键值对的数量

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a map {"name": "Alice", "age": "30"}
When I get its size
Then I get 2
```

---

### Map.isEmpty

#### 签名
```typescript
fn isEmpty<K, V>(map: Map<K, V>): Bool with ∅
```

#### 参数
- `map: Map<K, V>` - 源映射

#### 返回值
- `Bool` - 如果映射为空返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given an empty map
When I check if it is empty
Then I get True
```

---

## Result<T, E>（错误处理类型）

Result 类型用于显式的错误处理，避免异常和 null 值。

### Result.ok

#### 签名
```typescript
fn ok<T, E>(value: T): Result<T, E> with ∅
```

#### 参数
- `value: T` - 成功的值

#### 返回值
- `Result<T, E>` - 包含成功值的 Result

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a value 42
When I create an ok result
Then I get Ok(42)
```

---

### Result.err

#### 签名
```typescript
fn err<T, E>(error: E): Result<T, E> with ∅
```

#### 参数
- `error: E` - 错误值

#### 返回值
- `Result<T, E>` - 包含错误的 Result

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given an error "Invalid input"
When I create an error result
Then I get Err("Invalid input")
```

---

### Result.isOk

#### 签名
```typescript
fn isOk<T, E>(result: Result<T, E>): Bool with ∅
```

#### 参数
- `result: Result<T, E>` - 要检查的 Result

#### 返回值
- `Bool` - 如果是成功值返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a result Ok(42)
When I check if it is ok
Then I get True
```

---

### Result.isErr

#### 签名
```typescript
fn isErr<T, E>(result: Result<T, E>): Bool with ∅
```

#### 参数
- `result: Result<T, E>` - 要检查的 Result

#### 返回值
- `Bool` - 如果是错误返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a result Err("error")
When I check if it is error
Then I get True
```

---

### Result.unwrap

#### 签名
```typescript
fn unwrap<T, E>(result: Result<T, E>): T with ∅
```

#### 参数
- `result: Result<T, E>` - 要解包的 Result

#### 返回值
- `T` - 成功值

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a result Ok(42)
When I unwrap it
Then I get 42
```

#### 警告
- 如果 Result 是 `Err`，程序会 panic
- 仅在确定是 `Ok` 时使用，否则使用 `unwrapOr` 或 `match`

---

### Result.unwrapOr

#### 签名
```typescript
fn unwrapOr<T, E>(result: Result<T, E>, default: T): T with ∅
```

#### 参数
- `result: Result<T, E>` - 要解包的 Result
- `default: T` - 错误时的默认值

#### 返回值
- `T` - 成功值或默认值

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a result Err("error")
When I unwrap with default 0
Then I get 0

Given a result Ok(42)
When I unwrap with default 0
Then I get 42
```

---

### Result.map

#### 签名
```typescript
fn map<T, U, E, F>(result: Result<T, E>, f: T -> U with F): Result<U, E> with F
```

#### 参数
- `result: Result<T, E>` - 源 Result
- `f: T -> U with F` - 映射函数

#### 返回值
- `Result<U, E>` - 映射后的 Result，错误保持不变

#### 效果
- `F` - 继承映射函数的效果

#### 示例
```aster
Given a result Ok(5)
When I map it with double function
Then I get Ok(10)

Given a result Err("error")
When I map it with double function
Then I get Err("error")
```

#### 说明
- 仅在 `Ok` 时应用函数
- `Err` 值原样传递

---

### Result.flatMap

#### 签名
```typescript
fn flatMap<T, U, E1, E2, F>(
  result: Result<T, E1>,
  f: T -> Result<U, E2> with F
): Result<U, E1 | E2> with F
```

#### 参数
- `result: Result<T, E1>` - 源 Result
- `f: T -> Result<U, E2> with F` - 返回 Result 的函数

#### 返回值
- `Result<U, E1 | E2>` - 链式调用后的 Result

#### 效果
- `F` - 继承函数的效果

#### 示例（链式错误处理）
```aster
Define parseInteger(text: Text): Result<Int, Text>
  # 解析文本为整数

Define validatePositive(n: Int): Result<Int, Text>
  If n > 0
    Return Ok(n)
  Else
    Return Err("Number must be positive")

# 链式调用示例 1
Given a user input "42"
When I parse it as integer and then validate it is positive
Then I get Ok(42)

# 链式调用示例 2
Given a user input "-5"
When I parse it as integer and then validate it is positive
Then I get Err("Number must be positive")

# 链式调用示例 3
Given a user input "abc"
When I parse it as integer and then validate it is positive
Then I get Err("Invalid integer format")
```

#### 说明
- 用于链式错误处理，类似 Rust 的 `?` 运算符
- 第一个错误会短路整个链

---

### Result.mapErr

#### 签名
```typescript
fn mapErr<T, E1, E2, F>(result: Result<T, E1>, f: E1 -> E2 with F): Result<T, E2> with F
```

#### 参数
- `result: Result<T, E1>` - 源 Result
- `f: E1 -> E2 with F` - 错误映射函数

#### 返回值
- `Result<T, E2>` - 错误类型转换后的 Result

#### 效果
- `F` - 继承函数的效果

#### 示例
```aster
Given a result Err("404")
When I map error to "Not Found: 404"
Then I get Err("Not Found: 404")
```

#### 说明
- 仅在 `Err` 时应用函数
- 用于错误类型转换或增强错误信息

---

### Result.unwrapOrElse

#### 签名
```typescript
fn unwrapOrElse<T, E, F>(result: Result<T, E>, f: E -> T with F): T with F
```

#### 参数
- `result: Result<T, E>` - 源 Result
- `f: E -> T with F` - 从错误计算默认值的函数

#### 返回值
- `T` - 成功值或通过函数计算的值

#### 效果
- `F` - 继承函数的效果

#### 示例
```aster
Given a result Err("error")
When I unwrap or else return length of error message
Then I get 5
```

---

## Maybe\<T\>（可选值类型）

Maybe 类型表示可能存在或不存在的值，是类型安全的 null 替代。

### Maybe vs Null

| 特性 | Maybe\<T\> | null |
|------|----------|------|
| 类型安全 | ✅ 编译时检查 | ❌ 运行时 NullPointerException |
| 显式处理 | ✅ 必须处理 None 情况 | ❌ 容易忘记检查 null |
| 组合性 | ✅ 支持 map/flatMap | ❌ 需要手动检查 |
| 语义清晰 | ✅ 明确表示"可能没有值" | ⚠️ null 语义模糊 |

**推荐使用场景**：
- 函数可能返回空值（如查找、索引访问）
- 配置项可能不存在
- 用户输入可能为空

---

### Maybe.Some

#### 签名
```typescript
fn Some\<T\>(value: T): Maybe\<T\> with ∅
```

#### 参数
- `value: T` - 要包装的值

#### 返回值
- `Maybe\<T\>` - 包含值的 Maybe

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a value 42
When I create a Some
Then I get Some(42)
```

---

### Maybe.None

#### 签名
```typescript
fn None\<T\>(): Maybe\<T\> with ∅
```

#### 返回值
- `Maybe\<T\>` - 表示没有值的 Maybe

#### 效果
- `∅` (纯计算)

#### 示例
```aster
When I create a None
Then I get None
```

---

### Maybe.isSome

#### 签名
```typescript
fn isSome\<T\>(maybe: Maybe\<T\>): Bool with ∅
```

#### 参数
- `maybe: Maybe\<T\>` - 要检查的 Maybe

#### 返回值
- `Bool` - 如果包含值返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a maybe Some(42)
When I check if it is some
Then I get True
```

---

### Maybe.isNone

#### 签名
```typescript
fn isNone\<T\>(maybe: Maybe\<T\>): Bool with ∅
```

#### 参数
- `maybe: Maybe\<T\>` - 要检查的 Maybe

#### 返回值
- `Bool` - 如果没有值返回 `True`

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a maybe None
When I check if it is none
Then I get True
```

---

### Maybe.unwrap

#### 签名
```typescript
fn unwrap\<T\>(maybe: Maybe\<T\>): T with ∅
```

#### 参数
- `maybe: Maybe\<T\>` - 要解包的 Maybe

#### 返回值
- `T` - 包含的值

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a maybe Some(42)
When I unwrap it
Then I get 42
```

#### 警告
- 如果 Maybe 是 `None`，程序会 panic
- 仅在确定是 `Some` 时使用

---

### Maybe.unwrapOr

#### 签名
```typescript
fn unwrapOr\<T\>(maybe: Maybe\<T\>, default: T): T with ∅
```

#### 参数
- `maybe: Maybe\<T\>` - 要解包的 Maybe
- `default: T` - 没有值时的默认值

#### 返回值
- `T` - 包含的值或默认值

#### 效果
- `∅` (纯计算)

#### 示例
```aster
Given a maybe None
When I unwrap with default 0
Then I get 0
```

---

### Maybe.map

#### 签名
```typescript
fn map\<T, U, E\>(maybe: Maybe\<T\>, f: T -> U with E): Maybe\<U\> with E
```

#### 参数
- `maybe: Maybe\<T\>` - 源 Maybe
- `f: T -> U with E` - 映射函数

#### 返回值
- `Maybe\<U\>` - 映射后的 Maybe

#### 效果
- `E` - 继承映射函数的效果

#### 示例
```aster
Given a maybe Some(5)
When I map it with double function
Then I get Some(10)

Given a maybe None
When I map it with double function
Then I get None
```

---

### Maybe.flatMap

#### 签名
```typescript
fn flatMap\<T, U, E\>(maybe: Maybe\<T\>, f: T -> Maybe\<U\> with E): Maybe\<U\> with E
```

#### 参数
- `maybe: Maybe\<T\>` - 源 Maybe
- `f: T -> Maybe\<U\> with E` - 返回 Maybe 的函数

#### 返回值
- `Maybe\<U\>` - 链式调用后的 Maybe

#### 效果
- `E` - 继承函数的效果

#### 示例
```aster
Given a maybe Some("42")
When I flatMap with parseInt function
Then I get Some(42)

Given a maybe Some("abc")
When I flatMap with parseInt function
Then I get None
```

---

## 数值类型（Int, Long, Double）

### Int（32位整数）

#### 范围
- 最小值：-2,147,483,648 (-2³¹)
- 最大值：2,147,483,647 (2³¹ - 1)

#### Int.add

##### 签名
```typescript
fn add(a: Int, b: Int): Int with ∅
```

##### 参数
- `a: Int` - 第一个整数
- `b: Int` - 第二个整数

##### 返回值
- `Int` - 和

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 5
And a number 3
When I add them
Then I get 8
```

##### 溢出行为
- 溢出时环绕（wrap around）
- 例：`2147483647 + 1 = -2147483648`

---

#### Int.subtract

##### 签名
```typescript
fn subtract(a: Int, b: Int): Int with ∅
```

##### 参数
- `a: Int` - 被减数
- `b: Int` - 减数

##### 返回值
- `Int` - 差

##### 效果
- `∅` (纯计算)

---

#### Int.multiply

##### 签名
```typescript
fn multiply(a: Int, b: Int): Int with ∅
```

##### 参数
- `a: Int` - 第一个整数
- `b: Int` - 第二个整数

##### 返回值
- `Int` - 积

##### 效果
- `∅` (纯计算)

---

#### Int.divide

##### 签名
```typescript
fn divide(a: Int, b: Int): Result<Int, Text> with ∅
```

##### 参数
- `a: Int` - 被除数
- `b: Int` - 除数

##### 返回值
- `Result<Int, Text>` - 商或除零错误

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 10
And a number 2
When I divide them
Then I get Ok(5)

Given a number 10
And a number 0
When I divide them
Then I get Err("Division by zero")
```

##### 说明
- 整数除法向零截断
- 除零返回 `Err`，而非 panic

---

#### Int.modulo

##### 签名
```typescript
fn modulo(a: Int, b: Int): Result<Int, Text> with ∅
```

##### 参数
- `a: Int` - 被除数
- `b: Int` - 除数

##### 返回值
- `Result<Int, Text>` - 余数或除零错误

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 10
And a number 3
When I compute modulo
Then I get Ok(1)
```

---

#### Int.abs

##### 签名
```typescript
fn abs(n: Int): Int with ∅
```

##### 参数
- `n: Int` - 整数

##### 返回值
- `Int` - 绝对值

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number -5
When I get absolute value
Then I get 5
```

##### 边界情况
- `abs(-2147483648)` 溢出，返回 `-2147483648`

---

#### Int.toDouble

##### 签名
```typescript
fn toDouble(n: Int): Double with ∅
```

##### 参数
- `n: Int` - 整数

##### 返回值
- `Double` - 转换后的浮点数

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 42
When I convert to double
Then I get 42.0
```

---

### Long（64位整数）

#### 范围
- 最小值：-9,223,372,036,854,775,808 (-2⁶³)
- 最大值：9,223,372,036,854,775,807 (2⁶³ - 1)

#### API
Long 类型提供与 Int 相同的算术操作：
- `add(a: Long, b: Long): Long`
- `subtract(a: Long, b: Long): Long`
- `multiply(a: Long, b: Long): Long`
- `divide(a: Long, b: Long): Result<Long, Text>`
- `modulo(a: Long, b: Long): Result<Long, Text>`
- `abs(n: Long): Long`
- `toDouble(n: Long): Double`
- `toInt(n: Long): Result<Int, Text>` - 可能溢出

---

### Double（64位浮点数）

#### 精度
- 符合 IEEE 754 双精度浮点标准
- 约 15-17 位有效数字

#### Double.add

##### 签名
```typescript
fn add(a: Double, b: Double): Double with ∅
```

##### 参数
- `a: Double` - 第一个数
- `b: Double` - 第二个数

##### 返回值
- `Double` - 和

##### 效果
- `∅` (纯计算)

---

#### Double.divide

##### 签名
```typescript
fn divide(a: Double, b: Double): Result<Double, Text> with ∅
```

##### 参数
- `a: Double` - 被除数
- `b: Double` - 除数

##### 返回值
- `Result<Double, Text>` - 商或除零错误

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 10.0
And a number 0.0
When I divide them
Then I get Err("Division by zero")
```

##### 特殊值
- `Infinity` - 正无穷
- `-Infinity` - 负无穷
- `NaN` - 非数字（Not a Number）

---

#### Double.sqrt

##### 签名
```typescript
fn sqrt(n: Double): Result<Double, Text> with ∅
```

##### 参数
- `n: Double` - 数值

##### 返回值
- `Result<Double, Text>` - 平方根或错误（负数）

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 16.0
When I compute square root
Then I get Ok(4.0)

Given a number -1.0
When I compute square root
Then I get Err("Square root of negative number")
```

---

#### Double.pow

##### 签名
```typescript
fn pow(base: Double, exponent: Double): Double with ∅
```

##### 参数
- `base: Double` - 底数
- `exponent: Double` - 指数

##### 返回值
- `Double` - base 的 exponent 次方

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a base 2.0
And an exponent 3.0
When I compute power
Then I get 8.0
```

---

#### Double.round

##### 签名
```typescript
fn round(n: Double): Long with ∅
```

##### 参数
- `n: Double` - 浮点数

##### 返回值
- `Long` - 四舍五入后的整数

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 3.7
When I round it
Then I get 4

Given a number 3.2
When I round it
Then I get 3
```

---

#### Double.floor

##### 签名
```typescript
fn floor(n: Double): Long with ∅
```

##### 参数
- `n: Double` - 浮点数

##### 返回值
- `Long` - 向下取整的整数

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 3.9
When I floor it
Then I get 3
```

---

#### Double.ceil

##### 签名
```typescript
fn ceil(n: Double): Long with ∅
```

##### 参数
- `n: Double` - 浮点数

##### 返回值
- `Long` - 向上取整的整数

##### 效果
- `∅` (纯计算)

##### 示例
```aster
Given a number 3.1
When I ceil it
Then I get 4
```

---

## 使用说明

### 效果注解

所有 API 都标注了效果类型：
- `∅` - 纯函数，无副作用
- `CPU` - 计算密集型操作
- `IO` - 涉及 I/O 操作
- `E` - 效果多态，继承参数函数的效果

### 不可变性

Aster 标准库遵循函数式编程原则：
- 所有集合操作返回新集合，不修改原集合
- Text 类型是不可变的
- 使用 `with ∅` 确保函数的纯净性

### 泛型支持

List 和 Map 支持泛型参数：
- `List\<T\>` - T 是元素类型
- `Map<K, V>` - K 是键类型，V 是值类型

### 错误处理

使用 `Maybe\<T\>` 处理可能失败的操作：
- `Some(value)` - 包含值
- `None` - 表示不存在或失败

---

## 下一步

- 参考 [类型系统](./types.md) 了解类型定义
- 参考 [效果系统](./effects.md) 了解效果检查
- 参考 [Result 和 Maybe 类型](#) 了解错误处理（待补充）
