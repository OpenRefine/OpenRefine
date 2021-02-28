describe(__filename, function () {
  it('Ensures navigation works from project-preview page', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

    cy.navigateTo('Language Settings');
    cy.get('tbody').should('to.contain', 'Select preferred language');

    cy.navigateTo('Import Project');
    cy.get('tbody').should(
      'to.contain',
      'Locate an existing Refine project file (.tar or .tar.gz)'
    );

    cy.navigateTo('Create Project');
    cy.get('.create-project-ui-panel').should(
      'to.contain',
      'Configure Parsing Options'
    );
  });

  it('Ensures the working of Start-Over Button', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').should(
      'to.contain',
      'Configure Parsing Options'
    );
    cy.get('button[bind="startOverButton"]').click();

    cy.get('#or-create-question').should(
      'to.contain',
      'Create a project by importing data. What kinds of data files can I import?'
    );
  });

  it('Test project renaming', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
    cy.get(
      '.default-importing-wizard-header input[bind="projectNameInput"]'
    ).type('this is a test');
    cy.doCreateProjectThroughUserInterface();

    cy.get('#project-name-button').contains('this is a test');
  });

  it('Tests Parsing Options related to column seperation', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');

    cy.get('[type="radio"]').check('tab');
    // wait condiition is required as their is ajax reload in the project-preview table
    cy.wait(1000); // eslint-disable-line

    cy.contains('1.')
      .parent()
      .parent()
      .within(() => {
        cy.get('td').eq(0).contains('1.');
        cy.get('td').eq(1).contains('01001","BUTTER,WITH SALT","15.87","717');
      });

    cy.get('input[bind="columnSeparatorInput"]').type('{backspace};');
    cy.get('[type="radio"]').check('custom');
    // wait condiition is required as their is ajax reload in the project-preview table
    cy.wait(1000); // eslint-disable-line

    cy.contains('1.')
      .parent()
      .parent()
      .within(() => {
        cy.get('td').eq(0).contains('1.');
        cy.get('td').eq(1).contains('01001","BUTTER,WITH SALT","15.87","717');
      });

    cy.get('[type="radio"]').check('comma');
    // wait condiition is required as their is ajax reload in the project-preview table
    cy.wait(1000); // eslint-disable-line

    cy.contains('1.')
      .parent()
      .parent()
      .within(() => {
        cy.get('td').eq(0).contains('1.');
        cy.get('td').eq(1).contains('01001');
        cy.get('td').eq(2).contains('BUTTER,WITH SALT');
        cy.get('td').eq(3).contains('15.87');
        cy.get('td').eq(4).contains('717');
      });

    cy.get('input[bind="columnNamesCheckbox"]').check();
    // wait condiition is required as their is ajax reload in the project-preview table
    cy.wait(1000); // eslint-disable-line
    cy.contains('1.')
      .parent()
      .parent()
      .within(() => {
        cy.get('td').eq(0).contains('1.');
        cy.get('td').eq(1).contains('NDB_No');
        cy.get('td').eq(2).contains('Shrt_Desc');
        cy.get('td').eq(3).contains('Water');
        cy.get('td').eq(4).contains('Energ_Kcal');
      });
  });
  it('Test project tagging by adding various tags', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
    const uniqueProjectName = Date.now();
    const uniqueTagName1 = 'tag1_' + Date.now();
    const uniqueTagName2 = 'tag2_' + Date.now();

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
  it('Tests ignore-first of parsing options', function () {
    cy.visitOpenRefine();
    cy.createProjectThroughUserInterface('food.mini.csv');
    cy.get('.create-project-ui-panel').contains('Configure Parsing Options');
    cy.contains('1.')
      .parent()
      .parent()
      .within(() => {
        cy.get('td').eq(0).contains('1.');
        cy.get('td').eq(1).contains('01001');
        cy.get('td').eq(2).contains('BUTTER,WITH SALT');
        cy.get('td').eq(3).contains('15.87');
        cy.get('td').eq(4).contains('717');
      });

    cy.get('input[bind="ignoreInput"]').type('{backspace}1');
    cy.get('input[bind="ignoreCheckbox"]').check();
    cy.wait(1000); // eslint-disable-line
    cy.contains('1.')
      .parent()
      .parent()
      .within(() => {
        cy.get('td').eq(0).contains('1.');
        cy.get('td').eq(1).contains('01002');
        cy.get('td').eq(2).contains('BUTTER,WHIPPED,WITH SALT');
        cy.get('td').eq(3).contains('15.87');
        cy.get('td').eq(4).contains('717');
      });
  });
});
