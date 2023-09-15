describe(__filename, function () {
  it('Ensure the Edit button is visible on mouse over a cell', function () {
    cy.loadAndVisitProject('food.mini');

    cy.getCell(1, 'Water').trigger('mouseover');
    cy.getCell(1, 'Water').find('a.data-table-cell-edit').should('be.visible');
  });

  it('Ensure the Edit button opens a popup', function () {
    cy.loadAndVisitProject('food.mini');
    cy.getCell(1, 'Shrt_Desc')
      .trigger('mouseover')
      .find('a.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').should(
      'have.value',
      'BUTTER,WHIPPED,WITH SALT'
    );
  });

  it('Test a simple edit', function () {
    cy.loadAndVisitProject('food.mini');
    cy.getCell(1, 'Shrt_Desc')
      .trigger('mouseover')
      .find('a.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').type(
      'OpenRefine Testing'
    );
    cy.get('.menu-container button[bind="okButton"]').click();
    // ensure value has been changed in the grid
    cy.get('.menu-container.data-table-cell-editor').should('not.exist');
    cy.assertCellEquals(1, 'Shrt_Desc', 'OpenRefine Testing');
  });

  it('Test a simple edit, using keyboard shortcut', function () {
    cy.loadAndVisitProject('food.mini');
    cy.getCell(1, 'Shrt_Desc')
      .trigger('mouseover')
      .find('a.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').type(
      'OpenRefine Testing'
    );
    cy.get('body').type('{enter}');
    cy.get('.menu-container.data-table-cell-editor').should('not.exist');
    cy.assertCellEquals(1, 'Shrt_Desc', 'OpenRefine Testing');
  });

  it('Test the cancel button', function () {
    cy.loadAndVisitProject('food.mini');
    cy.getCell(1, 'Shrt_Desc')
      .trigger('mouseover')
      .find('a.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').type(
      'OpenRefine Testing'
    );
    cy.get('.menu-container button[bind="cancelButton"]').click();
    // ensure value has been changed in the grid
    cy.get('.menu-container.data-table-cell-editor').should('not.exist');
    cy.assertCellEquals(1, 'Shrt_Desc', 'BUTTER,WHIPPED,WITH SALT');
  });

  it('Test the cancel button, using keyboard shortcut', function () {
    cy.loadAndVisitProject('food.mini');
    cy.getCell(1, 'Shrt_Desc')
      .trigger('mouseover')
      .find('a.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').type(
      'OpenRefine Testing'
    );
    cy.get('body').type('{esc}');
    // ensure value has been changed in the grid
    cy.get('.menu-container.data-table-cell-editor').should('not.exist');
    cy.assertCellEquals(1, 'Shrt_Desc', 'BUTTER,WHIPPED,WITH SALT');
  });

  it('Test edit all identical cells', function () {
    cy.loadAndVisitProject('food.mini');
    cy.getCell(1, 'Water')
      .trigger('mouseover')
      .find('a.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').type(42);
    cy.get('.menu-container button[bind="okallButton"]').click();

    // ensure all values has been changed in the grid
    cy.get('.menu-container.data-table-cell-editor').should('not.exist');
    cy.get('#notification-container')
      .should('be.visible')
      .contains('Mass edit');
    cy.assertCellEquals(0, 'Water', '42');
    cy.assertCellEquals(1, 'Water', '42');
  });

  it('Test edit all identical cells, using the shortcut', function () {
    cy.loadAndVisitProject('food.mini');
    cy.getCell(1, 'Water')
      .trigger('mouseover')
      .find('a.data-table-cell-edit')
      .click();
    cy.get('.menu-container.data-table-cell-editor').should('exist');
    cy.get('.menu-container.data-table-cell-editor textarea').type(42);
    cy.get('body').type('{ctrl}{enter}');

    // ensure all values has been changed in the grid
    cy.get('.menu-container.data-table-cell-editor').should('not.exist');
    cy.get('#notification-container')
      .should('be.visible')
      .contains('Mass edit');
    cy.assertCellEquals(0, 'Water', '42');
    cy.assertCellEquals(1, 'Water', '42');
  });
});
