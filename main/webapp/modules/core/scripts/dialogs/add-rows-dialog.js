/**
 * AddRowsDialog
 */

const AddRowsDialog = (function(path) {
  const $dialog = $(DOM.loadHTML("core", path));
  const $elmts = DOM.bind($dialog);
  let rows = [];
  let level;

  /** Class representing a row of form inputs */
  class Row {
    #tr;
    #cells;
    removeBtn;
    columns;

    /** Create a row to input new data */
    constructor($container, columns, removeCallback) {
      this.$container = $container;
      this.removeCallback = removeCallback;
      this.columns = columns;
      this.#tr = document.createElement("TR");
      this.removeBtn = document.createElement("BUTTON");
      this.#cells = [];
    }

    /** Add row to the DOM */
    init() {
      this.removeBtn.innerText = "x";
      this.removeBtn.setAttribute("type", "button");
      this.removeBtn.addEventListener('click', () => this.remove());

      const td = document.createElement("TD");
      td.style.background = "#fff";
      td.style.textAlign = "right";
      td.appendChild(this.removeBtn);
      this.#tr.appendChild(td);

      this.#cells = this.columns.map((column) => new Cell("text", column, this.#tr));
      this.#cells.forEach(cell => cell.init());

      this.$container.append(this.#tr);

      return this;
    }

    /**
     * Remove row from the dialog.
     * @returns {Row}
     */
    remove() {
      const index = this.$container.find("tr").index(this.#tr);
      this.removeCallback(index);
      this.#tr.remove();
      return this;
    }

    /**
     * Transform row data into a transmittable form
     * @returns {{cells: *, starred: boolean, flagged: boolean}}
     */
    serialize() {
      const maxIndex = theProject["columnModel"]["columns"].slice(-1)[0]["cellIndex"];
      const cellData = Array(maxIndex + 1);
      this.#cells.forEach(cell => cellData[cell.cellIndex] = cell.serialize());

      return {
        starred: false,
        flagged: false,
        cells: cellData
      }
    }

    /**
     * Prevent user from removing this row
     * @returns {Row}
     */
    disableRemoveBtn() {
      this.removeBtn.disabled = true;
      return this;
    }

    /**
     * Allow user to remove this row
     * @returns {Row}
     */
    enableRemoveBtn() {
      this.removeBtn.disabled = false;
      return this;
    }
  }  // end Row class

  /** Class representing an input cell */
  class Cell {

    #cellType;
    #cellIndex;
    #container;
    #name;

    /**
     * Create a new cell to input data
     * @param {string} cellType - the type of cell
     */
    constructor(cellType, column, container) {
      this.td = document.createElement("TD");
      this.input = document.createElement("INPUT");
      this.#name = column.name;
      this.#cellIndex = column.cellIndex;
      this.#cellType = cellType;
      this.#container = container;
      this.init();
    }

    /**
     *
     * @returns {Cell} this (for chaining)
     */
    init() {
      this.td.style.textAlign = "center";
      this.input.setAttribute("type", this.#cellType);
      this.input.setAttribute("name", this.#name);
      this.input.setAttribute("placeholder", "null");
      this.td.appendChild(this.input);
      this.#container.appendChild(this.td);
      return this;
    }

    /**
     * Transform cell data into a transmittable form
     * @returns {{v: Object}}
     */
    serialize() {
      return { "v": this.input.value }
    }

    get cellIndex() {
      return this.#cellIndex;
    }

  }  // end Cell class

  // Expose public IIFE functions
  return {
    init: init,
  };

    function init() {
      const columns = theProject["columnModel"]["columns"];

      $elmts["dialogHeader"].html($.i18n("core-views/add-rows/header"));

      $elmts["dialogDescription"].html($.i18n("core-views/add-rows/description"));

      $elmts["moreRowLabel"].html($.i18n("core-views/add-rows/label"));

      $elmts["moreRowButton"].html($.i18n("core-buttons/apply"));
      $elmts["moreRowButton"].on('click', () => {
        const value = Number.parseInt($elmts["moreRowCount"].val());
        _createRows(value);
      });

      $elmts["cancelButton"].html($.i18n('core-buttons/cancel'));
      $elmts["cancelButton"].on('click', _dismissDialog);

      $elmts["okButton"].html($.i18n('core-buttons/ok'));

      $elmts["dialogForm"].on('submit', _commit);

      $elmts["tableHead"].html([
        "<th style='background:#fff'>",
        ...columns
          .map((column) => `<th style="padding-left:5px;">${column.name}</th>`)
        ].join("")
      );

      _createRows(1);

      level = DialogSystem.showDialog($dialog);

    } // end init

  /**
   * Remove a recently deleted row from the internal row cache
   * @param {number} index the index of the row to remove
   * @returns {AddRowsDialog}
   */
  function _removeInputRow(index) {
    rows.splice(index, 1);
    if (rows.length === 1) {
      rows[0].disableRemoveBtn();
    }
  }

  /**
   * Remove dialog
   * @returns {AddRowsDialog}
   */
  function _dismissDialog() {
    DialogSystem.dismissUntil(level - 1);
    rows = [];
    $elmts["tableBody"].html(null);
    $elmts["tableHead"].html(null);
  }
  
  function _createRows(count) {
    const prevCount = rows.length;

    let row;
    for (let i = 0; i < count; i++) {
      row = new Row(
        $elmts["tableBody"],
        theProject["columnModel"]["columns"],
        (index) => _removeInputRow(index)
      );

      row.init();

      rows.push(row);
    }

    if(prevCount === 0)  {
      rows[0].disableRemoveBtn();
    } else if (prevCount === 1) {
      rows[0].enableRemoveBtn();
    }
  }

  /**
   *
   * @returns {_commit}
   */
  function _commit(e) {
    e.preventDefault();

    // Serialize form data
    const data = rows.map(row => JSON.stringify(row.serialize()));

    Refine.postCoreProcess(
      "add-rows",
      null,
      { "rows[]": data },
      null,
      {
        "onDone": (o) => Refine.fetchRows(
          theProject["rowModel"]["start"],
          null,
          () => ui.dataTableView.update(_dismissDialog),
          null),
        "onError": (o) => window.alert(`Error: ${o.message}`),
      });
  }

})("scripts/dialogs/add-rows-dialog.html");
