describe(__filename, () => {
    afterEach(() => {
        cy.addProjectForDeletion();
    });
    
    it('Match a single cell to a candidate', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-with-identical-cells.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

        // Match the first one (and only the first one)
        cy.getCell(0, 'city').find('.data-table-recon-match').first().click();

        cy.assertNotificationContainingText(
            'Match Paris (Q90) to single cell on row 1, column city, containing "Paris"'
        );

        // ensure the first cell contains 'Choose new match'
        // which means it is matched
        cy.getCell(0, 'city').should('to.contain', 'Choose new match');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');
    });

    it('Match identical cells to a candidate', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-with-identical-cells.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

        // Match the first one and any other similar one, such as the second one
        cy.getCell(0, 'city').find('.data-table-recon-match-similar').first().click();

        cy.assertNotificationContainingText(
            'Match item Paris (Q90) for cells containing "Paris" in column city'
        );

        // ensure the first and second cells contains 'Choose new match'
        // which means they are matched
        cy.getCell(0, 'city').should('to.contain', 'Choose new match');
        cy.getCell(1, 'city').should('to.contain', 'Choose new match');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');
    });

    it('Match a single cell to a new item', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-with-identical-cells.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

        // Match the first one (and only the first one) to a new item
        cy.getCell(0, 'city').find('.data-table-recon-match').last().click();

        cy.assertNotificationContainingText(
            'Mark to create new item for single cell on row 1, column city, containing "Paris"'
        );

        // ensure the first cell contains 'Choose new match'
        // which means it is matched (and check there is a 'new' label next to it)
        cy.getCell(0, 'city').should('to.contain', 'Choose new match');
        cy.getCell(0, 'city').find('.data-table-recon-new').should('to.exist');

        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');
    });

    it('Match multiple cells to a new item', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-with-identical-cells.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

        // Match the first one (and only the first one) to a new item
        cy.getCell(0, 'city').find('.data-table-recon-match-similar').last().click();

        cy.assertNotificationContainingText(
            'Mark to create one single new item for all cells containing "Paris" in column city'
        );

        // ensure the first and second cells contain 'Choose new match'
        // which means they are matched (and check there is a 'new' label next to it)
        cy.getCell(0, 'city').should('to.contain', 'Choose new match');
        cy.getCell(0, 'city').find('.data-table-recon-new').should('to.exist');
        cy.getCell(1, 'city').should('to.contain', 'Choose new match');
        cy.getCell(1, 'city').find('.data-table-recon-new').should('to.exist');

        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');
    });

    it('Use dialog to match a single cell to a new item', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-with-identical-cells.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

        // Open the match dialog
        cy.getCell(0, 'city').find('.data-table-recon-extra a').click();
        // Select the option to only affect this cell
        cy.get('input[name="cell-recon-search-for-match-mode"]').last().click();
        // Click the "New item" button
        cy.get('.dialog-footer button[bind="newButton"]').click();

        cy.assertNotificationContainingText(
            'Mark to create new item for single cell on row 1, column city, containing "Paris"'
        );

        // ensure the first cell contains 'Choose new match'
        // which means it is matched (and check there is a 'new' label next to it)
        cy.getCell(0, 'city').should('to.contain', 'Choose new match');
        cy.getCell(0, 'city').find('.data-table-recon-new').should('to.exist');

        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');
    });

    it('Use dialog to match similar cells to a new item', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-with-identical-cells.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

        // Open the match dialog
        cy.getCell(0, 'city').find('.data-table-recon-extra a').click();
        // Select the option to affect all similar cells
        cy.get('input[name="cell-recon-search-for-match-mode"]').first().click();
        // Click the "New item" button
        cy.get('.dialog-footer button[bind="newButton"]').click();

        cy.assertNotificationContainingText(
            'Mark to create one single new item for all cells containing "Paris" in column city'
        );

        // ensure the first and second cells contain 'Choose new match'
        // which means they are matched (and check there is a 'new' label next to it)
        cy.getCell(0, 'city').should('to.contain', 'Choose new match');
        cy.getCell(0, 'city').find('.data-table-recon-new').should('to.exist');
        cy.getCell(1, 'city').should('to.contain', 'Choose new match');
        cy.getCell(1, 'city').find('.data-table-recon-new').should('to.exist');

        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

    });

    it('Use dialog to clear similar cells', () => {
        cy.visitOpenRefine();
        cy.navigateTo('Import project');
        cy.get('.grid-layout').should('to.contain', 'Locate an existing Refine project file');

        cy.get('#project-tar-file-input').attachFile('reconciled-project-with-identical-cells.zip')
        cy.get('#import-project-button').click();

        // before matching, ensure we have no matches, and candidates
        cy.getCell(0, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidate').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidate').should('to.exist');

        // Open the match dialog
        cy.getCell(0, 'city').find('.data-table-recon-extra a').click();
        // Select the option to affect all similar cells
        cy.get('input[name="cell-recon-search-for-match-mode"]').first().click();
        // Click the "Don't reconcile cell" button
        cy.get('.dialog-footer button[bind="clearButton"]').click();

        cy.assertNotificationContainingText(
            'Clear recon data for cells containing "Paris" in column city'
        );

        // ensure the first and second cells have been cleared of recon data
        cy.getCell(0, 'city').find('.data-table-recon-candidates').should('to.not.exist');
        cy.getCell(1, 'city').find('.data-table-recon-candidates').should('to.not.exist');
        cy.getCell(2, 'city').find('.data-table-recon-candidates').should('to.exist');
        cy.getCell(3, 'city').find('.data-table-recon-candidates').should('to.exist');

    });

});


