/**
 * The following scenarios are inspired by the official OpenRefine documentation
 * https://openrefine.org/docs/manual/transposing/
 */
describe(__filename, function () {
  /**
   * https://openrefine.org/docs/manual/transposing/#columnize-by-keyvalue-columns
   * and https://openrefine.org/docs/manual/transposing/#notes-column
   */
  it('Columnize by key/value columns', function () {
    const fixture = [
      ['Field', 'Data', 'Source'],

      ['Name', 'Galanthus nivalis', 'IUCN'],
      ['Color', 'White', 'Contributed by Martha'],
      ['IUCN ID', '162168', ''],
      ['Name', 'Narcissus cyclamineus', 'Legacy'],
      ['Color', 'Yellow', '2009 survey'],
      ['IUCN ID', '161899', ''],
    ];
    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('Field', [
      'Transpose',
      'Columnize by key/value columns…',
    ]);
    cy.get('select[bind="keyColumnSelect"]').select('Field');
    cy.get('select[bind="valueColumnSelect"]').select('Data');
    cy.get('select[bind="noteColumnSelect"]').select('Source');

    cy.confirmDialogPanel();
    cy.waitForOrOperation();
    cy.assertNotificationContainingText(
      'Columnize by key column Field and value column Data with note column Source'
    );

    const expected = [
      ['Name', 'Color', 'IUCN ID', 'Source : Name', 'Source : Color'],
      ['Galanthus nivalis', 'White', '162168', 'IUCN', 'Contributed by Martha'],
      ['Narcissus cyclamineus', 'Yellow', '161899', 'Legacy', '2009 survey'],
    ];
    cy.assertGridEquals(expected);
  });

  /**
   * https://openrefine.org/docs/manual/transposing/#extra-columns
   * Extra column Wikidata ID must be preserved
   */
  it('Columnize by key/value columns + Extra columns', function () {
    const fixture = [
      ['Field', 'Data', 'Wikidata ID'],
      ['Name', 'Galanthus nivalis', 'Q109995'],
      ['Color', 'White', 'Q109995'],
      ['IUCN ID', '162168', 'Q109995'],
      ['Name', 'Narcissus cyclamineus', 'Q1727024'],
      ['Color', 'Yellow', 'Q1727024'],
      ['IUCN ID', '161899', 'Q1727024'],
    ];
    cy.loadAndVisitProject(fixture);

    cy.columnActionClick('Field', [
      'Transpose',
      'Columnize by key/value columns…',
    ]);
    cy.get('select[bind="keyColumnSelect"]').select('Field');
    cy.get('select[bind="valueColumnSelect"]').select('Data');

    cy.confirmDialogPanel();
    cy.waitForOrOperation();
    cy.assertNotificationContainingText(
      'Columnize by key column Field and value column Data'
    );

    const expected = [
      ['Wikidata ID', 'Name', 'Color', 'IUCN ID'],
      ['Q109995', 'Galanthus nivalis', 'White', '162168'],
      ['Q1727024', 'Narcissus cyclamineus', 'Yellow', '161899'],
    ];
    cy.assertGridEquals(expected);
  });
});
