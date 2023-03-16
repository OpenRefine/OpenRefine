describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],

    ['0a', '0b', '0c'],
    ['1a', '1b', '1c']
  ];
  it('it checks the permalink', function () {
    cy.loadAndVisitProject(fixture);
    cy.get('.secondary').contains('Permalink');
  });
});
