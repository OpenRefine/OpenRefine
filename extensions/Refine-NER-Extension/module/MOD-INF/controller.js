var logger = Packages.org.slf4j.LoggerFactory.getLogger("NER-extension"),
    File = Packages.java.io.File,
    refineServlet = Packages.com.google.refine.RefineServlet,
    ner = Packages.org.freeyourmetadata.ner,
    services = ner.services,
    commands = ner.commands;

/* Initialize the extension. */
function init() {
  logger.info("Initializing service manager");
  var cacheFolder = new refineServlet().getCacheDir("ner-extension");
  var serviceManager = new services.NERServiceManager(new File(cacheFolder + "/services.json"));
  
  logger.info("Initializing commands");
  register("services", new commands.ServicesCommand(serviceManager));
  register("extractions", new commands.ExtractionCommand(serviceManager));
  
  logger.info("Initializing client resources");
  var resourceManager = Packages.com.google.refine.ClientSideResourceManager;
  resourceManager.addPaths(
    "project/scripts",
    module, [
      "scripts/config.js",
      "scripts/util.js",
      "dialogs/about.js",
      "dialogs/configuration.js",
      "dialogs/extraction.js",
      "scripts/menus.js",
    ]
  );
  resourceManager.addPaths(
    "project/styles",
    module, [
      "styles/main.less",
      "dialogs/dialogs.less",
      "dialogs/about.less",
      "dialogs/configuration.less",
      "dialogs/extraction.less",
    ]
  );
}

function register(path, command) {
  refineServlet.registerCommand(module, path, command);
}
