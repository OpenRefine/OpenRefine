describe(__filename, function () {
	it('Open an existing project', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName).then((projectId) => {
			cy.visitProject(projectId);
			cy.get('#project-name-button').contains(projectName);
		});
	});
});
