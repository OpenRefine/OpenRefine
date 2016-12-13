app.config(function(schemaFormDecoratorsProvider, schemaFormProvider, sfPathProvider) {
  var rdffield = function(name, schema, options) {
    if (schema.type === 'string' && schema.rdftype === 'uri') {
      var f = schemaFormProvider.stdFormObj(name, schema, options);
      f.key = options.path;
      f.type = 'rdffield';
      options.lookup[sfPathProvider.stringify(options.path)] = f;
      return f;
    }
  };

  schemaFormProvider.defaults.string.unshift(rdffield);

  schemaFormDecoratorsProvider.addMapping('bootstrapDecorator', 'rdffield',
    'components/custom/rdffield.html');
});