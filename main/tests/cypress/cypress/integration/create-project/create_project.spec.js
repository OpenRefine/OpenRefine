describe(__filename, function () {
  it('Test the create project from this computer based on CSV', function () {
    // navigate to the create page
    cy.visitOpenRefine();
    cy.navigateTo('Create Project');
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
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create Project »')
      .click();
    cy.get('#create-project-progress-message').contains('Done.');

    // ensure the project is loading by checking presence of a project-title element
    cy.get('#project-title').should('exist');
  });
  it('Test the create project from this computer based on TSV', function () {
    // navigate to the create page
    cy.visitOpenRefine();
    cy.navigateTo('Create Project');
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
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create Project »')
      .click();
    cy.get('#create-project-progress-message').contains('Done.');

    // ensure the project is loading by checking presence of a project-title element
    cy.get('#project-title').should('exist');
  });
  it('Test the create project from clipboard based on CSV', function () {
    // navigate to the create page
    cy.visitOpenRefine();
    cy.navigateTo('Create Project');
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
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create Project »')
      .click();
    cy.get('#create-project-progress-message').contains('Done.');

    // ensure the project is loading by checking presence of a project-title element
    cy.get('#project-title').should('exist');
  });
  it('Test the create project from clipboard based on TSV', function () {
    // navigate to the create page
    cy.visitOpenRefine();
    cy.navigateTo('Create Project');
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
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

    // preview and click next
    cy.get('.default-importing-wizard-header button[bind="nextButton"]')
      .contains('Create Project »')
      .click();
    cy.get('#create-project-progress-message').contains('Done.');

    // ensure the project is loading by checking presence of a project-title element
    cy.get('#project-title').should('exist');
  });
  // it('Test the create project from Web URL based on CSV', function () {
  //   // navigate to the create page
  //   cy.visitOpenRefine();
  //   cy.navigateTo('Create Project');
  //   cy.get('#create-project-ui-source-selection-tabs > div')
  //     .contains('Web Addresses (URLs)')
  //     .click();
  //   cy.get('#or-import-enterurl').should(
  //     'to.contain',
  //     'Enter one or more web addresses (URLs) pointing to data to download:'
  //   );
  //   // add file
  //   const csvURL =
  //     'https://raw.githubusercontent.com/OpenRefine/OpenRefine/master/main/tests/cypress/cypress/fixtures/food.mini.csv';
  //   cy.get('input[bind="urlInput"]').filter(':visible').type(csvURL);
  //   Cypress.config('pageLoadTimeout', 10000);
  //   Cypress.config('commandTimeout', 10000);
  //   Cypress.config('defaultCommandTimeout', 10000);

  //   cy.get(
  //     '.create-project-ui-source-selection-tab-body.selected button.button-primary'
  //   )
  //     .contains('Next »')
  //     .click();

  //   cy.get('.default-importing-wizard-header input[bind="projectNameInput"]', {
  //     timeout: 6000,
  //   }).should('have.value', 'food mini csveuh');

  //   // then ensure we are on the preview page
  //   cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

  //   // preview and click next
  //   cy.get('.default-importing-wizard-header button[bind="nextButton"]')
  //     .contains('Create Project »')
  //     .click();
  //   cy.get('#create-project-progress-message').contains('Done.');

  //   // ensure the project is loading by checking presence of a project-title element
  //   cy.get('#project-title').should('exist');
  // });

  // it('Test the create project from Multiple Web URLs based on CSV', function () {
  //   // navigate to the create page
  //   cy.visitOpenRefine();
  //   cy.navigateTo('Create Project');
  //   cy.get('#create-project-ui-source-selection-tabs > div')
  //     .contains('Web Addresses (URLs)')
  //     .click();
  //   cy.get('#or-import-enterurl').should(
  //     'to.contain',
  //     'Enter one or more web addresses (URLs) pointing to data to download:'
  //   );
  //   // add file
  //   const csvURL =
  //     'https://raw.githubusercontent.com/OpenRefine/OpenRefine/master/main/tests/cypress/cypress/fixtures/food.mini.csv';
  //   cy.get('input[bind="urlInput"]').filter(':visible').type(csvURL);
  //   cy.get('button[bind="addButton"]').contains('Add Another URL').click();

  //   cy.get(
  //     '.create-project-ui-source-selection-tab-body.selected button.button-primary'
  //   )
  //     .contains('Next »')
  //     .click();
  //   cy.get('.create-project-ui-panel', { timeout: 10000 })
  //     .contains('Configure Parsing Options »')
  //     .click();
  //   cy.get(
  //     '.default-importing-wizard-header input[bind="projectNameInput"]'
  //   ).should('have.value', 'food mini csv');

  //   // then ensure we are on the preview page
  //   // cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

  //   // preview and click next
  //   cy.get('.default-importing-wizard-header button[bind="nextButton"]')
  //     .contains('Create Project »')
  //     .click();
  //   cy.get('#create-project-progress-message').contains('Donuue.');

  //   // // ensure the project is loading by checking presence of a project-title element
  //   // cy.get('#project-title').should('exist');
  // });
});
