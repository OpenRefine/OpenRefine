The OpenRefine project welcomes contributions in a variety of forms.
This document contains information a few of the ways you can contribute to the OpenRefine project.
Please also review our [Governance model](https://github.com/OpenRefine/OpenRefine/blob/master/GOVERNANCE.md)

## Provide peer user support

We welcome users to the [OpenRefine forum](https://forum.openrefine.org/) to ask questions and request assistance.
If you can help answer questions in your area of expertise, it would be a benefit to the community.

If a forum discussion determines there is a bug in OpenRefine or a new feature is identified,
we welcome bug reports and feature requests. Please search the [issue tracker](https://github.com/OpenRefine/OpenRefine/issues) first to make sure
the bug / feature hasn't already been added. Note: the development team principally works from the issue
tracker, so anything not included there risks getting lost.

## Promote OpenRefine

Promoting OpenRefine is a great way to give back. Did you write a tutorial or article about OpenRefine on your blog or site?
Are you organizing a workshop or presentation for OpenRefine in your city? Let us know via our [forum](https://forum.openrefine.org/) or Twitter account ([@OpenRefine](http://twitter.com/OpenRefine)).
We will share the news via our monthly update and via our Twitter handle.

## Contribute translations

We want OpenRefine to be available in as many languages as possible to serve
the biggest community of users. You can help us [translate OpenRefine](https://docs.openrefine.org/technical-reference/translating-ui) into languages you are fluent in [via Weblate](https://hosted.weblate.org/engage/openrefine/?utm_source=widget).
Although we have the beginnings of translations for many languages, only a few are complete and popular languages
like Spanish, Brazilian Portuguese, and French could use help.

## Contribute documentation

When browsing our [user manual](https://openrefine.org/docs/) or other documentation, feel free to use the edit button to suggest improvements. For large changes, you might want to [prepare your changes locally](https://openrefine.org/docs/technical-reference/documentation-contributions).

##  Contribute code 

You can contribute code in various ways:
- Fix bugs or implement new features. Follow [our guide towards your first code contribution](https://openrefine.org/docs/technical-reference/code-contributions)
- Improve test coverage. Much of our code was originally written without tests, so help on this front is very much appreciated.
- Develop an OpenRefine extension
- Develop a reconciliation service

All developers including new distributions and plugin developers are invited to leverage the following OpenRefine project management areas.
- the [official documentation](https://openrefine.org/docs/) for shared documentation between both user docs and [technical reference](https://docs.openrefine.org/technical-reference/contributing)
- the [developer forum](https://forum.openrefine.org/c/dev/8) for technical questions, new feature development and anything code related. We invite you to share your idea there first. Someone may be able to point out to existing development saving you hours of research and development.
- the [issue tracker](https://github.com/OpenRefine/OpenRefine/issues) for requesting new features and bug reports.
- [Gitter Chat](https://gitter.im/OpenRefine/OpenRefine) (only occasionally monitored)

### How to submit PR's (pull requests), patches, and bug fixes

All code changes are made via GitHub Pull Requests which are reviewed before merging, even those by core committers.

If you are unfamiliar with git, GitHub, or open source development, please see [our guide towards your first code contribution](https://openrefine.org/docs/technical-reference/code-contributions).

- If you are looking for something to work on, please see our [issue list](https://github.com/OpenRefine/OpenRefine/issues). We have a separate tag for [Good First Issues](https://github.com/OpenRefine/OpenRefine/issues?q=is%3Aopen+is%3Aissue+label%3A%22Good+First+Issue%22).

- create a branch named with the issue number and a brief description
- avoid changes unrelated to fixing the issue
- create unit and/or end-to-end tests which cover the bug fix or new feature
- run `./refine lint` before submitting your PR (CI will fail if lint fails)
- make sure all tests are green before submitting your PR
- we attempt to prioritize PR reviews, but please be patient

### New functionality via extensions

OpenRefine supports a plugin architecture to extend its functionality. You can find more information on how to write
an extension on our [website](https://openrefine.org/docs/technical-reference/writing-extensions).
Giuliano Tortoreto also wrote separate documentation detailing how to build an extension for OpenRefine.
[PDF](https://github.com/giTorto/OpenRefineExtensionDoc/blob/master/main.pdf) and [LaTeX](https://github.com/giTorto/OpenRefineExtensionDoc/) versions are available. It dates from 2014, but still contains good information.

If you want your extension included in the [list of extensions](https://openrefine.org/extensions) advertised on openrefine.org,
please submit a pull request on the download page, please edit [this file](https://github.com/OpenRefine/openrefine.org/blob/master/src/pages/extensions.md).
