describe(__filename, function () {
  it('Test project naming', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
    cy.get(
      '.default-importing-wizard-header input[bind="projectNameInput"]'
    ).type('this is a test');
    cy.doCreateProjectThroughUserInterface();

    cy.get('#project-name-button').contains('this is a test');
  });

  it('Test project tagging', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
    const uniqueProjectName = Date.now();
    const uniqueTagName1 = 'tag1_' + Date.now();
    const uniqueTagName2 = 'tag2_' + Date.now();

    cy.get(
      '.default-importing-wizard-header input[bind="projectNameInput"]'
    ).type(uniqueProjectName);
    // triger the select input
    cy.get('#project-tags-container').click();
    // Type and Validate the tag, pressing enter
    cy.get('#project-tags-container .select2-input').type(uniqueTagName1);
    cy.get('body').type('{enter}');
    cy.get('#project-tags-container .select2-input').type(uniqueTagName2);
    cy.get('body').type('{enter}');
    cy.get('#or-import-parsopt').click();
    cy.doCreateProjectThroughUserInterface();

    cy.visitOpenRefine();
    cy.navigateTo('Open Project');
    cy.get('#projects-list')
      .contains(uniqueProjectName)
      .parent()
      .parent()
      .contains(uniqueTagName1);
    cy.get('#projects-list')
      .contains(uniqueProjectName)
      .parent()
      .parent()
      .contains(uniqueTagName2);
  });
});
