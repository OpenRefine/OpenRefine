---
id: maintainer-guidelines
title: Guidelines for maintaining OpenRefine
sidebar_label: Maintainer guidelines
---

This page describes our practices to review issues and pull requests in the OpenRefine project.

## Reviewing issues {#reviewing-issues}

When people create new issues, they automatically get assigned [the "to be reviewed" tag](https://github.com/OpenRefine/OpenRefine/issues?q=is%3Aissue+is%3Aopen+label%3A%22to+be+reviewed%22).

Ideally, for each of these issues, someone familiar with OpenRefine (not necessarily a developer!) should read the issue and try to determine if there is a genuine bug to fix, or if the enhancement request is legitimate. In those cases, we can remove the "to be reviewed" tag and leave the issue open. In the others, the issue should be politely closed.

### Bugs {#bugs}

For a bug, we should first check if it is a real unexpected behaviour or if just comes from a misunderstanding of the intended behaviour of the tool (which could suggest an improvement to the documentation). Then, if it sounds like a genuine problem, we need to check if it can be reproduced independently on the master branch. If the issue does not give enough details about the bug to reproduce it on master, mark it as "not reproducible" and ask the reporter for more information. After some time without any information from the reporter, we can close the issue.

### Enhancement requests {#enhancement-requests}

For an enhancement, we need to make a judgment call of whether the proposed functionality is in the scope of the project. There is no universal rule for this of course, so just use your own intuition: do you think this would improve the tool? Would it be consistent with the spirit of the project? Trust your own opinion - if people disagree, they can have a discussion on the issue.

### Tagging good first issues {#tagging-good-first-issues}

Adding [the "good first issue" tag](https://github.com/OpenRefine/OpenRefine/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) is something that requires a bit more familiarity with the development process. This tag is used by GitHub to showcase issues in some project lists and we point interested potential contributors to it. It is therefore important that tackling these issues gives them a nice onboarding experience, with as few hurdles as possible.

Develepers should add the "good first issue" tag when they are confident that they can provide a good pull request for the issue with at most a few hours of work. Also, solving the issue should not require any difficult design decision. The issue should be uncontentious: it should be clear that the proposed solution should be accepted by the team.

## Reviewing pull requests {#reviewing-pull-requests}

### Process {#process}

1. A committer reviews the PR to check for the requirements below and tests it. Each PR should be linked to one or more corresponding issues and the reviewer should check that those are correctly addressed by the PR. The reviewer should be someone else than the PR author. For PRs with an important impact or contentious issues, it is important to leave enough time for other contributors to give their opinion.

2. The reviewer merges the pull request by squashing its commits into one (except for Weblate PRs which should be merged without squashing).

3. The reviewer adds the linked issues to the milestone for the next release (such as [the 3.5 milestone](https://github.com/OpenRefine/OpenRefine/milestone/17))

4. If the change is worth noting for users or developers, the reviewer adds an entry in the changelog for the next release (such as [Changes for 3.5](https://github.com/OpenRefine/OpenRefine/wiki/Changes-for-3.5))

### Requirements {#requirements}

#### Code style {#code-style}

Currently, only our code style for integration tests (using Cypress) is codified and enforced by the CI.
For the rest, we rely on imitating the surrounding code. [We should decide on a code style and check it in the CI for other areas of the tool](https://github.com/OpenRefine/OpenRefine/issues/2338).

#### Testing {#testing}

We currently rely have two sorts of tests:
* Backend tests, in Java, written with the TestNG framework. Their granularity varies, but generally speaking they are unit tests which test components in isolation.
* UI tests, in Javascript, written with the Cypress framework. They are integration tests which test both the frontend and the backend at the same time.

Changes to the backend should generally come with the accompanying TestNG tests.
Functional changes to the UI should ideally come with corresponding Cypress tests as well.

Those tests should be supplied in the same PR as the one that touches the product code.

#### Documentation {#documentation}

Changes to user-facing functionality should be reflected in the docs. Those documentation changes should happen in the same PR as the one that touches the product code.

#### UI style {#ui-style}

We do not have formally defined UI style guidelines. Contributors are invited to imitate the existing style.

#### Licensing and dependencies {#licensing-and-dependencies}

Dependencies can only be added if they are released under a license that is compatible with our BSD Clause-3 license.
One should pay attention to the size of the dependencies since they inflate the size of the release bundles.

#### Continuous integration {#continuous-integration}

The various check statuses reported by our continuous integration suite should be green.

### Special pull requests {#special-pull-requests}

#### Weblate PRs {#weblate-prs}

Weblate PRs should not be squashed as it prevents Weblate from recognizing that the corresponding changes have been made in master. They should be merged without squashing.

Reviewing Weblate PRs only amonuts to a quick visual sanity check as maintainers are not expected to master the languages involved. If corrections need to be made, they should be done in Weblate itself.

#### Dependabot PRs {#dependabot-prs}

When reviewing a Dependabot PR it is generally useful to pay attention to:
* the type of version change: most libraries follow the "semver" versioning convention, which indicates the nature of the change.
* the library's changelog, especially if the version change is more significant than a patch release

