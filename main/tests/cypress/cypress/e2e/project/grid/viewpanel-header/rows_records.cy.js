describe(__filename, function () {
  afterEach(() => {
    cy.addProjectForDeletion();
  });

  it('ensures rows and records display same in csv file', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('span[bind="modeSelectors"]').contains('records').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);

    cy.get('body[ajax_in_progress="false"]');

    cy.get('span[bind="modeSelectors"]').contains('rows').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);
  });
  it('ensures rows and records are different for 3-level json file', function () {
    const projectName = Date.now();
    cy.fixture('donut-records').then((jsonValue) => {
      cy.loadAndVisitSampleJSONProject(projectName, jsonValue);
    });
    cy.get('span[bind="modeSelectors"]').contains('records').click();
    // check the labels explicitly, since the records mode notice also mentions "records"
    cy.get('span[bind="modeSelectors"] a.selected').should('contain', 'records');
    cy.get('#summary-bar').should('contain', 'records');
    cy.get('.viewpanel-pagesize').should('contain', 'records');
    cy.get('tr td:nth-child(3)').then((recordNumber) => {
      for (let i = 1; i <= 3; i++) {
        expect(recordNumber.text()).to.contain(i);
      }
    });

    cy.get('span[bind="modeSelectors"]').contains('row').click();
    cy.get('span:contains("rows")').should('length', 3);
    cy.get('tr td:nth-child(3)').then((rowNumber) => {
      for (let i = 1; i <= 10; i++) {
        expect(rowNumber.text()).to.contain(i);
      }
    });
  });
});

describe('records mode notice', function () {
  const preferenceName = 'ui.browsing.recordsModeNotice.dismissed';
  const notice = '.viewpanel-records-notice';
  let savedValue = null;

  const switchToRecords = () => {
    // clicking the already-selected mode is a no-op, so this works whatever the initial mode is
    cy.get('span[bind="modeSelectors"]').contains('records').click();
    cy.get('span[bind="modeSelectors"] a.selected').should('contain', 'records');
  };

  const loadJSONProject = () => {
    cy.fixture('donut-records').then((jsonValue) => {
      cy.loadAndVisitSampleJSONProject(Date.now(), jsonValue);
    });
  };

  before(() => {
    // save the workspace value so that it can be restored after these tests
    cy.request(`${Cypress.expose('OPENREFINE_URL')}/command/core/get-preference?name=${preferenceName}`).then(
      (response) => {
        savedValue = response.body.value;
      }
    );
  });

  beforeEach(() => {
    cy.deletePreference(preferenceName);
  });

  afterEach(() => {
    cy.addProjectForDeletion();
  });

  after(() => {
    if (savedValue === undefined || savedValue === null) {
      cy.deletePreference(preferenceName);
    } else {
      cy.setPreference(preferenceName, encodeURIComponent(savedValue));
    }
  });

  it('shows the notice with a docs link in records mode', function () {
    loadJSONProject();
    switchToRecords();

    cy.get(notice).should('be.visible').should('contain', 'records mode');
    cy.get(`${notice} a.viewpanel-records-notice-link`)
      .should('have.attr', 'href', 'https://openrefine.org/docs/manual/exploring#rows-vs-records')
      .should('have.attr', 'target', '_blank');
    cy.get(`${notice} a.viewpanel-records-notice-dismiss`).should('have.attr', 'title', "Don't show again");
  });

  it('hides the notice in rows mode and shows it again in records mode', function () {
    loadJSONProject();
    switchToRecords();
    cy.get(notice).should('be.visible');

    cy.get('span[bind="modeSelectors"]').contains('rows').click();
    cy.get('span[bind="modeSelectors"] a.selected').should('contain', 'rows');
    cy.get(notice).should('not.exist');

    switchToRecords();
    cy.get(notice).should('be.visible');
  });

  it('shows the notice when the user switches a rows-mode project to records', function () {
    cy.loadAndVisitProject('food.small');
    cy.get('span[bind="modeSelectors"] a.selected').should('contain', 'rows');
    cy.get(notice).should('not.exist');

    switchToRecords();
    cy.get(notice).should('be.visible');
  });

  it('keeps the notice dismissed after a reload once closed', function () {
    loadJSONProject();
    switchToRecords();

    cy.get(`${notice} a.viewpanel-records-notice-dismiss`).click();
    cy.get(notice).should('not.exist');

    cy.reload();
    cy.waitForProjectTable();
    switchToRecords();
    cy.get(notice).should('not.exist');
  });

  it('stays hidden after dismissing, without a reload', function () {
    loadJSONProject();
    switchToRecords();

    cy.get(`${notice} a.viewpanel-records-notice-dismiss`).click();
    cy.get(notice).should('not.exist');

    cy.get('span[bind="modeSelectors"]').contains('rows').click();
    cy.get('span[bind="modeSelectors"] a.selected').should('contain', 'rows');
    switchToRecords();
    cy.get(notice).should('not.exist');
  });

  it('stays dismissed in other projects', function () {
    cy.setPreference(preferenceName, 'true');
    loadJSONProject();
    switchToRecords();
    cy.get(notice).should('not.exist');
  });

  it('shows only one notice after the grid redraws', function () {
    loadJSONProject();
    switchToRecords();
    cy.get(notice).should('have.length', 1);

    // pick a page size that is not currently selected, so that the grid redraws
    cy.get('.viewpanel-pagesize a.action')
      .first()
      .invoke('text')
      .then((pageSize) => {
        cy.get('.viewpanel-pagesize a.action').first().click();
        cy.get('.viewpanel-pagesize a.selected').should('have.text', pageSize);
      });
    cy.get(notice).should('have.length', 1);
  });
});
