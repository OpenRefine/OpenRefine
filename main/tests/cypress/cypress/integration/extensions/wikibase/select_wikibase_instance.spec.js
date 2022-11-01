/**
 * Delete all previously added Wikibase test instances
 * They are shared across project, therefore some cleanup is required to ensure a Wikibase instance doesn't come from another test
 */
function cleanupWikibases() {
    cy.get('#extension-bar-menu-container').contains('Wikibase').click();
    cy.get('.menu-container a').contains('Manage Wikibase instances').click();

    cy.get(
        'div.wikibase-dialog ol.wikibase-list > li'
    ).each(($el) => {
        if ($el.text().includes('OpenRefine Wikibase Cypress Test')) {
            cy.wrap($el).contains('Delete').click();
        }
        if ($el.text().includes('OpenRefine Wikibase Test')) {
            cy.wrap($el).contains('Delete').click();
        }
    });
    cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click();
}
describe(__filename, function () {
    it('Add a wikibase instance, general navigation', function () {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase instances').click();

        // check dialog and header
        cy.get('.wikibase-dialog').should('to.exist');
        cy.get('.wikibase-dialog .dialog-header').should(
            'to.contain',
            'Manage Wikibase instances'
        );

        // click add
        cy.get('.wikibase-dialog .dialog-footer button')
            .contains('Add Wikibase')
            .click();

        // check panel
        cy.get('.add-wikibase-dialog .dialog-header').should(
            'to.contain',
            'Add Wikibase manifest'
        );
    });

    it('Add a wikibase instance (URL)', function () {
        cy.loadAndVisitProject('food.mini');

        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase instances').click();

        cy.get('.wikibase-dialog .dialog-footer button')
            .contains('Add Wikibase')
            .click();

        // add a manifest
        cy.get('.add-wikibase-dialog input[bind="manifestURLInput"]').invoke(
            'val',
            'https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/openrefine-wikibase-test-manifest.json'
        );
        cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();

        // ensure the new Wikibase is listed
        cy.get('.wikibase-dialog').should(
            'to.contain',
            'OpenRefine Wikibase Test'
        );
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click();
        cleanupWikibases();
    });

    it('Add a wikibase instance (JSON Manifest copy-pasted in the textarea)', function () {
        cy.loadAndVisitProject('food.mini');

        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase instances').click();

        cy.get('.wikibase-dialog .dialog-footer button')
            .contains('Add Wikibase')
            .click();

        // add a manifest
        const manifest = {
            version: '1.0',
            mediawiki: {
                name: 'OpenRefine Wikibase Cypress Test',
                root: 'https://or-wikibase-test.wiki.opencura.com/wiki/',
                main_page: 'https://or-wikibase-test.wiki.opencura.com/wiki/Main_Page',
                api: 'https://or-wikibase-test.wiki.opencura.com/w/api.php',
            },
            wikibase: {
                site_iri: 'http://or-wikibase-test.wiki.opencura.com/entity/',
                maxlag: 5,
                properties: {
                    instance_of: 'P1',
                    subclass_of: 'P2',
                },
            },
            oauth: {
                registration_page:
                    'https://or-wikibase-test.wiki.opencura.com/wiki/Special:OAuthConsumerRegistration/propose',
            },
            reconciliation: {
                endpoint: 'https://or-wikibase-test.reconci.link/${lang}/api',
            },
        };
        cy.get('.add-wikibase-dialog textarea').invoke(
            'val',
            JSON.stringify(manifest)
        );
        cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();

        // ensure the new Wikibase is listed
        cy.get('.wikibase-dialog').should(
            'to.contain',
            'OpenRefine Wikibase Cypress Test'
        );
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click();
        cleanupWikibases();
    });

    it('Add a wikibase instance (Invalid manifest provided)', function () {
        cy.loadAndVisitProject('food.mini');

        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase instances').click();

        cy.get('.wikibase-dialog .dialog-footer button')
            .contains('Add Wikibase')
            .click();

        cy.get('.add-wikibase-dialog textarea').type('This is an invalid manifest');
        cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();
        cy.get('.add-wikibase-dialog p.invalid-manifest')
            .should('be.visible')
            .contains('SyntaxError: Unexpected token \'T\', "This is an"... is not valid JSON');
        cy.get('.add-wikibase-dialog .dialog-footer button').contains('Cancel').click();
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click();
        cleanupWikibases();
    });

    it('Delete wikibase', function () {
        cy.loadAndVisitProject('food.mini');
        cy.addWikibaseInstance(
            'https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/openrefine-wikibase-test-manifest.json'
        );

        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase instances').click();

        cy.get('.wikibase-dialog li')
            .contains('OpenRefine Wikibase Test')
            .parents('li')
            .find('.wikibase-dialog-selector-delete')
            .click();

        cy.get('.wikibase-dialog').should(
            'not.to.contain',
            'OpenRefine Wikibase Test'
        );

        cy.get('.wikibase-dialog').should('to.contain', 'Wikibase');
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click();
        cleanupWikibases();
    });
});
