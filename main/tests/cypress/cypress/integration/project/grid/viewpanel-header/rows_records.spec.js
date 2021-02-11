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
function createJSONProject(projectName) {
   cy.visitOpenRefine();
   cy.navigateTo('Create Project');
   cy.get('#create-project-ui-source-selection-tabs > div')
     .contains('Clipboard')
     .click();
     cy.get('textarea').invoke('val', jsonValue);
     cy.get(
       '.create-project-ui-source-selection-tab-body.selected button.button-primary'
     )
       .contains('Next »')
       .click();
       cy.get(
         '.default-importing-wizard-header input[bind="projectNameInput"]'
       ).clear();
     cy.get(
       '.default-importing-wizard-header input[bind="projectNameInput"]'
     ).type(projectName);
     cy.get('[data-cy=element0]').first().scrollIntoView().click({force: true})
     cy.doCreateProjectThroughUserInterface();
}
describe(__filename, function () {
   //  it('Ensure it shows correct number of rows', function () {
   //    cy.loadAndVisitProject('food.small');
  
   //    cy.get('#summary-bar').should('to.contain', '199 rows');
   //  });
    it('creates an json project', function() {
        const projectName = Date.now();
        createJSONProject(projectName)
    });
})