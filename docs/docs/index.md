---
slug: /
id: index
title: OpenRefine user manual
sidebar_label: Introduction
---


This manual is designed to comprehensively walk through every aspect of setting up and using OpenRefine 3.4.1, including every interface function and feature. 

<!-- 
This documentation platform provides a separate version of the user manual for each version of OpenRefine (from 3.4.1 onwards) - if you're looking for a later version than 3.4.1, please select the correct version from the dropdown menu in the top bar of this page. 
-->

This user manual starts with instructions for [installing or upgrading OpenRefine on Windows, Mac, and Linux computers](manual/installing). It then walks you through [the interface and how to run OpenRefine](manual/running#jvm-preferences) from a program or command line, with or without setting custom preferences and modifications.

The manual then teaches you how to [start a project](manual/starting) by importing an existing dataset. We work through how to [view and learn about your data](manual/exploring) using facets, filters, and sorting. 

Then we launch into [transforming that data permanently](manual/transforming) through common and custom transformations, clustering, pulling data from the web, [reconciling](manual/reconciling), and [writing expressions](manual/expressions). 

Finally we discuss what to do with your improved dataset, whether [exporting](manual/exporting) it to a file or uploading statements to Wikidata. 

If you're stuck on any aspect and can't find an answer in the manual, try the [Troubleshooting page](manual/troubleshooting) for links to various places to find help. 

If you are new and want to learn how to use OpenRefine using an example dataset, you may wish to start with a user-contributed tutorial from our [recommendations list](https://github.com/OpenRefine/OpenRefine/wiki/External-Resources).

Bot passwords allow applications or bots to log in with a simple username-password combination even if the wiki employs some extra authorization steps that the bot framework could not handle; they also allow limiting the permissions given to the bot (e.g. the bot might be allowed to edit articles but not to create new ones). Clients using bot passwords can only access the API, not the normal web interface. The functionality is roughly equivalent to what some other sites like Google call "app passwords".

New bot passwords can be created and existing ones managed via Special:BotPasswords. Login happens via the login API module. When the user’s real password changes, bot passwords will not work until they’re reset; Special:BotPasswords will show a warning on each password that hasn’t been reset yet.

This is a simpler alternative to OAuth, meant for bots which do not support that. If the wiki and the bot support OAuth, use that instead; it is more secure, especially on wikis without robust HTTPS support.
