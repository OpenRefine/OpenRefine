describe(__filename, function () {
  it('Keeps the text transform dialog layout stable while the preview changes', function () {
    cy.loadAndVisitProject([
      ['a', 'b'],
      ['0a', 'change'],
      ['1a', 'change'],
    ]);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.get('.dialog-frame.text-transform-dialog').then(($dialog) => {
      const { width, height } = $dialog[0].getBoundingClientRect();
      const assertDialogLayout = () => {
        cy.get('.dialog-frame.text-transform-dialog').should(($currentDialog) => {
          const bounds = $currentDialog[0].getBoundingClientRect();
          expect(bounds.width).to.equal(width);
          expect(bounds.height).to.equal(height);
        });
        cy.get('.text-transform-dialog .dialog-footer button').each(($button) => {
          cy.wrap($button).should('be.visible').should(($visibleButton) => {
            const bounds = $visibleButton[0].getBoundingClientRect();
            expect(bounds.top).to.be.at.least(0);
            expect(bounds.bottom).to.be.at.most(Cypress.config('viewportHeight'));
          });
        });
      };

      cy.typeExpression('()');
      cy.get('.expression-preview-parsing-status').should('contain', 'Parsing error');
      assertDialogLayout();

      cy.typeExpression('value');
      cy.get('.expression-preview-parsing-status').should('contain', 'No syntax error.');
      assertDialogLayout();

      ['Preview', 'Help', 'History', 'Starred'].forEach((tab) => {
        cy.get('#expression-preview-tabs li').contains(tab).click();
        assertDialogLayout();
      });
    });
  });

  it('Ensure cells are transformed', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('replace(value,"change","a")');
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', 'a');
    cy.assertCellEquals(1, 'b', 'a');
    cy.assertCellEquals(2, 'b', 'a');
  });
  it('Ensure cells are set to blank when error occurs', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('value.replace()');
    cy.get('label[bind="or_views_setBlank"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', '');
    cy.assertCellEquals(1, 'b', '');
    cy.assertCellEquals(2, 'b', '');
  });
  it('Ensure cells cells contains error message when error occurs', function () {
    const fixture = [
      ['a', 'b', 'c'],

      ['0a', 'change', '0c'],
      ['1a', 'change', '1c'],
      ['2a', 'change', '2c'],
    ];

    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('b', ['Edit cells', 'Transform']);

    cy.typeExpression('value.replace()');
    cy.get('label[bind="or_views_storeErr"]').click();
    cy.confirmDialogPanel();

    cy.assertNotificationContainingText('Text transform on 3 cells');

    cy.assertCellEquals(0, 'b', 'replace expects three strings, or one string, one regex, and one string');
    cy.assertCellEquals(1, 'b', 'replace expects three strings, or one string, one regex, and one string');
    cy.assertCellEquals(2, 'b', 'replace expects three strings, or one string, one regex, and one string');
  });

  // TODO: Add test for repeated transforms
});
