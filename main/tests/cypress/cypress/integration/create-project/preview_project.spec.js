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
  it('Tests Parsing Options related to column seperation', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    cy.get('[type="radio"]').check('tab');
    cy.waitForImportUpdate();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', '01001","BUTTER,WITH SALT","15.87","717');

    cy.get('input[bind="columnSeparatorInput"]').type('{backspace};');
    cy.get('[type="radio"]').check('custom');
    cy.waitForImportUpdate();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', '01001","BUTTER,WITH SALT","15.87","717');

    cy.get('[type="radio"]').check('comma');

    cy.waitForImportUpdate();

    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01001');
    cy.get('table.data-table tr').eq(1).should('to.contain', '15.87');
    cy.get('table.data-table tr').eq(1).should('to.contain', '717');
    cy.get('table.data-table tr')
      .eq(1)
      .should('to.contain', 'BUTTER,WITH SALT');

    cy.get('input[bind="columnNamesCheckbox"]').check();

    cy.waitForImportUpdate();
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
    cy.get('tbody').should('to.contain', 'Select preferred language');

    cy.navigateTo('Import project');
    cy.get('tbody').should(
      'to.contain',
      'Locate an existing Refine project file (.tar or .tar.gz)'
    );

    cy.navigateTo('Create project');
    cy.get('.create-project-ui-panel').should(
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
    cy.get('button[bind="startOverButton"]').click();

    cy.get('#or-create-question').should(
      'to.contain',
      'Create a project by importing data. What kinds of data files can I import?'
    );
  });

  it('Test project renaming', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure parsing options');
    cy.get(
      '.default-importing-wizard-header input[bind="projectNameInput"]'
    ).type('this is a test');

    // click next to create the project, and wait until it's loaded
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.get('#create-project-progress-message').contains('Done.');

    cy.get('#project-name-button').contains('this is a test');
  });

  it('Test project tagging by adding various tags', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure parsing options');
    const uniqueProjectName = Date.now();
    const uniqueTagName1 = 'tag1_' + Date.now();
    const uniqueTagName2 = 'tag2_' + Date.now();

    cy.get('#project-tags-container').click();
    // Type and Validate the tag, pressing enter
    cy.get('#project-tags-container .select2-input').type(uniqueTagName1, { force: true });
    cy.get('body').type('{enter}');
    cy.get('#project-tags-container .select2-input').type(uniqueTagName2, { force: true });
    cy.get('body').type('{enter}');
    cy.get('#or-import-parsopt').click();

    // click next to create the project, and wait until it's loaded
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.get('#create-project-progress-message').contains('Done.');

    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.get('#projects-list')
      .contains(uniqueProjectName)
      .parent()
      .parent()
      .contains(uniqueTagName1);
    cy.get('#projects-list')
      .contains(uniqueProjectName)
      .parent()
      .parent()
      .contains(uniqueTagName2);
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
    cy.waitForImportUpdate();

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
    cy.get('input[bind="headerLinesCheckbox"]').uncheck();
    cy.waitForImportUpdate();
    cy.get('input[bind="headerLinesInput"]').type('{backspace}0');
    cy.get('input[bind="headerLinesCheckbox"]').check();
    cy.waitForImportUpdate();

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
    cy.waitForImportUpdate();

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
    cy.waitForImportUpdate();

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
    cy.waitForImportUpdate();

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
    cy.waitForImportUpdate();
    // Look for automatic preview update
    cy.get('table.data-table tr').eq(1);
    cy.get('table.data-table tr').eq(1).should('to.contain', '01002');

    cy.get('input[bind="ignoreCheckbox"]').uncheck();
    cy.waitForImportUpdate();

    // **Testing ignore feature with auto preview disabled** //
    cy.get('input[bind="disableAutoPreviewCheckbox"]').check();
    // Verify no auto update
    cy.get('input[bind="ignoreCheckbox"]').check();
    cy.wait(5000); // 5 second wait. No choice but to use this here because the dom is not rendered.
    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01001');
    // Verify update on button click
    cy.get('button[bind="previewButton"]').click();
    cy.waitForImportUpdate();
    cy.get('table.data-table tr').eq(1).should('to.contain', '1.');
    cy.get('table.data-table tr').eq(1).should('to.contain', '01002');
    cy.get('input[bind="disableAutoPreviewCheckbox"]').uncheck();
  });
});
