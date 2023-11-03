describe(__filename, function () {
  it('Ensures column is splitted by comma seperator', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Split into several columns…',
    ]);
    cy.waitForDialogPanel();

    cy.get('input[bind="separatorInput"]').type('{backspace},');

    cy.confirmDialogPanel();
    cy.get('body[ajax_in_progress="false"]');
    cy.assertNotificationContainingText(
      'Split 2 cell(s) in column Shrt_Desc into several columns by separator '
    );

    cy.get('.data-table-header').find('th').should('have.length', 7)
    cy.get('th:nth-child(3) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 1');
    cy.get('th:nth-child(4) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 2');
    cy.get('th:nth-child(5) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 3');

    cy.assertCellEquals(0, 'Shrt_Desc 1', 'BUTTER');
    cy.assertCellEquals(0, 'Shrt_Desc 2', 'WITH SALT');
    cy.assertCellEquals(0, 'Shrt_Desc 3', null);
  });
  it('Ensures column is splitted by comma seperator without removing original column', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Split into several columns…',
    ]);
    cy.waitForDialogPanel();

    cy.get('input[bind="separatorInput"]').type('{backspace},');
    cy.get('input[bind="removeColumnInput"]').uncheck();

    cy.confirmDialogPanel();
    cy.get('body[ajax_in_progress="false"]');
    cy.assertNotificationContainingText(
      'Split 2 cell(s) in column Shrt_Desc into several columns by separator '
    );

    cy.get('.data-table-header').find('th').should('have.length', 8);
    cy.get('th:nth-child(3) > div.column-header-title > span').should('to.contain', 'Shrt_Desc');
    cy.get('th:nth-child(5) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 2');
    cy.get('th:nth-child(6) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 3');

    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(0, 'Shrt_Desc 1', 'BUTTER');
    cy.assertCellEquals(0, 'Shrt_Desc 2', 'WITH SALT');
    cy.assertCellEquals(0, 'Shrt_Desc 3', null);
  });
  it('Ensures column is splitted by field-lengths seperator', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Split into several columns…',
    ]);
    cy.waitForDialogPanel();

    cy.get('[type="radio"]').check('lengths');
    cy.get('textarea[bind="lengthsTextarea"]').type('1, 2, 3');

    cy.confirmDialogPanel();
    cy.get('body[ajax_in_progress="false"]');
    cy.assertNotificationContainingText(
      'Split 2 cell(s) in column Shrt_Desc into several columns by field lengths'
    );

    cy.get('.data-table-header').find('th').should('have.length', 7);
    cy.get('th:nth-child(3) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 1');
    cy.get('th:nth-child(4) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 2');
    cy.get('th:nth-child(5) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 3');

    cy.assertCellEquals(0, 'Shrt_Desc 1', 'B');
    cy.assertCellEquals(0, 'Shrt_Desc 2', 'UT');
    cy.assertCellEquals(0, 'Shrt_Desc 3', 'TER');
  });
  it('Ensures column is splitted by field-lengths seperator without removing original column', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', [
      'Edit column',
      'Split into several columns…',
    ]);
    cy.waitForDialogPanel();

    cy.get('[type="radio"]').check('lengths');
    cy.get('textarea[bind="lengthsTextarea"]').type('1, 2, 3, 4');
    cy.get('input[bind="removeColumnInput"]').uncheck();

    cy.confirmDialogPanel();
    cy.get('body[ajax_in_progress="false"]');
    cy.assertNotificationContainingText(
      'Split 2 cell(s) in column Shrt_Desc into several columns by field lengths'
    );

    cy.get('.data-table-header').find('th').should('have.length', 9);
    cy.get('th:nth-child(3) > div.column-header-title > span').should('to.contain', 'Shrt_Desc');
    cy.get('th:nth-child(5) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 2');
    cy.get('th:nth-child(6) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 3');
    cy.get('th:nth-child(7) > div.column-header-title > span').should('to.contain', 'Shrt_Desc 4');

    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(0, 'Shrt_Desc 1', 'B');
    cy.assertCellEquals(0, 'Shrt_Desc 2', 'UT');
    cy.assertCellEquals(0, 'Shrt_Desc 3', 'TER');
    cy.assertCellEquals(0, 'Shrt_Desc 4', ',WIT');
  });
});
