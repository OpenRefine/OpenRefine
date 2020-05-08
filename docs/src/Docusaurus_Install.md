How to build these docs
=======================

We use [Docusaurus](https://docusaurus.io/) for our docs. Assuming you have [Node.js](https://nodejs.org/en/download/) installed (which includes npm), you can install Docusaurus with:

You will need to install [Yarn](https://yarnpkg.com/getting-started/install) before you can build the site.
```sh
npm install -g yarn
```

Once you have installed yarn, navigate to docs directory & set-up the dependencies.

```sh
cd docs
yarn
```

Once this is done, generate the docs with:

```sh
yarn build
```

You can also spin a local web server to serve the docs for you, with auto-refresh when you edit the source files, with:
```sh
yarn start
```

