describe(__filename, function () {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  
  it('Test the create project from this computer based on CSV', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > div')
      .contains('This Computer')
      .click();
    cy.get('.create-project-ui-source-selection-tab-body.selected').contains(
      'Locate one or more files on your computer to upload'
    );
    // add file
    const csvFile = {
      filePath: 'food.mini.csv',
      mimeType: 'application/csv',
    };
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected input[type="file"]'
    ).attachFile(csvFile);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();
    cy.get('.default-importing-wizard-header input[bind="projectNameInput"]', {
      timeout: 6000,
    }).should('have.value', 'food mini csv');

    // then ensure we are on the preview page
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
  });
  it('Test the create project from this computer based on TSV', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > div')
      .contains('This Computer')
      .click();
    cy.get('.create-project-ui-source-selection-tab-body.selected').contains(
      'Locate one or more files on your computer to upload'
    );
    // add file
    const tsvFile = {
      filePath: 'shop.mini.tsv',
      mimeType: 'text/tab-separated-values',
    };
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected input[type="file"]'
    ).attachFile(tsvFile);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();
    cy.get('.default-importing-wizard-header input[bind="projectNameInput"]', {
      timeout: 6000,
    }).should('have.value', 'shop mini tsv');

    // then ensure we are on the preview page
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
  });
  it('Test the create project from clipboard based on CSV', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > div')
      .contains('Clipboard')
      .click();
    cy.get('#or-import-clipboard').should(
      'to.contain',
      'Paste data from clipboard here:'
    );
    // add file
    const csvFile = `Username; Identifier;First name;Last name
    booker12;9012;Rachel;Booker
    grey07;2070;Laura;Grey
    johnson81;4081;Craig;Johnson
    jenkins46;9346;Mary;Jenkins
    smith79;5079;Jamie;Smith`;
    cy.get('textarea').invoke('val', csvFile);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();
    cy.get('.default-importing-wizard-header input[bind="projectNameInput"]', {
      timeout: 6000,
    }).should('have.value', 'Clipboard');

    // then ensure we are on the preview page
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
  });
  it('Test the create project from clipboard based on TSV', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > div')
      .contains('Clipboard')
      .click();
    cy.get('#or-import-clipboard').should(
      'to.contain',
      'Paste data from clipboard here:'
    );
    // add file
    const tsvFile = `Some parameter Other parameter Last parameter
    CONST 123456  12.45`;
    cy.get('textarea').invoke('val', tsvFile);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();
    cy.get('.default-importing-wizard-header input[bind="projectNameInput"]', {
      timeout: 6000,
    }).should('have.value', 'Clipboard');

    // then ensure we are on the preview page
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
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
    cy.waitForProjectTable();

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
    cy.get('#project-tags-container .select2-search__field').type(uniqueTagName1+'{enter}');
    cy.get('#project-tags-container .select2-search__field').type(uniqueTagName2+'{enter}');
    cy.get('#or-import-parsopt').click();

    // click next to create the project, and wait until it's loaded
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
        .contains('Create project »')
        .click();
    cy.waitForProjectTable();

    cy.addProjectForDeletion();
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

  const csvURL =
      'https://raw.githubusercontent.com/OpenRefine/OpenRefine/master/main/tests/cypress/cypress/fixtures/food.mini.csv';

  it('Test the create project from Web URL based on CSV', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > div')
      .contains('Web Addresses (URLs)')
      .click();
    cy.get('#or-import-enterurl').should(
      'to.contain',
      'Enter one or more web addresses (URLs) pointing to data to download:'
    );
    cy.get('input[bind="urlInput"]').filter(':visible').type(csvURL);

    cy.get('.create-project-ui-source-selection-tab-body.selected button.button-primary')
      .contains('Next »')
      .click();

    // then ensure we are on the preview page
    cy.get('.default-importing-wizard-header input[bind="projectNameInput"]')
        .should('have.value', 'food mini csv');

    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
  });

  it('Test the create project from Multiple Web URLs based on CSV', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > div')
      .contains('Web Addresses (URLs)')
      .click();
    cy.get('#or-import-enterurl').should(
      'to.contain',
      'Enter one or more web addresses (URLs) pointing to data to download:'
    );
    cy.get('input[bind="urlInput"]').filter(':visible').type(csvURL);
    cy.get('button[bind="addButton"]').contains('Add another URL').click();

    cy.get('.create-project-ui-source-selection-tab-body.selected button.button-primary')
      .contains('Next »')
      .click();

    cy.get('.create-project-ui-panel')
      .contains('Configure parsing options »')
      .click();

    // then ensure we are on the preview page
    cy.get(
      '.default-importing-wizard-header input[bind="projectNameInput"]'
    ).should('have.value', 'food mini csv');

    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
  });
});
