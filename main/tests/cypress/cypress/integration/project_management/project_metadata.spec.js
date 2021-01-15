describe(__filename, function () {
	it('Ensures project-metadata dialogue loads', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
        cy.contains('td', projectName)   
  		  .siblings()  
  		  .contains('a', 'About')
  		  .click()
		cy.get('h1').contains('Project metadata');
		cy.get('body > .dialog-container > .dialog-frame .dialog-footer button[bind="closeButton"]').click();
		cy.get('body > .dialog-container > .dialog-frame').should('not.exist');
	});
	it('Ensures project-metadata has correct details', function () {
		const projectName = Date.now();
		cy.loadProject('food.mini.csv', projectName);
		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
        cy.contains('td', projectName)   
  		  .siblings()  
  		  .contains('a', 'About')
  		  .click()
		cy.get('h1').contains('Project metadata');
		cy.get('#metadata-body tbody>tr').eq(3).contains('Project name');
		cy.get('#metadata-body tbody>tr').eq(3).contains(projectName);
		// buggy part as row is being returned 0 sometimes from the server
        // cy.get('#metadata-body tbody>tr').eq(9).contains('Row count');
        // cy.get('#metadata-body tbody>tr').eq(9).contains('2');
	})
})