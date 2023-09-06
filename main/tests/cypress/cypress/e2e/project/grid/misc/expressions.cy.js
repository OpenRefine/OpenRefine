/**
 * Utility method to load an expression panel, used by almost all the tests
 */
function loadExpressionPanel() {
  cy.columnActionClick('Shrt_Desc', ['Facet', 'Custom text facet']);
}

/**
 * Generate a unique GREL expression to be used for testing
 */
function generateUniqueExpression() {
  return `value+${Date.now()}`;
}

describe(__filename, function () {
  it('Test the layout of the expression panel', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();

    cy.get('.dialog-container').within(() => {
      cy.get('label[bind="or_dialog_expr"]').contains('Expression');
      cy.get('select[bind="expressionPreviewLanguageSelect"] option').should(
        'have.length',
        3
      );
      cy.get('textarea.expression-preview-code').should('exist');

      cy.get('.dialog-footer button:nth-child(1)').should('to.contain', 'OK');
      cy.get('.dialog-footer button:nth-child(2)').should(
        'to.contain',
        'Cancel'
      );
    });
  });

  it('Test a valid Grel expression', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.typeExpression('value.toLowercase()');
    cy.get('.expression-preview-parsing-status').contains('No syntax error.');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
  });

  it('Test a valid Python expression', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('jython');
    cy.typeExpression('return value.lower()');
    cy.get('.expression-preview-parsing-status').contains('No syntax error.');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
  });

  it('Test a valid Clojure expression', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('clojure');
    cy.typeExpression('(.. value (toLowerCase) )');
    cy.get('.expression-preview-parsing-status').contains('No syntax error.');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
  });

  it('Test a Grel syntax error', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.typeExpression('()');
    cy.get('.expression-preview-parsing-status').contains('Parsing error');
  });

  it('Test a Python syntax error', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('jython');
    cy.typeExpression('(;)');
    cy.get('.expression-preview-parsing-status').contains('Internal error');
  });

  it('Test a Clojure syntax error', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('clojure');
    cy.typeExpression('(;)');
    cy.get('.expression-preview-parsing-status').contains(
      'Syntax error reading source'
    );
  });

  it('Test a Grel language error', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.typeExpression('value.thisGrelFunctionDoesNotExists()');
    cy.get('.expression-preview-parsing-status').contains(
      'Unknown function thisGrelFunctionDoesNotExists'
    );
  });

  it('Test a Python language error', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('jython');
    cy.typeExpression('return value.thisPythonFunctionDoesNotExists()');

    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'Error:');
  });

  it('Test a Clojure language error', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('clojure');
    cy.typeExpression('(.. value (thisClojureFunctionDoesNotExists) )');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'Error: No matching method');
  });

  it('Test switching from one langage to another', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.typeExpression('(.. value (toLowerCase) )');
    // error is expected, this is clojure language
    cy.get('.expression-preview-parsing-status').should(
      'to.contain',
      'Parsing error'
    );
    // switching to clojure
    cy.get('select[bind="expressionPreviewLanguageSelect"]').select('clojure');
    cy.get('.expression-preview-parsing-status').should(
      'not.to.contain',
      'Parsing error'
    );
  });

  it('Test the preview (GREL)', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.typeExpression('value.toLowercase()');

    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(1) td:last-child'
    ).should('to.contain', 'value.toLowercase()');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(2) td:last-child'
    ).should('to.contain', 'butter,with salt');
    cy.get(
      '.expression-preview-table-wrapper tr:nth-child(3) td:last-child'
    ).should('to.contain', 'butter,whipped,with salt');
  });

  it('Test the help tab', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('#expression-preview-tabs li').contains('Help').click();
    cy.get('#expression-preview-tabs-help').should('be.visible');
    cy.get('#expression-preview-tabs-help').should(
      'to.contain',
      `The current cell. It has a few fields: 'value', 'recon' and 'errorMessage'.`
    );
  });

  it('Test the history behavior, ensure expressions are stored', function () {
    cy.loadAndVisitProject('food.mini');
    // Because history is shared across projects, we need to use an expression that is unique

    // Use a first unique expression
    const uniqueExpression = generateUniqueExpression();
    loadExpressionPanel();
    cy.typeExpression(uniqueExpression);
    cy.get('.dialog-footer button').contains('OK').click();
    // ensure the function has been added to the facet
    cy.get('#refine-tabs-facets').contains(uniqueExpression.replace('()', ''));

    // Reload and review history
    // Ensure the previously used expression is listed
    loadExpressionPanel();
    cy.get('#expression-preview-tabs li').contains('History').click();
    cy.get('#expression-preview-tabs-history')
      .should('be.visible')
      .should('to.contain', uniqueExpression);
  });

  it('Test the reuse of expressions from the history', function () {
    cy.loadAndVisitProject('food.mini');
    // Because history is shared across projects, we need to build and use an expression that is unique
    const uniqueExpression = generateUniqueExpression();
    loadExpressionPanel();
    cy.typeExpression(uniqueExpression);
    cy.get('.dialog-footer button').contains('OK').click();
    cy.get('#refine-tabs-facets').contains(uniqueExpression.replace('()', ''));

    // Reload and review history
    // Ensure the previously used expression is there
    // Use it
    loadExpressionPanel();
    cy.get('#expression-preview-tabs li').contains('History').click();
    cy.get('tbody > tr:nth-child(2) > td:nth-child(5)')
      .contains(uniqueExpression)
      .parent()
      .find('a')
      .contains('Reuse')
      .click();

    // Expression must be populated in the textarea, after clicking on 'reuse'
    cy.get('textarea.expression-preview-code').should(
      'have.value',
      uniqueExpression
    );
  });

  it('Test the history, star', function () {
    cy.loadAndVisitProject('food.mini');

    // Cleanup step
    // Because starred expression are shared across projects, see #3499
    // We need to un-star all previously starred expressions
    loadExpressionPanel();
    cy.get('#expression-preview-tabs li').contains('Starred').click();
    cy.get(
      '#expression-preview-tabs-starred .expression-preview-table-wrapper table'
    ).then(($table) => {
      if ($table.find('tr').length > 1) {
        cy.get(
          '#expression-preview-tabs-starred .expression-preview-table-wrapper table a'
        )
          .contains('Remove')
          .each(($btn) => {
            cy.wrap($btn).click();
            cy.get('.dialog-container:last-child button')
              .contains('OK')
              .click();
          });
      }
    });
    cy.get('.dialog-footer button').contains('Cancel').click();
    // End cleanup

    // Load an expression
    loadExpressionPanel();
    const uniqueExpression = generateUniqueExpression();
    cy.typeExpression(uniqueExpression);
    cy.get('.dialog-footer button').contains('OK').click();

    // Star the expression
    loadExpressionPanel();
    cy.get('#expression-preview-tabs li').contains('History').click();
    cy.get('tbody > tr:nth-child(2) > td:nth-child(5)')
      .contains(uniqueExpression)
      .parent()
      .find('a.data-table-star-off')
      .click();

    // List starred expressions, en ensure the expression is listed
    cy.get('#expression-preview-tabs li').contains('Starred').click();
    cy.get(
      '#expression-preview-tabs-starred .expression-preview-table-wrapper table'
    ).contains(uniqueExpression);
  });

  it('Simple test to ensure the expression panel can be closed with OK', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('.dialog-footer button').contains('OK').click();
    cy.get('.dialog-container').should('not.to.exist');
  });

  it('Simple test to ensure the expression panel can be closed with Cancel', function () {
    cy.loadAndVisitProject('food.mini');
    loadExpressionPanel();
    cy.get('.dialog-footer button').contains('Cancel').click();
    cy.get('.dialog-container').should('not.to.exist');
  });
});
