describe(__filename, function () {
	it('Test the create project page', function () {
		cy.visitOpenRefine();
		cy.navigateTo('Create Project');
		cy.get('#create-project-ui-source-selection-tabs > div').contains('This Computer').click();
		cy.get('.create-project-ui-source-selection-tab-body.selected').contains('Locate one or more files on your computer to upload');

		// load a file
		const csvFile = { filePath: 'food.mini.csv', mimeType: 'application/csv' };
		cy.get('.create-project-ui-source-selection-tab-body.selected input[type="file"]').attachFile(csvFile);
		cy.get('.create-project-ui-source-selection-tab-body.selected button.button-primary').click();
		cy.get('.default-importing-wizard-header input[bind="projectNameInput"]').should('have.value', 'food mini csv');

		// then ensure we are on the preview page
		cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
	});

	it('Test project naming', function () {
		cy.visitOpenRefine();
		cy.createProjectThroughUserInterface('food.mini.csv');
		cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
		cy.get('.default-importing-wizard-header input[bind="projectNameInput"]').type('this is a test');
		cy.doCreateProjectThroughUserInterface();

		cy.get('#project-name-button').contains('this is a test');
	});

	it('Test project tagging', function () {
		cy.visitOpenRefine();
		cy.createProjectThroughUserInterface('food.mini.csv');
		cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
		const uniqueProjectName = Date.now();
		const uniqueTagName1 = 'tag1_' + Date.now();
		const uniqueTagName2 = 'tag2_' + Date.now();

		cy.get('.default-importing-wizard-header input[bind="projectNameInput"]').type(uniqueProjectName);
		// triger the select input
		cy.get('#project-tags-container').click();
		// Type and Validate the tag, pressing enter
		cy.get('#project-tags-container .select2-input').type(uniqueTagName1);
		cy.get('body').type('{enter}');
		cy.get('#project-tags-container .select2-input').type(uniqueTagName2);
		cy.get('body').type('{enter}');
		cy.get('#or-import-parsopt').click();
		cy.doCreateProjectThroughUserInterface();

		cy.visitOpenRefine();
		cy.navigateTo('Open Project');
		cy.get('#projects-list').contains(uniqueProjectName).parent().parent().contains(uniqueTagName1);
		cy.get('#projects-list').contains(uniqueProjectName).parent().parent().contains(uniqueTagName2);
	});

	it('E2E, Creates a simple project, based on a CSV', function () {
		// navigate to the create page
		cy.visitOpenRefine();
		cy.navigateTo('Create Project');
		// add file
		const csvFile = { filePath: 'food.mini.csv', mimeType: 'application/csv' };
		cy.get('.create-project-ui-source-selection-tab-body.selected input[type="file"]').attachFile(csvFile);
		cy.get('.create-project-ui-source-selection-tab-body.selected button.button-primary').click();

		// preview and click next
		cy.get('.default-importing-wizard-header button[bind="nextButton"]').click();
		cy.get('#create-project-progress-message').contains('Done.');

		// workaround to ensure project is loaded
		// cypress does not support window.location = ...
		cy.get('h2').contains('HTTP ERROR 404');
		cy.location().should((location) => {
			expect(location.href).contains(Cypress.env('OPENREFINE_URL')+'/__/project?');
		});

		cy.location().then((location) => {
			const projectId = location.href.split('=').slice(-1)[0];
			cy.visitProject(projectId);
		});
	});
});
