describe(__filename, function () {
	it('Visit project-metadata page', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
        cy.get('a[href*="about"]').click();
        cy.get('h1').contains('Project metadata')
	});

	it('Ensures project-metadata has correct details', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
        cy.get('a[href*="about"]').click();
        cy.get('tbody>tr').eq(3).contains('Project name');
        cy.get('tbody>tr').eq(9).contains(projectName);
        cy.get('tbody>tr').eq(9).contains('Row count');
        cy.get('tbody>tr').eq(9).contains('2');
	});
});