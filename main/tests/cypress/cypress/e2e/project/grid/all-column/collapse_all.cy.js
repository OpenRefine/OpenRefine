describe(__filename, function () {
const fixture = [
        ['a', 'b', 'c'],

        ['0a', '0b', '0c'],
        ['1a', '1b', '1c']
      ];
  it('it collapses all columns', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('All', ['View', 'Collapse all columns']);

    cy.get('[title="Expand column \'a\'"]').should('to.contain', '');
    cy.get('[title="Expand column \'b\'"]').should('to.contain', '');
    cy.get('[title="Expand column \'c\'"]').should('to.contain', '');

  });
});
