describe(__filename, function () {
  it('Edit a preference', function () {
    cy.visitOpenRefine();
    const testPreferenceName = `PreferenceName_${Date.now()}`;
    const testPreferenceValue = `"PreferenceValue_${Date.now()}"`;

    cy.setPreference(testPreferenceName, testPreferenceValue);

    cy.get('#project-links a').contains('Preferences').click();
    cy.get('table.preferences tr').contains(testPreferenceName);

    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns(testPreferenceValue + '_Edited');
      cy.get('table.preferences tr')
        .contains(testPreferenceName)
        .parentsUntil('tbody')
        .find('td:last-child button:first-child')
        .click();
    });

    cy.get('table.preferences tr').contains(testPreferenceValue + '_Edited');
  });

  it('Add a new preference', function () {
    cy.visitOpenRefine();
    cy.get('#project-links a').contains('Preferences').click();

    const testPreferenceName = Date.now();

    cy.window().then(($win) => {
      cy.stub($win, 'prompt').returns(testPreferenceName);
      cy.get('table.preferences tr:last-child button.button').click();
    });

    cy.get('table.preferences tr:nth-last-child(2)').contains(
      testPreferenceName
    );
  });
});
