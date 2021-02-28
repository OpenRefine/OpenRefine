describe(__filename, function () {
  it('Ensure escaped html entities are unescaped', function () {
    const fixture = [
      ['NDB_No', 'A column'],
      ['01001', '<img src="test" />'],
      ['01001', '&lt;img src=&quot;test&quot; /&gt;'],
    ];

    cy.loadAndVisitProject(fixture, 'ok');

    cy.columnActionClick('A column', [
      'Edit cells',
      'Common transforms',
      'Unescape HTML entities',
    ]);

    // ensure notification and cell content
    cy.assertNotificationContainingText('Text transform on 1 cells');
    cy.assertCellEquals(0, 'A column', '<img src="test" />');
    cy.assertCellEquals(1, 'A column', '<img src="test" />');
  });
});
