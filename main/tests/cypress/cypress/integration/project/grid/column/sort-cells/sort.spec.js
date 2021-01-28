describe(__filename, function () {
    it('Perform a text sort', function () {
        cy.loadAndVisitProject('food.sort')

        // sort and confirm
        cy.columnActionClick('Shrt_Desc', ['Sort'])
        cy.waitForDialogPanel()
        cy.confirmDialogPanel()

        // ensure sorting is active
        cy.getCell(0, 'Shrt_Desc').should(
            'to.contain',
            'BUTTER,WHIPPED,WITH SALT'
        )
        cy.getCell(1, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT')
        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('By Shrt_Desc').trigger('mouseover')
        cy.get('.menu-item').contains('Reverse').click()
        cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT')
        cy.getCell(1, 'Shrt_Desc').should(
            'to.contain',
            'BUTTER,WHIPPED,WITH SALT'
        )
        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('Reorder rows permanently').click()
        cy.reload()
        cy.getCell(0, 'Shrt_Desc').should('to.contain', 'BUTTER,WITH SALT')
        cy.getCell(1, 'Shrt_Desc').should(
            'to.contain',
            'BUTTER,WHIPPED,WITH SALT'
        )
    })
    it('Perform a number sort and change sort by viewpanel header', function () {
        cy.loadAndVisitProject('food.sort')

        // sort and confirm
        cy.columnActionClick('NDB_No', [
            'Edit cells',
            'Common transforms',
            'To number',
        ])
        cy.columnActionClick('NDB_No', ['Sort'])

        cy.waitForDialogPanel()
        cy.get('[type="radio"]').check('number')
        cy.get('[type="radio"]').check('reverse')
        cy.confirmDialogPanel()

        // ensure sorting is active
        cy.getCell(0, 'NDB_No').should('to.contain', 1002)
        cy.getCell(1, 'NDB_No').should('to.contain', 1001)

        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('By NDB_No').trigger('mouseover')
        cy.get('.menu-item').contains('Reverse').click()
        cy.getCell(0, 'NDB_No').should('to.contain', 1001)
        cy.getCell(1, 'NDB_No').should('to.contain', 1002)
        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('Reorder rows permanently').click()
        cy.reload()
        cy.getCell(0, 'NDB_No').should('to.contain', 1001)
        cy.getCell(1, 'NDB_No').should('to.contain', 1002)
    })
    it('Perform a date sort and change sort by viewpanel header', function () {
        cy.loadAndVisitProject('food.sort')

        // sort and confirm
        cy.columnActionClick('Date', [
            'Edit cells',
            'Common transforms',
            'To date',
        ])
        cy.columnActionClick('Date', ['Sort'])

        cy.waitForDialogPanel()
        cy.get('[type="radio"]').check('date')
        cy.get('[type="radio"]').check('reverse')
        cy.confirmDialogPanel()

        // ensure sorting is active
        cy.getCell(0, 'Date').should('to.contain', '2020-12-17T00:00:00Z')
        cy.getCell(1, 'Date').should('to.contain', '2020-08-17T00:00:00Z')

        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('By Date').trigger('mouseover')
        cy.get('.menu-item').contains('Reverse').click()
        cy.getCell(0, 'Date').should('to.contain', '2020-08-17T00:00:00Z')
        cy.getCell(1, 'Date').should('to.contain', '2020-12-17T00:00:00Z')

        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('Reorder rows permanently').click()
        cy.reload()
        cy.getCell(0, 'Date').should('to.contain', '2020-08-17T00:00:00Z')
        cy.getCell(1, 'Date').should('to.contain', '2020-12-17T00:00:00Z')
    })
    it('Perform a bool sort, and converts text into bool to check boolean sort', function () {
        cy.loadAndVisitProject('food.sort')

        // // sort and confirm
        // cy.columnActionClick('Fat', ['Sort'])
        // cy.waitForDialogPanel()
        // cy.confirmDialogPanel()

        // need to first convert each column into boolean one by one

        cy.getCell(0, 'Fat')
            .trigger('mouseover')
            .within(() => {
                cy.get('a.data-table-cell-edit').click()
            })
        cy.get('select').select('boolean')
        cy.get('button').contains(new RegExp('Apply', 'g')).click()

        cy.getCell(1, 'Fat')
            .trigger('mouseover')
            .within(() => {
                cy.get('a.data-table-cell-edit').click()
            })
        cy.get('select').select('boolean')
        cy.get('button').contains(new RegExp('Apply', 'g')).click()

        cy.columnActionClick('Fat', ['Sort'])

        cy.waitForDialogPanel()
        cy.get('[type="radio"]').check('boolean')
        // cy.get('[type="radio"]').check('reverse')
        cy.confirmDialogPanel()

        // ensure sorting is active
        cy.getCell(0, 'Fat').should('to.contain', 'false')
        cy.getCell(1, 'Fat').should('to.contain', 'true')
        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('By Fat').trigger('mouseover')
        cy.get('.menu-item').contains('Reverse').click()
        cy.getCell(0, 'Fat').should('to.contain', 'true')
        cy.getCell(1, 'Fat').should('to.contain', 'false')
        cy.get('.viewpanel-sorting a').contains('Sort').click()
        cy.get('.menu-container').contains('Reorder rows permanently').click()
        cy.reload()
        cy.getCell(0, 'Fat').should('to.contain', 'true')
        cy.getCell(1, 'Fat').should('to.contain', 'false')
    })
})
