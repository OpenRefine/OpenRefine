// !/usr/bin/env bash
// set -e

// GROUP1=cypress/integration/open-project/*.spec.js
// GROUP2=cypress/integration/language/*.spec.js
// BROWSERS=
// echo

const glob = require('glob');

const groups = [
	{
		specs: ['integration/open-project/*.spec.js', 'b'],
	},
];

const merged_groups = groups.map((group) => group.specs.join(','));

// step1 ,find files matched by existing groups
const matchedFiles = [];
groups.forEach((group) => {
	group.specs.forEach((pattern) => {
		const files = glob.sync(`main/tests/cypress/cypress/${pattern}`);
		matchedFiles.push(...files);
	});
});

// step2 , add a last group that contains missed files
const allSpecFiles = glob.sync(
	`main/tests/cypress/cypress/integration/**/*.spec.js`
);
const missedFiles = [];
// console.log(allSpecFiles);
for (file of allSpecFiles) {
	if (!matchedFiles.includes(file)) {
		missedFiles.push(file);
	}
}

if (missedFiles.length) {
	merged_groups.push(missedFiles.join(','));
}
// console.log(matchedFiles);
// console.log('---');
// console.log(missedFiles);
// matchedFiles.push(...missedFiles);

// glob(
// 	`main/tests/cypress/cypress/integration/open-project/*.spec.js`,
// 	{},
// 	(err, files) => {
// 		console.log(...files);
// 		// matchedFiles.push(...files);
// 	}
// );
// console.log(matchedFiles);
const browsers = ['chrome', 'edge'];

console.log(
	`::set-output name=matrix::{"browser":${JSON.stringify(
		browsers
	)}, "machines":${JSON.stringify(merged_groups)}}`
);
