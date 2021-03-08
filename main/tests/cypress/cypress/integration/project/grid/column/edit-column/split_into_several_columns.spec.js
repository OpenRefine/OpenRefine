describe(__filename, function () {
    it('Ensures column is splitted by presence of comma', function () {
      cy.loadAndVisitProject('food.mini');

      cy.columnActionClick('Shrt_Desc', ['Edit column', 'Split into several columns...']);
      cy.waitForDialogPanel();

      cy.get('input[bind="separatorInput"]').type('{backspace},');

      cy.confirmDialogPanel();
      cy.get('body[ajax_in_progress="false"]');
      cy.assertNotificationContainingText(
        'Split 2 cell(s) in column Shrt_Desc into several columns by separator '
      );
 
      cy.get('.data-table-header').find('th').should('have.length', 7)
      cy.get('.data-table-header').find('th').should('to.contain', 'Shrt_Desc 2')
      cy.get('.data-table-header').find('th').should('to.contain', 'Shrt_Desc 3')
    });
});