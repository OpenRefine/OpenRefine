describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],

    ['0a', '0b', '0c'],
    ['1a', '1b', '1c']
  ];
  it('it renames a facet name', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('a', ['Facet', 'Text facet']);

    cy.window().then(win=>{
      const stub=cy.stub(win,'prompt');
      stub.returns('facet_new_name');
      cy.get('.facet-title-span').click();
    })
    cy.get('.facet-title-span').contains('facet_new_name');
  });
});