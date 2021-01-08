describe(__filename, function () {
	it('Perform a basic sort', function () {
		cy.loadAndVisitProject('food.mini.csv');

		// sort and confirm
		cy.columnActionClick('Shrt_Desc', ['Sort']);
		cy.waitForDialogPanel();
		cy.confirmDialogPanel();

		// ensure sorting is active
		cy.getCell(0, 'Shrt_Desc').contains('BUTTER,WHIPPED,WITH SALT');
		cy.getCell(1, 'Shrt_Desc').contains('BUTTER,WITH SALT');
	});

	it('Perform a basic sort + Reverse', function () {
		cy.loadAndVisitProject('food.mini.csv');

		// sort and confirm
		cy.columnActionClick('Shrt_Desc', ['Sort']);
		cy.waitForDialogPanel();
		cy.confirmDialogPanel();

		// check the sorting
		cy.getCell(0, 'Shrt_Desc').contains('BUTTER,WHIPPED,WITH SALT');
		cy.getCell(1, 'Shrt_Desc').contains('BUTTER,WITH SALT');

		// do a reverse sort
		cy.columnActionClick('Shrt_Desc', ['Sort', 'Reverse']);

		// re-check the sorting
		cy.getCell(0, 'Shrt_Desc').contains('BUTTER,WITH SALT');
		cy.getCell(1, 'Shrt_Desc').contains('BUTTER,WHIPPED,WITH SALT');
	});

	it('Perform a basic sort + Remove Sort', function () {
		cy.loadAndVisitProject('food.mini.csv');

		// sort and confirm
		cy.columnActionClick('Shrt_Desc', ['Sort']);
		cy.waitForDialogPanel();
		cy.confirmDialogPanel();

		// check the sorting
		cy.getCell(0, 'Shrt_Desc').contains('BUTTER,WHIPPED,WITH SALT');
		cy.getCell(1, 'Shrt_Desc').contains('BUTTER,WITH SALT');

		// remove
		cy.columnActionClick('Shrt_Desc', ['Sort', 'Remove sort']);

		// re-check the sorting
		cy.getCell(0, 'Shrt_Desc').contains('BUTTER,WITH SALT');
		cy.getCell(1, 'Shrt_Desc').contains('BUTTER,WHIPPED,WITH SALT');
	});
});
