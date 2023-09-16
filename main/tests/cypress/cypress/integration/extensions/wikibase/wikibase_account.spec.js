describe(__filename, function () {
    it('Test the elements of the login panel', function () {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase account').click();

        cy.get('.dialog-container').should('be.visible');
        cy.get('.dialog-container .dialog-header').should(
            'to.contain',
            'Wikidata account'
        );

        cy.get('.dialog-container input#wb-username').should('be.visible');
        cy.get('.dialog-container input#wb-password').should('be.visible');
    });

    it('Login with invalid credentials', function () {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase account').click();

        cy.get('.dialog-container input#wb-username').type('cypress');
        cy.get('.dialog-container input#wb-password').type('dummy password');

        cy.get('.dialog-container button').contains('Log in').click();
        cy.get('.dialog-container .wikibase-invalid-credentials').should(
            'be.visible'
        );
    });

    it('Close the panel', function () {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase account').click();

        cy.get('.dialog-container button').contains('Close').click();

        cy.get('.dialog-container').should('not.to.exist');
    });

    it('Login with owner only consumer credentials (invalid credentials)', function () {
        cy.loadAndVisitProject('food.mini');
        cy.get('#extension-bar-menu-container').contains('Wikibase').click();
        cy.get('.menu-container a').contains('Manage Wikibase account').click();

        cy.get('.dialog-container a')
            .contains('login with your owner-only consumer')
            .click();

        cy.get('.dialog-container input#wb-consumer-token').should('be.visible');
        cy.get('.dialog-container input#wb-consumer-secret').should('be.visible');
        cy.get('.dialog-container input#wb-access-token').should('be.visible');
        cy.get('.dialog-container input#wb-access-secret').should('be.visible');

        cy.get('.dialog-container button').contains('Log in').click();
        cy.get('.dialog-container .wikibase-invalid-credentials').should(
            'be.visible'
        );

        // switch back to regular login panel
        cy.get('.dialog-container a')
            .contains('login with your username/password')
            .click();
        cy.get('.dialog-container input#wb-username').should('be.visible');
    });
});
