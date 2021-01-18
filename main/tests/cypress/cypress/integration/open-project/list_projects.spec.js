describe(__filename, function () {
	it('List an existing project, ensure a newly created project is displayed', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list table').should('contain',projectName);
	});

	it('Visit a project from the Open project page, ensure link is working', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list table').contains(projectName).click();
		cy.get('#project-name-button').should('contain',projectName);
	});
	it('Ensure projects are sorted on basis of names', function () {
		const projectName = "projectA";
		const projectName2 = "projectZ";
		var positionA, positionZ;
		cy.loadProject('food.mini.csv', projectName);
		cy.loadProject('food.mini.csv', projectName2);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list table').contains("Name").click();
		// cy.get('#projects-list tbody>tr').eq(0).should('contain','projectA');
		// cy.get('#projects-list table').contains("Name").click();
		// cy.get('#projects-list tbody>tr').eq(0).should('contain', 'projectZ'); 
		// hack since all projects are not being deleted
		cy.get("#projects-list tbody")
		.find("tr")
		.then((rows) => {
		  rows.toArray().forEach((element) => {
			if (element.innerHTML.includes("projectZ")) {
				positionZ = (rows.index(element));
			}
			if (element.innerHTML.includes("projectA")) {
					positionA = (rows.index(element));
				}
		  });
		  expect(positionA).to.be.lessThan(positionZ);
		});
		cy.get('#projects-list table').contains("Name").click();
		cy.get("#projects-list tbody")
		.find("tr")
		.then((rows) => {
		  rows.toArray().forEach((element) => {
			if (element.innerHTML.includes("projectZ")) {
				positionZ = (rows.index(element));
			}
			if (element.innerHTML.includes("projectA")) {
					positionA = (rows.index(element));
				}
		  });
		  expect(positionA).to.be.greaterThan(positionZ);
		});
		
	});
	it('Ensure project is deleted from database as well as UI', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.contains('td', projectName).siblings().find('.delete-project').click();
		cy.get('#projects-list').should('not.contain', projectName);
		cy.request('GET', 'http://127.0.0.1:3333/command/core/get-all-project-metadata').then((response) => {
			var responseText = JSON.stringify(response.body);
			expect(responseText).to.not.have.string(projectName);
  		});
	});
});
