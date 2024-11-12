# OpenRefine WebApp

See the main [README.md](../../README.md) for how to build and run OpenRefine.

## Dependencies

Dependencies available in the npm registry are added to package.json.

A few of the dependencies are not available in npm.

* imgAreaSelect: cannot find 0.9.2 version in npm, found 0.9.11-rc.1 on GitHub [@odyniec/imgareaselect](https://github.com/odyniec/imgareaselect).
* suggest (4.3a): not in npm, contains fixes from Google Code SVN.

These dependencies are located under `webapp/modules/core/externals`.

## postinstall

When `npm install` runs, a `postinstall` script  copies the necessary files from `node_modules` into `webapp\modules\core\3rdparty`.
