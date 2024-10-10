/**
 * Navigate to project preview page and check its contents
 */
function navigateToProjectPreview() {
  cy.visitOpenRefine();
  cy.createProjectThroughUserInterface('food.mini.csv');
  cy.get('.create-project-ui-panel').contains('Configure parsing options');
  cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
  cy.get('table.data-table tr').eq(1).should('to.contain', '01001');
  cy.get('table.data-table tr').eq(1).should('to.contain', 'BUTTER,WITH SALT');
  cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
  cy.get('table.data-table tr').eq(1).should('to.contain', '717');
}
describe(__filename, function () {
  it('Tests Parsing Options related to column separation', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // Since the quotes in our input file aren't escaped, they aren't legal for any separator except comma(,)
    cy.get('input[bind="processQuoteMarksCheckbox"]').uncheck();
    cy.get('[type="radio"]').check('tab');

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', '"01001","BUTTER,WITH SALT","15.87","717"');

    cy.get('input[bind="columnSeparatorInput"]').type('{backspace};');
    cy.get('[type="radio"]').check('custom');

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', '"01001","BUTTER,WITH SALT","15.87","717"');

    // Re-enable quotes for CSV case since they're now in a legal configuration
    cy.get('input[bind="processQuoteMarksCheckbox"]').check();
    cy.get('[type="radio"]').check('comma');

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01001');
    cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
    cy.get('table.data-table tr').eq(1).should('to.contain', '717');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', 'BUTTER,WITH SALT');

    cy.get('input[bind="columnNamesCheckbox"]').check();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'NDB_No');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'Shrt_Desc');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'Water');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'Energ_Kcal');
  });
  it('Ensures navigation works from project-preview page', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    cy.navigateTo('Language settings');
    cy.get('#project-upload-form > table > tbody > tr:nth-child(1) > td > label').should(
        'to.contain', 'Select preferred language');

    cy.navigateTo('Import project');
    cy.get('#or-import-locate').should(
      'to.contain',
      'Locate an existing Refine project file or use a URL (.tar or .tar.gz):'
    );

    cy.navigateTo('Create project');
    cy.get('#or-import-parsopt').should(
      'to.contain',
      'Configure parsing options'
    );
  });

  it('Ensures the working of Start-Over Button', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').should(
      'to.contain',
      'Configure parsing options'
    );

    // Make sure all our column headers are rendered before starting over so we don't get errors from a deleted project
    cy.get('table.data-table tr').eq(0).should('to.contain', 'Energ_Kcal');

    cy.get('button[bind="startOverButton"]').click();

    cy.get('#or-create-question').should(
      'to.contain',
      'Create a project by importing data. What kinds of data files can I import?'
    );
  });

  it('Tests ignore-first of parsing options', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01001');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', 'BUTTER,WITH SALT');
    cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
    cy.get('table.data-table tr').eq(1).should('to.contain', '717');

    cy.get('input[bind="ignoreInput"]').type('{backspace}1');
    cy.get('input[bind="ignoreCheckbox"]').check();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01002');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', 'BUTTER,WHIPPED,WITH SALT');
    cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
    cy.get('table.data-table tr').eq(1).should('to.contain', '717');
  });
  it('Tests parse-next of parsing options', function () {
    navigateToProjectPreview();
    cy.get('input[bind="columnNamesCheckbox"]').check();
    cy.get('input[bind="headerLinesInput"]').type('{backspace}0');
    cy.get('input[bind="headerLinesCheckbox"]').check();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'NDB_No');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'Shrt_Desc');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'Water');
    cy.get('table.data-table tr').eq(1).should('to.contain', 'Energ_Kcal');
  });
  it('Tests discard-initial of parsing options', function () {
    navigateToProjectPreview();
    cy.get('input[bind="skipInput"]').type('{backspace}1');
    cy.get('input[bind="skipCheckbox"]').check();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01002');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', 'BUTTER,WHIPPED,WITH SALT');
    cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
    cy.get('table.data-table tr').eq(1).should('to.contain', '717');
  });
  it('Tests load-at-most of parsing options', function () {
    navigateToProjectPreview();
    cy.get('input[bind="limitInput"]').type('{backspace}1');
    cy.get('input[bind="limitCheckbox"]').check();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01001');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', 'BUTTER,WITH SALT');
    cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
    cy.get('table.data-table tr').eq(1).should('to.contain', '717');
  });
  it('Tests attempt to parse into numbers of parsing options', function () {
    navigateToProjectPreview();
    cy.get('input[bind="guessCellValueTypesCheckbox"]').check();

    cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
    cy.get('table.data-table tr').eq(1).should('to.contain', '717');
  });

  /*
  The test case below uses the ignore feature to test the disable automatic preview update checkbox
  We first test with automatic preview updates enabled
  Then, we test with automatic preview updates disabled, which requires the update button to change the preview
  */
  it('Tests disabling of automatic preview', function () {
    navigateToProjectPreview();
    // **Testing ignore feature with auto preview enabled** //
    cy.get('input[bind="ignoreInput"]').type('{backspace}1');
    cy.get('input[bind="ignoreCheckbox"]').check();

    // Look for automatic preview update
    cy.get('table.data-table tr').eq(1);
    cy.get('table.data-table tr').eq(1).should('to.contain', '01002');

    cy.get('input[bind="ignoreCheckbox"]').uncheck();
    cy.get('table.data-table tr').eq(1);
    cy.get('table.data-table tr').eq(1).should('to.contain', '01001');

    // **Testing ignore feature with auto preview disabled** //
    cy.get('input[bind="disableAutoPreviewCheckbox"]').check();
    // Verify no auto update
    cy.get('input[bind="ignoreCheckbox"]').check();
    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01001');
    // Verify update on button click
    cy.get('button[bind="previewButton"]').click();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01002');
    cy.get('input[bind="disableAutoPreviewCheckbox"]').uncheck();
  });

  it('Tests save blank columns of parsing options', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food-blank-column.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure parsing options');
    // FIXME: data-table in preview has no thead, so first tr in tbody is a header
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(1)').should('to.contain', '1.');
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(2)').should('to.contain', '01001');
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(3)').should('to.contain', 'BUTTER,WITH SALT');
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(4)').should('to.contain', '15.87');
    // empty cells are filled with NBSP when rendered
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(5)').should('to.contain', '\u00a0');
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(6)').should('to.contain', '717');

    cy.get('input[bind="storeBlankColumnsCheckbox"]').uncheck();

    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(1)').should('to.contain', '1.');
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(2)').should('to.contain', '01001');
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(3)').should('to.contain', 'BUTTER,WITH SALT');
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(4)').should('to.contain', '15.87');
    // The next column should be one further to the left with the empty column gone
    cy.get('table.data-table > tbody > tr:nth-child(2) > td:nth-child(6)').should('to.contain', '717');
  });
});
