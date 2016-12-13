app.controller('RepositoryCtrl', function($scope, $http, Metadata, Output) {
  $http.get('fdp.schema.json').then(function(response) {
    $scope.repositorySchema = response.data;
  });

  $scope.repositoryForm = [
    '*',
    { type: 'submit', title: 'Save' }
  ];

  $scope.model = Metadata.getRepository() || {};
  console.log($scope.model);

  var example = {
    title: 'Example repository',
    identifier: 'A1',
    hasVersion: '1.0',
    description: 'This is an exampe repository',
    publisher: 'http://www.dtls.nl/',
    language: 'http://id.loc.gov/vocabulary/iso639-1/en',
    license: 'http://purl.org/NET/rdflicense/MIT1.0',
    rights: 'http://dev.rdf.biosemantics.org/examples/rights',
    label: 'Example repository'
  };

  $scope.example = function() {
    $scope.model = example;
  }

  $scope.onSubmit = function() {
    Output.write($scope.model, $scope.repositorySchema, 'http://www.w3.org/ns/ldp#Container').then(function(result) {
      $scope.output = result;
    });
  }
});