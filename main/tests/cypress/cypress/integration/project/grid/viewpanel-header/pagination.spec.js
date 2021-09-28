describe(__filename, function () {
  it('Ensure it shows correct number of rows', function () {
    cy.loadAndVisitProject('food.small');
    cy.get('#summary-bar').should('to.contain', '199 rows');
  });

  it('Change pagination for and check the rows', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-header').should('be.visible');
    cy.get('.data-table-container').should('be.visible');
    cy.wait(250); // eslint-disable-line
    cy.get('.data-table tbody').find('tr').should('have.length', 50);

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('.viewpanel-header').should('be.visible');
    cy.get('.data-table-container').should('be.visible');
    cy.wait(250); // eslint-disable-line
    cy.get('.data-table tbody').find('tr').should('have.length', 25);
  });

  it('Test the "next" button', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-header').should('be.visible');
    cy.get('.data-table-container').should('be.visible');
    cy.wait(250); // eslint-disable-line
    cy.get('.viewpanel-paging').find('a').contains('next').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CREAM,FLUID,LT WHIPPING');
    cy.assertCellEquals(49, 'Shrt_Desc', 'MILK SHAKES,THICK VANILLA');
  });

  it('Test the "previous" button', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-header').should('be.visible');
    cy.get('.data-table-container').should('be.visible');
    cy.wait(250); // eslint-disable-line
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

  it('Test the "last" button', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('10').click();
    cy.get('.viewpanel-header').should('be.visible');
    cy.get('.data-table-container').should('be.visible');
    cy.wait(250); // eslint-disable-line
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 20);
    cy.assertCellEquals(0, 'Shrt_Desc', 'SPICES,BASIL,DRIED');
    cy.assertCellEquals(8, 'Shrt_Desc', 'CLOVES,GROUND');
  });

  it('Test the "first" button', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('25').click();
    cy.get('.viewpanel-header').should('be.visible');
    cy.get('.data-table-container').should('be.visible');
    cy.wait(250); // eslint-disable-line
    cy.get('.viewpanel-paging').find('a').contains('last').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 8);
    cy.get('.viewpanel-paging').find('a').contains('first').click();
    cy.get('#viewpanel-paging-current-input').should('have.value', 1);
    cy.assertCellEquals(0, 'Shrt_Desc', 'BUTTER,WITH SALT');
    cy.assertCellEquals(24, 'Shrt_Desc', 'CHEESE,MONTEREY');
  });

  it('Test entering an arbitrary page number', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('.viewpanel-pagesize').find('a').contains('50').click();
    cy.get('.viewpanel-header').should('be.visible');
    cy.get('.data-table-container').should('be.visible');
    cy.wait(250); // eslint-disable-line
    cy.get('#viewpanel-paging-current-input').type('{backspace}2{enter}');
    cy.get('#viewpanel-paging-current-input').should('have.value', 2);
    cy.assertCellEquals(0, 'Shrt_Desc', 'CREAM,FLUID,LT WHIPPING');
    cy.assertCellEquals(49, 'Shrt_Desc', 'MILK SHAKES,THICK VANILLA');
  });
});
