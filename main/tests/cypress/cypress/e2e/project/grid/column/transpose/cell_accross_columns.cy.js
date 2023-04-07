/**
 * The following scenarios are inspired by the official OpenRefine documentation
 * https://openrefine.org/docs/manual/transposing/
 */
describe(__filename, function () {
  /**
   * https://openrefine.org/docs/manual/transposing/#one-column
   */
  it('Transpose cells across columns into rows (One column)', function () {
    const fixture = [
      ['Name', 'Street', 'City', 'State/Province', 'Country', 'Postal code'],

      ['Jacques Cousteau', '23 quai de Conti', 'Paris', '', 'France', '75270'],
      [
        'Emmy Noether',
        '010 N Merion Avenue',
        'Bryn Mawr',
        'Pennsylvania',
        'USA',
        '19010',
      ],
    ];
    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('Street', [
      'Transpose',
      'Transpose cells across columns into rows…',
    ]);

    cy.get('.dialog-container select[bind="fromColumnSelect"]').select(
      'Street'
    );
    cy.get('.dialog-container select[bind="toColumnSelect"]').select(
      '(last column)'
    );

    // add the column name and "test" string to each transposed cell
    cy.get('label')
      .contains("prepend the original column's name to each cell")
      .click();
    cy.get('input[bind="separatorInput"]').type(' test ');
    cy.get('.dialog-container label').contains('One column').click();
    cy.get('.dialog-container label')
      .contains('One column')
      .parent()
      .find('input')
      .type('Address');

    cy.confirmDialogPanel();
    cy.waitForOrOperation();
    cy.assertNotificationContainingText(
      'Transpose cells in columns starting with Street'
    );

    cy.assertGridEquals([
      ['Name', 'Address'],
      ['Jacques Cousteau', 'Street: test 23 quai de Conti'],
      ['', 'City: test Paris'],
      ['', 'Country: test France'],
      ['', 'Postal code: test 75270'],

      ['Emmy Noether', 'Street: test 010 N Merion Avenue'],
      ['', 'City: test Bryn Mawr'],
      ['', 'State/Province: test Pennsylvania'],
      ['', 'Country: test USA'],
      ['', 'Postal code: test 19010'],
    ]);
  });

  /**
   * https://openrefine.org/docs/manual/transposing/#two-columns
   */
  it('Transpose cells across columns into rows (Two columns)', function () {
    const fixture = [
      ['Name', 'Street', 'City', 'State/Province', 'Country', 'Postal code'],

      ['Jacques Cousteau', '23 quai de Conti', 'Paris', '', 'France', '75270'],
      [
        'Emmy Noether',
        '010 N Merion Avenue',
        'Bryn Mawr',
        'Pennsylvania',
        'USA',
        '19010',
      ],
    ];
    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('Street', [
      'Transpose',
      'Transpose cells across columns into rows…',
    ]);

    cy.get('.dialog-container label').contains('Two new columns').click();

    cy.get('.dialog-container input[bind="keyColumnNameInput"]').type(
      'Address part'
    );
    cy.get('.dialog-container input[bind="valueColumnNameInput"]').type(
      'Address'
    );

    cy.confirmDialogPanel();
    cy.waitForOrOperation();
    cy.assertNotificationContainingText(
      'Transpose cells in columns starting with Street'
    );

    cy.assertGridEquals([
      ['Name', 'Address part', 'Address'],
      ['Jacques Cousteau', 'Street', '23 quai de Conti'],
      ['', 'City', 'Paris'],
      ['', 'Country', 'France'],
      ['', 'Postal code', '75270'],
      ['Emmy Noether', 'Street', '010 N Merion Avenue'],
      ['', 'City', 'Bryn Mawr'],
      ['', 'State/Province', 'Pennsylvania'],
      ['', 'Country', 'USA'],
      ['', 'Postal code', '19010'],
    ]);
  });
});
