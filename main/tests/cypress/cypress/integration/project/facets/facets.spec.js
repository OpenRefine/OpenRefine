describe(__filename, function () {
	it('Test the facets text (no facets yet)', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('Water', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets').should('exist').contains('Using facets and filters');
	});

	it('Create a simple text facet', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('Water', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets').should('exist');

		// check the layout
		cy.get('#refine-tabs-facets .facets-container li:first-child span[bind="titleSpan"]').contains('Water');
		cy.get('#refine-tabs-facets .facets-container li:first-child ').contains('182 choices');
		cy.get('#refine-tabs-facets .facets-container li:first-child ').contains('Sort by');
		cy.get('#refine-tabs-facets .facets-container li:first-child ').contains('Cluster');
	});

	it('Create a facet, Sort by count', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('Water', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets').should('exist');

		// Ensure sort should be by name by default
		cy.get('.facet-container .facet-body .facet-choice:first-child').contains('0.24');
		cy.get('#refine-tabs-facets .facets-container li:first-child a[bind="sortByCountLink"]').click();
		// Sort should now be by count
		cy.get('.facet-container .facet-body .facet-choice:first-child').contains('15.87');
	});

	it('Create a facet, Sort by count, then by name', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('Water', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets').should('exist');

		// Sort should be by name by default
		cy.get('.facet-container .facet-body .facet-choice:first-child').contains('0.24');
		cy.get('#refine-tabs-facets .facets-container li:first-child a[bind="sortByCountLink"]').click();
		// Sort should now be by count
		cy.get('.facet-container .facet-body .facet-choice:first-child').contains('15.87');
		cy.get('#refine-tabs-facets .facets-container li:first-child a[bind="sortByNameLink"]').click();
		cy.get('.facet-container .facet-body .facet-choice:first-child').contains('0.24');
	});

	it('Test the display of multiple facets', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('NDB_No', ['Facet', 'Text facet']);
		cy.columnActionClick('Shrt_Desc', ['Facet', 'Text facet']);
		cy.columnActionClick('Water', ['Facet', 'Text facet']);

		cy.get('#refine-tabs-facets .facets-container li:nth-child(1) span[bind="titleSpan"]').contains('NDB_No');
		cy.get('#refine-tabs-facets .facets-container li:nth-child(2) span[bind="titleSpan"]').contains('Shrt_Desc');
		cy.get('#refine-tabs-facets .facets-container li:nth-child(3) span[bind="titleSpan"]').contains('Water');
	});

	// it('Test collapsing facet panels', function () {
	// The following test does not work
	// Because the facet panels uses soem weird CSS with overflow:hidden, Cypress can not detect it
	// //// # cy.loadAndVisitProject('food.small.csv');
	// //// # cy.columnActionClick('NDB_No', ['Facet', 'Text facet']);
	// //// # ensure facet inner panel is visible
	// //// # cy.get('#refine-tabs-facets .facets-container li:nth-child(1) .facet-body-inner').should('be.visible');
	// //// #collapse the panel
	// //// # cy.get('#refine-tabs-facets .facets-container li:nth-child(1) a[bind="minimizeButton"]').click();
	// //// # cy.get('#refine-tabs-facets .facets-container li:nth-child(1) .facet-body-inner').should('not.be.visible');
	// });

	it('Test editing a facet', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('NDB_No', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets .facets-container li:nth-child(1) a[bind="changeButton"]').contains('change');
		cy.get('#refine-tabs-facets .facets-container li:nth-child(1) a[bind="changeButton"]').click();
		cy.get('.dialog-container .dialog-header').contains(`Edit Facet's Expression`);
	});

	it('Test editing a facet / Preview', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('NDB_No', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets .facets-container li:nth-child(1) a[bind="changeButton"]').click();
		// test the tab
		cy.get('.dialog-container #expression-preview-tabs-preview').should('be.visible');
		// test the content
		cy.get('.dialog-container #expression-preview-tabs-preview').contains('row');
	});

	it('Test editing a facet / History', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('NDB_No', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets .facets-container li:nth-child(1) a[bind="changeButton"]').click();

		cy.get('.dialog-container a[bind="or_dialog_history"]').click();
		// test the tab
		cy.get('.dialog-container #expression-preview-tabs-history').should('be.visible');
		// test the content
		cy.get('.dialog-container #expression-preview-tabs-history').contains('Reuse');
	});

	it('Test editing a facet / Starred', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('NDB_No', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets .facets-container li:nth-child(1) a[bind="changeButton"]').click();

		cy.get('.dialog-container a[bind="or_dialog_starred"]').click();
		// test the tab
		cy.get('.dialog-container #expression-preview-tabs-starred').should('be.visible');
		// test the content
		cy.get('.dialog-container #expression-preview-tabs-starred').contains('Expression');
	});

	it('Test editing a facet / Help', function () {
		cy.loadAndVisitProject('food.small.csv');
		cy.columnActionClick('NDB_No', ['Facet', 'Text facet']);
		cy.get('#refine-tabs-facets .facets-container li:nth-child(1) a[bind="changeButton"]').click();

		cy.get('.dialog-container a[bind="or_dialog_help"]').click();
		// test the tab
		cy.get('.dialog-container #expression-preview-tabs-help').should('be.visible');
		// test the content
		cy.get('.dialog-container #expression-preview-tabs-help').contains('Variables');
	});
});
