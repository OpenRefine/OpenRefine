 // librarycarpentry.org/lc-open-refine/03-working-with-data/index.html

describe(__filename, function () {
  it('Working with rows and records in openrefine', function () {
    /** ************
    Splitting Cells
    **************/

    // For sake of the tutorial we will assume we have already covered the previous chapter for importing data
    //   cy.loadAndVisitProject('doaj-article-sample.csv');
    const projectName = Date.now();
    cy.loadProject('doaj-article-sample.csv', projectName).then((projectId) => {
      cy.visitProject(projectId);
      cy.get('#project-name-button').contains(projectName);
    });

    // Click the dropdown menu at the top of the Author column and Choose Edit cells->Split multi-valued cells
    cy.columnActionClick('Authors', [
      'Edit cells',
      'Split multi-valued cells…',
    ]);

    //   In the prompt type the ( | ) symbol and click OK
    cy.get('.dialog-container label').contains('by separator').click();
    cy.get('.dialog-container input[bind="separatorInput"]').type('|');
    cy.confirmDialogPanel();

    cy.get('#summary-bar').should('to.contain', '4009 rows');

    // Click the Records option to change to Records mode,
    // Note how the numbering has changed - indicating that several rows are related to the same record
    cy.get('span[bind="modeSelectors"]').contains('records').click();

    cy.get('#summary-bar').should('to.contain', '1001 records');

    /** ************
    Joining Cells
    **************/

    //     Click the dropdown menu at the top of the Author column, Choose Edit cells->Join multi-valued cells

    // Hack to imitate the window prompt which will be shown used in the later code
    // In the prompt type the ( | ) symbol
    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns(',');
    });

    cy.columnActionClick('Authors', [
      'Edit cells',
      'Join multi-valued cells…',
    ]);

    cy.get('#summary-bar').should('to.contain', '1001 records');

    //       Click both the Rows and Records options and observe how the numbers of Rows and Records are equal

    cy.get('span[bind="modeSelectors"]').contains('rows').click();

    cy.get('#summary-bar').should('to.contain', '1001 rows');
  });
});
