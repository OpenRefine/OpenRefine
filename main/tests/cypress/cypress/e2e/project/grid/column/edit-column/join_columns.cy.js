describe(__filename, function () {
  it('Ensures two columns are joined', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Join columns…']);
    cy.waitForDialogPanel();

    cy.get('input[column="NDB_No"]').check();

    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT01001');
    cy.assertCellEquals(1, 'Shrt_Desc', 'BUTTER,WHIPPED,WITH SALT01002');
  });
  it('Ensures two columns are joined respecting the column ordering', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Join columns…']);
    cy.waitForDialogPanel();

    cy.get('input[column="NDB_No"]').check();
    cy.dragAndDrop('div[column="NDB_No"]', 'div[column="Shrt_Desc"]','top');

    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Shrt_Desc', '01001BUTTER,WITH SALT');
    cy.assertCellEquals(1, 'Shrt_Desc', '01002BUTTER,WHIPPED,WITH SALT');
  });
  it('Ensures two columns are joined with given seperator', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Join columns…']);
    cy.waitForDialogPanel();

    cy.get('input[column="NDB_No"]').check();
    cy.get('input[bind="field_separatorInput"]').type(':-:');

    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT:-:01001');
    cy.assertCellEquals(1, 'Shrt_Desc', 'BUTTER,WHIPPED,WITH SALT:-:01002');
  });
  it('Ensures two columns are joined with given seperator with columns reorder', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Join columns…']);
    cy.waitForDialogPanel();

    cy.dragAndDrop('div[column="NDB_No"]', 'div[column="Shrt_Desc"]','top');
    cy.get('input[column="NDB_No"]').check();
    cy.get('input[bind="field_separatorInput"]').type(':-:');

    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Shrt_Desc', '01001:-:BUTTER,WITH SALT');
    cy.assertCellEquals(1, 'Shrt_Desc', '01002:-:BUTTER,WHIPPED,WITH SALT');
  });
  it('Ensures three columns are joined and written into a new column', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Join columns…']);
    cy.waitForDialogPanel();

    cy.get('input[column="NDB_No"]').check();
    cy.get('input[column="Water"]').check();

    cy.get('[type="radio"]').check('copy-to-new-column');
    cy.get('input[bind="new_column_nameInput"]').type('Test_Merged_Column');

    cy.confirmDialogPanel();

    cy.assertCellEquals(0, 'Test_Merged_Column', 'BUTTER,WITH SALT0100115.87');
    cy.assertCellEquals(
      1,
      'Test_Merged_Column',
      'BUTTER,WHIPPED,WITH SALT0100215.87'
    );
  });
  it('Ensures three columns are joined and written into a new column with a given seperator', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Join columns…']);
    cy.waitForDialogPanel();

    cy.get('input[column="NDB_No"]').check();
    cy.get('input[column="Water"]').check();

    cy.get('[type="radio"]').check('copy-to-new-column');
    cy.get('input[bind="new_column_nameInput"]').type('Test_Merged_Column');
    cy.get('input[bind="field_separatorInput"]').type(':-:');

    cy.confirmDialogPanel();

    cy.assertCellEquals(
      0,
      'Test_Merged_Column',
      'BUTTER,WITH SALT:-:01001:-:15.87'
    );
    cy.assertCellEquals(
      1,
      'Test_Merged_Column',
      'BUTTER,WHIPPED,WITH SALT:-:01002:-:15.87'
    );
  });
  it('Ensures columns are joined and original columns are deleted', function () {
    cy.loadAndVisitProject('food.mini');

    cy.columnActionClick('Shrt_Desc', ['Edit column', 'Join columns…']);
    cy.waitForDialogPanel();

    cy.get('button[bind="selectAllButton"]').click();
    cy.get('[type="radio"]').check('copy-to-new-column');
    cy.get('input[bind="delete_joined_columnsInput"]').check();

    cy.get('input[bind="new_column_nameInput"]').type('Test_Merged_Grid');

    cy.confirmDialogPanel();
    cy.get('body[ajax_in_progress="false"]');
    cy.assertGridEquals([
      ['Test_Merged_Grid'],
      ['BUTTER,WITH SALT0100115.87717'],
      ['BUTTER,WHIPPED,WITH SALT0100215.87717'],
    ]);
  });
});
