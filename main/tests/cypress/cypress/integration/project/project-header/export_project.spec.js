describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],

    ['0a', '0b', '0c'],
    ['1a', '1b', '1c']
  ];

  /*
     Workaround for https://github.com/cypress-io/cypress/issues/14857
   */
  function triggerLoadEvent() {
    cy.window().document().then(function (doc) {
      doc.addEventListener('click', () => {
        setTimeout(function () {
          Cypress.$(cy.state("$autIframe")).trigger("load");
        }, 1000)
      })
    })
  }

  // Ref: https://github.com/cypress-io/cypress-example-recipes/blob/master/examples/testing-dom__download/cypress/e2e/utils.js
  const path = require('path')
  function validateFile(filename,length=10) {
    const downloadsFolder = Cypress.config('downloadsFolder')
    const downloadedFilename = path.join(downloadsFolder, filename)

    // ensure the file has been saved before trying to parse it
    cy.readFile(downloadedFilename, 'binary', {timeout: 15000})
        .should((buffer) => {
          // by having length assertion we ensure the file has text
          // since we don't know when the browser finishes writing it to disk

          // Tip: use expect() form to avoid dumping binary contents
          // of the buffer into the Command Log
          expect(buffer.length).to.be.gte(length);
        });
  }

  it('Export a project through "OpenRefine project archive to file"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    triggerLoadEvent();
    cy.get('.menu-container a')
      .contains('OpenRefine project archive to file')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.openrefine.tar.gz`);
    });

  });
  it('Export a project through "Tab-separated value"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    triggerLoadEvent();
    cy.get('.menu-container a')
      .contains('Tab-separated value')
      .click();
    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.tsv`);
    });
  });
  it('Export a project through "Comma-separated value"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    triggerLoadEvent();
    cy.get('.menu-container a')
      .contains('Comma-separated value')
      .click();
    cy.get('.app-path-section').invoke('text').then((name)=> {
      validateFile(`${name}.csv`);
    });
  });
  it('Export a project through "HTML table"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    triggerLoadEvent();
    cy.get('.menu-container a')
      .contains('HTML table')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.html`);
    });

  });
  it('Export a project through "Excel (.xls)"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    triggerLoadEvent();
    cy.get('.menu-container a')
      .contains('Excel (.xls)')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.xls`);
    });

  });
  it('Export a project through "Excel 2007+ (.xlsx)"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    triggerLoadEvent();
    cy.get('.menu-container a')
      .contains('Excel 2007+ (.xlsx)')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.xlsx`);
    });

  });
  it('Export a project through "ODF spreadsheet"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    triggerLoadEvent();
    cy.get('.menu-container a')
      .contains('ODF spreadsheet')
      .click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.ods`);
    });

  });
  it('Export a project through "Custom tabular exporter"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Custom tabular')
      .click();
    cy.get('a[bind="or_dialog_download"]').click();
    triggerLoadEvent();
    cy.get('button[bind="downloadButton"]').click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.tsv`);
    });

  });
  it('Export a project through "SQL Exporter"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('SQL')
      .click();
    cy.get('a[bind="or_dialog_download"]').click();
    triggerLoadEvent();
    cy.get('button[bind="downloadButton"]').click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.sql`);
    });

  });
  it('Export a project through "Templating"', function () {

    cy.loadAndVisitProject(fixture, Date.now());

    cy.get('#export-button').click();
    cy.get('.menu-container a')
      .contains('Templating')
      .click();

    triggerLoadEvent();
    cy.get('button[bind="exportButton"]').click();

    cy.get('.app-path-section').invoke('text').then((name)=>{
      validateFile(`${name}.txt`);
    });

  });
});
