describe(__filename, function () {
  const openProjectList = function () {
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
  };

  const selectorFor = function (projectName) {
    return cy.contains('td', projectName).siblings().find('input.project-selector');
  };

  it('The bulk action bar stays hidden until a project is selected', function () {
    const projectName = 'bulk-hidden-' + Date.now();
    cy.loadProject('food.mini', projectName);
    openProjectList();

    cy.get('#projects-bulk-actions').should('not.be.visible');
    selectorFor(projectName).check();
    cy.get('#projects-bulk-actions').should('be.visible');
    cy.get('#projects-selection-count').should('contain', '1');

    selectorFor(projectName).uncheck();
    cy.get('#projects-bulk-actions').should('not.be.visible');
  });

  it('Deletes every selected project and leaves the others alone', function () {
    const suffix = Date.now();
    const doomedA = 'bulk-doomed-a-' + suffix;
    const doomedB = 'bulk-doomed-b-' + suffix;
    const survivor = 'bulk-survivor-' + suffix;
    cy.loadProject('food.mini', doomedA);
    cy.loadProject('food.mini', doomedB);
    cy.loadProject('food.mini', survivor);
    openProjectList();

    selectorFor(doomedA).check();
    selectorFor(doomedB).check();
    cy.get('#projects-selection-count').should('contain', '2');
    cy.get('#delete-selected-projects').click();

    cy.get('#projects-list').should('not.contain', doomedA);
    cy.get('#projects-list').should('not.contain', doomedB);
    cy.get('#projects-list').should('contain', survivor);
    cy.get('#projects-bulk-actions').should('not.be.visible');

    cy.request('GET', Cypress.expose('OPENREFINE_URL') + '/command/core/get-all-project-metadata').then((response) => {
      const responseText = JSON.stringify(response.body);
      expect(responseText).to.not.have.string(doomedA);
      expect(responseText).to.not.have.string(doomedB);
      expect(responseText).to.have.string(survivor);
    });
  });

  it('The header checkbox selects and clears every listed project', function () {
    const suffix = Date.now();
    const first = 'bulk-all-a-' + suffix;
    const second = 'bulk-all-b-' + suffix;
    cy.loadProject('food.mini', first);
    cy.loadProject('food.mini', second);
    openProjectList();

    cy.get('#select-all-projects').check();
    cy.get('#tableBody input.project-selector:visible').each(($el) => {
      cy.wrap($el).should('be.checked');
    });

    cy.get('#select-all-projects').uncheck();
    cy.get('#projects-bulk-actions').should('not.be.visible');
  });

  it('The header checkbox reflects whether every listed project is selected', function () {
    const suffix = Date.now();
    const tag = 'bulktag' + suffix;
    const tagged = 'bulk-tagged-' + suffix;
    const untagged = 'bulk-untagged-' + suffix;
    cy.loadProject('food.mini', tagged, tag);
    cy.loadProject('food.mini', untagged);
    openProjectList();

    // One of several listed projects: the header checkbox must stay clear.
    selectorFor(tagged).check();
    cy.get('#select-all-projects').should('not.be.checked');

    // Restrict the list to the tag. That project is now the whole list, so the header
    // checkbox must tick itself - and the one it hid must have left the selection.
    cy.get('#tagsUl').contains(tag).click();
    selectorFor(tagged).check();
    cy.get('#select-all-projects').should('be.checked');
    cy.get('#projects-selection-count').should('contain', '1');
  });

  it('Filtering the list clears the selection of the projects it hides', function () {
    const suffix = Date.now();
    const tag = 'bulktag' + suffix;
    const tagged = 'bulk-filter-tagged-' + suffix;
    const hidden = 'bulk-filter-hidden-' + suffix;
    cy.loadProject('food.mini', tagged, tag);
    cy.loadProject('food.mini', hidden);
    openProjectList();

    selectorFor(hidden).check();
    cy.get('#projects-selection-count').should('contain', '1');

    // Restricting the list to the tag hides the selected project, which must drop out of the
    // selection: a bulk delete may never remove a project the user cannot see.
    cy.get('#tagsUl').contains(tag).click();
    cy.get('#projects-bulk-actions').should('not.be.visible');

    cy.get('#tagsUl').contains('All').click();
    cy.get('#tableBody input.project-selector:checked').should('have.length', 0);
    cy.request('GET', Cypress.expose('OPENREFINE_URL') + '/command/core/get-all-project-metadata').then((response) => {
      expect(JSON.stringify(response.body)).to.have.string(hidden);
    });
  });

  it('Editing metadata still writes into the right cells of the project row', function () {
    // The row is refreshed by cell index, so the selector column shifts every one of these.
    // The existing metadata tests only assert the dialog, never the row behind it.
    const projectName = 'bulk-refresh-' + Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visit(Cypress.expose('OPENREFINE_URL'), {
      onBeforeLoad(win) {
        cy.stub(win, 'prompt').returns('refreshedValue');
      },
    });
    cy.navigateTo('Open project');

    cy.contains('td', projectName).siblings().contains('a', 'About').click();
    cy.get('#metadata-body').contains('td', 'Project name').siblings().contains('button', 'Edit').click();
    cy.get('#metadata-body').contains('td', 'Creator').siblings().contains('button', 'Edit').click();
    cy.get('body > .dialog-container > .dialog-frame .dialog-footer button[bind="closeButton"]').click();

    cy.contains('#projects-list tr', 'refreshedValue').within(function () {
      cy.get('a.project-name').should('have.text', 'refreshedValue');
      cy.get('td').eq(6).should('contain', 'refreshedValue');
    });
  });
});
