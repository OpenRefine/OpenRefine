# ext-name [![Build Status](http://img.shields.io/travis/kevva/ext-name.svg?style=flat)](https://travis-ci.org/kevva/ext-name)

> Get the file extension and MIME type from a file


## Install

```
$ npm install --save ext-name
```


## Usage

```js
var extName = require('ext-name');

console.log(extName('foobar.tar'));
//=> {'ext': 'tar', 'mime': 'application/x-tar'}
```


## CLI

```
$ npm install --global ext-name
```

```
  $ ext-name --help

  Usage
    $ ext-name <file>

  Example
    $ ext-name file.tar.gz
```


## License

MIT © [Kevin Mårtensson](https://github.com/kevva)
