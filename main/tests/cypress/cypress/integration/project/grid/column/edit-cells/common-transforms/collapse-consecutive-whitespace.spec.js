describe(__filename, function () {
  it('Ensure multiple whitespaces are collapsed', function () {
    const fixture = [
      ['NDB_No', 'Shrt_Desc'],
      ['01001', 'THIS    IS A     TEST'],
      // Test NBSP, Java whitespace (HT, LF, VT, FF, CR), & a variety of widths of Unicode spaces
      ['01002', 'THIS \u00A0  IS\t\n\x0B\f\r ANOTHER \u2000\u2001\u2002\u2003\u2004\u200A\u3000    TEST'],
      ['01003', 'THIS IS a\u00A0THIRD TEST'],
    ];
    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('Shrt_Desc', [
      'Edit cells',
      'Common transforms',
      'Collapse consecutive whitespace',
    ]);

    // Check notification and cell content
    cy.assertNotificationContainingText('Text transform on 3 cells');
    cy.assertCellEquals(0, 'Shrt_Desc', 'THIS IS A TEST');
    cy.assertCellEquals(1, 'Shrt_Desc', 'THIS IS ANOTHER TEST');
    cy.assertCellEquals(2, 'Shrt_Desc', 'THIS IS a THIRD TEST');
  });
});
