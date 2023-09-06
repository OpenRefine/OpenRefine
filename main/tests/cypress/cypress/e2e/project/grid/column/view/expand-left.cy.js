// CS427 Issue Link: https://github.com/OpenRefine/OpenRefine/issues/4067
describe(__filename, function () {
const fixture = [
        ['a', 'b', 'c'],

        ['0a', '0b', '0c'],
        ['1a', '1b', '1c']
      ];
  it('expands all columns to left', function () {
    cy.loadAndVisitProject(fixture);
    // Start by collapsing
    cy.columnActionClick('c', ['View', 'Collapse all columns to left']);

    // Verify collapsed columns
    cy.get('[title="a"]').should('to.contain', '');
    cy.get('[title="b"]').should('to.contain', '');

    // Expand
    cy.columnActionClick('c', ['View', 'Expand all columns to the left']);

    // Verify expanded columns
    cy.get('[title="a"]').should('to.contain', 'a');
    cy.get('[title="b"]').should('to.contain', 'b');

  });
});
