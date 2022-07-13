describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],

    ['0a', '0b', '0c'],
    ['1a', '1b', '1c']
  ];
  it('Export a project through "OpenRefine project archive to file"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('OpenRefine project archive to file')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.openrefine.tar.gz`).should('not.be.empty');
    });

  });
  it('Export a project through "Tab-separated value"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Tab-separated value')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.tsv`).should('not.be.empty');
    });

  });
  it('Export a project through "Comma-separated value"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Comma-separated value')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.csv`).should('not.be.empty');
    });

  });
  it('Export a project through "HTML table"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('HTML table')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.html`).should('not.be.empty');
    });

  });
  it('Export a project through "Excel (.xls)"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Excel (.xls)')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.xls`).should('not.be.empty');
    });

  });
  it('Export a project through "Excel 2007+ (.xlsx)"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Excel 2007+ (.xlsx)')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.xlsx`).should('not.be.empty');
    });

  });
  it('Export a project through "ODF spreadsheet"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('ODF spreadsheet')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.ods`).should('not.be.empty');
    });

  });
  it('Export a project through "Custom tabular exporter"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Custom tabular exporter')
      .click();
    cy.get('a[bind="or_dialog_download"]').click();
    cy.get('button[bind="downloadButton"]').click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.tsv`).should('not.be.empty');
    });

  });
  it('Export a project through "SQL Exporter"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('SQL Exporter')
      .click();
    cy.get('a[bind="or_dialog_download"]').click();
    cy.get('button[bind="downloadButton"]').click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.sql`).should('not.be.empty');
    });

  });
  it('Export a project through "Templating"', function () {

    cy.loadAndVisitProject(fixture);

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Templating')
      .click();
    cy.get('button[bind="exportButton"]').click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      cy.readFile(`cypress/downloads/${name}.txt`).should('not.be.empty');
    });

  });
});
