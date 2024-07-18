const AddRowsDialog = (function (path) {
  let level;
  const $dialog = $(DOM.loadHTML("core", path));

  class Row {
    #isFlagged;
    #isStarred;
    #cells;

    constructor(isFlagged= false, isStarred= false, cells = []) {
      this.#isFlagged = isFlagged;
      this.#isStarred = isStarred;
      this.#cells = cells;

      return this;
    }

    serialize() {
      return JSON.stringify({});
    }
  }

  return {
    prependBlankRow: () => _submit([ new Row().serialize() ], 0),
    appendBlankRow: () => _submit([ new Row().serialize() ], theProject.metadata.rowCount),
    initDialog: initDialog,
  };


  /**
   * Initialize and open dialog
   */
  function initDialog() {
    const $elements = DOM.bind($dialog);

    $elements["dialogHeader"].html($.i18n("core-views/add-rows"));
    $elements["addRowsCountLabel"].html($.i18n("core-views/add-rows/count-label"));
    $elements["addRowsPositionLabel"].html($.i18n("core-views/add-rows/insertion-label"));

    $elements["optionBeginning"].html($.i18n("core-views/add-rows/beginning-option"));
    $elements["optionBeginning"].val(0);

    $elements["optionEnd"].html($.i18n("core-views/add-rows/end-option"));
    $elements["optionEnd"].val(theProject.metadata.rowCount);

    $elements["cancelButton"].html($.i18n('core-buttons/cancel'));
    $elements["cancelButton"].on('click', _dismissDialog);

    $elements["okButton"].html($.i18n('core-buttons/ok'));

    $elements["dialogForm"].on('submit', function(event) {
      event.preventDefault();
      const $form = $(this);
      let index = $form.find("select#add-rows-position").val();

      const rowCount = parseInt($form.find("input#add-rows-count").val());
      const data = Array(rowCount).fill(new Row().serialize());

      _submit(data, index);
    });

    level = DialogSystem.showDialog($dialog);

  } // end initDialog

  /**
   * Remove dialog
   * @private
   */
  function _dismissDialog() {
      DialogSystem.dismissUntil(level - 1);
  }

  /**
   * Send POST request to the add rows command
   * @param {array} data: Serialized row data
   * @param {number} index: the project index into which new rows are inserted
   * @private
   */
  function _submit(data, index) {
    return Refine.postCoreProcess(
      "add-rows",
      null,
      {
        "rows[]": data,
        "index": index,
      },
      { modelsChanged: true },
      {
        "onDone": _dismissDialog,
        "onError": (o) => window.alert(`Error: ${o.message}`),
      });
  }

})("scripts/dialogs/add-rows-dialog.html");
