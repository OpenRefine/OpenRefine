describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],

    ['0a', '0b', '0c'],
    ['1a', '1b', '1c'],
  ];
  it('it collapses all columns', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('a', ['Hide / Show', 'Hide this column']);

    cy.get('[title="Show column \'a\'"]').should('to.contain', '');
  });

  it('resizes intended column after hiding preceding columns', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('a', ['Hide / Show', 'Hide this column']);

    cy.get('[title="Show column \'a\'"]').should('to.contain', '');

    cy.get('th[title="b"] .column-header-resizer-left').trigger('mousedown', { button: 0, pageX: 200 });
    cy.get('body').trigger('mousemove', { pageX: 250 });
    cy.get('body').trigger('mouseup', { button: 0 });

    cy.get('th[title="b"]').should('have.class', 'resized-column');
    cy.get('th[title="c"]').should('not.have.class', 'resized-column');
  });
});
