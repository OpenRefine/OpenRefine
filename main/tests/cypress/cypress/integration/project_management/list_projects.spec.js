describe(__filename, function () {
	it('List an existing project, ensure a newly created project is displayed', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list table').contains(projectName);
	});

	it('Visit a project from the Open project page, ensure link is working', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list table').contains(projectName).click();
		cy.get('#project-name-button').contains(projectName);
	});
});
