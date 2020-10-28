describe(__filename, function () {
	it('Creates a simple project, based on a CSV', function () {
		// navigate to the create page
		cy.visitOpenRefine();
		cy.navigateTo('Create Project');
		cy.get('#create-project-ui-source-selection-tabs > div').contains('This Computer').click();
		cy.get('.create-project-ui-source-selection-tab-body.selected').contains('Locate one or more files on your computer to upload');

		// load a file
		const csvFile = { filePath: 'food.mini.csv', mimeType: 'application/csv' };
		cy.get('.create-project-ui-source-selection-tab-body.selected input[type="file"]').attachFile(csvFile);
		cy.get('.create-project-ui-source-selection-tab-body.selected button.button-primary').click();
		cy.get('.default-importing-wizard-header input[bind="projectNameInput"]').should('have.value', 'food mini csv');

		// name the project
		cy.get('.default-importing-wizard-header input[bind="projectNameInput"]').type('this is a test');

		// ensure file is loaded
		cy.get(`table.data-table tbody tr:nth-child(2) td:nth-child(3)`).contains('BUTTER,WITH SALT');
		cy.get('.default-importing-wizard-header button[bind="nextButton"]').click();
		cy.get('#create-project-progress-message').contains('Done.');

		// workaround to ensure project is loaded
		// cypress does not support window.location = ...
		cy.get('h2').contains('HTTP ERROR 404');
		cy.location().should((location) => {
			expect(location.href).contains('http://localhost:3333/__/project?');
		});

		cy.location().then((location) => {
			const projectId = location.href.split('=').slice(-1)[0];
			cy.visitProject(projectId);
		});
	});
});
