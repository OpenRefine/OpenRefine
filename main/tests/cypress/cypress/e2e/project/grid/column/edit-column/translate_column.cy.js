describe(__filename, function () {
  beforeEach(function () {
    cy.loadAndVisitProject([
      ['review', 'other'],
      ['hello', 'keep 1'],
      ['world', 'keep 2'],
    ]);
  });

  it('shows the five supported languages and masks the API key', function () {
    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();

    cy.get('.dialog-container select[bind="languageInput"] option').should('have.length', 5);
    cy.get('.dialog-container select[bind="languageInput"] option').then((options) => {
      const values = [...options].map((option) => option.value);
      expect(values).to.deep.equal(['SPANISH', 'MANDARIN', 'TELUGU', 'TAMIL', 'TAGALOG']);
    });
    cy.get('.dialog-container input[bind="apiKeyInput"]').should('have.attr', 'type', 'password');

    cy.cancelDialogPanel();
  });

  it('suggests a new column name that follows the language until edited', function () {
    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();

    cy.get('.dialog-container input[bind="newColumnNameInput"]').should('have.value', 'review (Spanish)');
    cy.get('.dialog-container select[bind="languageInput"]').select('TAMIL');
    cy.get('.dialog-container input[bind="newColumnNameInput"]').should('have.value', 'review (Tamil)');

    cy.get('.dialog-container input[bind="newColumnNameInput"]').clear();
    cy.get('.dialog-container input[bind="newColumnNameInput"]').type('my translations');
    cy.get('.dialog-container select[bind="languageInput"]').select('TAGALOG');
    cy.get('.dialog-container input[bind="newColumnNameInput"]').should('have.value', 'my translations');

    cy.cancelDialogPanel();
  });

  it('rejects a new column name that already exists', function () {
    cy.intercept('POST', '**/command/core/translate-column*').as('translateColumn');
    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();

    cy.get('.dialog-container input[bind="newColumnNameInput"]').clear();
    cy.get('.dialog-container input[bind="newColumnNameInput"]').type('other');
    cy.get('.dialog-container input[bind="modelInput"]').clear();
    cy.get('.dialog-container input[bind="modelInput"]').type('test-model');
    cy.get('.dialog-container input[bind="apiKeyInput"]').type('test-key');
    cy.get('.dialog-container .dialog-footer button[type="submit"]').click();

    cy.get('.dialog-container [bind="validationMessage"]')
      .should('be.visible')
      .and('contain', 'already exists');
    cy.get('@translateColumn.all').should('have.length', 0);

    cy.cancelDialogPanel();
  });

  it('shows inline validation for an invalid endpoint', function () {
    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();

    cy.get('.dialog-container input[bind="endpointInput"]').clear();
    cy.get('.dialog-container input[bind="endpointInput"]').type('ftp://example.invalid/chat/completions');
    cy.get('.dialog-container input[bind="modelInput"]').clear();
    cy.get('.dialog-container input[bind="modelInput"]').type('test-model');
    cy.get('.dialog-container input[bind="apiKeyInput"]').clear();
    cy.get('.dialog-container input[bind="apiKeyInput"]').type('test-key');
    cy.get('.dialog-container .dialog-footer button[type="submit"]').click();

    cy.get('.dialog-container [bind="validationMessage"]')
      .should('be.visible')
      .and('contain', 'valid translation endpoint');
    cy.waitForDialogPanel();

    cy.cancelDialogPanel();
  });

  it('submits the selected column and translation configuration', function () {
    cy.intercept('POST', '**/command/core/translate-column*', (request) => {
      const parameters = new URLSearchParams(request.url.split('?')[1]);
      const requestBody = new URLSearchParams(request.body);
      expect(parameters.get('columnName')).to.equal('review');
      expect(parameters.get('newColumnName')).to.equal('review (Spanish)');
      expect(parameters.get('language')).to.equal('SPANISH');
      expect(parameters.get('endpoint')).to.equal('https://translation.example.invalid/chat/completions');
      expect(parameters.get('model')).to.equal('test-model');
      expect(parameters.get('batchSize')).to.equal('10');
      expect(parameters.get('timeoutMillis')).to.equal('30000');
      expect(parameters.get('maxRetries')).to.equal('3');
      expect(requestBody.get('apiKey')).to.equal('test-key');
      expect(request.url).to.not.contain('test-key');
      expect(request.url).to.not.contain('apiKey=');
      request.reply({ statusCode: 200, body: {} });
    }).as('translateColumn');

    cy.columnActionClick('review', ['Edit column', 'Translate column…']);
    cy.waitForDialogPanel();
    cy.get('.dialog-container input[bind="endpointInput"]').clear();
    cy.get('.dialog-container input[bind="endpointInput"]').type(
      'https://translation.example.invalid/chat/completions'
    );
    cy.get('.dialog-container input[bind="modelInput"]').clear();
    cy.get('.dialog-container input[bind="modelInput"]').type('test-model');
    cy.get('.dialog-container input[bind="apiKeyInput"]').clear();
    cy.get('.dialog-container input[bind="apiKeyInput"]').type('test-key');
    cy.get('.dialog-container .dialog-footer button[type="submit"]').click();

    cy.wait('@translateColumn');
  });
});
