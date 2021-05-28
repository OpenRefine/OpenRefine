---
id: contributing
title: Contributing
sidebar_label: Contributing
---

Please read the general [guidelines on contributing to OpenRefine](https://github.com/OpenRefine/OpenRefine/blob/master/CONTRIBUTING.md) first, then review the information on [reporting and tracking issues](#reporting-and-tracking-issues), and on making your [first pull request](#your-first-pull-request) below)

## Reporting and tracking issues {#reporting-and-tracking-issues}

If you need to file a bug or request a feature, [create an Issue in the OpenRefine Github repository](https://github.com/OpenRefine/OpenRefine/issues). Github issues should be used for reporting specific bugs and requesting specific features. If you just don't know how to do something using OpenRefine, or want to discuss some ideas, please:

- [Try the user manual](/)
- [post to our OpenRefine mailing list](http://groups.google.com/group/openrefine/)

## Contributing to the documentation {#contributing-to-the-documentation}

We use [Docusaurus](https://docusaurus.io/) for our docs. For small documentation changes, you should be able to edit the Markdown files directly and submit them as a pull request. A preview of the docs will be generated automatically. But it is also
possible to preview your changes locally. Assuming you have [Node.js](https://nodejs.org/en/download/) installed (which includes npm), you can install Docusaurus with:

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

## Your first code pull request {#your-first-code-pull-request}

This describes the overall steps to your first code contribution in OpenRefine. If you have trouble with any of these steps feel free to reach out on the [developer mailing list](https://groups.google.com/forum/#!forum/openrefine-dev) or the [Gitter channel](https://gitter.im/OpenRefine/OpenRefine).

- Install OpenRefine, learn to use it by following some tutorials or watching [some videos](http://openrefine.org/). That will ensure you understand the user workflows and get familiar with the terminology used in the tool.

- Fork the GitHub repository, clone it on your machine and set up your IDE to work on it. We have [instructions for this](https://github.com/OpenRefine/OpenRefine/wiki/Building-OpenRefine-From-Source).

- Browse through the list of issues to find an issue that you find interesting. You should pick one where you understand what the problem is as a user, you can see why fixing it would be an improvement to the tool. It is also a good idea to pick an issue that matches your technical skills: some require work on the backend (in Java) or in the frontend (Javascript), often both. We try to maintain a list of [good first issues](https://github.com/OpenRefine/OpenRefine/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) which should be easier than others and should not require any difficult design decision.

- Reproduce the issue locally, by following the steps described in the issue. You might need to locate a particular dialog, use a specific importer on a sample file, or follow any other user workflow. If you have followed all the steps described in the issue and cannot observe the issue mentioned, write a comment on the issue explaining that you are not able to reproduce it (perhaps it was fixed by another change).

- Locate the code that is relevant for the issue you want to solve. Text search across files is often useful for that. For instance, if the issue you want to solve is about a dialog entitled "Columnize by key/values", you can search for "Columnize" in the entire source code. For more details about this technique, see [this comment](https://github.com/OpenRefine/OpenRefine/issues/3137#issuecomment-691649962).

- Study how the current code works. You might want to use a debugger to put breakpoints at the relevant locations (for inspecting the backend, use your IDE's debugger, for the frontend, use your browser's developer tools).

- Create a git branch for your fix. The name of your branch should contain the issue number, and a few words to describe the topic of the fix, for instance "issue-1234-columnize-layout".

- Make changes to the code to fix the issue. If you are changing backend code, it would be great if you could also write a test in Java to demonstrate the fix. You can imitate existing tests for that. We currently do not have frontend tests.

- commit your changes, using a message that contains "closes #1234" or "fixes #1234", this will link the commit to the issue you are working on.

- push your branch to your fork and create a pull request for it, explaining the approach you have used, any design decisions you have made.

Thank you!
