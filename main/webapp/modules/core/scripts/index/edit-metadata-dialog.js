

function EditMetadataDialog(metaData, targetRowElem) {
  this._metaDataUIs = [];
  this._metaData = metaData;
  
  this._MetadataUI = function(tr, key, value, project) {
      var self = this;
      
      if (key === "date") {
          return;
      }
      
      var td0 = tr.insertCell(0);

      var td1 = tr.insertCell(1);
      var keyLable = $.i18n._('core-index')[key] || key;
      $(td1).text(keyLable);

      var td2 = tr.insertCell(2);
      $(td2).text((value !== null) ? value : "");
      
      if(key==="tags"){
          $('<button class="button">').text($.i18n._('core-index')["edit"]).appendTo(td0).click(function() {
              var oldTags = $(td1).text().replace("[","").replace("]","");
              oldTags = replaceAll(oldTags,"\"","");
              var newTags = window.prompt($.i18n._('core-index')["change-metadata-value"]+" " + key, $(td2).text());
              newTags = newTags.replace("[","").replace("]","");
              newTags = replaceAll(newTags,"\"","");
              if (newTags !== null) {
                  $(td2).text(newTags);
                  metaData[key] = newTags;
                  $.ajax({
                      type : "POST",
                      url : "command/core/set-project-tags",
                      data : {
                          "project" : project,
                          "old" : oldTags,
                          "new" : newTags
                      },
                      dataType : "json",
                  });
              }
              
              Refine.OpenProjectUI.refreshProject(targetRowElem, metaData, project);
            });
      }
      
      if (key !== "created" && 
              key !== "modified" && 
              key !== "rowCount" && 
              key !== "importOptionMetadata" && 
              key !== "id" &&
              key !== "tags")  {
          $('<button class="button">').text($.i18n._('core-index')["edit"]).appendTo(td0).click(function() {
            var newValue = window.prompt($.i18n._('core-index')["change-metadata-value"]+" " + key, value);
            if (newValue !== null) {
              $(td2).text(newValue);
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
            
            Refine.OpenProjectUI.refreshProject(targetRowElem, metaData, project);
          });
      }
  };
  
  this._createDialog();
}

EditMetadataDialog.prototype._createDialog = function() {
  var self = this;
  
  var frame = $(DOM.loadHTML("core", "scripts/project/edit-metadata-dialog.html"));
  this._elmts = DOM.bind(frame);  

  this._level = DialogSystem.showDialog(frame);
  this._elmts.closeButton.html($.i18n._('core-buttons')["close"]);
  this._elmts.closeButton.click(function() { self._dismiss();Refine.OpenProjectUI.prototype._addTagFilter()});
  
  var body = $("#metadata-body");
    
  $('<h1>').text($.i18n._('core-index')["metaDatas"]).appendTo(body);

  var metadataTable = $("<table>")
  .addClass("list-table")
  .addClass("preferences")
  .html('<tr><th></th><th>'+$.i18n._('core-index')["key"]+'</th><th>'+$.i18n._('core-index')["value"]+'</th><th></th></tr>')
  .appendTo(body)[0];

    var flattenObject = function(ob, key) {
        var toReturn = {};
        for ( var i in ob) {
            if (i !== key) {
                toReturn[i] = ob[i];
                continue;
            }
            for ( var x in ob[i]) {
                toReturn[ob[i][x].name] = ob[i][x].value;
            }
        }
        return toReturn;
    };
    
  var flatMetadata = flattenObject(this._metaData, "userMetadata");
      
  for (var k in flatMetadata) {
    var tr = metadataTable.insertRow(metadataTable.rows.length);
    var v;
    
    if (typeof flatMetadata[k] === 'string') {
        v = flatMetadata[k].replace(/\"/g, "");  
    } else {
        v = JSON.stringify(flatMetadata[k]);
    }
    
    this._metaDataUIs.push(new this._MetadataUI(tr, k, v, flatMetadata.id));
  }
  
  $(".dialog-container").css("top", Math.round(($(".dialog-overlay").height() - $(frame).height()) / 2) + "px");
};

EditMetadataDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

function escapeRegExp(str) {
    return str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
  }

function replaceAll(str, find, replace) {
    return str.replace(new RegExp(escapeRegExp(find), 'g'), replace);
  }

