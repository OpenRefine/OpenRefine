How to build these docs
=======================

We use [Docusaurus 2](https://v2.docusaurus.io/) for our docs, a modern static website generator.

### Requirements
Assuming you have [Node.js](https://nodejs.org/en/download/) installed (which includes npm), you can install Docusaurus with:

You will need to install [Yarn](https://yarnpkg.com/getting-started/install) before you can build the site.
```sh
npm install -g yarn
```

### Installation

Once you have installed yarn, navigate to docs directory & set-up the dependencies.

```sh
cd docs
yarn
```

### Local Development

```sh
yarn start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

### Build

```sh
yarn build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

### Deployment

```sh
GIT_USER=<Your GitHub username> USE_SSH=true yarn deploy
```

If you are using GitHub pages for hosting, this command is a convenient way to build the website and push to the `gh-pages` branch.
