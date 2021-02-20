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
describe(__filename, function () {
  it('ensures rows and records display same in csv file', function () {
    cy.loadAndVisitProject('food.small');

    cy.get('span[bind="modeSelectors"]').contains('records').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);

    cy.get('span[bind="modeSelectors"]').contains('rows').click();
    cy.get('.data-table tbody').find('tr').should('have.length', 10);
  });
  it('ensures rows and records are different for 3-level json file', function () {
    const projectName = Date.now();
    cy.loadAndVisitSampleJSONProject(projectName, jsonValue);
    cy.get('span[bind="modeSelectors"]').contains('records').click();
    for (let i = 1; i <= 3; i++) {
      cy.get('tr td:nth-child(3)').should('to.contain', i);
    }
    cy.get('span[bind="modeSelectors"]').contains('row').click();
    for (let i = 1; i <= 10; i++) {
      cy.get('tr td:nth-child(3)').should('to.contain', i);
    }
  });
});
