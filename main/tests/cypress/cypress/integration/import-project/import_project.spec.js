describe(__filename, function () {
  it('Check the layout for importing a project', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Import Project');
    cy.get('.grid-layout').contains('Locate an existing Refine project file');
  });

  it('Import a project', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Import Project');
    // make sure the dataset was loaded properly
    const projectFile = {
      filePath: 'food-small-csv.openrefine.tar.zip',
      mimeType: 'application/gzip',
    };
    cy.get('#project-upload-form input#project-tar-file-input').attachFile(
      projectFile
    );
    cy.get('#project-upload-form').submit();

    // ensure is loaded
    cy.get('div[bind="summaryBarDiv"]').contains('199 rows');
  });

  it('Import a project, test the renaming', function () {
    cy.visitOpenRefine();
    cy.navigateTo('Import Project');
    // make sure the dataset was loaded properly
    const projectFile = {
      filePath: 'food-small-csv.openrefine.tar.zip',
      mimeType: 'application/gzip',
    };
    cy.get('#project-upload-form input#project-tar-file-input').attachFile(
      projectFile
    );

    // rename
    const projectName = Date.now();
    cy.get('#project-name-input').type(projectName);
    cy.get('#project-upload-form').submit();

    // ensure is loaded
    cy.get('#project-name-button').contains(projectName);
  });
});
