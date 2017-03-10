#!/usr/bin/env node
'use strict';

var extName = require('./');
var meow = require('meow');

var cli = meow({
	help: [
		'Usage',
		'  $ ext-name <file>',
		'',
		'Example',
		'  $ ext-name file.tar.gz'
	].join('\n')
});

if (!cli.input.length) {
	console.error([
		'Specify a file',
		'',
		'Example',
		'  $ ext-name file.tar.gz'
	].join('\n'));

	process.exit(1);
}

console.log(extName(cli.input[0]));
