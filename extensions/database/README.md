This project is an OpenRefine extension for importing database data using JDBC.
For exporting to a database, other code can be found under folder `OpenRefine/main/src/com/google/refine/exporters/sql`

INSTALL

1. Before installing this extension download OpenRefine code from http://github.com/OpenRefine/OpenRefine 

2. Pull this extension's code into folder `/database` under folder `/extensions`. 
For more information on how to write OpenRefine extensions and where to put the files see https://docs.openrefine.org/technical-reference/writing-extensions#introduction

The folder structure should resemble this:
OpenRefine/
----------/extensions
--------------/database
------------------/module
------------------/src
------------------pom.xml
------------------README (this file)

3. Update `pom.xml` in folder `/extensions` with your dependencies, or build step changes as necessary for a new database vendor.
