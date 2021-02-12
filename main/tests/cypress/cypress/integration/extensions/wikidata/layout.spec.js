describe(__filename, function () {
	it('Ensure the wikidata extension is loaded by default', function () {
		cy.loadAndVisitProject('food.mini');
		cy.get('#extension-bar-menu-container').should('to.contain', 'Wikidata');
		cy.get('#extension-bar-menu-container')
			.should('to.contain', 'Wikidata')
			.click();

		// we do only one assertion to ensure the menu appear properly
		cy.get('.menu-container').should('to.contain', 'Select Wikibase instance');
	});
});
