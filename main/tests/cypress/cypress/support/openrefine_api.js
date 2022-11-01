const fixtures = require('../fixtures/fixtures.js');

Cypress.Commands.add('addProjectForDeletion', () => {
const path = '/project?project=';
  cy.url().then(($url) => {
    if($url.includes(path)) {
      const projectId = $url.split('=').slice(-1)[0];
      cy.get('@loadedProjectIds', { log: true }).then((loadedProjectIds) => {
        loadedProjectIds.push(projectId);
        cy.wrap(loadedProjectIds, { log: true })
            .as('loadedProjectIds');
      });
    }
  })
});

Cypress.Commands.add('setPreference', (preferenceName, preferenceValue) => {
  const openRefineUrl = Cypress.env('OPENREFINE_URL');
  return cy
    .request(openRefineUrl + '/command/core/get-csrf-token')
    .then((response) => {
      return cy
        .request({
          method: 'POST',
          url: `${openRefineUrl}/command/core/set-preference`,
          body: `name=${preferenceName}&value=${preferenceValue}&csrf_token=${response.body.token}`,
          form: false,
          headers: {
            'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
          },
        })
        .then((resp) => {
          cy.log(
            'Set preference ' +
            preferenceName +
            ' with value ' +
            preferenceValue
          );
        });
    });
});

Cypress.Commands.add('deletePreference', (preferenceName) => {
  const openRefineUrl = Cypress.env('OPENREFINE_URL');
  return cy
      .request(openRefineUrl + '/command/core/get-csrf-token')
      .then((response) => {
        return cy
            .request({
              method: 'POST',
              url: `${openRefineUrl}/command/core/set-preference`,
              body: `name=${preferenceName}&csrf_token=${response.body.token}`,
              form: false,
              headers: {
                'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
              },
            })
            .then((resp) => {
              cy.log(
                  'Delete preference ' +
                  preferenceName 
              );
            });
      });
});

Cypress.Commands.add('importProject', (projectTarFile, projectName) => {
  const openRefineUrl = Cypress.env('OPENREFINE_URL');


  cy.get('@token', { log: false }).then((token) => {
    // cy.request(Cypress.env('OPENREFINE_URL')+'/command/core/get-csrf-token').then((response) => {
    const openRefineFormat = 'text/line-based/*sv';

    // the following code can be used to inject tags in created projects
    // It's conflicting though, breaking up the CSV files
    // It is a hack to parse out CSV files in the openrefine while creating a project with tags
    const options = {
      encoding: 'US-ASCII',
      separator: ',',
      ignoreLines: -1,
      headerLines: 1,
      skipDataLines: 0,
      limit: -1,
      storeBlankRows: true,
      guessCellValueTypes: false,
      processQuotes: true,
      quoteCharacter: '"',
      storeBlankCellsAsNulls: true,
      includeFileSources: false,
      includeArchiveFileName: false,
      trimStrings: false,
      projectName: openRefineProjectName,
      projectTags: [tagName],
    };
    let postData;
    if (tagName == undefined) {
      postData =
        '------BOUNDARY\r\nContent-Disposition: form-data; name="project-file"; filename="test.csv"' +
        '\r\nContent-Type: "text/csv"\r\n\r\n' +
        content +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="project-name"\r\n\r\n' +
        openRefineProjectName +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="format"\r\n\r\n' +
        openRefineFormat +
        '\r\n------BOUNDARY--';
    } else {
      postData =
        '------BOUNDARY\r\nContent-Disposition: form-data; name="project-file"; filename="test.csv"' +
        '\r\nContent-Type: "text/csv"\r\n\r\n' +
        content +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="project-name"\r\n\r\n' +
        openRefineProjectName +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="options"\r\n\r\n' +
        JSON.stringify(options) +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="format"\r\n\r\n' +
        openRefineFormat +
        '\r\n------BOUNDARY--';
    }

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


})

Cypress.Commands.add('cleanupProjects', () => {
  const openRefineUrl = Cypress.env('OPENREFINE_URL');
  cy.get('@deletetoken', { log: false }).then((token) => {
    cy.get('@loadedProjectIds', { log: false }).then((loadedProjectIds) => {
      for (const projectId of loadedProjectIds) {
        cy.request({
          method: 'POST',
          url:
            `${openRefineUrl}/command/core/delete-project?csrf_token=` + token,
          body: { project: projectId },
          form: true,
        }).then((resp) => {
          cy.log('Deleted OR project' + projectId);
        });
      }
    });
  });
});

Cypress.Commands.add('loadProject', (fixture, projectName, tagName) => {
  const openRefineUrl = Cypress.env('OPENREFINE_URL');
  const openRefineProjectName = projectName ? projectName : 'cypress-test';

  let jsonFixture;
  let content;
  const csv = [];

  if (fixture.includes('.csv')) {
    cy.fixture(fixture).then((value) => {
      content = value;
    });
  } else {
    if (typeof fixture == 'string') {
      jsonFixture = fixtures[fixture];
    } else {
      jsonFixture = fixture;
    }
    jsonFixture.forEach((item) => {
      csv.push('"' + item.join('","') + '"');
    });
    content = csv.join('\n');
  }
  
  cy.get('@token', { log: false }).then((token) => {
    // cy.request(Cypress.env('OPENREFINE_URL')+'/command/core/get-csrf-token').then((response) => {
    const openRefineFormat = 'text/line-based/*sv';

    // the following code can be used to inject tags in created projects
    // It's conflicting though, breaking up the CSV files
    // It is a hack to parse out CSV files in the openrefine while creating a project with tags
    const options = {
      encoding: 'US-ASCII',
      separator: ',',
      ignoreLines: -1,
      headerLines: 1,
      skipDataLines: 0,
      limit: -1,
      storeBlankRows: true,
      guessCellValueTypes: false,
      processQuotes: true,
      quoteCharacter: '"',
      storeBlankCellsAsNulls: true,
      includeFileSources: false,
      includeArchiveFileName: false,
      trimStrings: false,
      projectName: openRefineProjectName,
      projectTags: [tagName],
    };
    let postData;
    if (tagName == undefined) {
      postData =
        '------BOUNDARY\r\nContent-Disposition: form-data; name="project-file"; filename="test.csv"' +
        '\r\nContent-Type: "text/csv"\r\n\r\n' +
        content +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="project-name"\r\n\r\n' +
        openRefineProjectName +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="format"\r\n\r\n' +
        openRefineFormat +
        '\r\n------BOUNDARY--';
    } else {
      postData =
        '------BOUNDARY\r\nContent-Disposition: form-data; name="project-file"; filename="test.csv"' +
        '\r\nContent-Type: "text/csv"\r\n\r\n' +
        content +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="project-name"\r\n\r\n' +
        openRefineProjectName +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="options"\r\n\r\n' +
        JSON.stringify(options) +
        '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="format"\r\n\r\n' +
        openRefineFormat +
        '\r\n------BOUNDARY--';
    }

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
