const jsonValue = `[
  {
     "id":"0001",
     "type":"donut",
     "name":"Cake",
     "ppu":0.55,
     "batters":{
        "batter":[
           {
              "id":"1001",
              "type":"Regular"
           },
           {
              "id":"1002",
              "type":"Chocolate"
           },
           {
              "id":"1003",
              "type":"Blueberry"
           },
           {
              "id":"1004",
              "type":"Devil's Food"
           }
        ]
     },
     "topping":[
        {
           "id":"5001",
           "type":"None"
        },
        {
           "id":"5002",
           "type":"Glazed"
        },
        {
           "id":"5005",
           "type":"Sugar"
        },
        {
           "id":"5007",
           "type":"Powdered Sugar"
        },
        {
           "id":"5006",
           "type":"Chocolate with Sprinkles"
        },
        {
           "id":"5003",
           "type":"Chocolate"
        },
        {
           "id":"5004",
           "type":"Maple"
        }
     ]
  },
  {
     "id":"0002",
     "type":"donut",
     "name":"Raised",
     "ppu":0.55,
     "batters":{
        "batter":[
           {
              "id":"1001",
              "type":"Regular"
           }
        ]
     },
     "topping":[
        {
           "id":"5001",
           "type":"None"
        },
        {
           "id":"5002",
           "type":"Glazed"
        },
        {
           "id":"5005",
           "type":"Sugar"
        },
        {
           "id":"5003",
           "type":"Chocolate"
        },
        {
           "id":"5004",
           "type":"Maple"
        }
     ]
  },
  {
     "id":"0003",
     "type":"donut",
     "name":"Old Fashioned",
     "ppu":0.55,
     "batters":{
        "batter":[
           {
              "id":"1001",
              "type":"Regular"
           },
           {
              "id":"1002",
              "type":"Chocolate"
           }
        ]
     },
     "topping":[
        {
           "id":"5001",
           "type":"None"
        },
        {
           "id":"5002",
           "type":"Glazed"
        },
        {
           "id":"5003",
           "type":"Chocolate"
        },
        {
           "id":"5004",
           "type":"Maple"
        }
     ]
  }
]`;

