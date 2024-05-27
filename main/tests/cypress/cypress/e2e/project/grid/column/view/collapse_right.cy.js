describe(__filename, function () {
const fixture = [
        ['a', 'b', 'c'],

        ['0a', '0b', '0c'],
        ['1a', '1b', '1c']
      ];
  it('it collapses all columns to right', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('a', ['View', 'Collapse all columns to right']);

    cy.get('[title="Expand column \'b\'"]').should('to.contain', '');
    cy.get('[title="Expand column \'c\'"]').should('to.contain', '');

  });
});
