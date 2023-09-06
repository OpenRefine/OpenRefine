describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],

    ['0a', '0b', '0c'],
    ['1a', '1b', '1c']
  ];
  it('it renames the project name', function () {
    cy.loadAndVisitProject(fixture);
    cy.window().then(win=>{
      const stub=cy.stub(win,'prompt');
      stub.returns('rename');
      cy.get('.app-path-section').click();
    })
    cy.get('.app-path-section').contains('rename');
  });
});