describe(__filename,  () => {

  beforeEach(() => {




  });

  describe("All menu", () => {

    beforeEach(() => {
      cy.loadAndVisitProject('food.small');
    });

    it("contains a item called \"Add blank rows\"", () => {
      cy.columnActionClick("All", []);
      cy.get("div.menu-container a.menu-item")
        .eq(3)
        .invoke("text")
        .should("equal", "Add blank rows");
    });

    describe("\"Add blank rows\" submenu", () => {
      it("contains a sub-item called \"Prepend one row\"", () => {
        cy.columnActionClick("All", ["Add blank rows"]);
        cy.get("div.menu-container")
          .eq(1)
          .find("a.menu-item")
          .eq(0)
          .invoke("text")
          .should("equal", "Prepend one row");
      });

      it("contains a sub-item called \"Append one rows\"", () => {
        cy.columnActionClick("All", ["Add blank rows"]);
        cy.get("div.menu-container")
          .eq(1)
          .find("a.menu-item")
          .eq(1)
          .invoke("text")
          .should("equal", "Append one row");
      });

      it("contains a sub-item called \"Add multiple rows\"", () => {
        cy.columnActionClick("All", ["Add blank rows"]);
        cy.get("div.menu-container")
          .eq(1)
          .find("a.menu-item")
          .eq(2)
          .invoke("text")
          .should("equal", "Add multiple rows");
      });

    });
  });

  describe("Peripheral interface", () => {

    function addRows() {
      // Trigger add rows action
      cy.columnActionClick("All", ["Add blank rows", "Add multiple rows"]);
      cy.waitForDialogPanel();
      cy.get("form[data-cy=add-rows-form]").submit();
      cy.wait(1000);  // wait for response
    }

    beforeEach(() => {
      cy.loadAndVisitProject('food.small');
    });

    it("displays a notification message", () => {
      cy.get("div#notification > div[bind=undoDiv] > span[bind=undoDescription]")
        .as("notificationDescription");

      // Verify default state
      cy.get("@notificationDescription")
        .invoke("text")
        .should("equal", "");

      addRows();

      cy.get("@notificationDescription")
        .invoke("text")
        .should("equal", "Add 1 row");
    });

    it("increments history count by 1", () => {
      cy.get("a#or-proj-undoRedo > span.count")
        .as("historyCount");

      // Verify default state
      cy.get("@historyCount")
        .invoke("text")
        .should("equal", "0 / 0");

      addRows();

      cy.get("@historyCount")
        .invoke("text")
        .should("equal", "1 / 1");
    });

    it("increments row count summary by 1", () => {
      cy.get("#summary-bar > span")
        .as("summaryText");

      // Verify default state
      cy.get("@summaryText")
        .invoke("text")
        .should("equal", "199 rows");

      addRows();

      cy.get("@summaryText")
        .invoke("text")
        .should("equal", "200 rows");
    });

    it("increments total history entries by 1", () => {
      // Verify default state
      cy.get("a.history-entry")
        .should("have.length", 0);

      addRows();

      cy.get("a.history-entry")
        .should("have.length", 2);
    });

    it("updates the UI when faceting by blank rows", () => {

      cy.columnActionClick("All", ["Facet", "Facet by blank (null or empty string)"]);

      cy.get("li#facet-0 div.facet-body-inner div.facet-choice")
        .as("facetChoices");

      // Verify default state
      cy.get("@facetChoices").then($divs => {
        cy.wrap($divs).should('have.length', 1);
        assert.equal($divs.first().find("a.facet-choice-label").text(), "false");
        assert.equal($divs.first().find("span.facet-choice-count").text(), "199");
      });

      addRows();

      cy.get("@facetChoices")
        .then($divs => {
          cy.wrap($divs).should('have.length', 2);
          assert.equal($divs.first().find("a.facet-choice-label").text(), "false");
          assert.equal($divs.first().find("span.facet-choice-count").text(), "199");
          assert.equal($divs.eq(1).find("a.facet-choice-label").text(), "true");
          assert.equal($divs.eq(1).find("span.facet-choice-count").text(), "1");
        });
    });
  });

  describe("Modal dialog", () => {

    beforeEach(() => {
      cy.loadAndVisitProject('food.small');
      cy.columnActionClick("All", [ "Add blank rows", "Add multiple rows" ]);
      cy.waitForDialogPanel();

      cy.get("div[data-cy=add-rows-dialog]")
        .as("dialog");
      cy.get("@dialog")
        .find("button[data-cy=cancel-button]")
        .as("cancelButton");
      cy.get("@dialog")
        .find("button[data-cy=submit-button]")
        .as("submitInput");
      cy.get("@dialog")
        .find("input[data-cy=row-count]")
        .as("countInput");
      cy.get("@dialog")
        .find("form[data-cy=add-rows-form]")
        .as("form");
      cy.get("@dialog")
        .find("select[data-cy=insert-position]")
        .as("positionSelect");
    });

    it("appears on menu item click", () => {
      cy.get("@dialog") // implicitly tests existence in DOM
        .should('be.visible');
    });

    it('closes on cancel button click', () => {
      cy.get("@cancelButton")
        .click();
      cy.get("@dialog")
        .should('not.exist');
    });

    it('closes on form submission', () => {
      cy.get("@form")
        .submit();
      cy.get("@dialog")
        .should('not.exist');
    });

    describe("Row count input", () => {
      it("is not valid if value is a negative integer", () => {
        cy.get("@countInput")
          .clear()
          .type("-1")
          .then($input => $input[0].checkValidity()).should("be.false");
      });

      it("is not valid if value is zero", () => {
        cy.get("@countInput")
          .clear()
          .type("0")
          .then($input => $input[0].checkValidity()).should("be.false");
      });

      it("is not valid if value is a fraction", () => {
        cy.get("@countInput")
          .clear()
          .type("1.5")
          .then($input => $input[0].checkValidity()).should("be.false");
      });

      it("is valid if `0 < value ≤ 50`", () => {
        for (let i = 1; i <= 10; i++) {
          cy.get("@countInput")
            .clear()
            .type(i)
            .then($input => $input[0].checkValidity()).should("be.true");
        }
      });
    }); // end describe row count input
  });  // end describe modal dialog window

  describe("Data table", () => {

    describe("Prepended blank row", () => {
      beforeEach(() => {
        cy.loadAndVisitProject('food.small');

        cy.get("div#view-panel > div.data-table-container > table > tbody > tr")
          .as("tableRows");

        cy.columnActionClick("All", [ "Add blank rows", "Prepend one row" ]);
        cy.wait(1000);  // Wait 1 second for data to be retrieved
      });

      it("is the first row in the project", () => {
        cy.get("@tableRows")
          .first()
          .find("td > div.data-table-cell-content > div > span")
          .each($span => {
            expect($span.text()).to.equal("null");
            expect($span.attr("class")).to.equal("data-table-null");
            expect($span.css("display")).to.equal("none");
          });
      });

      it("does not increase the number of visible rows", () => {
        cy.get("@tableRows")
          .should("have.length", 10);
      });

    });

    describe("Appended blank row", () => {

      function appendRow() {
        cy.columnActionClick("All", [ "Add blank rows", "Append one row" ]);
        cy.wait(1000);  // Wait 1 second for data to be retrieved
      }

      function assertRowIsBlank($tr) {
        $tr.find("td > div.data-table-cell-content > div > span")
          .each((i, span) => {
            expect(span.innerText).to.equal("null");
            expect(span.className).to.equal("data-table-null");
            expect(span.style.display).to.equal("none");
          });
      }

      describe("In row mode", () => {

        beforeEach(() => {
          cy.loadAndVisitProject('food.small');

          cy.get("div#view-panel > div.data-table-container > table > tbody > tr")
            .as("tableRows");

          appendRow();
          // TODO
          // cy.get("@tableRows")
          //   .last()
          //   .find("td > div.data-table-cell-content > div > span")
          //   .as("lastRow");
        });

        it("is the last row in the project", () => {
          cy.get("div#view-panel div.viewpanel-pagesize a.viewPanel-pagingControls-page.action")
            .last()
            .click();
          cy.get("@tableRows")
            .last()
            .then(assertRowIsBlank);
        });

        it("does not modify the first row", () => {
          cy.get("@tableRows")
            .first()
            .find("td > div.data-table-cell-content > div > span")
            .first()
            .then($span => {
              expect($span.text()).to.equal("01001");
              expect($span.attr("class")).to.not.equal("data-table-null");
              expect($span.css("display")).to.not.equal("none");
            })
        });

        it("does not increase the number of visible rows", () => {
          cy.get("@tableRows")
            .should("have.length", 10);
        });

      }); // end describe data table in row mode

      describe("in records mode", () => {
        const pricerunner = [
          ["Cluster Label", "Product Title"],
          ["Apple iPhone 8 Plus 64GB", "apple iphone 8 plus 64gb silver"],
          ["Apple iPhone 8 Plus 64GB", "apple iphone 8 plus 64 gb spacegrau"],
          ["Apple iPhone 7 128GB",     "iphone 7 128gb silver"],
          ["Apple iPhone 7 128GB",     "iphone 7 128gb silver mn932b/a wc01"]
        ];

        beforeEach(() => {
          cy.loadProject(pricerunner, "pricerunner.mini", "fooTag").then(projectId => {
            cy.visit(`${Cypress.env("OPENREFINE_URL")}/project?project=${projectId}`);
            cy.waitForProjectTable();
          });

          cy.get("div#view-panel > div.data-table-container > table > tbody > tr")
            .as("tableRows");

          // Format data for records mode
          cy.columnActionClick("All", [ "Edit columns", "Blank down" ]);
          cy.get('span[bind="modeSelectors"]')
            .contains('records')
            .click();

        });

        it("appends a new row in records mode", () => {
          appendRow();
          // cy.assertGridEquals(pricerunner.push([null, null]));
          cy.get("@tableRows")
            .last()
            .then(assertRowIsBlank);
            // .find("td > div.data-table-cell-content > div > span")
            // .each(expectRowToBeBlank);
        });

      }); // end describe data table in records

    });
  });
});  // end Cypress tests
