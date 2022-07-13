describe(__filename, function () {
  it('Open an existing project by visiting the URL directly', function () {
    const projectName = Date.now();
    cy.loadProject('food.mini', projectName).then((projectId) => {
      cy.visitProject(projectId);
      cy.get('#project-name-button').contains(projectName);
    });
  });
});
