function cleanupWikibases() {
  cy.get('#extension-bar-menu-container').contains('Wikidata').click();
  cy.get('.menu-container a').contains('Select Wikibase instance').click();
  cy.get(
    '.wikibase-dialog table .wikibase-dialog-selector-remove'
  ).each(($el) => cy.wrap($el).click());
  cy.get('.dialog-container .wikibase-dialog button').contains('Close').click();
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

    //click add
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
    cleanupWikibases();

    cy.get('#extension-bar-menu-container').contains('Wikidata').click();
    cy.get('.menu-container a').contains('Select Wikibase instance').click();

    cy.get('.dialog-container .wikibase-dialog button')
      .contains('Add Wikibase')
      .click();

    // ad a manifest
    cy.get('.add-wikibase-dialog input[bind="manifestURLInput"]').type(
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
    cleanupWikibases();

    cy.get('#extension-bar-menu-container').contains('Wikidata').click();
    cy.get('.menu-container a').contains('Select Wikibase instance').click();

    cy.get('.dialog-container .wikibase-dialog button')
      .contains('Add Wikibase')
      .click();

    // ad a manifest
    const manifest = {
      version: '1.0',
      mediawiki: {
        name: 'Cypress Testing Wikibase Test',
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
    cy.get('.add-wikibase-dialog textarea').type(JSON.stringify(manifest), {
      parseSpecialCharSequences: false,
      delay: 0,
      waitForAnimations: false,
    });
    cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();

    // ensure the new Wikibase is listed
    cy.get('.dialog-container .wikibase-dialog').should(
      'to.contain',
      'Cypress Testing Wikibase Test'
    );
  });

  it('Add a wikibase instance (Invalid manifest provided)', function () {
    cy.loadAndVisitProject('food.mini');
    cleanupWikibases();

    cy.get('#extension-bar-menu-container').contains('Wikidata').click();
    cy.get('.menu-container a').contains('Select Wikibase instance').click();

    cy.get('.dialog-container .wikibase-dialog button')
      .contains('Add Wikibase')
      .click();

    cy.get('.add-wikibase-dialog textarea').type('thisIsAnInvalidManifest');
    cy.get('.add-wikibase-dialog button').contains('Add Wikibase').click();
    cy.get('.add-wikibase-dialog p.invalid-manifest')
      .should('be.visible')
      .contains('Invalid Wikibase manifest.');
  });

  it('Switch from one wikibase to another', function () {
    cy.loadAndVisitProject('food.mini');
    cleanupWikibases();
    cy.addWikibaseInstance(
      'https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/openrefine-wikibase-test-manifest.json'
    );

    cy.get('#extension-bar-menu-container').contains('Wikidata').click();
    cy.get('.menu-container a').contains('Select Wikibase instance').click();

    // check that wikidata is selected by default
    cy.get('.wikibase-dialog td').contains('Wikidata').click();
    cy.get('.wikibase-dialog p[bind="currentSelectedWikibase"] a').should(
      'to.contain',
      'Wikidata'
    );

    // switch to OpenRefine Wikibase Test
    cy.get('.wikibase-dialog td').contains('OpenRefine Wikibase Test').click();
    cy.get('.wikibase-dialog p[bind="currentSelectedWikibase"] a').should(
      'to.contain',
      'OpenRefine Wikibase Test'
    );

    // switch back to Wikidata
    cy.get('.wikibase-dialog td').contains('Wikidata').click();
    cy.get('.wikibase-dialog p[bind="currentSelectedWikibase"] a').should(
      'to.contain',
      'Wikidata'
    );
  });

  it('Remove wikibase', function () {
    cy.loadAndVisitProject('food.mini');
    cleanupWikibases();
    cy.addWikibaseInstance(
      'https://raw.githubusercontent.com/OpenRefine/wikibase-manifests/master/openrefine-wikibase-test-manifest.json'
    );

    cy.get('#extension-bar-menu-container').contains('Wikidata').click();
    cy.get('.menu-container a').contains('Select Wikibase instance').click();

    // Click on the little cross to remove the test wikibase and ensure it's been removed
    cy.get('.wikibase-dialog td')
      .contains('OpenRefine Wikibase Test')
      .parent()
      .find('.wikibase-dialog-selector-remove')
      .click();

    cy.get('.wikibase-dialog').should(
      'not.to.contain',
      'OpenRefine Wikibase Test'
    );

    //wikidata should still be there
    cy.get('.wikibase-dialog').should('to.contain', 'Wikidata');
  });
});
