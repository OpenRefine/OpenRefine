# requireg  [![Build Status](https://secure.travis-ci.org/h2non/requireg.png?branch=master)][2] [![NPM version](https://badge.fury.io/js/requireg.png)][3]

Require and resolve global modules in node/io.js like a boss

## Differences with require()

`requireg` tries to find modules in global locations which are
not natively supported by the node.js [module resolve algorithm][1]. 

Supported locations:

- $HOME/node_modules (instead of $HOME/.node_modules)
- $HOME/node_libraries (instead of $HOME/.node_libraries)
- $HOME/node_packages (specific of `requireg`)
- $PREFIX/lib/node_modules (instead of $PREFIX/lib/node)
- $NODE_MODULES (use the specific modules path environment variable)

## Resolution priority

1. Resolve via native `require()`
2. User home directory (`$HOME` or `%USERPROFILE%`)
3. Node installation path
4. $NODE_MODULES (can have different multiple paths, semicolon separated)
5. Common operative system installation paths

## Installation

```
$ npm install requireg --save[-dev]
```

## Usage

### Load global modules

```js
var requireg = require('requireg')
// require a globally installed package
var npm = requireg('npm')
```

### Resolve module path

```js
var modulePath = requireg.resolve('npm')
// returns '/usr/local/lib/node_modules/npm/lib/npm.js'
```

### Globalize it

```js
require('requireg').globalize()
```

Now it is globally available from any source file

```js
var globalModule = requireg('npm')
```

### Module not found

`requireg` maintains the same behavior as the native `require()`. 
It will throw an `Error` exception if the module was not found

## Considerations

- Require global modules in node.js are considered anti-pattern. 
Note that you can experiment unreliability or inconsistency across different environments.
I hope you know exactly what you do with `requireg`
- Only node packages installed with [NPM](https://npmjs.org) are supported (which means only standardized NPM paths are supported)

## Wish list

- Custom environment variable with custom path to resolve global modules
- Possible configuration object (force to use only global modules...)

## License

Released under MIT license

[1]: http://nodejs.org/docs/latest/api/modules.html#modules_all_together
[2]: http://travis-ci.org/h2non/requireg
[3]: http://badge.fury.io/js/requireg

