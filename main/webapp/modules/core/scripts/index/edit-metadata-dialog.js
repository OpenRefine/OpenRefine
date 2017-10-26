

function EditMetadataDialog(metaData) {
  this._metaDataUIs = [];
  this._metaData = metaData;
  
  this._MetaDataUI = function(tr, key, value, project) {
      var self = this;

      var td0 = tr.insertCell(0);
      $(td0).text(key);

      var td1 = tr.insertCell(1);
      $(td1).text((value !== null) ? value : "");

      var td2 = tr.insertCell(2);

      $('<button class="button">').text($.i18n._('core-index')["edit"]).appendTo(td2).click(function() {
        var newValue = window.prompt($.i18n._('core-index')["change-value"]+" " + key, value);
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
      });
  }
  
  this._createDialog();
}

EditMetadataDialog.prototype._createDialog = function() {
  var self = this;
  
  var frame = $(DOM.loadHTML("core", "scripts/project/edit-metadata-dialog.html"));
  this._elmts = DOM.bind(frame);  

  this._level = DialogSystem.showDialog(frame);
  this._elmts.closeButton.html($.i18n._('core-buttons')["close"]);
  this._elmts.closeButton.click(function() { self._dismiss(); });
  
  var body = $("#metadata-body");
    
  $('<h1>').text($.i18n._('core-index')["metaDatas"]).appendTo(body);

  var metadataTable = $("<table>")
  .addClass("list-table")
  .addClass("preferences")
  .html('<tr><th>'+$.i18n._('core-index')["key"]+'</th><th>'+$.i18n._('core-index')["value"]+'</th><th></th></tr>')
  .appendTo(body)[0];

  for (var k in this._metaData) {
    var tr = metadataTable.insertRow(metadataTable.rows.length);
    
    if (typeof this._metaData[k] === 'string')
        v = this._metaData[k].replace(/\"/g, "");  
    else
        v = JSON.stringify(this._metaData[k]);
        
    
    this._metaDataUIs.push(new this._MetaDataUI(tr, k, v, this._metaData.id));
  }
  
}

EditMetadataDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

