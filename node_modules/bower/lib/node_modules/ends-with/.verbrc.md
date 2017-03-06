# {%= name %} {%= badge("fury") %}

> {%= description %}

## Install
{%= include("install") %}

## Run benchmarks

This uses the fasted version of 9 different [implementations](benchmark/implementations) that were benchmarked.

```bash
node benchmark
```

## Run tests

```bash
npm test
```

## Usage

```js
var endsWith = require('{%= name %}');

endsWith('abc', 'c');
//=> true

endsWith(['a', 'b', 'c'], 'c');
//=> true

endsWith(['abc', 'abc', 'abc'], 'c');
//=> false

endsWith(['abc', 'abc', 'abc'], 'c');
//=> false

endsWith(['a', 'b', 42], 42)
//=> true

endsWith(['a', 'b', 42], '42')
//=> false
```

## Author
{%= include("author") %}

## License
{%= copyright() %}
{%= license() %}

***

{%= include("footer") %}