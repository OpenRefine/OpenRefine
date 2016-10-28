/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
 * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

Refine.OpenProjectUI = function(elmt) {
  var self = this;

  elmt.html(DOM.loadHTML("core", "scripts/index/open-project-ui.html"));

  this._elmt = elmt;
  this._elmts = DOM.bind(elmt);

  $("#project-file-input").change(function() {
    if ($("#project-name-input")[0].value.length === 0) {
      var fileName = this.files[0].fileName;
      if (fileName) {
        $("#project-name-input")[0].value = fileName.replace(/\.\w+/, "").replace(/[_\-]/g, " ");
      }
      $("#project-name-input").focus().select();
    }
  }).keypress(function(evt) {
    if (evt.keyCode == 13) {
      return self._onClickUploadFileButton(evt);
    }
  });

  $("#upload-file-button").click(function(evt) {
    return self._onClickUploadFileButton(evt);
  });

  $('#projects-workspace-open').text($.i18n._('core-index-open')["browse"]);
  $('#projects-workspace-open').click(function() {
    $.ajax({
      type: "POST",
      url: "command/core/open-workspace-dir",
      dataType: "json",
      success: function (data) {
        if (data.code != "ok" && "message" in data) {
          alert(data.message);
        }
      }
    });
  });

  this._fetchProjects();
};

Refine.OpenProjectUI.prototype.resize = function() {
  var height = this._elmt.height();
  var width = this._elmt.width();
  var controlsHeight = this._elmts.workspaceControls.outerHeight();

  this._elmts.projectsContainer
  .css("height", (height - controlsHeight - DOM.getVPaddings(this._elmts.projectsContainer)) + "px");

  this._elmts.workspaceControls
  .css("bottom", "0px")
  .css("width", (width - DOM.getHPaddings(this._elmts.workspaceControls)) + "px");
};

Refine.OpenProjectUI.prototype._fetchProjects = function() {
  var self = this;
  $.getJSON(
      "command/core/get-all-project-metadata",
      null,
      function(data) {
        self._renderProjects(data);
        self.resize();
      },
      "json"
  );
};

Refine.OpenProjectUI.prototype._renderProjects = function(data) {
  var self = this;
  var projects = [];
  for (var n in data.projects) {
    if (data.projects.hasOwnProperty(n)) {
      var project = data.projects[n];
      project.id = n;
      project.date = Date.parseExact(project.modified, "yyyy-MM-ddTHH:mm:ssZ");
      projects.push(project);
    }
  }
  projects.sort(function(a, b) { return b.date.getTime() - a.date.getTime(); });

  var container = $("#projects-container").empty();
  if (!projects.length) {
    $("#no-project-message").clone().show().appendTo(container);
  } else {
    Refine.selectActionArea('open-project');

    var table = $(
      '<table class="list-table"><tr>' +
      '<th></th>' +
      '<th></th>' +
      '<th>'+$.i18n._('core-index-open')["last-mod"]+'</th>' +
      '<th>'+$.i18n._('core-index-open')["name"]+'</th>' +
      '</tr></table>'
    ).appendTo(container)[0];

    var renderProject = function(project) {
      var tr = table.insertRow(table.rows.length);
      tr.className = "project";

      var deleteLink = $('<a></a>')
      .addClass("delete-project")
      .attr("title",$.i18n._('core-index-open')["del-title"])
      .attr("href","")
      .css("visibility", "hidden")                
      .html("<img src='images/close.png' />")
      .click(function() {
        if (window.confirm($.i18n._('core-index-open')["del-body"] + project.name + "\"?")) {
          $.ajax({
            type: "POST",
            url: "command/core/delete-project",
            data: { "project" : project.id },
            dataType: "json",
            success: function (data) {
              if (data && typeof data.code != 'undefined' && data.code == "ok") {
                self._fetchProjects();
              }
            }
          });
        }
        return false;
      }).appendTo(
        $(tr.insertCell(tr.cells.length)).css('width', '1%')
      );

      var renameLink = $('<a></a>')
      .text($.i18n._('core-index-open')["rename"])
      .addClass("secondary")
      .attr("href", "javascript:{}")
      .css("visibility", "hidden")
      .click(function() {
        var name = window.prompt($.i18n._('core-index-open')["new-title"], project.name);
        if (name === null) {
          return;
        }

        name = $.trim(name);
        if (project.name == name || name.length === 0) {
          return;
        }

        $.ajax({
          type: "POST",
          url: "command/core/rename-project",
          data: { "project" : project.id, "name" : name },
          dataType: "json",
          success: function (data) {
            if (data && typeof data.code != 'undefined' && data.code == "ok") {
              nameLink.text(name);
            } else {
              alert($.i18n._('core-index-open')["warning-rename"]+" " + data.message);
            }
          }
        });
      }).appendTo(
        $(tr.insertCell(tr.cells.length)).css('width', '1%')
      );

      $('<div></div>')
      .html(formatRelativeDate(project.date))
      .addClass("last-modified")
      .attr("title", project.date.toString())
      .appendTo($(tr.insertCell(tr.cells.length)).attr('width', '1%'));

      var nameLink = $('<a></a>')
      .addClass("project-name")
      .text(project.name)
      .attr("href", "project?project=" + project.id)
      .appendTo(tr.insertCell(tr.cells.length));

      $(tr).mouseenter(function() {
        renameLink.css("visibility", "visible");
        deleteLink.css("visibility", "visible");
      }).mouseleave(function() {
        renameLink.css("visibility", "hidden");
        deleteLink.css("visibility", "hidden");
      });
    };

    for (var i = 0; i < projects.length; i++) {
      renderProject(projects[i]);
    }
  }
};

Refine.OpenProjectUI.prototype._onClickUploadFileButton = function(evt) {
  var projectName = $("#project-name-input")[0].value;
  var dataURL = $.trim($("#project-url-input")[0].value);
  if (! $.trim(projectName).length) {
    window.alert($.i18n._('core-index-open')["warning-proj-name"]);

  } else if ($("#project-file-input")[0].files.length === 0 && ! dataURL.length) {
    window.alert($.i18n._('core-index-open')["warning-data-file"]);

  } else {
    $("#file-upload-form").attr("action",
        "command/core/create-project-from-upload?" + [
          "url=" +                encodeURIComponent(dataURL),
          "split-into-columns=" + $("#split-into-columns-input")[0].checked,
          "separator=" +          $("#separator-input")[0].value,
          "ignore=" +             $("#ignore-input")[0].value,
          "header-lines=" +       $("#header-lines-input")[0].value,
          "skip=" +               $("#skip-input")[0].value,
          "limit=" +              $("#limit-input")[0].value,
          "guess-value-type=" +   $("#guess-value-type-input")[0].checked,
          "ignore-quotes=" +      $("#ignore-quotes-input")[0].checked
        ].join("&"));

    return true;
  }

  evt.preventDefault();
  return false;
};

Refine.actionAreas.push({
  id: "open-project",
  label: $.i18n._('core-index-open')["open-proj"],
  uiClass: Refine.OpenProjectUI
});
