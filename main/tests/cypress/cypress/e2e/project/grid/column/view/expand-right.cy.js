// CS427 Issue Link: https://github.com/OpenRefine/OpenRefine/issues/4067
describe(__filename, function () {
const fixture = [
        ['a', 'b', 'c'],

        ['0a', '0b', '0c'],
        ['1a', '1b', '1c']
      ];
  it('it collapses all columns to right', function () {
    cy.loadAndVisitProject(fixture);
    // Start by collapsing columns
    cy.columnActionClick('a', ['View', 'Collapse all columns to right']);

    // Verify collapse
    cy.get('[title="b"]').should('to.contain', '');
    cy.get('[title="c"]').should('to.contain', '');

    // Expand columns
    cy.columnActionClick('a', ['View', 'Expand all columns to the right']);

    // Verify expansion
    cy.get('[title="b"]').should('to.contain', 'b');
    cy.get('[title="c"]').should('to.contain', 'c');

  });
});
