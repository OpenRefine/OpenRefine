// add test for SchemaAlignment wikibase schema
describe('SchemaAlignment.setUpTabs', () => {
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
  it('should populate manifest schema templates without switching Wikibase', () => {
    const commonsSiteIri = 'https://commons.wikimedia.org/entity/';
    const manifestTemplateName = 'Information (basic data for every Wikimedia Commons file)';
    const commonsSchema = JSON.stringify({
      entityEdits: [],
      siteIri: commonsSiteIri,
      mediaWikiApiEndpoint: 'https://commons.wikimedia.org/w/api.php',
    });

    let savedTemplates;
    cy.request(Cypress.env('OPENREFINE_URL') + '/command/core/get-preference?name=wikibase.templates').then(
      (response) => {
        savedTemplates = response.body.value;
      }
    );

    cy.setPreference('wikibase.templates', JSON.stringify([{}]));

    cy.loadProject('food.mini', Cypress.currentTest.title + '-' + Date.now()).then((projectId) => {
      cy.get('@token', { log: false }).then((token) => {
        cy.request({
          method: 'POST',
          url: `${Cypress.env('OPENREFINE_URL')}/command/wikidata/save-wikibase-schema?project=${projectId}&csrf_token=${token}`,
          form: true,
          body: { schema: commonsSchema },
        });
      });

      cy.visit(Cypress.env('OPENREFINE_URL') + '/project?project=' + projectId);
      cy.waitForProjectTable();

      cy.get('#extension-bar-menu-container').contains('Wikibase').click();
      cy.get('.menu-container a').contains('Edit Wikibase schema').click();

      cy.get('#wikibase-instance-selector').should('have.value', 'Wikimedia Commons');
      cy.get('#wikibase-template-select option').should('contain', manifestTemplateName);
    });

    cy.then(() => {
      if (savedTemplates === undefined || savedTemplates === null) {
        cy.deletePreference('wikibase.templates');
      } else {
        cy.setPreference('wikibase.templates', savedTemplates);
      }
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
