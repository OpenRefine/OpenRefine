describe(__filename, function () {
  beforeEach(function () {
    cy.loadAndVisitProject([
      ['review', 'other'],
      ['hello', 'keep 1'],
      ['world', 'keep 2'],
    ]);
  });

  it('shows the supported source and target languages', function () {
    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();

    const expectedLanguages = ['ENGLISH', 'SPANISH', 'MANDARIN'];
    cy.get('.dialog-container select[bind="sourceLanguageInput"] option').then((options) => {
      expect([...options].map((option) => option.value)).to.deep.equal(expectedLanguages);
    });
    cy.get('.dialog-container select[bind="targetLanguageInput"] option').then((options) => {
      expect([...options].map((option) => option.value)).to.deep.equal(expectedLanguages);
    });

    cy.cancelDialogPanel();
  });

  it('warns when the target column already exists', function () {
    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();

    cy.get('.dialog-container input[bind="targetColumnInput"]').clear();
    cy.get('.dialog-container input[bind="targetColumnInput"]').type('other');
    cy.get('.dialog-container .dialog-footer button[type="submit"]').click();

    cy.get('.dialog-container [bind="validationMessage"]')
      .should('be.visible')
      .and('contain', 'already exists');
    cy.waitForDialogPanel();

    cy.cancelDialogPanel();
  });

  it('submits the selected columns, languages, and LibreTranslate configuration', function () {
    cy.intercept('POST', '**/command/core/translate-column*', (request) => {
      const parameters = new URLSearchParams(request.url.split('?')[1]);
      expect(parameters.get('columnName')).to.equal('review');
      expect(parameters.get('sourceLanguage')).to.equal('ENGLISH');
      expect(parameters.get('targetLanguage')).to.equal('SPANISH');
      expect(parameters.get('targetColumnName')).to.equal('review_spanish');
      expect(parameters.get('endpoint')).to.equal('http://translation.example.invalid:5000/translate');
      expect(parameters.get('timeoutMillis')).to.equal('30000');
      expect(parameters.get('maxRetries')).to.equal('3');
      expect(request.body).to.equal('');
      request.reply({ statusCode: 200, body: {} });
    }).as('translateColumn');

    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();
    cy.get('.dialog-container input[bind="targetColumnInput"]').clear();
    cy.get('.dialog-container input[bind="targetColumnInput"]').type('review_spanish');
    cy.get('.dialog-container input[bind="endpointInput"]').clear();
    cy.get('.dialog-container input[bind="endpointInput"]').type(
      'http://translation.example.invalid:5000/translate'
    );
    cy.get('.dialog-container .dialog-footer button[type="submit"]').click();

    cy.wait('@translateColumn');
  });
});
