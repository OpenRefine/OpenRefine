describe(__filename, function () {
  it('Check elements on the langage page', function () {
    cy.visitOpenRefine();
    cy.get('.main-layout-panel').contains('Select preferred language');
    cy.get('select#langDD').should('exist');
    cy.get('#set-lang-button').should('exist');
  });

  // This test can't be implemented
  // It's changing the UI for all subsequent tests
  // If it fails, the interface will remains in German, making subsequent tests fails
  // it('Change the langage', function () {
  // 	cy.visitOpenRefine();
  // 	cy.navigateTo('Language settings');
  // 	cy.get('#langDD').select('de');
  // 	cy.get('#set-lang-button').click();
  // 	cy.get('#slogan').contains('Ein leistungsstarkes Werkzeug für die Bearbeitung von ungeordneten Daten.');
  // });
});
