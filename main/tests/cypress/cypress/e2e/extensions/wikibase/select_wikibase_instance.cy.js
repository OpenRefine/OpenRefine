
/**
 * Delete all previously added Wikibase test instances
 * They are shared across project, therefore some cleanup is required to ensure a Wikibase instance doesn't come from another test
 */
describe(__filename, function () {
    const WIKIBASE_TEST_NAME = 'OpenRefine Wikibase Cypress Test';
    const WIKIBASE_TEST_NAME2 = 'OpenRefine Wikibase Test';

    let savedValue;

    beforeEach(() => {
        getPreference('wikibase.manifests');
    });

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
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click()
            .then( () => resetWikibases(savedValue));
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
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click()
            .then( () => resetWikibases(savedValue))
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
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click()
            .then( () => resetWikibases(savedValue))
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
        cy.get('.wikibase-dialog .dialog-footer button').contains('OK').click()
            .then( () => resetWikibases(savedValue));
    });
    function getPreference(name) {
        return cy.request({
            method: 'GET',
            url: `http://127.0.0.1:3333/command/core/get-preference?name=${name}`,
        })
            .then((response) => {
                savedValue = JSON.parse(response.body.value);
                savedValue = savedValue.filter(object => {
                    return object.mediawiki.name !== WIKIBASE_TEST_NAME
                        && object.mediawiki.name !== WIKIBASE_TEST_NAME2;
                });
            });
    }
    function setPreference(name, value) {
        cy.request(
            {
                method: 'GET',
                url: 'http://127.0.0.1:3333/command/core/get-csrf-token'
            }
        ).then( (response) => {
            cy.expect(response).to.not.be.null;
            let token = response.body['token'];
            cy.expect(token).to.not.be.null;
            cy.request({
                method: 'POST',
                url: `http://127.0.0.1:3333/command/core/set-preference?name=${name}`,
                form: true,
                body: {
                    value: JSON.stringify(savedValue),
                    csrf_token: token
                },
            }).then((response) => {
                expect(response.body).to.deep.equal({ code: 'ok' });
            });
        });
    }

    function resetWikibases(savedValue) {
        setPreference('wikibase.manifests', savedValue);
    }
});
