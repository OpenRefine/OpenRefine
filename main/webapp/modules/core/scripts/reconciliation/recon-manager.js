var ReconciliationManager = {
    "customServices" : [],     // services registered by core and extensions
    "standardServices" : []    // services registered by user
};

ReconciliationManager.getAllServices = function() {
    return ReconciliationManager.customServices.concat(ReconciliationManager.standardServices);
};

ReconciliationManager.registerService = function(service) {
    ReconciliationManager.customServices.push(service);
};

ReconciliationManager.registerStandardService = function(url) {
    $.ajax({
        async: false,
        url: url + (url.contains("?") ? "&" : "?") + "callback=?",
        success: function(data) {
            data.url = url;
            data.ui = { "handler" : "ReconStandardServicePanel" };
            
            ReconciliationManager.standardServices.push(data);
            ReconciliationManager.save();
        },
        dataType: "jsonp"
    });
};

ReconciliationManager.save = function(f) {
    $.ajax({
        async: false,
        type: "POST",
        url: "/command/set-preference?" + $.param({ 
            name: "standard-reconciliation-services" 
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
        url: "/command/get-preference?" + $.param({ 
            name: "standard-reconciliation-services" 
        }),
        success: function(data) {
            if (data.value && data.value != "null") {
                ReconciliationManager.standardServices = JSON.parse(data.value);
            } else {
                ReconciliationManager.registerStandardService(
                    "http://gridworks-helper.dfhuynh.user.dev.freebaseapps.com/reconcile");
            }
        },
        dataType: "json"
    });
})();
