describe(__filename, function () {
  const fixture = [
    ['a', 'b', 'c'],
    ['0a', '0b', '0c'],
    ['1a', '1b', '1c'],
  ];

  function resizeColumn(columnName, handleColumnName = columnName, handleClass = 'column-header-resizer-left') {
    cy.get('.data-table th').then(($headers) => {
      const widths = [...$headers].map((header) => header.getBoundingClientRect().width);

      cy.get(`.data-table th[title="${handleColumnName}"] .${handleClass}`).then(($handle) => {
        const rect = $handle[0].getBoundingClientRect();
        const pageX = rect.left + rect.width / 2;
        const pageY = rect.top + rect.height / 2;

        cy.wrap($handle).trigger('mousedown', { button: 0, pageX, pageY });
        cy.get('body').trigger('mousemove', { pageX: pageX + 120, pageY });
        cy.get('body').trigger('mouseup', { button: 0, pageX: pageX + 120, pageY });
      });

      cy.get('.data-table th').should(($resizedHeaders) => {
        [...$resizedHeaders].forEach((header, index) => {
          const width = header.getBoundingClientRect().width;
          if (header.title === columnName) {
            expect(width, `${columnName} is wider`).to.be.greaterThan(widths[index]);
          } else {
            expect(width, `${header.title || 'All'} keeps its width`).to.be.closeTo(widths[index], 1);
          }
        });
      });
    });
  }

  beforeEach(function () {
    cy.loadAndVisitProject(fixture);
  });

  it('resizes a column when all columns are expanded', function () {
    resizeColumn('c');
  });

  it('resizes the intended column after hiding a preceding column', function () {
    cy.columnActionClick('a', ['Hide / Show', 'Hide this column']);

    resizeColumn('b');
  });

  it('resizes a column after hiding all preceding columns', function () {
    cy.columnActionClick('c', ['Hide / Show', 'Hide all columns to left']);

    resizeColumn('c');
  });

  it('resizes the previous column from the next header after hiding a preceding column', function () {
    cy.columnActionClick('a', ['Hide / Show', 'Hide this column']);

    resizeColumn('b', 'c', 'column-header-resizer-right');
  });
});
