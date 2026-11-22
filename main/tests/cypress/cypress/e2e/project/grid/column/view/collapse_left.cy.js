describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],

    ['0a', '0b', '0c'],
    ['1a', '1b', '1c'],
  ];
  it('it collapses all columns to left', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('c', ['Hide / Show', 'Hide all columns to left']);

    cy.get('[title="Show column \'a\'"]').should('to.contain', '');
    cy.get('[title="Show column \'b\'"]').should('to.contain', '');
  });

  it('resizes column after hiding all columns to left', function () {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('c', ['Hide / Show', 'Hide all columns to left']);

    cy.get('[title="Show column \'a\'"]').should('to.contain', '');
    cy.get('[title="Show column \'b\'"]').should('to.contain', '');

    cy.get('th[title="c"] .column-header-resizer-left').trigger('mousedown', { button: 0, pageX: 200 });
    cy.get('body').trigger('mousemove', { pageX: 250 });
    cy.get('body').trigger('mouseup', { button: 0 });

    cy.get('th[title="c"]').should('have.class', 'resized-column');
  });
});
