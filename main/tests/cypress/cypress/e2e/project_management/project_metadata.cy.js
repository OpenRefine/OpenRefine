describe(__filename, function () {
  it('Ensures project-metadata dialogue loads', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.get('h1').contains('Project metadata');
    cy.get(
      'body > .dialog-container > .dialog-frame .dialog-footer button[bind="closeButton"]'
    ).click();
    cy.get('body > .dialog-container > .dialog-frame').should('not.exist');
  });
  it('Ensures project-metadata has correct details', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.get('#metadata-body > div > table > tr:nth-child(4) > td:nth-child(1)').contains('Project name');
    cy.get('#metadata-body > div > table > tr:nth-child(4) > td:nth-child(2)').contains(projectName);
  });
  it('Ensures project-metadata can be edit project name', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.env('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('testProject');
      },
    });
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.contains('td', 'Project name')
      .siblings()
      .contains('button', 'Edit')
      .click();
    cy.get('#metadata-body > div > table > tr:nth-child(4) > td:nth-child(1)').contains('Project name');
    cy.get('#metadata-body > div > table > tr:nth-child(4) > td:nth-child(2)').contains('testProject');
  });
  it('Ensures project-metadata can be edit tags', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.env('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('tagTest');
      },
    });
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.contains('td', 'Tags').siblings().contains('button', 'Edit').click();
    cy.get('#metadata-body > div > table > tr:nth-child(5) > td:nth-child(1)').contains('Tags');
    cy.get('#metadata-body > div > table > tr:nth-child(5) > td:nth-child(2)').contains('tagTest');
  });
  it('Ensures project-metadata can be edit creator', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.env('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('testCreator');
      },
    });
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.contains('td', 'Creator').siblings().contains('button', 'Edit').click();
    cy.get('#metadata-body > div > table > tr:nth-child(6) > td:nth-child(1)').contains('Creator');
    cy.get('#metadata-body > div > table > tr:nth-child(6) > td:nth-child(2)').contains('testCreator');
  });
  it('Ensures project-metadata can be edit contributors', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.env('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('testcontributor');
      },
    });
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.contains('td', 'Contributors')
      .siblings()
      .contains('button', 'Edit')
      .click();
    cy.get('#metadata-body > div > table > tr:nth-child(7) > td:nth-child(1)').contains('Contributors');
    cy.get('#metadata-body > div > table > tr:nth-child(7) > td:nth-child(2)').contains('testcontributor');
  });
  it('Ensures project-metadata can be edit subject', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.env('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('testSubject');
      },
    });
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.contains('td', 'Subject').siblings().contains('button', 'Edit').click();
    cy.get('#metadata-body > div > table > tr:nth-child(8) > td:nth-child(1)').contains('Subject');
    cy.get('#metadata-body > div > table > tr:nth-child(8) > td:nth-child(2)').contains('testSubject');
  });
  it('Ensures project-metadata can be edit license', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.env('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('GPL-3');
      },
    });
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.contains('td', 'License').siblings().contains('button', 'Edit').click();
    cy.get('#metadata-body > div > table > tr:nth-child(13) > td:nth-child(1)').contains('License');
    cy.get('#metadata-body > div > table > tr:nth-child(13) > td:nth-child(2)').contains('GPL-3');
  });
  it('Ensures project-metadata can be edit homepage', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.env('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('openrefine.org');
      },
    });
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.contains('td', 'Homepage')
      .siblings()
      .contains('button', 'Edit')
      .click();
    cy.get('#metadata-body > div > table > tr:nth-child(14) > td:nth-child(1)').contains('Homepage');
    cy.get('#metadata-body > div > table > tr:nth-child(14) > td:nth-child(2)').contains('openrefine.org');
  });
});
