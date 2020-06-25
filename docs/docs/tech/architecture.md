---
id: architecture
title: Architecture
sidebar_label: Architecture
---

OpenRefine is a web application, but is designed to be run locally on your own machine. The server-side maintains states of the data (undo/redo history, long-running processes, etc.) while the client-side maintains states of the user interface (facets and their selections, view pagination, etc.). The client-side makes GET and POST ajax calls to cause changes to the data and to fetch data and data-related states from the server-side.

This architecture provides a good separation of concerns (data vs. UI); allows the use of familiar web technologies (HTML, CSS, Javascript) to implement user interface features; and enables the server side to be called by third-party software through standard GET and POST operations.

- [Technology Stack](techstack): What languages, libraries and frameworks are used in the OpenRefine application
- [Server Side](server): how the data is modeled, stored, changed, etc.
- [Client Side](client): how the UI is built
- [Importing](importing): how OpenRefine supports the import of data to create projects
- [Faceted Browsing](faceted_browsing): how faceted browsing is implemented (this straddles the client and the server)
