describe(__filename, function () {
const fixture = [
        ['a', 'b', 'c'],

        ['0a', '0b', '0c'],
        ['1a', '1b', '1c']
      ];
  it('it collapses all columns to right', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('a', ['Hide / Show', 'Hide all columns to right']);

    cy.get('[title="Show column \'b\'"]').should('to.contain', '');
    cy.get('[title="Show column \'c\'"]').should('to.contain', '');

  });
});
