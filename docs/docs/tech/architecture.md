---
id: architecture
title: OpenRefine Architecture
sidebar_label: Architecture
---

OpenRefine is a web application, but is designed to be run locally on your own machine. The server-side maintains states of the data (undo/redo history, long-running processes, etc.) while the client-side maintains states of the user interface (facets and their selections, view pagination, etc.). The client-side makes GET and POST ajax calls to cause changes to the data and to fetch data and data-related states from the server-side.

This architecture provides a good separation of concerns (data vs. UI); allows the use of familiar web technologies (HTML, CSS, Javascript) to implement user interface features; and enables the server side to be called by third-party software through standard GET and POST operations.

- [Technology Stack](Technology-Stack): What languages, libraries and frameworks are used in the OpenRefine application
- [Server Side Architecture](Server-Side-Architecture): how the data is modeled, stored, changed, etc.
- [Client Side Architecture](Client-Side-Architecture): how the UI is built
- [Importing Architecture](Importing-Architecture): how OpenRefine supports the import of data to create projects
- [Faceted Browsing Architecture](Faceted-Browsing-Architecture): how faceted browsing is implemented (this straddles the client and the server)
