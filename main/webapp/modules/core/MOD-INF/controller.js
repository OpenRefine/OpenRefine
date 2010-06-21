var html = "text/html";
var encoding = "UTF-8";
var ClientSideResourceManager = Packages.com.metaweb.gridworks.ClientSideResourceManager;

var templatedFiles = {
    // Requests with last path segments mentioned here 
    // will get served from .vt files with the same names
    "project" : true
};

/*
 *  This optional function is invoked from the module's init() Java function.
 */
function init() {
    // Packages.java.lang.System.err.println("Initializing by script " + module);
    
    ClientSideResourceManager.addPaths(
        "project/scripts",
        module,
        [
            "wirings.js",
            
            "externals/jquery-1.4.2.min.js",
            "externals/jquery.cookie.js",
            "externals/suggest/suggest-1.2.min.js",
            "externals/jquery-ui/jquery-ui-1.8.custom.min.js",
            "externals/imgareaselect/jquery.imgareaselect.js",
            "externals/date.js",
            
            "scripts/util/misc.js",
            "scripts/util/url.js",
            "scripts/util/string.js",
            "scripts/util/ajax.js",
            "scripts/util/menu.js",
            "scripts/util/dialog.js",
            "scripts/util/dom.js",
            "scripts/util/sign.js",
            "scripts/util/freebase.js",
            "scripts/util/custom-suggest.js",

            "scripts/widgets/history-widget.js",
            "scripts/widgets/process-widget.js",
            "scripts/widgets/histogram-widget.js",
            "scripts/widgets/slider-widget.js",

            "scripts/project.js",
            "scripts/project/menu-bar.js",
            "scripts/project/browsing-engine.js",
            "scripts/project/scripting.js",

            "scripts/facets/list-facet.js",
            "scripts/facets/range-facet.js",
            "scripts/facets/scatterplot-facet.js",
            "scripts/facets/text-search-facet.js",

            "scripts/views/data-table/data-table-view.js",
            "scripts/views/data-table/data-table-cell-ui.js",
            "scripts/views/data-table/data-table-column-header-ui.js",

            "scripts/dialogs/recon-dialog.js",
            "scripts/dialogs/expression-preview-dialog.js",
            "scripts/dialogs/freebase-loading-dialog.js",
            "scripts/dialogs/clustering-dialog.js",
            "scripts/dialogs/scatterplot-dialog.js",
            "scripts/dialogs/extend-data-preview-dialog.js",
            "scripts/dialogs/templating-exporter-dialog.js",

            "scripts/protograph/schema-alignment.js",
            "scripts/protograph/schema-alignment-ui-node.js",
            "scripts/protograph/schema-alignment-ui-link.js"
        ]
    );
    
    ClientSideResourceManager.addPaths(
        "project/styles",
        module,
        [
            "externals/suggest/css/suggest-1.2.min.css",
            "externals/jquery-ui/css/ui-lightness/jquery-ui-1.8.custom.css",
            "externals/imgareaselect/css/imgareaselect-default.css",
    
            "styles/common.css",
            "styles/jquery-ui-overrides.css",
    
            "styles/util/menu.css",
            "styles/util/dialog.css",
            "styles/util/custom-suggest.css",

            "styles/project.css",
            "styles/project/browsing.css",
            "styles/project/process.css",
            "styles/project/menu-bar.css",
            
            "styles/widgets/history.css",
            "styles/widgets/histogram-widget.css",
            "styles/widgets/slider-widget.css",
            
            "styles/views/data-table-view.css",
            
            "styles/dialogs/expression-preview-dialog.css",
            "styles/dialogs/recon-dialog.css",
            "styles/dialogs/clustering-dialog.css",
            "styles/dialogs/scatterplot-dialog.css",
            "styles/dialogs/freebase-loading-dialog.css",
            "styles/dialogs/extend-data-preview-dialog.css",
            
            "styles/protograph/schema-alignment-dialog.css"
        ]
    );
}

/*
 * This is the function that is invoked by Butterfly
 */
function process(path, request, response) {
    if (path == "wirings.js") {
        var wirings = butterfly.getWirings(request);
        butterfly.sendString(
            request, 
            response, 
            "var ModuleWirings = " + butterfly.toJSONString(wirings) + ";", 
            encoding, 
            "text/javascript"
        );
    } else {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1);
        }
    
        var slash = path.lastIndexOf("/");
        var lastSegment = slash >= 0 ? path.substring(slash + 1) : path;
        if (lastSegment in templatedFiles) {
            var context = {};
            context.scripts = ClientSideResourceManager.getPaths(lastSegment + "/scripts");
            context.styles = ClientSideResourceManager.getPaths(lastSegment + "/styles");
        
            send(request, response, path + ".vt", context);
        }
    }
}

function send(request, response, template, context) {
    butterfly.sendTextFromTemplate(request, response, context, template, encoding, html);
}
