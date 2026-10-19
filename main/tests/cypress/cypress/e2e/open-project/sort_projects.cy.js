describe(__filename, function () {
  beforeEach(function () {
    cy.loadProject('food.mini', 'Project A');
    cy.loadProject('food.mini', 'Project B');
    cy.loadProject('food.mini', 'Project C');
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.get('#projects-list table').should('be.visible');
  });

  it('Sort by Modified date - descending (default)', function () {
    // Initial state should be sorted by Modified descending (newest first)
    cy.get('#projects-list thead th[data-sort="date"]').should('have.attr', 'aria-sort', 'descending');
  });

  it('Sort by Modified date - toggles asc/desc (no default state)', function () {
    cy.get('#projects-list thead th[data-sort="date"]').as('dateHeader');
    cy.get('@dateHeader').should('have.attr', 'aria-sort', 'descending');

    // 1st click: asc
    cy.get('@dateHeader').click();
    cy.get('@dateHeader').should('have.attr', 'aria-sort', 'ascending');

    // 2nd click: desc
    cy.get('@dateHeader').click();
    cy.get('@dateHeader').should('have.attr', 'aria-sort', 'descending');

    // 3rd click: asc again (Modified date only toggles asc/desc, no null state)
    cy.get('@dateHeader').click();
    cy.get('@dateHeader').should('have.attr', 'aria-sort', 'ascending');

    // 4th click: desc again
    cy.get('@dateHeader').click();
    cy.get('@dateHeader').should('have.attr', 'aria-sort', 'descending');
  });

  it('Sort by Name - tri-state cycle (default → asc → desc → default)', function () {
    cy.get('#projects-list thead th[data-sort="text"]').eq(0).as('nameHeader');
    // Name starts with no sort (default is Modified date)
    cy.get('@nameHeader').should('not.have.attr', 'aria-sort');

    // 1st click: asc
    cy.get('@nameHeader').click();
    cy.get('@nameHeader').should('have.attr', 'aria-sort', 'ascending');

    // 2nd click: desc
    cy.get('@nameHeader').click();
    cy.get('@nameHeader').should('have.attr', 'aria-sort', 'descending');

    // 3rd click: default (reverts to Modified date desc)
    cy.get('@nameHeader').click();
    cy.get('@nameHeader').should('not.have.attr', 'aria-sort');
    cy.get('#projects-list thead th[data-sort="date"]').should('have.attr', 'aria-sort', 'descending');
  });

  it('Sort by Row Count - tri-state cycle (default → asc → desc → default)', function () {
    cy.get('#projects-list thead th[data-sort="number"]').as('rowCountHeader');
    cy.get('@rowCountHeader').should('not.have.attr', 'aria-sort');

    // 1st click: asc
    cy.get('@rowCountHeader').click();
    cy.get('@rowCountHeader').should('have.attr', 'aria-sort', 'ascending');

    // 2nd click: desc
    cy.get('@rowCountHeader').click();
    cy.get('@rowCountHeader').should('have.attr', 'aria-sort', 'descending');

    // 3rd click: default (reverts to Modified date desc)
    cy.get('@rowCountHeader').click();
    cy.get('@rowCountHeader').should('not.have.attr', 'aria-sort');
    cy.get('#projects-list thead th[data-sort="date"]').should('have.attr', 'aria-sort', 'descending');
  });

  it('Sort by Modified date orders rows correctly', function () {
    const getTestProjectDates = () =>
      cy
        .get('#projects-list tbody tr td:nth-child(4) .last-modified')
        .then(($els) => $els.map((i, el) => Cypress.$(el).text()).get())
        .then((dates) => {
          // Get corresponding project names to filter test projects
          return cy
            .get('#projects-list tbody tr td:nth-child(5) a.project-name')
            .then(($names) => $names.map((i, el) => Cypress.$(el).text()).get())
            .then((names) => dates.filter((_, i) => names[i].startsWith('Project ')));
        });

    // First click sorts ascending (oldest first)
    cy.get('#projects-list thead th[data-sort="date"]').click();
    getTestProjectDates().then((dates1) => {
      const sortedAsc = [...dates1].sort((a, b) => new Date(a) - new Date(b));
      expect(dates1).to.deep.equal(sortedAsc);
    });

    // Second click sorts descending (newest first)
    cy.get('#projects-list thead th[data-sort="date"]').click();
    getTestProjectDates().then((dates2) => {
      const sortedDesc = [...dates2].sort((a, b) => new Date(b) - new Date(a));
      expect(dates2).to.deep.equal(sortedDesc);
    });
  });

  it('Sort by Name orders rows alphabetically', function () {
    const getTestProjectNames = () =>
      cy
        .get('#projects-list tbody tr td:nth-child(5) a.project-name')
        .then(($els) => $els.map((i, el) => Cypress.$(el).text()).get())
        .then((names) => names.filter((n) => n.startsWith('Project ')));

    // Click Name header once (ascending A-Z)
    cy.get('#projects-list thead th[data-sort="text"]').eq(0).click();
    getTestProjectNames().then((names1) => {
      const sortedAsc = [...names1].sort((a, b) => a.localeCompare(b));
      expect(names1).to.deep.equal(sortedAsc);
    });

    // Click again (descending Z-A)
    cy.get('#projects-list thead th[data-sort="text"]').eq(0).click();
    getTestProjectNames().then((names2) => {
      const sortedDesc = [...names2].sort((a, b) => b.localeCompare(a));
      expect(names2).to.deep.equal(sortedDesc);
    });
  });

  it('Sort by Row Count orders rows numerically', function () {
    const getTestProjectRowCounts = () =>
      cy
        .get('#projects-list tbody tr td:nth-child(10)')
        .then(($els) => $els.map((i, el) => parseInt(Cypress.$(el).text().trim(), 10)).get())
        .then((counts) => {
          return cy
            .get('#projects-list tbody tr td:nth-child(5) a.project-name')
            .then(($names) => $names.map((i, el) => Cypress.$(el).text()).get())
            .then((names) => counts.filter((_, i) => names[i].startsWith('Project ')));
        });

    // Click Row Count header once (ascending)
    cy.get('#projects-list thead th[data-sort="number"]').click();
    getTestProjectRowCounts().then((counts1) => {
      const sortedAsc = [...counts1].sort((a, b) => a - b);
      expect(counts1).to.deep.equal(sortedAsc);
    });

    // Click again (descending)
    cy.get('#projects-list thead th[data-sort="number"]').click();
    getTestProjectRowCounts().then((counts2) => {
      const sortedDesc = [...counts2].sort((a, b) => b - a);
      expect(counts2).to.deep.equal(sortedDesc);
    });
  });

  it('Default sort (Modified date desc) restores correct row order after tri-state cycle', function () {
    const getModifiedDates = () =>
      cy
        .get('#projects-list tbody tr td:nth-child(4) .last-modified')
        .then(($els) => $els.map((i, el) => Cypress.$(el).text()).get());

    // Initial state: Modified date descending
    getModifiedDates().then((initialDates) => {
      const sortedDesc = [...initialDates].sort((a, b) => new Date(b) - new Date(a));
      expect(initialDates).to.deep.equal(sortedDesc);
    });

    // Click Name header 3 times to go through full cycle and return to default
    cy.get('#projects-list thead th[data-sort="text"]').eq(0).click(); // asc
    cy.get('#projects-list thead th[data-sort="text"]').eq(0).click(); // desc
    cy.get('#projects-list thead th[data-sort="text"]').eq(0).click(); // default

    // Should be back to Modified date descending
    getModifiedDates().then((finalDates) => {
      const sortedDesc = [...finalDates].sort((a, b) => new Date(b) - new Date(a));
      expect(finalDates).to.deep.equal(sortedDesc);
    });
  });
});
