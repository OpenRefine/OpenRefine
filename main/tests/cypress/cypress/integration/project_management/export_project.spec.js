describe(__filename, function () {
	it('Export a project', function () {
		const testProjectName = Date.now();
		cy.loadAndVisitProject('food.mini.csv', testProjectName);
		cy.get('#export-button').click();
		cy.get('.menu-container a').contains('OpenRefine project archive to file').click();
	});
});
