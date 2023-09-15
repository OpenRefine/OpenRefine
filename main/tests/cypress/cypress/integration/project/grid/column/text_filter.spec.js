/**
 * Test for text_filter
 */

describe('Checks Text-filter + Case Sensitive + Regex', () => {
  it('Test Exact string, check the number of occurrences', () => {
    cy.loadAndVisitProject('food.small');
    cy.columnActionClick('Shrt_Desc', ['Text filter']);
    cy.get('.input-container > input').should('be.visible').type('CHEESE');
    cy.get('#summary-bar > span').should(
      'have.text',
      '65 matching rows (199 total)'
    );
  });
  it('Test Exact String with case sensitive', () => {
    cy.loadAndVisitProject('food.small');
    cy.columnActionClick('Shrt_Desc', ['Text filter']);
    cy.get('.input-container > input').should('be.visible').type('Cheese');
    cy.get('#summary-bar > span').should(
      'have.text',
      '65 matching rows (199 total)'
    );
    cy.get('#caseSensitiveCheckbox0').should('be.visible').click();
    cy.get('#summary-bar > span').should(
      'have.text',
      '0 matching rows (199 total)'
    );
  });

  it('Test Partial String with case sensitive', () => {
    cy.loadAndVisitProject('food.small');
    cy.columnActionClick('Shrt_Desc', ['Text filter']);
    cy.get('.input-container > input').should('be.visible').type('CHE');
    cy.get('#summary-bar > span').should(
      'have.text',
      '70 matching rows (199 total)'
    );
    cy.get('#caseSensitiveCheckbox0').click();
    cy.get('#summary-bar > span').should(
      'have.text',
      '70 matching rows (199 total)'
    );
  });

  it('check Regex option', () => {
    cy.loadAndVisitProject('food.small');
    cy.columnActionClick('Shrt_Desc', ['Text filter']);
    cy.get('.input-container > input').should('be.visible').type('[oO]G');
    cy.get('#summary-bar > span').should(
      'have.text',
      '0 matching rows (199 total)'
    );
    cy.get('#regexCheckbox0').click();
    cy.get('#summary-bar > span').should(
      'have.text',
      '15 matching rows (199 total)'
    );
  });

  it('check multiple regex option', () => {
    cy.loadAndVisitProject('food.mini');
    cy.columnActionClick('Shrt_Desc', ['Text filter']);
    cy.columnActionClick('Shrt_Desc', ['Text filter']);
    cy.get('#regexCheckbox0').should('be.visible');
    cy.get('#regexCheckbox1').should('be.visible').click();
    cy.get('#regexCheckbox0').should('not.be.checked');
    cy.get('#regexCheckbox1').should('be.checked');
  });

  it('check Invert option and Reset Option', () => {
    cy.loadAndVisitProject('food.small');
    cy.columnActionClick('Shrt_Desc', ['Text filter']);
    cy.get('.input-container > input').should('be.visible').type('Cheese');
    cy.get(
      '#facet-0 > div.facet-title.ui-sortable-handle > div > table > tbody > tr > td:nth-child(3) > a:nth-child(2)'
    )
      .should('be.visible')
      .click();
    cy.get('#summary-bar > span').should(
      'have.text',
      '134 matching rows (199 total)'
    );

    cy.get(
      '#facet-0 > div.facet-title.ui-sortable-handle > div > table > tbody > tr > td:nth-child(3) > a:nth-child(1)'
    )
      .should('be.visible')
      .click();

    cy.get('.input-container > input').should('have.value', '');
  });
});
