describe(__filename, function () {
  afterEach(() => {
    cy.addProjectForDeletion();
  });
  
  it('Test the create project from this computer based on CSV', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > a')
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
    cy.get('#create-project-ui-source-selection-tabs > a')
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
    cy.get('#create-project-ui-source-selection-tabs > a')
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
    cy.get('#create-project-ui-source-selection-tabs > a')
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
    cy.get('#create-project-ui-source-selection-tabs > a')
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
    cy.get('#create-project-ui-source-selection-tabs > a')
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
    ).should('have.value', 'food mini 2 csv');

    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();
    cy.waitForProjectTable();
  });
  it('Test the create project from XML file after selecting record path', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > a')
      .contains('This Computer')
      .click();
    cy.get('.create-project-ui-source-selection-tab-body.selected').contains(
      'Locate one or more files on your computer to upload'
    );
    // add file
    const xmlFile = {
      filePath: 'AAT-with-xsi.xml',
      mimeType: 'text/xml',
    };
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected input[type="file"]'
    ).attachFile(xmlFile);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();
    cy.get('.default-importing-wizard-header input[bind="projectNameInput"]', {
      timeout: 6000,
    }).should('have.value', 'AAT with xsi xml');

    // then ensure we are on the preview page
    cy.get('.create-project-ui-panel').contains('Configure parsing options');

    // tree record selector
    cy.get('#or-import-clickXML').contains('Click on the first XML element corresponding to the first record to load.')

    // Cypress automatically clicks OK on all alerts, but we want to check the contents
    const stub = cy.stub()
    cy.on('window:alert', stub)

    // This should error without any record selected
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click()
      .then(() => {
        expect(stub.getCall(0)).to.be.calledWith('Please specify a record path first.')
      });

    // This long selector refers to the Preferred_Term element
    cy.get('div.xml-parser-ui-select-dom > div > div.children > div.elmt > div.children > div:nth-child(14) > div.children > div:nth-child(2) > div:nth-child(1)')
      .contains('Preferred_Term')
      .click();

    // Verify that the first cell in our grid has the right contents
    cy.get('div.default-importing-parsing-data-panel > table > tbody > tr.odd > td:nth-child(2) > div > span')
      .contains('Top of the AAT hierarchies')

    // Create the project
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create project »')
      .click();

    cy.waitForProjectTable();
  });

  it('Test the create project from invalid XML generates error dialog', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Create project');
    cy.get('#create-project-ui-source-selection-tabs > a')
      .contains('This Computer')
      .click();
    cy.get('.create-project-ui-source-selection-tab-body.selected').contains(
      'Locate one or more files on your computer to upload'
    );
    // add file
    const xmlFile = {
      filePath: 'AAT.xml',
      mimeType: 'text/xml',
    };
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected input[type="file"]'
    ).attachFile(xmlFile);
    cy.get(
      '.create-project-ui-source-selection-tab-body.selected button.button-primary'
    )
      .contains('Next »')
      .click();

    cy.get('body > div.dialog-container.ui-draggable > div > div.dialog-header.ui-draggable-handle').contains('Error');
    // Full error message should be the text below, but we won't bother checking it all
    // Error during initialization - javax.xml.stream.XMLStreamException: ParseError at [row,col]:[1,205]
    // Message: http://www.w3.org/TR/1999/REC-xml-names-19990114#AttributePrefixUnbound?Vocabulary&xsi:noNamespaceSchemaLocation&xsi
  });

});
