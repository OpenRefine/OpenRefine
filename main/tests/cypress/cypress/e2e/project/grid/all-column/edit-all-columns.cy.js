describe(__filename, function () {
  it('Ensure columns are filled down', function () {
    cy.loadAndVisitProject([
      ['Column A', 'Column B'],
      ['&lt;html&gt;&lt;body&gt;', '&lt;html&gt;&lt;head&gt;'],
    ]);

    cy.columnActionClick('All', ['Edit all columns', 'Unescape HTML entities…']);
    cy.get('.dialog-footer button').contains('OK').click();

    cy.assertGridEquals([
      ['Column A', 'Column B'],
      ['<html><body>', '<html><head>'],
    ]);
  });
});
