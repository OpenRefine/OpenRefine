describe(__filename, function () {
  it('List an existing project, ensure a newly created project is displayed', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.get('#projects-list table').should('contain', projectName);
  });

  it('Visit a project from the Open project page, ensure link is working', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.get('#projects-list table').contains(projectName).click();
    cy.get('#project-name-button').should('contain', projectName);
  });
  it('Ensure projects are sorted on basis of names', function () {
    const projectName = 'projectA';
    const projectName2 = 'projectZ';
    let positionA;
    let positionZ;
    cy.loadProject('food.mini', projectName);
    cy.loadProject('food.mini', projectName2);
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.get('#projects-list table').contains('Name').click();
    // cy.get('#projects-list tbody>tr').eq(0).should('contain','projectA');
    // cy.get('#projects-list table').contains("Name").click();
    // cy.get('#projects-list tbody>tr').eq(0).should('contain', 'projectZ');
    // hack since all projects are not being deleted

    cy.get('a.project-name')
      .contains('projectA')
      .then((projectALink) => {
        cy.get('a.project-name')
          .contains('projectZ')
          .then((projectZLink) => {
            positionA = projectALink.parent().parent().index();
            positionZ = projectZLink.parent().parent().index();
            expect(positionA).to.be.lessThan(positionZ);
          });
      });
    cy.get('#projects-list table').contains('Name').click();
    cy.get('a.project-name')
      .contains('projectA')
      .then((projectALink) => {
        cy.get('a.project-name')
          .contains('projectZ')
          .then((projectZLink) => {
            positionA = projectALink.parent().parent().index();
            positionZ = projectZLink.parent().parent().index();
            expect(positionA).to.be.greaterThan(positionZ);
          });
      });
  });
  it('Ensure project is deleted from database as well as UI', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName);
    cy.visitOpenRefine();
    cy.navigateTo('Open project');
    cy.contains('td', projectName).siblings().find('.delete-project').click();
    cy.get('#projects-list').should('not.contain', projectName);
    cy.request(
      'GET',
      'http://127.0.0.1:3333/command/core/get-all-project-metadata'
    ).then((response) => {
      const responseText = JSON.stringify(response.body);
      expect(responseText).to.not.have.string(projectName);
    });
  });
});
