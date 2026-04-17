/**
 * OpenRefine server API shared client-side helpers.
 * This file defines the global `OpenRefine` namespace.
 */
(function (global) {
  "use strict";

  var OpenRefine = global.OpenRefine || (global.OpenRefine = {});

  OpenRefine.getPreference = function (name) {
    return $.getJSON("command/core/get-preference", {name: name});
  };

  OpenRefine.setPreference = function (name, value) {
    return CSRFUtil.post({
      url: "command/core/set-preference",
      data: {
        name: name,
        value: JSON.stringify(value),
      }
    });
  }

})(window);
