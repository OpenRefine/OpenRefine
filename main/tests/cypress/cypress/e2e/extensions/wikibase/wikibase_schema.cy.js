// add test for SchemaAlignment wikibase schema
describe('SchemaAlignment.setUpTabs', () => {
  let savedTemplates;
  let savedManifests;
  let preferencesOverridden = false;

  function restorePreference(name, savedValue) {
    if (savedValue === undefined || savedValue === null) {
      cy.deletePreference(name);
    } else {
      const value = typeof savedValue === 'string' ? savedValue : JSON.stringify(savedValue);
      cy.setPreference(name, encodeURIComponent(value));
    }
  }

  afterEach(() => {
    if (!preferencesOverridden) {
      return;
    }
    preferencesOverridden = false;
    restorePreference('wikibase.templates', savedTemplates);
    restorePreference('wikibase.manifests', savedManifests);
  });

  it('should create tabs', () => {
    cy.loadAndVisitProject('food.mini');
    cy.get('#extension-bar-menu-container').contains('Wikibase').click();
    cy.get('.menu-container a').contains('Edit Wikibase schema').click();

    // Check tabs
    cy.get('.main-view-panel-tab-header').should('to.exist');
    cy.get('.main-view-panel-tab-header').should('to.have.length', 4);
    cy.get('#wikibase-schema-panel').should('to.exist');
    cy.get('#wikibase-issues-panel').should('to.exist');
    cy.get('#wikibase-preview-panel').should('to.exist');
    cy.get('.schema-alignment-total-warning-count').should('to.exist');
  });

  it('should show the correct number of warnings', () => {
    cy.loadAndVisitProject('food.mini');
    cy.get('#extension-bar-menu-container').contains('Wikibase').click();
    cy.get('.menu-container a').contains('Edit Wikibase schema').click();

    // Check warnings
    cy.get('.schema-alignment-total-warning-count').should('to.contain', '1');
  });

  // Regression for #6939: manifest schema_templates must appear when the schema UI
  // opens with a Wikibase already auto-selected from the saved schema site IRI.
  // Auto-select only runs when wikibase.manifests is non-empty, and that payload
  // must not replace the built-in Commons manifest (it holds schema_templates).
  it('should populate manifest schema templates without switching Wikibase', () => {
    const openRefineUrl = Cypress.expose('OPENREFINE_URL');
    const commonsSiteIri = 'https://commons.wikimedia.org/entity/';
    const manifestTemplateName = 'Information (basic data for every Wikimedia Commons file)';
    const commonsSchema = JSON.stringify({
      entityEdits: [],
      siteIri: commonsSiteIri,
      mediaWikiApiEndpoint: 'https://commons.wikimedia.org/w/api.php',
    });
    const dummyManifests = JSON.stringify([
      {
        mediawiki: { name: 'Cypress Dummy Wikibase' },
        wikibase: { site_iri: 'http://example.invalid/entity/' },
      },
    ]);

    cy.request(openRefineUrl + '/command/core/get-preference?name=wikibase.templates').then((response) => {
      savedTemplates = response.body.value;
    });
    cy.request(openRefineUrl + '/command/core/get-preference?name=wikibase.manifests').then((response) => {
      savedManifests = response.body.value;
      preferencesOverridden = true;
    });

    // Empty object clears saved templates so the dropdown cannot pass from preferences alone.
    cy.setPreference('wikibase.templates', encodeURIComponent(JSON.stringify([{}])));
    cy.setPreference('wikibase.manifests', encodeURIComponent(dummyManifests));

    cy.loadProject('food.mini', Cypress.currentTest.title + '-' + Date.now()).then((projectId) => {
      cy.get('@token', { log: false }).then((token) => {
        cy.request({
          method: 'POST',
          url: `${openRefineUrl}/command/wikidata/save-wikibase-schema?project=${projectId}&csrf_token=${token}`,
          form: true,
          body: { schema: commonsSchema },
        }).then(() => {
          cy.visit(openRefineUrl + '/project?project=' + projectId);
          cy.waitForProjectTable();
          // loadWikibases applies the site IRI only after the manifests preference returns.
          cy.window().should((win) => {
            expect(win.WikibaseManager.getSelectedWikibaseName()).to.equal('Wikimedia Commons');
          });

          cy.get('#extension-bar-menu-container').contains('Wikibase').click();
          cy.get('.menu-container a').contains('Edit Wikibase schema').click();

          cy.get('#wikibase-instance-selector').should('have.value', 'Wikimedia Commons');
          cy.contains('#wikibase-template-select option', manifestTemplateName).should('exist');
        });
      });
    });
  });

  // add 2 item in schema and check if issue count is updated
  it('should update the number of warnings', () => {
    cy.loadAndVisitProject('food.mini');
    cy.get('#extension-bar-menu-container').contains('Wikibase').click();
    cy.get('.menu-container a').contains('Edit Wikibase schema').click();

    // Check warnings
    cy.get('.schema-alignment-total-warning-count').should('to.contain', '1');

    cy.get('#wikibase-schema-panel').click();

    // Add 2 items
    cy.get('.wbs-toolbar button').click();
    cy.get('.wbs-toolbar button').click();

    // Check warnings
    cy.get('.schema-alignment-total-warning-count').should('to.contain', '2');
  });
});
