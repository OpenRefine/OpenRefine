// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

import 'cypress-file-upload';
import 'cypress-wait-until';
// const fs = require('fs-extra');
// var uniqid = require('uniqid');
//
//
Cypress.Commands.add('assertTextareaHaveJsonValue', (selector, json) => {
	cy.get(selector).then((el) => {
		// expected json needs to be parsed / restringified, to avoid inconsitencies about spaces and tabs
		const present = JSON.parse(el.val());
		cy.expect(JSON.stringify(present)).to.equal(JSON.stringify(json));
	});
});
Cypress.Commands.add('visitOpenRefine', (options) => {
	cy.visit(Cypress.env('OPENREFINE_URL'), options);
});

Cypress.Commands.add('createProjectThroughUserInterface', (fixtureFile) => {
	cy.navigateTo('Create Project');

	const uploadFile = { filePath: fixtureFile, mimeType: 'application/csv' };
	cy.get('.create-project-ui-source-selection-tab-body.selected input[type="file"]').attachFile(uploadFile);
	cy.get('.create-project-ui-source-selection-tab-body.selected button.button-primary').click();
});

Cypress.Commands.add('doCreateProjectThroughUserInterface', () => {
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
		cy.wrap(projectId).as('createdProjectId');
	});
});

Cypress.Commands.add('getCell', (rowIndex, columnName) => {
	const cssRowIndex = rowIndex + 1;
	// first get the header, to know the cell index
	cy.get(`table.data-table thead th[title="${columnName}"]`).then(($elem) => {
		// there are 3 td at the beginning of each row
		const columnIndex = $elem.index() + 3;
		return cy.get(`table.data-table tbody tr:nth-child(${cssRowIndex}) td:nth-child(${columnIndex})`);
	});
});

Cypress.Commands.add('assertCellEquals', (rowIndex, columnName, value) => {
	const cssRowIndex = rowIndex + 1;
	// first get the header, to know the cell index
	cy.get(`table.data-table thead th[title="${columnName}"]`).then(($elem) => {
		// there are 3 td at the beginning of each row
		const columnIndex = $elem.index() + 3;
		cy.get(`table.data-table tbody tr:nth-child(${cssRowIndex}) td:nth-child(${columnIndex}) div.data-table-cell-content > span`).should(($cellSpan)=>{
			expect($cellSpan.text()).equals(value);
		});
	});
});

Cypress.Commands.add('navigateTo', (target) => {
	cy.get('#action-area-tabs li').contains(target).click();
});

Cypress.Commands.add('waitForOrOperation', () => {
	cy.get('body[ajax_in_progress="true"]');
	cy.get('body[ajax_in_progress="false"]');
});

Cypress.Commands.add('deleteColumn', (columnName) => {
	cy.get('.data-table th[title="' + columnName + '"]').should('exist');
	cy.columnActionClick(columnName, ['Edit column', 'Remove this column']);
	cy.get('.data-table th[title="' + columnName + '"]').should('not.exist');
});

Cypress.Commands.add('waitForDialogPanel', () => {
	cy.get('body > .dialog-container > .dialog-frame').should('be.visible');
});

Cypress.Commands.add('confirmDialogPanel', () => {
	cy.get('body > .dialog-container > .dialog-frame .dialog-footer button[bind="okButton"]').click();
	cy.get('body > .dialog-container > .dialog-frame').should('not.exist');
});

Cypress.Commands.add('columnActionClick', (columnName, actions) => {
	cy.get('.data-table th:contains("' + columnName + '") .column-header-menu').click();

	for (var i = 0; i < actions.length; i++) {
		cy.get('body > .menu-container').eq(i).contains(actions[i]).click();
	}
	cy.get('body[ajax_in_progress="false"]');
});

Cypress.Commands.add('visitProject', (projectId) => {
	cy.visit(Cypress.env('OPENREFINE_URL') + '/project?project=' + projectId);
	cy.get('#project-title').should('exist');
});

Cypress.Commands.add('loadAndVisitProject', (fixture, projectName = Date.now()) => {
	cy.loadProject(fixture, projectName).then((projectId) => {
		cy.visit(Cypress.env('OPENREFINE_URL') + '/project?project=' + projectId);
	});
});
