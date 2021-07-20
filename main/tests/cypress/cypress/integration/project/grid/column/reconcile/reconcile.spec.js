// import 'fixture-species';

const fixture = [
  ['record_id', 'date', 'location', 'species'],
  ['1', '2017-06-23', 'Maryland', 'Hypsibius dujardini'],
  ['2', '2018-06-09', 'South Carolina', 'Protula bispiralis'],
  ['3', '2018-06-09', 'West Virginia', 'Monstera deliciosa'],
  ['15', '2018-09-06', 'West Virginia', 'Bos taurus'],
  ['16', '2017-10-05', 'Maryland', 'Amandinea devilliersiana'],
  ['24', '2015-05-01', 'West Virginia', 'Faciolela oxyrynca'],
];

/**
 * Utility method used by several tests of this scenario
 * It adds the CSV reconciliation service in the interface
 */
const addReconciliationService = () => {
  cy.get('.recon-dialog-service-list', { log: false }).then(($list) => {
    if ($list.find('.recon-dialog-service-selector').length > 0) {
      // cy.get('.recon-dialog-service-selector-remove').click({ multiple: true });

      cy.get('.recon-dialog-service-selector', { log: false }).each(($btn) => {
        cy.get(
          '.recon-dialog-service-selector:first-child .recon-dialog-service-selector-remove',
          { log: false }
        ).click({ log: false });
      });
    }
  });
  // Checking that the list is empty ensure the deletion of all previous services is completed
  cy.get('.recon-dialog-service-list', { log: false }).should('be.empty');

  cy.get('.dialog-container button', { log: false })
    .contains('Add Standard Service...', { log: false })
    .click({ log: false });
  cy.get('.dialog-container:last-child input', {
    log: false,
  }).type('http://localhost:8000/reconcile', { log: false });

  cy.get('.dialog-container:last-child button', { log: false })
    .contains('Add Service', { log: false })
    .click({ log: false });

  // Checking that the list have only one service ensures that the services has been added
  cy.get('.recon-dialog-service-selector', { log: false }).should('have.length', 1);

  cy.get('.recon-dialog-service-selector:last-child').should(
    'to.contain',
    'CSV Reconciliation service'
  );

  cy.get('.recon-dialog-service-selector:last-child', { log: false }).click({
    log: false,
  });

  cy.get('.recon-dialog-service-list').should('to.have.css', 'display', 'none');
};

