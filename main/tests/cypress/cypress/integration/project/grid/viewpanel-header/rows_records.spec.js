describe(__filename, function () {
    it('Ensure it shows correct number of rows', function () {
      cy.loadAndVisitProject('food.small');
  
      cy.get('#summary-bar').should('to.contain', '199 rows');
    });
    it('creates an json project', function() {
        const openRefineUrl = Cypress.env('OPENREFINE_URL');
        const openRefineProjectName = 'cypress-test';
      
        // let jsonFixture;
        // if (typeof fixture == 'string') {
        //   jsonFixture = fixtures[fixture];
        // } else {
        //   jsonFixture = fixture;
        // }
      
        // const csv = [];
        // jsonFixture.forEach((item) => {
        //   csv.push('"' + item.join('","') + '"');
        // });
        const content = `[
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

         const options = '{"recordPath":["_","_"],"limit":-1,"trimStrings":false,"guessCellValueTypes":false,"storeEmptyStrings":true,"includeFileSources":false,"includeArchiveFileName":false}';
      
        cy.get('@token', { log: false }).then((token) => {
          // cy.request(Cypress.env('OPENREFINE_URL')+'/command/core/get-csrf-token').then((response) => {
          const openRefineFormat = 'text/json';
      
          // the following code can be used to inject tags in created projects
          // It's conflicting though, breaking up the CSV files
          // It is a hack to parse out CSV files in the openrefine while creating a project with tags


            const postData =
            '------BOUNDARY\r\nContent-Disposition: form-data; name="project-file"; filename="' +
            "kush-test" +
            '"\r\nContent-Type: "text/json"\r\n\r\n' +
            content +
            '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="project-name"\r\n\r\n' +
            openRefineProjectName +
            '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="options"\r\n\r\n' +
            JSON.stringify(options) +
            '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="format"\r\n\r\n' +
            openRefineFormat +
            '\r\n------BOUNDARY--';

      
          cy.request({
            method: 'POST',
            url:
              `${openRefineUrl}/command/core/create-project-from-upload?csrf_token=` +
              token,
            body: postData,
            headers: {
              'content-type': 'multipart/form-data; boundary=----BOUNDARY',
            },
          }).then((resp) => {
            const location = resp.allRequestResponses[0]['Response Headers'].location;
            const projectId = location.split('=').slice(-1)[0];
            cy.log('Created OR project', projectId);
      
            cy.get('@loadedProjectIds', { log: false }).then((loadedProjectIds) => {
              loadedProjectIds.push(projectId);
              cy.wrap(loadedProjectIds, { log: false })
                .as('loadedProjectIds')
                .then(() => {
                  return projectId;
                });
            });
          });
        });
    });
})