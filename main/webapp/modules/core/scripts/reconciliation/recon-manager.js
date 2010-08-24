var ReconciliationManager = {
    customServices : [],     // services registered by core and extensions
    standardServices : [],   // services registered by user
    _urlMap : {}
};

ReconciliationManager.isFreebaseId = function(s) {
    return s == "http://rdf.freebase.com/ns/type.object.id";
};

ReconciliationManager._rebuildMap = function() {
    var map = {};
    $.each(ReconciliationManager.getAllServices(), function(i, service) {
        if ("url" in service) {
            map[service.url] = service;
        }
    });
    ReconciliationManager._urlMap = map;
};

ReconciliationManager.getServiceFromUrl = function(url) {
    return ReconciliationManager._urlMap[url];
};

ReconciliationManager.getAllServices = function() {
    return ReconciliationManager.customServices.concat(ReconciliationManager.standardServices);
};

ReconciliationManager.registerService = function(service) {
    ReconciliationManager.customServices.push(service);
    ReconciliationManager._rebuildMap();
    
    return ReconciliationManager.customServices.length - 1;
};

ReconciliationManager.registerStandardService = function(url, f) {
    var dismissBusy = DialogSystem.showBusy();
    
    $.getJSON(
        url + (url.contains("?") ? "&" : "?") + "callback=?",
        null,
        function(data) {
            data.url = url;
            data.ui = { "handler" : "ReconStandardServicePanel" };
            
            index = ReconciliationManager.customServices.length + 
                ReconciliationManager.standardServices.length;
            
            ReconciliationManager.standardServices.push(data);
            ReconciliationManager._rebuildMap();
            
            ReconciliationManager.save();
            
            dismissBusy();
            
            if (f) {
                f(index);
            }
        },
        "jsonp"
    );
};

ReconciliationManager.unregisterService = function(service, f) {
    for (var i = 0; i < ReconciliationManager.customServices.length; i++) {
        if (ReconciliationManager.customServices[i] === service) {
            ReconciliationManager.customServices.splice(i, 1);
            break;
        }
    }
    for (var i = 0; i < ReconciliationManager.standardServices.length; i++) {
        if (ReconciliationManager.standardServices[i] === service) {
            ReconciliationManager.standardServices.splice(i, 1);
            break;
        }
    }
    ReconciliationManager.save(f);
};

ReconciliationManager.save = function(f) {
    $.ajax({
        async: false,
        type: "POST",
        url: "/command/core/set-preference?" + $.param({ 
            name: "reconciliation.standardServices" 
        }),
        data: { "value" : JSON.stringify(ReconciliationManager.standardServices) },
        success: function(data) {
            if (f) { f(); }
        },
        dataType: "json"
    });
};

(function() {
    ReconciliationManager.customServices.push({
        "name" : "Freebase Query-based Reconciliation",
        "ui" : { "handler" : "ReconFreebaseQueryPanel" }
    });
    
    $.ajax({
        async: false,
        url: "/command/core/get-preference?" + $.param({ 
            name: "reconciliation.standardServices" 
        }),
        success: function(data) {
            if (data.value && data.value != "null") {
                ReconciliationManager.standardServices = JSON.parse(data.value);
                ReconciliationManager._rebuildMap();
            } else {
                ReconciliationManager.registerStandardService(
                    "http://2.standard-reconcile.dfhuynh.user.dev.freebaseapps.com/reconcile");
            }
        },
        dataType: "json"
    });
})();
