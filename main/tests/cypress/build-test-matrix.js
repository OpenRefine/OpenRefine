// !/usr/bin/env bash
// set -e

// GROUP1=cypress/integration/open-project/*.spec.js
// GROUP2=cypress/integration/language/*.spec.js
// BROWSERS=
// echo

const glob = require('glob');

// Those specs paths are glob patterns
const groups = [
	{
		specs: [
			'cypress/integration/create-project/*.spec.js',
			'cypress/integration/extensions/*.spec.js',
			'cypress/integration/import-project/*.spec.js',
			'cypress/integration/language/*.spec.js',
			'cypress/integration/open-project/*.spec.js',
			'cypress/integration/preferences/*.spec.js',
			'cypress/integration/project-management/*.spec.js',
		],
	},
	,
	{
		specs: [
			'project/grid/all-column/*.spec.js',
			'project/grid/column/edit-cells/*.spec.js',
		],
	},
];

const merged_groups = groups.map((group) => group.specs.join(','));

// step1 ,find files matched by existing groups
const matchedFiles = [];
groups.forEach((group) => {
	group.specs.forEach((pattern) => {
		const files = glob.sync(`main/tests/cypress/${pattern}`);
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

// if (missedFiles.length) {
// 	merged_groups.push(missedFiles.join(','));
// }

const browsers = process.env.browsers.split(',');

console.log(
	`::set-output name=matrix::{"browser":${JSON.stringify(
		browsers
	)}, "specs":${JSON.stringify(merged_groups)}}`
);
