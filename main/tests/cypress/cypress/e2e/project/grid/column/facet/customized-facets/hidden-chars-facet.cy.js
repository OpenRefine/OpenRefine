describe(__filename, function () {
  it('Lists the hidden characters found in a column', function () {
    // build the values from code points, so that the invisible characters stay visible in this file
    const nbsp = String.fromCodePoint(0x00a0);
    const zeroWidthSpace = String.fromCodePoint(0x200b);
    const byteOrderMark = String.fromCodePoint(0xfeff);
    const fixture = [
      ['Text'],
      ['no' + nbsp + 'break'],
      ['plain text'],
      ['zero' + zeroWidthSpace + 'width' + byteOrderMark],
      ['two' + nbsp + 'non' + nbsp + 'breaking'],
      ['tab\there'],
    ];
    cy.loadAndVisitProject(fixture);
    cy.columnActionClick('Text', ['Facet', 'Customized facets', 'Hidden characters facet']);

    // regular spaces and tabs are not reported, and rows without hidden characters are not listed
    cy.getFacetContainer('Text')
      .find('.facet-choice')
      .then(($choices) => {
        const counts = {};
        $choices.each((i, choice) => {
          const label = Cypress.$(choice).find('.facet-choice-label').text();
          counts[label] = Cypress.$(choice).find('.facet-choice-count').text();
        });
        expect(counts).to.deep.equal({
          '\\u00A0': '2',
          '\\u200B': '1',
          '\\uFEFF': '1',
        });
      });

    // Wait to ensure the facet choice click handlers are set up
    // Ref:  window.setTimeout(wireEvents, 100); in list-facet.js
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(150);
    cy.getFacetContainer('Text').contains('a.facet-choice-label', '\\u00A0').click();
    cy.get('#summary-bar').contains('2 matching rows');
  });
});
