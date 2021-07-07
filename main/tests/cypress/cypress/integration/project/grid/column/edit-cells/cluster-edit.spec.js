describe(__filename, function () {
  it('Ensure cells are filled down', function () {
    const fixture = [
      ['district', 'count'],

      ['SWABBYS HOMES', "12"],
      ['SWABBYS HOME', "45"],
      ['SWABBYS HOMER', "45"],

      ['BALLARDS RIVER', "45"],
      ['BALLARDS RIVER', "45"],

      ['MOUNT ZION',165],
    ];

    cy.loadAndVisitProject(fixture);
  });
});
