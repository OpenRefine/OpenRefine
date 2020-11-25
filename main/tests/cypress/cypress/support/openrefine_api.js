Cypress.Commands.add('setPreference', (preferenceName, preferenceValue) => {
	const openRefineUrl = Cypress.env('OPENREFINE_URL')
	cy.request( openRefineUrl + '/command/core/get-csrf-token').then((response) => {
		cy.request({
			method: 'POST',
			url: `${openRefineUrl}/command/core/set-preference`,
			body: `name=${preferenceName}&value="${preferenceValue}"&csrf_token=${response.body.token}`,
			form: false,
			headers: {
				'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
			},
		}).then((resp) => {
			cy.log('Set preference ' + preferenceName + ' with value ' + preferenceValue);
		});
	});
});

Cypress.Commands.add('cleanupProjects', () => {
	const openRefineUrl = Cypress.env('OPENREFINE_URL')
	cy.get('@deletetoken', { log: false }).then((token) => {
		cy.get('@loadedProjectIds', { log: false }).then((loadedProjectIds) => {
			for (const projectId of loadedProjectIds) {
				cy.request({
					method: 'POST',
					url: `${openRefineUrl}/command/core/delete-project?csrf_token=` + token,
					body: { project: projectId },
					form: true,
				}).then((resp) => {
					cy.log('Deleted OR project' + projectId);
				});
			}
		});
	});
});

Cypress.Commands.add('loadProject', (fixture, projectName) => {
	const openRefineUrl = Cypress.env('OPENREFINE_URL');
	const openRefineProjectName = projectName ? projectName : fixture;
	cy.fixture(fixture).then((content) => {
		cy.get('@token', { log: false }).then((token) => {
			// cy.request(Cypress.env('OPENREFINE_URL')+'/command/core/get-csrf-token').then((response) => {
			const openRefineFormat = 'text/line-based/*sv';
			// const options = { projectTags: ['OpenRefineTesting'] };
			// '\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="options"\r\n\r\n' +
			// JSON.stringify(options) +

			var postData =
				'------BOUNDARY\r\nContent-Disposition: form-data; name="project-file"; filename="' +
				fixture +
				'"\r\nContent-Type: "text/csv"\r\n\r\n' +
				content +
				'\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="project-name"\r\n\r\n' +
				openRefineProjectName +
				'\r\n------BOUNDARY\r\nContent-Disposition: form-data; name="format"\r\n\r\n' +
				openRefineFormat +
				'\r\n------BOUNDARY--';

			cy.request({
				method: 'POST',
				url: `${openRefineUrl}/command/core/create-project-from-upload?csrf_token=` + token,
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
});
