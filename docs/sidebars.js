module.exports = {
  docs: {
    'User Manual': [
      'index',
      'manual/installing',
      'manual/running',
      'manual/starting',
	        {
      type: 'category',
      label: 'Exploring data',
      items: ['manual/exploring', 'manual/facets', 'manual/sortview'],
    },
	    {
      type: 'category',
      label: 'Transforming data',
      items: ['manual/transforming', 'manual/cellediting','manual/columnediting','manual/transposing'],
    },
      'manual/reconciling',
      {
        type: 'category',
        label: 'Wikibase',
        items: ['manual/wikibase/overview', 'manual/wikibase/configuration', 'manual/wikibase/reconciling', 'manual/wikibase/schema-alignment',
                'manual/wikibase/new-entities', 'manual/wikibase/quality-assurance', 'manual/wikibase/uploading'],
      },
	    {
      type: 'category',
      label: 'Expressions',
      items: ['manual/expressions', 'manual/grel', 'manual/grelfunctions', 'manual/jythonclojure'],
    },
      'manual/exporting',
      'manual/troubleshooting'
    ],
   'GREL Reference': [
	   'manual/grelfunctions'],
    'Technical Reference': [
      'technical-reference/technical-reference-index',
      'technical-reference/architecture',
      'technical-reference/openrefine-api',
      'technical-reference/reconciliation-api',
      'technical-reference/contributing',
      'technical-reference/build-test-run',
      'technical-reference/development-roadmap',
      'technical-reference/version-release-process',
      'technical-reference/homebrew-cask-process',
      'technical-reference/writing-extensions',
      'technical-reference/migrating-older-extensions',
      'technical-reference/translating',
      'technical-reference/functional-tests',
      'technical-reference/maintainer-guidelines'
  ]
  },
};
