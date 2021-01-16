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
	it('Ensure projects are sorted on basis of names', function () {
		const projectName = "projectA";
		const projectName2 = "projectZ";
		cy.loadProject('food.mini.csv', projectName);
		cy.loadProject('food.mini.csv', projectName2);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list table').contains("Name").click();
		cy.get('#projects-list tbody>tr').eq(1).contains('projectA');
		cy.get('#projects-list table').contains("Name").click();
		cy.get('#projects-list tbody>tr').eq(1).contains('projectZ'); 
	});
	it('Ensure project is deleted from database as well as UI', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.contains('td', projectName).siblings().find('.delete-project').click();
		cy.get('#projects-list table>tbody>tr').should('not.contain', projectName);
		cy.request('GET', 'http://127.0.0.1:3333/command/core/get-all-project-metadata').then((response) => {
			var responseText = JSON.stringify(response.body);
			expect(responseText).to.not.have.string(projectName);
  		});
	});
});
