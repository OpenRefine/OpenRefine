#!/usr/bin/env node
/*eslint no-console:0*/

'use strict';

var path      = require('path');
var fs        = require('fs');
var util      = require('util');
var Benchmark = require('benchmark');
var ansi      = require('ansi');
var cursor    = ansi(process.stdout);

var variant   = path.join(__dirname, 'implementations');
var filename  = {};
var files     = [];

fs.readdirSync(variant).sort().forEach(function (name) {
  var file = path.join(variant, name);
  var code = require(file);

  filename[name] = file;
  files.push({
    name: name,
    code: code
  });
});


var samples = path.join(__dirname, 'samples');
var SAMPLES = [];

fs.readdirSync(samples).sort().forEach(function (sample) {
  var filepath = path.join(samples, sample),
      extname  = path.extname(filepath),
      basename = path.basename(filepath, extname);

  var content    = {};
  content.string = fs.readFileSync(filepath, 'utf8');
  var title      = util.format('(%d bytes)', content.string.length);

  function onComplete() {
    cursor.write('\n');
  }

  var suite = new Benchmark.Suite(title, {
    onStart: function onStart() {
      console.log('\nSample: %s %s', sample, title);
    },
    onComplete: onComplete
  });

  files.forEach(function (impl) {
    suite
      .add(impl.name, {
        onCycle: function onCycle(event) {
          cursor.horizontalAbsolute();
          cursor.eraseLine();
          cursor.write(' > ' + event.target);
        },
        onComplete: onComplete,

        fn: function () {
          impl.code(content.string);
          return;
        }
      });
  });

  SAMPLES.push({
    name: basename,
    title: title,
    content: content,
    suite: suite
  });
});

function select(patterns) {
  var result = [];

  if (!Array.isArray(patterns)) {
    patterns = [patterns];
  }

  function verify(name) {
    return patterns.length === 0 || patterns.some(function (regexp) {
      return regexp.test(name);
    });
  }

  SAMPLES.forEach(function (sample) {
    if (verify(sample.name)) {
      result.push(sample);
    }
  });

  return result;
}


function run(patterns) {
  var selected = select(patterns);
  var len = selected.length;

  if (len > 0) {
    console.log('Selected fixture' + (len > 1 ? 's' : '') + ': (%d of %d)', len, SAMPLES.length);
    selected.forEach(function (sample) {
      console.log(' > %s', sample.name);
    });
  } else {
    console.log('There aren\'t any samples matching the given patterns: %s', util.inspect(patterns));
  }

  selected.forEach(function (sample) {
    // console.log('sample:', sample)
    sample.suite.run();
  });
}

module.exports.variant  = variant;
module.exports.filename = filename;
module.exports.files    = files;
module.exports.samples  = samples;
module.exports.SAMPLES  = SAMPLES;
module.exports.select   = select;
module.exports.run      = run;

/**
 * Provide regex patterns for the samples to run against.
 *
 * @param  {[type]} patterns
 * @return {[type]}
 */

run(process.argv.slice(2).map(function (patterns) {
  return new RegExp(patterns, 'i');
}));
