describe(__filename,  () => {

  function openDialog() {
    cy.columnActionClick("All", [ "Add rows" ]);
    cy.waitForDialogPanel();
  }

  beforeEach(() => {
    cy.loadAndVisitProject('food.small');

    openDialog();

    cy.get("[data-cy=addRowsDialog]")
      .as("dialog");
    cy.get("@dialog")
      .find("[data-cy=dialogBody]")
      .as("dialogBody");
    cy.get("@dialog")
      .find("[data-cy=moreRowButton]")
      .as("moreRowButton");
    cy.get("@dialog")
      .find("[data-cy=cancelButton]")
      .as("cancelButton");
    cy.get("@dialogBody")
      .find("tbody tr:first")
      .as("firstRow");
    cy.get("@dialogBody")
      .find("tbody tr")
      .as("tableRows");
    cy.get("@firstRow")
      .find("button[type=button]")
      .as("firstRemoveRowButton");
    cy.get("[data-cy=submitButton]")
      .as("submitButton");
  });

  it("Dialog appears on menu item click", () => {
    cy.get("@dialog")
      .should('exist');
  });

  it("Delete row button is disabled on load", () => {
    cy.get("@firstRemoveRowButton")
      .should('be.disabled');
  });

  it('Dialog closes on button click', () => {
    cy.get("@cancelButton")
      .click();
    cy.get("@dialog")
      .should("not.exist");
  });

  it("Only displays one row on open", () => {
    cy.get("@tableRows")
      .should("have.length", 1);
  });

  it('Add row button creates new row', () => {
    cy.get("@tableRows")
      .should('have.length', 1);
    cy.get("@moreRowButton")
      .click();
    cy.get("@tableRows")
      .should('have.length', 2);
  });

  it("Delete row button is enabled when row count > 1", () => {
    cy.get("@moreRowButton")
      .click();
    cy.get("@firstRemoveRowButton")
      .should('not.be.disabled');
  });

  it('Delete row button removes one row', () => {
    cy.get("@moreRowButton")
      .click();
    cy.get("@tableRows")
      .should('have.length', 2);
    cy.get("@firstRemoveRowButton")
      .click();
    cy.get("@tableRows")
      .should('have.length', 1);
  });

  it("Delete row button is disabled when row count equals 1", () => {
    cy.get("@moreRowButton")
      .click();
    cy.get("@firstRemoveRowButton")
      .click();
    cy.get("@firstRemoveRowButton")
      .should("be.disabled");
  });

  it('Dialog body scrolls horizontally when project has many columns', () => {
    cy.get("@dialogBody")
      .find("th")
      .last()
      .should("not.be.visible")
      .then($th => cy.wrap($th).scrollIntoView())
      .should("be.visible");
  });

  it("Prepends a blank row when form is empty", () => {
    cy.get("@submitButton").click();
    cy.get("tbody.data-table td > div.data-table-cell-content span")
      .should("be.empty");
  });

  it("Adds text when inputs have value", () => {
    const inputValue = "01000"
    cy.get("@firstRow")
      .find("input[type=text]:first")
      .type(inputValue);

    cy.get("@submitButton").click();

    cy.get("tbody.data-table td > div.data-table-cell-content span")
      .eq(0)
      .should("have.text", inputValue);
  });

  it("Cells data is serialized correctly when column is removed", () => {
    cy.loadAndVisitProject('food.mini');

    const data = {
      "NDB_No": "01003",
      "Shrt_Desc": "BUTTER",
      "Water": "15.87",
      "Energ_Kcal": "717"
    };

    // // Close open dialog
    // cy.get("@cancelButton")
    //   .click();

    cy.columnActionClick("Water", [
      "Edit column",
      "Remove this column"
    ]);

    // Re-open dialog
    openDialog();

    cy.get("@firstRow")
      .find("input[type=text]")
      .as("inputs");

    cy.get("@inputs").eq(0)
      .type(data["NDB_No"]);

    cy.get("@inputs").eq(1)
      .type(data["Shrt_Desc"]);

    cy.get("@inputs").eq(2)
      .type(data["Energ_Kcal"]);

    cy.get("@submitButton").click();

    cy.get("tbody.data-table td > div.data-table-cell-content span")
      .as("tableData");

    cy.get("@tableData")
      .eq(0)
      .should("have.text", data["NDB_No"]);

    cy.get("@tableData")
      .eq(1)
      .should("have.text", data["Shrt_Desc"]);

    cy.get("@tableData")
      .eq(2)
      .should("have.text", data["Energ_Kcal"]);

  });

});
