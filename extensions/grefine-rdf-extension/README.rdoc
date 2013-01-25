This project adds a graphical user interface(GUI) for exporting data of Google Refine projects in RDF format. The export is based on mapping the data to a template graph using the GUI.

INSTALL

you need to have the code of Google Refine http://code.google.com/p/google-refine/source/checkout . Then pull the code of this extension into the extensions folder available with Google Refine code. see http://code.google.com/p/google-refine/wiki/WriteAnExtension

* Rename the extension source folder to "rdf-extension"
* make sure to update the extensions/build.xml file*  you need to add build and clean ant tasks to the extension.

The two lines that need to be added are:
	<ant dir="rdf-export/" target="build" />
	<ant dir="rdf-export/" target="clean" />
	
the whole file is shown below:


  <project name="google-refine-extensions" default="build" basedir=".">
  <target name="build">
    <echo message="Building extensions" />
    <ant dir="sample/" target="build" />
    <ant dir="jython/" target="build" />
    <ant dir="freebase/" target="build" />
    <ant dir="gdata/" target="build" />
    <ant dir="rdf-extension/" target="build" />
  </target>

  <target name="clean">
    <echo message="cleaning extensions" />
    <ant dir="sample/" target="clean" />
    <ant dir="jython/" target="clean" />
    <ant dir="freebase/" target="clean" />
    <ant dir="gdata/" target="clean" />
    <ant dir="rdf-extension/" target="clean" />
  </target>
  </project>