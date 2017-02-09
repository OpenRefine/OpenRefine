/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

function FairDataPointPostCatalogDialog(callback){
    this._createDialog()
    this._callback = callback;
};

FairDataPointPostCatalogDialog.prototype._createDialog = function() {
    var frame = DialogSystem.createDialog();
    this._level = DialogSystem.showDialog(frame);
    frame.width("900px");
    
    $('<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"/>').prependTo("head");

    var header = $('<div></div>').addClass("dialog-header").text("Add new catalog to FAIR Data Point").appendTo(frame);
    var body = $('<div id="metadata-target"><metadata-form view="catalog"></metadata-form></div>').appendTo(frame);

    $('<script>\n' +
        '(function() {\n'+
            "var $injector = angular.bootstrap($('div#metadata-target'), ['metadata.form']);\n"+
            "var service = $injector.get('CommService');\n"+
            "service.setCallback(function(model) {\n"+
                "fairDataPointPostCatalogDialog._setModel(model);\n" + 
            "});\n"+
        '})();\n'+
    '</script>').appendTo("body");
};

FairDataPointPostCatalogDialog.prototype._setModel = function(model) {
    this._callback(model);
    DialogSystem.dismissUntil(this._level - 1);
};



