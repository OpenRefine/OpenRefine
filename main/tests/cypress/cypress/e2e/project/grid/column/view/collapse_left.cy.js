describe(__filename, function () {
const fixture = [
        ['a', 'b', 'c'],

        ['0a', '0b', '0c'],
        ['1a', '1b', '1c']
      ];
  it('it collapses all columns to left', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('c', ['Hide / Show', 'Hide all columns to left']);

    cy.get('[title="Show column \'a\'"]').should('to.contain', '');
    cy.get('[title="Show column \'b\'"]').should('to.contain', '');

  });
});
