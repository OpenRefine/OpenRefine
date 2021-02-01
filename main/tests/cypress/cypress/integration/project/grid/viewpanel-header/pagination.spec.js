describe(__filename, function () {
    it('Ensure it shows correct number of rows', function () {
        cy.loadAndVisitProject('food.small')

        cy.get('#summary-bar').should('to.contain','199 rows')
    })
    it('Ensure it shows only 5 rows when pagesize is 5', function () {
        cy.loadAndVisitProject('food.small')

        cy.get('.viewpanel-pagesize').find('a').contains('5').click()
        cy.get('.data-table tbody').find('tr').should('have.length', 5)
    })
    it('Ensure it shows only 10 rows when pagesize is 10', function () {
        cy.loadAndVisitProject('food.small')

        cy.get('.viewpanel-pagesize').find('a').contains('10').click()
        cy.get('.data-table tbody').find('tr').should('have.length', 10)

    })
    it('Ensure it shows only 25 rows when pagesize is 25', function () {
        cy.loadAndVisitProject('food.small')

        cy.get('.viewpanel-pagesize').find('a').contains('25').click()
        cy.get('.data-table tbody').find('tr').should('have.length', 25)

    })
    it('Ensure it shows only 50 rows when pagesize is 50', function () {
        cy.loadAndVisitProject('food.small')

        cy.get('.viewpanel-pagesize').find('a').contains('50').click()
        cy.get('.data-table tbody').find('tr').should('have.length', 50)

    })
})