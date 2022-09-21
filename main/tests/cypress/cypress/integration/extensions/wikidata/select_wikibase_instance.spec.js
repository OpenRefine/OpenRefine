/**
 * Delete all previsouly added wikibase instances
 * They are shared across project, therefore some cleanup is required to ensure a wikibase instance doesn't come from another test
 */
function cleanupWikibases() {
    cy.get('#extension-bar-menu-container').contains('Wikidata').click();
    cy.get('.menu-container a').contains('Select Wikibase instance').click();
    cy.get(
        '.wikibase-dialog a:visible'
    ).each(($el) => cy.wrap($el).click());
    cy.get('div.dialog-container div.dialog-body ol > li > a:visible').contains('Delete').click();
}

describe(__filename, function () {
    it('Add a wikibase instance, general navigation', function () {
        //
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikidata').click();
        cy.get('.menu-container a').contains('Select Wikibase instance').click();

        // check dialog and header
        cy.get('.dialog-container .wikibase-dialog').should('to.exist');
        cy.get('.dialog-container .wikibase-dialog .dialog-header').should(
            'to.contain',
            'Select Wikibase instance'
        );

        // click add
        cy.get('.dialog-container .wikibase-dialog button')
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
        // cleanupWikibases();

        cy.get('#extension-bar-menu-container').contains('Wikidata').click();
        cy.get('.menu-container a').contains('Select Wikibase instance').click();

        cy.get('.dialog-container .wikibase-dialog button')
            .contains('Add Wikibase')
            .click();

        // ad a manifest
        cy.get('.add-wikibase-dialog input[bind="manifestURLInput"]').invoke(
            'val',
            'https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/openrefine-wikibase-test-manifest.json'
        );
        cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();

        // ensure the new Wikibase is listed
        cy.get('.dialog-container .wikibase-dialog').should(
            'to.contain',
            'OpenRefine Wikibase Test'
        );
    });

    it('Add a wikibase instance (JSON Manifest copy-pasted in the textarea)', function () {
        cy.loadAndVisitProject('food.mini');
        // cleanupWikibases();

        cy.get('#extension-bar-menu-container').contains('Wikidata').click();
        cy.get('.menu-container a').contains('Select Wikibase instance').click();

        cy.get('.dialog-container .wikibase-dialog button')
            .contains('Add Wikibase')
            .click();

        // add a manifest
        const manifest = {
            version: '1.0',
            mediawiki: {
                name: 'OpenRefine Wikibase Test',
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
        cy.get('.dialog-container .wikibase-dialog').should(
            'to.contain',
            'Cypress Testing Wikibase Test'
        );
    });

    it('Add a wikibase instance (Invalid manifest provided)', function () {
        cy.loadAndVisitProject('food.mini');
        // cleanupWikibases();

        cy.get('#extension-bar-menu-container').contains('Wikidata').click();
        cy.get('.menu-container a').contains('Select Wikibase instance').click();

        cy.get('.dialog-container .wikibase-dialog button')
            .contains('Add Wikibase')
            .click();

        cy.get('.add-wikibase-dialog textarea').type('This is an invalid manifest');
        cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();
        cy.get('.add-wikibase-dialog p.invalid-manifest')
            .should('be.visible')
            .contains('SyntaxError: Unexpected token \'T\', "This is an"... is not valid JSON');
    });

    it('Switch from one wikibase to another', function () {
        cy.loadAndVisitProject('food.mini');
        // cleanupWikibases();
        cy.addWikibaseInstance(
            'https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/openrefine-wikibase-test-manifest.json'
        );

        cy.get('#extension-bar-menu-container').contains('Wikidata').click();
        cy.get('.menu-container a').contains('Select Wikibase instance').click();

        // check that wikidata is selected by default
        cy.get('.wikibase-dialog li').contains('Wikidata').click();
        cy.get('.wikibase-dialog li').should(
            'to.contain',
            'active'
        );

        // switch to OpenRefine Wikibase Test
        cy.get('.wikibase-dialog li').contains('OpenRefine Wikibase Test').click();
        cy.get('.wikibase-dialog li').should(
            'to.contain',
            'active'
        );

        // switch back to Wikidata
        cy.get('.wikibase-dialog li').contains('Wikidata').click();
        cy.get('.wikibase-dialog li').should(
            'to.contain',
            'active'
        );
    });

    it('Remove wikibase', function () {
        cy.loadAndVisitProject('food.mini');
        // cleanupWikibases();
        cy.addWikibaseInstance(
            'https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/openrefine-wikibase-test-manifest.json'
        );

        cy.get('#extension-bar-menu-container').contains('Wikidata').click();
        cy.get('.menu-container a').contains('Select Wikibase instance').click();

        // Click on the little cross to remove the test wikibase and ensure it's been removed
        cy.get('.wikibase-dialog li')
            .contains('OpenRefine Wikibase Test')
            .parent()
            .parent()
            .find('.wikibase-dialog-selector-delete')
            .click();

        cy.get('.wikibase-dialog').should(
            'not.to.contain',
            'OpenRefine Wikibase Test'
        );

        // wikidata should still be there
        cy.get('.wikibase-dialog').should('to.contain', 'Wikidata');
    });
});
