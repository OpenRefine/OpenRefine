
var doTextTransform = function(columnName, expression, onError, repeat, repeatCount) {
    Refine.postCoreProcess(
      "text-transform",
      {
        columnName: columnName, 
        expression: expression, 
        onError: onError,
        repeat: repeat,
        repeatCount: repeatCount
      },
      null,
      { cellsChanged: true }
    );
};

function ExpressionColumnDialog(expression, onError, repeat, repeatCount) {
  this._expression = expression;
  this._onError = onError;
  this._repeat = repeat;
  this._repeatCount = repeatCount;
  
  this._createDialog();
}

ExpressionColumnDialog.prototype._createDialog = function() {
  var self = this;
  
  this._dialog = $(DOM.loadHTML("core", "scripts/dialogs/expression-column-dialog.html"));
  this._elmts = DOM.bind(this._dialog);
  this._level = DialogSystem.showDialog(this._dialog);
  
  
  this._elmts.dialogHeader.html($.i18n._('core-dialogs')["custom-tab-exp"]);
  this._elmts.or_dialog_content.html($.i18n._('core-dialogs')["content"]);
  this._elmts.or_dialog_selAndOrd.html($.i18n._('core-dialogs')["sel-and-ord"]);
  this._elmts.selectAllButton.html($.i18n._('core-buttons')["select-all"]);
  this._elmts.deselectAllButton.html($.i18n._('core-buttons')["deselect-all"]);
  this._elmts.okButton.html($.i18n._('core-buttons')["ok"]);
  this._elmts.cancelButton.html($.i18n._('core-buttons')["cancel"]);

  
  /*
   * Populate column list.
   */
  for (var i = 0; i < theProject.columnModel.columns.length; i++) {
    var column = theProject.columnModel.columns[i];
    var name = column.name;
    var div = $('<div>')
        .addClass("custom-tabular-exporter-dialog-column")
        .attr("column", name)
        .appendTo(this._elmts.columnList);
    
    $('<input>')
      .attr('type', 'checkbox')
      .attr('checked', 'checked')
      .appendTo(div);
    $('<span>')
      .text(name)
      .appendTo(div);
    
  }
  this._elmts.columnList.sortable({});
  
  /*
   * Hook up event handlers.
   */
  
  this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').click(function() {
    self._elmts.columnList.find('.custom-tabular-exporter-dialog-column').removeClass('selected');
    $(this).addClass('selected');
  });
  this._elmts.selectAllButton.click(function() {
    self._elmts.columnList.find('input[type="checkbox"]').attr('checked', true);
  });
  this._elmts.deselectAllButton.click(function() {
    self._elmts.columnList.find('input[type="checkbox"]').attr('checked', false);
  });
  
  this._elmts.okButton.click(function() { self._transform(); });
  this._elmts.cancelButton.click(function() { self._dismiss(); });
}  
  

ExpressionColumnDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 2);
};


ExpressionColumnDialog.prototype._transform = function() {
  this._postSelect();
  this._dismiss();
};

ExpressionColumnDialog.prototype._postSelect = function() {
	var self = this;
	this._elmts.columnList.find('.custom-tabular-exporter-dialog-column').each(function() {
    if ($(this).find('input[type="checkbox"]')[0].checked) {
      var name = this.getAttribute('column');
      // alert("doTextTransform on: " + name + "; expression: " + self._expression);
	  doTextTransform(name, self._expression, self._onError, self._repeat, self._repeatCount)
    }
  });
  
};



