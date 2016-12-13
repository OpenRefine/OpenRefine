app.service('Schema', function($q, $http) {
  var cache = {};

  return {
    getSchema: function(file) {
      var deferred = $q.defer();

      if (cache[file]) {
        deferred.resolve(cache[file]);
      } else {
        $http.get(file).then(function(response) {
          var schema = response.data;
          var mapping = {};

          angular.forEach(schema.properties, function(value, key) {
            mapping[value.uri] = key;
          });

          cache[file] = {
            schema: schema,
            mapping: mapping
          };

          deferred.resolve(cache[file]);
        }, function(response) {
          deferred.reject(response);
        });
      }

      return deferred.promise;
    }
  };
});