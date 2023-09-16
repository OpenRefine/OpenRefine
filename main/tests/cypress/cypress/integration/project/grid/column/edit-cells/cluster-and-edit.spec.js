const fixture = [
    ['location'],
    ['SWABYS HOME'],
    ['SWABYS   HOME'],
    ['BALLARDS river'],
    ['BALLARDS River'],
    ['MOUNT ZION'],
    ['GRAVEL HILL'],
];

describe(__filename, function () {
    it('Test Layout, Popup & Close', function () {
        cy.loadAndVisitProject(fixture);
        cy.columnActionClick('location', ['Edit cells', 'Cluster and edit']);

        cy.get('.dialog-container').within(() => {
            cy.get('.dialog-header').contains('Cluster and edit column "location"');
            cy.get('.dialog-body').contains(
                'Find groups of different cell values'
            );
            cy.get('.dialog-footer').contains('Export clusters');
        });

        cy.get('.dialog-container .dialog-footer button[bind="closeButton"]')
            .contains('Close')
            .click();
        cy.get('.dialog-container').should('not.exist');
    });

    it('Test the footer buttons, Select/Unselect All', function () {
        cy.loadAndVisitProject(fixture);
        cy.columnActionClick('location', ['Edit cells', 'Cluster and edit']);

        cy.get('.dialog-container').within(() => {
            cy.get('.dialog-footer button[bind="selectAllButton"]').click();
            cy.get('.clustering-dialog-entry-table input[type="checkbox"]').should(
                'be.checked'
            );

            cy.get('.dialog-footer button[bind="deselectAllButton"]').click();
            cy.get('.clustering-dialog-entry-table input[type="checkbox"]').should(
                'not.be.checked'
            );
        });
    });

    it('Test the different clustering options for rendering and expected inputs', function () {
        cy.loadAndVisitProject(fixture);
        cy.columnActionClick('location', ['Edit cells', 'Cluster and edit']);

        // test the rendering of the main content when selecting different clustering methods
        // the following code simply iterate over every clustering method (dropdow) and functions (ngram, levenshtein ...) to ensure the rendering is not broken
        const methods = [
            {
                methodName: 'Key collision',
                selector: 'keyingFunctionSelector',
                functions: [
                    'Fingerprint',
                    'ngram-fingerprint',
                    'metaphone3',
                    'cologne-phonetic',
                    'Daitch-Mokotoff',
                    'Beider-Morse',
                ],
            },
            {
                methodName: 'Nearest neighbor',
                selector: 'distanceFunctionSelector',
                functions: ['levenshtein', 'ppm'],
            },
        ];

        cy.get('.dialog-container').within(() => {
            cy.get('.clustering-dialog-entry-table')
                .should('to.exist')
                .should('be.visible');

            // select a method
            for (const method of methods) {
                console.log(method);
                cy.get('.clustering-dialog-entry-table')
                    .should('to.exist')
                    .should('be.visible');
                cy.get(`.dialog-body select[bind="methodSelector"]`).select(
                    method['methodName']
                );

                // select each function
                for (const functionName of method['functions']) {
                    cy.get(`select[bind="${method['selector']}"]`).select(functionName);
                    cy.get('.clustering-dialog-entry-table')
                        .should('to.exist')
                        .should('be.visible');
                    cy.get('.clustering-dialog-entry-table').should('be.visible');

                    // OR is adding &nbsp; for spaces, so we can't test BALLARDS River
                    // This assertion on text BALLARDS simply ensure the content has been rendered\
                    cy.get('.clustering-dialog-entry-table').should(
                        'to.contain',
                        'BALLARDS'
                    );
                }
            }
        });
    });

    it('Merge Select & Close', function () {
        cy.loadAndVisitProject(fixture);
        cy.columnActionClick('location', ['Edit cells', 'Cluster and edit']);

        cy.get('.dialog-container').within(() => {
            // check lines to be merged
            cy.get(
                '.clustering-dialog-entry-table tr.odd input[type="checkbox"]'
            ).check();

            // enter a new cell value
            cy.get('.clustering-dialog-entry-table tr.odd input[type="text"]').type(
                'testing'
            );

            cy.get('.dialog-footer button[bind="applyCloseButton"]').click();
        });

        cy.assertGridEquals([
            ['location'],
            ['SWABYS HOME'],
            ['SWABYS   HOME'],
            ['BALLARDS Rivertesting'],
            ['BALLARDS Rivertesting'],
            ['MOUNT ZION'],
            ['GRAVEL HILL'],
        ]);
    });

    it('Merge Select & Re Cluster', function () {
        cy.loadAndVisitProject(fixture);
        cy.columnActionClick('location', ['Edit cells', 'Cluster and edit']);

        cy.get('.dialog-container').within(() => {
            // Merge BALLARDS RIVER
            cy.get(
                '.clustering-dialog-entry-table tr.odd input[type="checkbox"]'
            ).check();

            // enter a new cell value
            cy.get('.clustering-dialog-entry-table tr.odd input[type="text"]').type(
                ' TESTING A'
            );

            cy.get('.dialog-footer button[bind="applyReClusterButton"]').click();
            cy.get('#resultSummaryId').contains('1 cluster found');

            // Re Cluster

            // Merge Swabbys
            cy.get(
                '.clustering-dialog-entry-table tr.odd input[type="checkbox"]'
            ).check();

            // enter a new cell value
            cy.get('.clustering-dialog-entry-table tr.odd input[type="text"]').type(
                ' TESTING B'
            );

            cy.get('.dialog-footer button[bind="applyReClusterButton"]').click();

            // Close

            cy.get('.dialog-footer button[bind="closeButton"]')
                .contains('Close')
                .click();
        });

        cy.assertGridEquals([
            ['location'],
            ['SWABYS   HOME TESTING B'],
            ['SWABYS   HOME TESTING B'],
            ['BALLARDS River TESTING A'],
            ['BALLARDS River TESTING A'],
            ['MOUNT ZION'],
            ['GRAVEL HILL'],
        ]);
    });

    // https://github.com/OpenRefine/OpenRefine/issues/4004
    it('Ensure duplicate whitespace are preserved, #4004', function () {
        const fixture = [['test'], ['foo'], ['b ar'], ['b  ar'], ['b   ar']];

        cy.loadAndVisitProject(fixture);
        cy.columnActionClick('test', ['Edit cells', 'Cluster and edit']);

        cy.get('.dialog-container').within(() => {
            cy.get('.clustering-dialog-entry-table tr td:nth-child(3)').should(
                'to.contain',
                'b ar'
            );
            cy.get('.clustering-dialog-entry-table tr td:nth-child(3)').should(
                'to.contain',
                'b  ar'
            );
            cy.get('.clustering-dialog-entry-table tr td:nth-child(3)').should(
                'to.contain',
                'b   ar'
            );
        });
    });
});
