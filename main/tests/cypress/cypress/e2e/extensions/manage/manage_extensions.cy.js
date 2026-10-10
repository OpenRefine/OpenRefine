describe(__filename, function () {
  it('Loads installed extensions via a relative command URL', function () {
    cy.intercept('GET', '**/command/core/get-version').as('getVersion');
    cy.visitOpenRefine();
    cy.navigateTo('Extensions');
    cy.wait('@getVersion')
      .its('request.url')
      .should('match', /\/command\/core\/get-version(?:\?|$)/);
    cy.window().then((win) => {
      // Absolute "/command/..." breaks path-prefixed installs (#7387).
      const source = win.Refine.ManageExtensionsUI._fetchExtensions.toString();
      expect(source).to.include('"command/core/get-version"');
      expect(source).to.not.include('"/command/');
    });
    cy.get('#extensionList').should('contain', 'core');
  });
});
