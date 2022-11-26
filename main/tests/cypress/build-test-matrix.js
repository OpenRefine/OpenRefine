const glob = require('glob');

// Those specs paths are glob patterns
const groups = [
  {
    specs: [
      'cypress/e2e/create-project/**/*.spec.js',
      'cypress/e2e/extensions/**/*.spec.js',
      'cypress/e2e/import-project/**/*.spec.js',
      'cypress/e2e/language/**/*.spec.js',
      'cypress/e2e/open-project/**/*.spec.js',
      'cypress/e2e/preferences/**/*.spec.js',
      'cypress/e2e/project_management/**/*.spec.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/all-column/**/*.spec.js',
      'cypress/e2e/project/grid/column/*.spec.js',
      'cypress/e2e/project/grid/column/edit-cells/**/*.spec.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/column/edit-column/**/*.spec.js',
      'cypress/e2e/project/grid/column/facet/**/*.spec.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/column/reconcile/**/*.spec.js',
      'cypress/e2e/project/grid/column/transpose/**/*.spec.js',
      'cypress/e2e/project/grid/column/view/**/*.spec.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/misc/**/*.spec.js',
      'cypress/e2e/project/grid/row/**/*.spec.js',
      'cypress/e2e/project/grid/viewpanel-header/**/*.spec.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/project-header/**/*.spec.js',
      'cypress/e2e/project/undo_redo/**/*.spec.js',
      'cypress/e2e/tutorial/*.spec.js',
    ],
  },
];

const mergedGroups = groups.map((group) => group.specs.join(','));

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
  `./main/tests/cypress/cypress/e2e/**/*.spec.js`
);
const missedFiles = [];

for (const file of allSpecFiles) {
  const relativeFile = file.substring('./main/tests/cypress/'.length);
  if (!matchedFiles.includes(file.substring(2))) {
    missedFiles.push(relativeFile);
  }
}

if (missedFiles.length) {
  mergedGroups.push(missedFiles.join(','));
}

const browsers = process.env.browsers.split(',');
console.log(
  `::set-output name=matrix::{"browser":${JSON.stringify(
    browsers
  )}, "specs":${JSON.stringify(mergedGroups)}}`
);
