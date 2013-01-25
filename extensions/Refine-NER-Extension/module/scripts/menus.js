/* Add menu to extension bar */
ExtensionBar.addExtensionMenu({
  id: "named-entity-recognition",
  label: "Named-entity recognition",
  submenu: [
    {
      id   : "named-entity-recognition/configuration",
      label: "Configure API keys...",
      click: dialogHandler(ConfigurationDialog),
    },
    { /* separator */ },
    {
      id   : "named-entity-recognition/about",
      label: "About...",
      click: dialogHandler(AboutDialog),
    },
  ]
});

/* Add submenu to column header menu */
DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
  MenuSystem.appendTo(menu, "", [
    { /* separator */ },
    {
      id: "named-entity-recognition/extract",
      label: "Extract named entities...",
      click: dialogHandler(ExtractionDialog, column),
    },
  ]);
});

function dialogHandler(dialogConstructor) {
  var dialogArguments = Array.prototype.slice.call(arguments, 1);
  function Dialog() { return dialogConstructor.apply(this, dialogArguments); }
  Dialog.prototype = dialogConstructor.prototype;
  return function () { new Dialog().show(); };
}
