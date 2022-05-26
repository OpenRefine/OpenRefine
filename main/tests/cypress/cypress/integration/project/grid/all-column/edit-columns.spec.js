describe(__filename, function () {
  it('Ensure columns are reordered as per order', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', [
      'Edit columns',
      'Re-order / remove columns…',
    ]);
    cy.waitForDialogPanel();

    cy.dragAndDrop('div[column="Shrt_Desc"]', 'div[column="NDB_No"]', 'top');
    cy.dragAndDrop('div[column="Energ_Kcal"]', 'div[column="Water"]', 'top');

    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Reorder columns');

    cy.assertGridEquals([
      ['Shrt_Desc', 'NDB_No', 'Energ_Kcal', 'Water'],
      ['BUTTER,WITH SALT', '01001', '717', '15.87'],
      ['BUTTER,WHIPPED,WITH SALT', '01002', '717', '15.87'],
    ]);
  });
  it('Ensure columns are removed using remove column', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', [
      'Edit columns',
      'Re-order / remove columns…',
    ]);
    cy.waitForDialogPanel();

    cy.dragAndDrop('div[column="Shrt_Desc"]', 'div[bind="trashContainer"]','center');
    cy.dragAndDrop('div[column="Water"]', 'div[bind="trashContainer"]','center');

    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Reorder columns');

    cy.assertGridEquals([
      ['NDB_No', 'Energ_Kcal'],
      ['01001', '717'],
      ['01002', '717'],
    ]);
  });
  it('Ensure columns are blanked down', function () {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('All', ['Edit columns', 'Blank down']);

    cy.assertGridEquals([
      ['NDB_No', 'Shrt_Desc', 'Water', 'Energ_Kcal'],
      ['01001', 'BUTTER,WITH SALT', '15.87', '717'],
      ['01002', 'BUTTER,WHIPPED,WITH SALT', '', ''],
    ]);
  });

  it('Ensure columns are filled down', function () {
    cy.loadAndVisitProject([
      ['NDB_No', 'Shrt_Desc', 'Water', 'Energ_Kcal'],
      ['01001', 'BUTTER,WITH SALT', '15.87', '717'],
      ['01002', 'BUTTER,WHIPPED,WITH SALT', '', ''],
    ]);

    cy.columnActionClick('All', ['Edit columns', 'Fill down']);

    cy.assertGridEquals([
      ['NDB_No', 'Shrt_Desc', 'Water', 'Energ_Kcal'],
      ['01001', 'BUTTER,WITH SALT', '15.87', '717'],
      ['01002', 'BUTTER,WHIPPED,WITH SALT', '15.87', '717'],
    ]);
  });
});
