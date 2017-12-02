

function EditGeneralMetadataDialog(projectId, callback) {
    this._callback = callback;
    /*
  this._metaDataUIs = [];
  this._metaData = metaData;
  
  this._MetadataUI = function(tr, key, value, project) {
      var self = this;
      
      if (key === "date") {
          return;
      }
      
      var td0 = tr.insertCell(0);
      
      var keyLable = $.i18n._('core-index')[key] || key;
      $(td0).text(keyLable);

      var td1 = tr.insertCell(1);
      $(td1).text((value !== null) ? value : "");

      var td2 = tr.insertCell(2);
      
      if (key !== "created" && 
              key !== "modified" && 
              key !== "rowCount" && 
              key !== "importOptionMetadata" && 
              key !== "id")  {
          $('<button class="button">').text($.i18n._('core-index')["edit"]).appendTo(td2).click(function() {
            var newValue = window.prompt($.i18n._('core-index')["change-metadata-value"]+" " + key, value);
            if (newValue !== null) {
              $(td1).text(newValue);
              metaData[key] = newValue;
              $.post(
                "command/core/set-metaData",
                {
                  project : project,
                  name : key,
                  value : newValue
                },
                function(o) {
                  if (o.code === "error") {
                    alert(o.message);
                  } 
                },
                "json"
              );
            }
            
            Refine.OpenProjectUI.refreshProject(targetRowElem, metaData);
          });
      }
  };
  */
  this._createDialog();
  
  alert("_createDialog done");
}

EditGeneralMetadataDialog.prototype._createDialog = function() {
  var self = this;
  
  var frame = $(DOM.loadHTML("core", "scripts/project/edit-general-metadata-dialog.html"));
  this._elmts = DOM.bind(frame);  

  this._level = DialogSystem.showDialog(frame);
  this._elmts.closeButton.html($.i18n._('core-buttons')["close"]);
  this._elmts.closeButton.click(function() { self._dismiss(); });
  
  // var body = $("#metadata-body");
    
  $('<h1>').text($.i18n._('core-index')["metaDatas"]).appendTo(body);

/////////
//create the editor
  var editor = new JSONEditor(document.getElementById('jsoneditor'));

  // Load a JSON document

///////////
  

  
  $(".dialog-container").css("top", Math.round(($(".dialog-overlay").height() - $(frame).height()) / 2) + "px");
};

EditGeneralMetadataDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
    
    if (typeof this._callback === "function") 
        this._callback();
};

