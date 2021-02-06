describe(__filename, function () {
    it('verify <a>href tags created for URLs within cell text', function () {
        const fixture = [
            ['tests'],
            ['2021-01-31https://www.google.com'],
            [
                'https://www.wikidata.org/wiki/Property:P670 https://www.wikidata.org/wiki/Property:P669 are now mapped to https://schema.org/streetAddress via https://www.wikidata.org/wiki/Property:P2235',
            ],
            ['vhjhjjj https://github.com/OpenRefine/OpenRefine/issues/2519'],
        ]
        cy.loadAndVisitProject(fixture)

        cy.getCell(0, 'tests').contains('2021-01-31https://www.google.com')
        cy.getCell(1, 'tests')
            .children('div')
            .children('a')
            .should('have.attr', 'href')
        cy.getCell(2, 'tests')
            .children('div')
            .children('a')
            .should('have.attr', 'href')
    })
})
