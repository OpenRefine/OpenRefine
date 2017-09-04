This document present how you can contribute to the OpenRefine project. 

## Documentation, Questions or Problem

Our issue list is only for reporting specific bugs and requesting specific features. If you just don't know how to do something using OpenRefine, or want to discuss some ideas, please
- try the [documentation wiki](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Users)
- ask on the [OpenRefine mailing list](https://groups.google.com/forum/?fromgroups#!forum/openrefine).

If you really want to file a bug or request a feature, go to this [issue list](https://github.com/OpenRefine/OpenRefine/issues). Please use the search function first to make sure a similar issue doesn't already exist. 

### Guidelines for Reporting a Bug

When reporting a bug please provide the following information to help reproduce the bug:
- Version of OpenRefine used (Google Refine 2.6, OpenRefine2.6, an other distribution?)
- Operating Systems and version
- Browser + version used - Please note that OpenRefine doesn't support Internet Explorer
- Steps followed to create the issues
- If you are allowed, it is awesome if you can join the data generating the issue
- Current Results
- Expected Results


## Promote OpenRefine

You don't need to be coder to contribute to OpenRefine. Did you wrote a tutorial or article about OpenRefine on your blog or site? Are you organizing a workshop or presentation OpenRefine in your city? Let us know via our [user discussion list](https://groups.google.com/forum/?fromgroups#!forum/openrefine) or twitter account ([@OpenRefine](http://twitter.com/OpenRefine)). We will share the news via our monthly update and via our twitter handle. 

## Contributing translations

You can help us [translate OpenRefine](https://github.com/OpenRefine/OpenRefine/wiki/Translate-OpenRefine) in as many languages as possible [via Weblate](https://hosted.weblate.org/engage/openrefine/?utm_source=widget).

##  Contributing code 

You can contribute code in three different way:
- fix minor bug
- develop an OpenRefine extension 
- start your own distribution. 

All developers including new distributions and plugin developers are invited to leverage existing OpenRefine project managements tools to avoid the dispersion of the community in different communication channels.
- the [wiki](https://github.com/OpenRefine/OpenRefine/wiki) for shared documentation between distribution both user and [documentation for developer](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Developers)
- the [developer mailing list](https://groups.google.com/forum/?fromgroups#!forum/openrefine-dev) for technical questions, new feature development and anything code related. We invite you to share you idea first via the developer mailing list. Someone may be able to point out to existing development saving your hours of research and development. 
- [OpenRefine github issue tracker](https://github.com/OpenRefine/OpenRefine/issues) for new feature and bug reports common with OpenRefine.

### How to submit minor changes and bug fix

If you make trivial changes, you can send them directly via a pull request. **Please make your changes in a new git branch and send your patch**, including appropriate test cases.

We want to keep the quality of the trunk at a very high level, since this is ultimately where the Stable Releases are built from after bugs are fixed. Please take the time to test your changes (including travis-ci) before sending a pull request.

OpenRefine is volunteer supported. Pull Request are reviewed and merged by volunteers. All Pull Request will be answered, however it may take some time to get back to you. Thank you in advance for your patience.

If you don't where to start and are looking for a bug to fix, please see our [issue list](https://github.com/OpenRefine/OpenRefine/issues). 

### New functionalities via plugin

OpenRefine support a plugin architecture to extend its functionality. You can find more information on how to write extension on [our wiki](https://github.com/OpenRefine/OpenRefine/wiki/Write-An-Extension). Giuliano Tortoreto wrote a separate documentation detailling how to build extension for OpenRefine. A [LaTeX](https://github.com/OpenRefine/OpenRefineExtensionDoc) and [PDF version](https://github.com/OpenRefine/OpenRefineExtensionDoc/blob/master/main.pdf) are available.

If you want to list your extension on the download page, please edit [this file](https://github.com/OpenRefine/openrefine.github.com/blob/master/download.md).

### New distribution

OpenRefine is already available in many different distribution (see the [download page](http://openrefine.org/download.html)). New distribution often package OpenRefine for a specific usage or port it. We are fine with new fork ([see discussion](https://groups.google.com/forum/#!msg/openrefine/pasNnMDJ3p8/LrZz_GiFCwAJ)) but we invite you to engage with the community to share your roadmap and progress.

Github offer a powerful system to work between different repository and we encourage you to leverage it:
- You can cross reference issues and pull request between Github repository using `user/repository#number` ([see more here](https://github.com/blog/967-github-secrets#cross-repository-issue-references))
- If you want to merge a Pull Request that is pending for review to you own repository check the pull request locally ([see more here](https://help.github.com/articles/checking-out-pull-requests-locally/)).

Don't forget to contribute to the upstream (main OpenRefine) repository so your changes can be reviewed and merge and to keep other developers aware of your progress. If you want to list your distribution on the download page, please edit [this file](https://github.com/OpenRefine/openrefine.github.com/blob/master/download.md).
