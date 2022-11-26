const glob = require('glob');

// Those specs paths are glob patterns
const groups = [
  {
    specs: [
      'cypress/e2e/create-project/**/*.cy.js',
      'cypress/e2e/extensions/**/*.cy.js',
      'cypress/e2e/import-project/**/*.cy.js',
      'cypress/e2e/language/**/*.cy.js',
      'cypress/e2e/open-project/**/*.cy.js',
      'cypress/e2e/preferences/**/*.cy.js',
      'cypress/e2e/project_management/**/*.cy.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/all-column/**/*.cy.js',
      'cypress/e2e/project/grid/column/*.cy.js',
      'cypress/e2e/project/grid/column/edit-cells/**/*.cy.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/column/edit-column/**/*.cy.js',
      'cypress/e2e/project/grid/column/facet/**/*.cy.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/column/reconcile/**/*.cy.js',
      'cypress/e2e/project/grid/column/transpose/**/*.cy.js',
      'cypress/e2e/project/grid/column/view/**/*.cy.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/grid/misc/**/*.cy.js',
      'cypress/e2e/project/grid/row/**/*.cy.js',
      'cypress/e2e/project/grid/viewpanel-header/**/*.cy.js',
    ],
  },
  {
    specs: [
      'cypress/e2e/project/project-header/**/*.cy.js',
      'cypress/e2e/project/undo_redo/**/*.cy.js',
      'cypress/e2e/tutorial/*.cy.js',
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
  `./main/tests/cypress/cypress/e2e/**/*.cy.js`
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
