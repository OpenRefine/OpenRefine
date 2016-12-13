app.service('Input', function($q, $http) {
  return {
    read: function(endpoint, schema, mapping) {
      var deferred = $q.defer();

      $http.get(endpoint).then(function(response) {
        var parser = N3.Parser();

        var metadata = {};

        parser.parse(response.data, function(error, triple) {
          if (error) {
            deferred.reject(error);
          }

          if (triple) {
            var property = mapping[triple.predicate];
            if (!property) {
              console.log('unknown property', triple.predicate);
              return;
            }

            var schemaItem = schema.properties[property];
            
            if (schemaItem.type == 'array') {
              if (!metadata[property]) {
                metadata[property] = [];
              }
              metadata[property].push(triple.object);
            } else {
              metadata[property] = triple.object;
            }
          } else {
            deferred.resolve(metadata);
          }
        });
      });

      return deferred.promise;
    }
  };
});