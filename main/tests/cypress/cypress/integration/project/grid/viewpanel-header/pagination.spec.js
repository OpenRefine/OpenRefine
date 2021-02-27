describe(__filename, function () {
  it('Ensure it shows correct number of rows', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('#summary-bar').should('to.contain', '199 rows');
  });
  it('Ensure it shows only 5 rows when pagesize is 5', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('5').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 5);
  });
  it('Ensure it shows only 10 rows when pagesize is 10', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('10').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);
  });
  it('Ensure it shows only 25 rows when pagesize is 25', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 25);
  });
  it('Ensure it shows only 50 rows when pagesize is 50', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 50);
  });
  it('Ensure it redirects to next-page when pagesize is 5', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('5').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CHEESE,BRIE');
    cy.assertCellEquals(4, 'Shrt_Desc', 'CHEESE,CHESHIRE');
  });
  it('Ensure it redirects to next-page when pagesize is 10', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('10').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CHEESE,COLBY');
    cy.assertCellEquals(9, 'Shrt_Desc', 'CHEESE,FONTINA');
  });
  it('Ensure it redirects to next-page when pagesize is 25', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CHEESE,MOZZARELLA,WHL MILK');
    cy.assertCellEquals(
      24,
      'Shrt_Desc',
      'CREAM,FLUID,LT (COFFEE CRM OR TABLE CRM)'
    );
  });
  it('Ensure it redirects to next-page when pagesize is 50', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CREAM,FLUID,LT WHIPPING');
    cy.assertCellEquals(49, 'Shrt_Desc', 'MILK SHAKES,THICK VANILLA');
  });
  it('Ensure it redirects to previous-page when pagesize is 5', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('5').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();

    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.get('.viewpanel-paging').find('a').contains('previous').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(4, 'Shrt_Desc', 'CHEESE,BRICK');
  });
  it('Ensure it redirects to previous-page when pagesize is 10', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('10').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.get('.viewpanel-paging').find('a').contains('previous').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(9, 'Shrt_Desc', 'CHEESE,CHESHIRE');
  });
  it('Ensure it redirects to previous-page when pagesize is 25', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.get('.viewpanel-paging').find('a').contains('previous').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(24, 'Shrt_Desc', 'CHEESE,MONTEREY');
  });
  it('Ensure it redirects to previous-page when pagesize is 50', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.get('.viewpanel-paging').find('a').contains('previous').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(
      49,
      'Shrt_Desc',
      'CREAM,FLUID,LT (COFFEE CRM OR TABLE CRM)'
    );
  });
  it('Ensure last button redirects to last-page when pagesize is 5', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('5').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 40);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CHERVIL,DRIED');
    cy.assertCellEquals(3, 'Shrt_Desc', 'CLOVES,GROUND');
  });
  it('Ensure last button redirects to last-page when pagesize is 10', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('10').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 20);
    cy.assertCellEquals(0, 'Shrt_Desc', 'SPICES,BASIL,DRIED');
    cy.assertCellEquals(8, 'Shrt_Desc', 'CLOVES,GROUND');
  });
  it('Ensure last button redirects to last-page when pagesize is 25', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 8);
    cy.assertCellEquals(
      0,
      'Shrt_Desc',
      "KRAFT BREYERS LT N' LVLY LOWFAT STR'BERY YOGURT (1% MILKFAT)"
    );
    cy.assertCellEquals(23, 'Shrt_Desc', 'CLOVES,GROUND');
  });
  it('Ensure last button redirects to last-page when pagesize is 50', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 4);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CHEESE SAU,PREP FROM RECIPE');
    cy.assertCellEquals(48, 'Shrt_Desc', 'CLOVES,GROUND');
  });
  it('Ensure first button redirects to first-page when pagesize is 5', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('5').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 40);
    cy.get('.viewpanel-paging').find('a').contains('first').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(4, 'Shrt_Desc', 'CHEESE,BRICK');
  });
  it('Ensure first button redirects to first-page when pagesize is 10', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('10').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 20);
    cy.get('.viewpanel-paging').find('a').contains('first').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(9, 'Shrt_Desc', 'CHEESE,CHESHIRE');
  });
  it('Ensure first button redirects to first-page when pagesize is 25', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 8);
    cy.get('.viewpanel-paging').find('a').contains('first').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(24, 'Shrt_Desc', 'CHEESE,MONTEREY');
  });
  it('Ensure first button redirects to first-page when pagesize is 50', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 4);
    cy.get('.viewpanel-paging').find('a').contains('first').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(
      49,
      'Shrt_Desc',
      'CREAM,FLUID,LT (COFFEE CRM OR TABLE CRM)'
    );
  });
  it('Ensure page-number input redirects to correct-page when pagesize is 5', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('5').click();
    cy.get('#viewpanel-paging-current-input').type('{backspace}2{enter}');
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CHEESE,BRIE');
    cy.assertCellEquals(4, 'Shrt_Desc', 'CHEESE,CHESHIRE');
  });
  it('Ensure page-number input redirects to correct-page when pagesize is 10', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('10').click();
    cy.get('#viewpanel-paging-current-input').type('{backspace}2{enter}');
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CHEESE,COLBY');
    cy.assertCellEquals(9, 'Shrt_Desc', 'CHEESE,FONTINA');
  });
  it('Ensure page-number input redirects to correct-page when pagesize is 25', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('#viewpanel-paging-current-input').type('{backspace}2{enter}');
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);

    cy.assertCellEquals(0, 'Shrt_Desc', 'CHEESE,MOZZARELLA,WHL MILK');
    cy.assertCellEquals(
      24,
      'Shrt_Desc',
      'CREAM,FLUID,LT (COFFEE CRM OR TABLE CRM)'
    );
  });
  it('Ensure page-number input redirects to correct-page when pagesize is 50', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('#viewpanel-paging-current-input').type('{backspace}2{enter}');
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CREAM,FLUID,LT WHIPPING');
    cy.assertCellEquals(49, 'Shrt_Desc', 'MILK SHAKES,THICK VANILLA');
  });
});
