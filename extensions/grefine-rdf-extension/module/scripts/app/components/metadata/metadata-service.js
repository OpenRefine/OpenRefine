app.service('Metadata', function(Schema, Input, $q) {
  var repository;

  return {
    init: function(endpoint) {
      var deferred = $q.defer();

      Schema.getSchema('fdp.schema.json').then(function(response) {
        var schema = response.schema;
        var mapping = response.mapping;
        Input.read(endpoint, schema, mapping).then(function(metadata) {
          repository = metadata;
          deferred.resolve();
        });
      });

      return deferred.promise;
    },
    getRepository: function() {
      return repository;
    },
    setRepository: function(repo) {
      repository = repo;
    }
  };
});