This document presents how you can contribute to the OpenRefine project. Please also review our [Governance model](https://github.com/OpenRefine/OpenRefine/blob/master/GOVERNANCE.md)

## Documentation, Questions or Problem

Our issue list is only for reporting specific bugs and requesting specific features. If you just don't know how to do something using OpenRefine, or want to discuss some ideas, please
- try the [documentation wiki](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Users)
- ask on the [OpenRefine mailing list](https://groups.google.com/d/forum/openrefine).

If you really want to file a bug or request a feature, go to this [issue list](https://github.com/OpenRefine/OpenRefine/issues). Please use the search function first to make sure a similar issue doesn't already exist. 

## Promote OpenRefine

You don't need to be a coder to contribute to OpenRefine. Did you write a tutorial or article about OpenRefine on your blog or site? Are you organizing a workshop or presentation for OpenRefine in your city? Let us know via our [user discussion list](https://groups.google.com/d/forum/openrefine) or Twitter account ([@OpenRefine](http://twitter.com/OpenRefine)). We will share the news via our monthly update and via our Twitter handle. 

## Contributing translations

You can help us [translate OpenRefine](https://github.com/OpenRefine/OpenRefine/wiki/Translate-OpenRefine) in as many languages as possible [via Weblate](https://hosted.weblate.org/engage/openrefine/?utm_source=widget).

##  Contributing code 

You can contribute code in three different ways:
- Fix minor bugs - you can check the issues flagged as [help wanted](https://github.com/OpenRefine/OpenRefine/labels/help%20wanted) or [good first issue](https://github.com/OpenRefine/OpenRefine/labels/good%20first%20issue) or [good second issue](https://github.com/OpenRefine/OpenRefine/labels/good%20second%20issue)
- Develop an OpenRefine extension 
- Start your own distribution or fork

All developers including new distributions and plugin developers are invited to leverage the following OpenRefine project management areas to avoid splitting the community in different communication channels.
- the [wiki](https://github.com/OpenRefine/OpenRefine/wiki) for shared documentation between both user docs and [documentation for developer](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Developers)
- the [developer mailing list](https://groups.google.com/forum/?fromgroups#!forum/openrefine-dev) for technical questions, new feature development and anything code related. We invite you to share you idea first via the developer mailing list. Someone may be able to point out to existing development saving you hours of research and development. 
- [OpenRefine github issue tracker](https://github.com/OpenRefine/OpenRefine/issues) for requesting new features and bug reports.
- [Gitter Chat](https://gitter.im/OpenRefine/OpenRefine)

### How to submit PR's (pull requests), patches, and bug fixes

Welcome new contributor!
Follow this guide [Your first pull request](https://gitter.im/OpenRefine/OpenRefine)

- Avoid merging master in your branch because it makes code review a lot harder. **Please make your changes in a new git branch and send your patch**, including appropriate test cases.
- If you want to keep your branch up to date with our master, it would be nicer if you could just rebase your branch instead. That would keep the history a lot cleaner.
- Please avoid adding unrelated changes in a PR. Use separate PR's and rebase once they get merged can work really well.
- If you make trivial changes, you can send them directly via a PR.

### Code Quality ###
We want to keep the quality of the trunk at a very high level, since this is ultimately where the Stable Releases are built from after bugs are fixed.
- For Java code, we follow **Google Java Style** (Use the [Eclipse Plugin](https://github.com/google/google-java-format#eclipse)).
- For JavaScript, we follow **JavaScript Standard Style** (Eclipse Orion and greater [defaults to ESLint](https://eslint.org/docs/3.0.0/user-guide/integrations)).
- Test your changes locally before sending a PR.
- After sending a PR, review your PR on GitHub for Travis and Appveyor failures.
- Code style checks are currently NOT part of our GitHub CI/CD automation, but will be in the future. See issue [#2338](https://github.com/OpenRefine/OpenRefine/issues/2338).

OpenRefine is volunteer supported. PR's are reviewed and merged by volunteers. All PR's will be answered, however it may take some time to get back to you. Thank you in advance for your patience.

### New functionalities via extensions

OpenRefine support a plugin architecture to extend its functionality. You can find more information on how to write extension on [our wiki](https://github.com/OpenRefine/OpenRefine/wiki/Write-An-Extension). Giuliano Tortoreto wrote a separate documentation detailing how to build an extension for OpenRefine. A [LaTeX](https://github.com/OpenRefine/OpenRefineExtensionDoc) and [PDF version](https://github.com/OpenRefine/OpenRefineExtensionDoc/blob/master/main.pdf) are available.

If you want to list your extension on the download page, please edit [this file](https://github.com/OpenRefine/openrefine.github.com/blob/master/download.md).

### New distributions

OpenRefine is already available in many different distributions (see the [download page](http://openrefine.org/download.html)). New distributions often package OpenRefine for a specific usage or port it. We are fine with new forks ([see discussion](https://groups.google.com/forum/#!msg/openrefine/pasNnMDJ3p8/LrZz_GiFCwAJ)) but we invite you to engage with the community to share your roadmap and progress.

Github offers a powerful system to work between different repositories and we encourage you to leverage it:
- You can cross reference issues and PR's between Github repository using `user/repository#number` ([see more here](https://github.com/blog/967-github-secrets#cross-repository-issue-references))
- If you want to merge a PR that is pending for review to your own repository check the PR locally ([see more here](https://help.github.com/articles/checking-out-pull-requests-locally/)).

Don't forget to contribute to the upstream ([main OpenRefine repository](https://github.com/openrefine/openrefine.git)) so your changes from your distribution can be reviewed and merged and to keep other developers aware of your progress. If you want to list your distribution on the download page, please edit [this file](https://github.com/OpenRefine/openrefine.github.com/blob/master/download.md).