describe('Base reconciliation tests', () => {
  it('Load the reconciliation panel, test the layout', () => {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('species', ['Reconcile', 'Start reconciling']);
    addReconciliationService();
    cy.get('.recon-dialog-service-panel.recon-dialog-standard-service-panel').should('be.visible');
    cy.get('.dialog-header').should('to.contain', 'Reconcile column "species"');
    cy.get('.dialog-body').should(
      'to.contain',
      'Reconcile each cell to an entity of one of these types'
    );
    cy.get('.dialog-body').should(
      'to.contain',
      'Reconcile each cell to an entity of one of these types'
    );
    cy.get('.dialog-body .recon-dialog-service-panel').should(
      'to.contain',
      'CSV-recon'
    );
  });

  it('Reconcile with automatch disabled', () => {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('species', ['Reconcile', 'Start reconciling']);
    addReconciliationService();

    cy.get('.recon-dialog-service-panel.recon-dialog-standard-service-panel').should('be.visible');

    cy.get('.dialog-container span')
      .contains('Auto-match')
      .siblings('input')
      .uncheck();
    cy.get('.dialog-container button').contains('Start Reconciling...').click();
    cy.assertNotificationContainingText('Reconcile cells in column species');
    cy.assertColumnIsReconciled('species');

    // "Choose new match" appear when there is a match, if it's not there it means nothing is matched
    cy.get('table.data-table td .data-table-cell-content').should(
      'not.to.contain',
      'Choose new match'
    );
  });

  it('Reconcile with automatch enabled', () => {
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('species', ['Reconcile', 'Start reconciling']);
    addReconciliationService();
    cy.get('.recon-dialog-service-panel.recon-dialog-standard-service-panel').should('be.visible');

    cy.get('.dialog-container span')
      .contains('Auto-match')
      .siblings('input')
      .check();
    cy.get('.dialog-container button').contains('Start Reconciling...').click();
    cy.assertNotificationContainingText('Reconcile cells in column species');
    cy.assertColumnIsReconciled('species');

    // 4 rows should have been automatched
    cy.get(
      'table.data-table td .data-table-cell-content:contains("Choose new match")'
    ).should('have.length', 4);
  });

  it('Max number of candidates', () => {
    const fixture = [
      ['record_id', 'date', 'location', 'species'],
      ['15', '2018-09-06', 'West Virginia', 'Bos taurus'],
    ];
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('species', ['Reconcile', 'Start reconciling']);
    addReconciliationService();
    cy.get('.recon-dialog-service-panel.recon-dialog-standard-service-panel').should('be.visible');
    cy.get('.dialog-container input[bind="maxCandidates"]').type(2);
    cy.get('.dialog-container button').contains('Start Reconciling...').click();
    cy.assertColumnIsReconciled('species');
    cy.get('.data-table-cell-content .data-table-recon-topic').should(
      'have.length',
      2
    );
  });

  it('Test reconciliation, in the grid', () => {
    cy.loadAndVisitProject(fixture);
    cy.reconcileColumn('species');
    cy.assertColumnIsReconciled('species');
    // Test 1, when there is a match
    // simple assertion to ensure the content of the iframe is loaded for row 0 / species
    // (recon loads match details from the recon endpoint in an iframe)
    cy.getCell(0, 'species').within(() => {
      cy.get('a[href^="http://localhost:8000/"]').trigger('mouseover');
      cy.get(
        'a[href^="http://localhost:8000/"] .data-table-topic-popup'
      ).should('to.exist');

      cy.get('iframe').its('0.contentDocument').should('exist');
    });

    // Test 2, when there are candidates
    // ensure the first one loads an iframe on hover
    cy.getCell(3, 'species').within(() => {
      cy.get(
        '.data-table-recon-candidate:first-child .data-table-recon-topic'
      ).trigger('mouseover');
      cy.get(
        '.data-table-recon-candidate:first-child .data-table-topic-popup'
      ).should('to.exist');

      cy.get(
        '.data-table-recon-candidate:first-child .data-table-topic-popup iframe'
      )
        .its('0.contentDocument')
        .should('exist');

      // verify the three buttons inside the popup
      cy.get(
        '.data-table-recon-candidate:first-child .data-table-topic-popup'
      ).within(() => {
        cy.get('button').contains('Match this Cell').should('to.exist');
        cy.get('button')
          .contains('Match All Identical Cells')
          .should('to.exist');
        cy.get('button').contains('Cancel').should('to.exist');
      });
    });
  });

  it('Match this cell', () => {
    const fixture = [
      ['record_id', 'date', 'location', 'species'],
      ['15', '2018-09-06', 'West Virginia', 'Bos taurus'],
      ['3', '2018-06-09', 'West Virginia', 'Monstera deliciosa'],
    ];

    cy.loadAndVisitProject(fixture);
    cy.reconcileColumn('species');
    cy.assertColumnIsReconciled('species');

    // over on a candidate (Lineus longissimus)
    cy.getCell(0, 'species')
      .find('a')
      .contains('Lineus longissimus')
      .trigger('mouseover');

    // click on match
    cy.getCell(0, 'species').find('button').contains('Match this Cell').click();

    // check notification
    cy.assertNotificationContainingText('Match Lineus longissimus');

    // check that candidates have disappeared, means matched
    cy.getCell(0, 'species')
      .find('.data-table-recon-candidates')
      .should('not.to.exist');
  });

  it('Match All identical cell', () => {
    const fixture = [
      ['record_id', 'date', 'location', 'species'],
      ['15', '2018-09-06', 'West Virginia', 'Bos taurus'],
      ['16', '2018-09-06', 'West Virginia', 'Bos taurus'],
    ];
    cy.loadAndVisitProject(fixture);
    cy.reconcileColumn('species');
    cy.assertColumnIsReconciled('species');

    // ensure both rows have candidates
    cy.getCell(0, 'species')
      .find('.data-table-recon-candidate')
      .should('have.length', 6);

    cy.getCell(1, 'species')
      .find('.data-table-recon-candidate')
      .should('have.length', 6);

    // over on a candidate (Lineus longissimus)
    cy.getCell(0, 'species')
      .find('a')
      .contains('Lineus longissimus')
      .trigger('mouseover');

    // click on match all
    cy.getCell(0, 'species')
      .find('button')
      .contains('Match All Identical Cells')
      .click();

    // check notification
    cy.assertNotificationContainingText(
      'Match item Lineus longissimus (2508430) for 2 cells'
    );

    // check that candidates have disappeared on both rows, means matched all
    cy.getCell(0, 'species')
      .find('.data-table-recon-candidates')
      .should('not.to.exist');

    cy.getCell(1, 'species')
      .find('.data-table-recon-candidates')
      .should('not.to.exist');
  });
});
