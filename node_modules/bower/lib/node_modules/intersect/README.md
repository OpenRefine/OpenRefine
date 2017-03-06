
# intersect

[![build status](https://secure.travis-ci.org/juliangruber/intersect.svg)](http://travis-ci.org/juliangruber/intersect)
[![downloads](https://img.shields.io/npm/dm/intersect.svg)](https://www.npmjs.org/package/intersect)

[![browser support](https://ci.testling.com/juliangruber/intersect.png)](https://ci.testling.com/juliangruber/intersect)

## Usage

```js
var intersect = require('intersect');

var a = ['foo', 'bar', 'baz'];
var b = ['nope', 'bar', 'baz'];

console.log(intersect(a, b));
// => ['bar', 'baz']
```

## Installation

With [npm](http://npmjs.org) do

```bash
$ npm install intersect
```

With [component](https://github.com/component/component) do

```bash
$ component install juliangruber/intersect
```

## API

### intersect(a, b)

Find the intersection of arrays `a` and `b`. The individual arrays are expected to be sets, i.e. there's no duplicate elements in them.

### intersect(arrays)

You can also pass an array of arrays as a single argument, to get the intersection of `n` arrays.

### intersect.big(a, b)

This module contains an alternative intersection algorithm by @minuteman3, better suited for intersecting big arrays.

The implementations *might* compare like this, depending on your host environment:

![graph of intersect](http://minuteman3.github.io/minuteman3/graph.svg)

## License

(MIT)

Copyright (c) 2013 Julian Gruber &lt;julian@juliangruber.com&gt;

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
