describe(__filename, function () {
	it('List an existing project', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list table').contains(projectName);
	});
});
