app.service('Output', function($q) {
  return {
    write : function(model, schema, type) {
      var deferred = $q.defer();

      var writer = N3.Writer({
        prefixes : {
          'rdf': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
          'dcterms': 'http://purl.org/dc/terms/',
          'dcat': 'http://www.w3.org/ns/dcat#',
          'rdfs': 'http://www.w3.org/2000/01/rdf-schema#',
          'foaf': 'http://xmlns.com/foaf/0.1/'
        }
      });
      writer.addTriple({
        subject: '',
        predicate: 'rdf:type',
        object: type
      });

      for (var key in model) {
        var meta = schema.properties[key];
        var type = meta.rdftype;
        var object = model[key];

        if (object !== undefined && object.length !== 0) {
          var values = angular.isArray(object) ? object : [object];
          
          angular.forEach(values, function(value, key) {
            if (meta.rdftype === 'literal') {
              value = '"' + value + '"';
            }
            writer.addTriple({
              subject: '',
              predicate: meta.uri,
              object: value
            });
          });
        }
      }

      writer.end(function(error, result) {
        if (error) {
          console.log('error!', error);
          deferred.reject(error);
        } else {
          deferred.resolve(result);
        }
      });

      return deferred.promise;
    }
  };
});