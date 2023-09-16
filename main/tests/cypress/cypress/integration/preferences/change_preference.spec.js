describe(__filename, function () {
  const testPreferenceName = 'PreferenceName_Test';
  const testPreferenceValue = '"PreferenceValue_Test"';
  
  afterEach(function () {
    cy.deletePreference(testPreferenceName);
  });

  it('Add a new preference', function () {
    cy.visitOpenRefine();
    cy.get('#project-links a').contains('Preferences').click();

    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns(testPreferenceName);
      cy.get('table.preferences tr:last-child button.button').click();
    });

    cy.get('table.preferences tr:nth-last-child(2)').contains(
        testPreferenceName
    );
  });
  
  it('Edit a preference', function () {
    cy.visitOpenRefine();

    cy.setPreference(testPreferenceName, testPreferenceValue);

    cy.get('#project-links a').contains('Preferences').click();
    cy.get('table.preferences tr').contains(testPreferenceName);

    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns(testPreferenceValue + '_Edited');
      cy.get('table.preferences tr')
        .contains(testPreferenceName)
        .parent()
        .find('td:last-child button:first-child')
        .click();
    });

    cy.get('table.preferences tr').contains(testPreferenceValue + '_Edited');
  });
});
