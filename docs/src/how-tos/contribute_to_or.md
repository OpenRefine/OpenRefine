---
id: howto-contribute-or
title: How-To Contribute to OpenRefine's documentation
sidebar_label: How-To Contribute to docs
---


If you are interested to help make OpenRefine’s documentation better, here's some informations.

The documentation is separated in the Product Reference part, and How-to. There is some documentation mainly written for Users, and there is some documentation more geared towards the development of OpenRefine. 

First, you have to download the OpenRefine project on your computer. To do so, you can use `git` or GitDesktop. You can also use a git client integrated in an IDE like Eclipse.

Then, you can configure [Docusaurus](https://docusaurus.io/), the tool which OpenRefine uses to manage its documentation library.

```sh
cd docs
yarn; yarn build; yarn start
```

This process is explained in more details at [Docusaurus Installation](/Docusaurus_Install.md).

Once you have Docusaurus setup on your computer (you can install and run the Docusaurus server, and be able to see the modification as you make), you are ready to make your changes.

To keep track of your work, create a pull request on GitHub OpenRefine/OpenRefine repository, and commit your changes there. Once you have achieved a substantial amount of work, you can now ask that your changes be reviewed. At this stage, members of the community may suggest changes to your work, in order to be better integrated with the overall of OpenRefine's documentation.

Note that OpenRefine's documentation shares the same repository as the OpenRefine source codebase. So, your PR for the documentation changes will share the same space as the code modification PRs. You might want to add the filter « documentation » to GitHub searches, like this : « `is:open label:documentation` ».
