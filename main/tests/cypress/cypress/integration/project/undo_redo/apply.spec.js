describe(__filename, function () {
	it('Apply a JSON', function () {
		cy.loadAndVisitProject('food.mini.csv');

		cy.get('#or-proj-undoRedo').click();
		cy.get('#refine-tabs-history .history-panel-controls').contains('Apply').click();

		cy.get('table.data-table thead th[title="Shrt_Desc"]').should('exist');
		cy.get('table.data-table thead th[title="Water"]').should('exist');

		// JSON for operations that will be applied
		const operations = [
			{
				op: 'core/column-removal',
				columnName: 'Shrt_Desc',
				description: 'Remove column Shrt_Desc',
			},
			{
				op: 'core/column-removal',
				columnName: 'Water',
				description: 'Remove column Water',
			},
		];

		cy.get('.dialog-container .history-operation-json').type(JSON.stringify(operations), {
			parseSpecialCharSequences: false,
			delay: 0,
			waitForAnimations: false,
		});
		cy.get('.dialog-container button[bind="applyButton"]').click();

		cy.get('table.data-table thead th[title="Shrt_Desc"]').should('not.to.exist');
		cy.get('table.data-table thead th[title="Water"]').should('not.to.exist');
	});
});
