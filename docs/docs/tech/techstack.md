---
id: techstack
title: Technology Stack
sidebar_label: Technology Stack
---

The server-side part of OpenRefine is implemented in Java as one single servlet which is executed by the [Jetty](http://jetty.codehaus.org/jetty/) web server + servlet container. The use of Java strikes a balance between performance and portability across operating system (there is very little OS-specific code and has mostly to do with starting the application).

OpenRefine has no database using its own in-memory data-store that is built up-front to be optimized for the operations required by faceted browsing and infinite undo.

The client-side part of OpenRefine is implemented in HTML, CSS and Javascript and uses the following libraries:
* [jQuery](http://jquery.com/)
* [jQueryUI](http:jqueryui.com/)
* [Recurser jquery-i18n](https://github.com/recurser/jquery-i18n)

The functional extensibility of OpenRefine is provided by the [SIMILE Butterfly](http://code.google.com/p/simile-butterfly/) modular web application framework.

Several projects provide the functionality to read and write custom format files (POI, opencsv, JENA, marc4j).

String clustering is provided by the [SIMILE Vicino](http://code.google.com/p/simile-vicino/) project.

OAuth functionality is provided by the [Signpost](https://github.com/mttkay/signpost) project.