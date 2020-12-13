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
  
  if (Host.isLocalhost()) {
    $('#projects-workspace-open').text($.i18n('core-index-open/browse'));
    $('#projects-workspace-open').on('click',function() {
      Refine.postCSRF(
        "command/core/open-workspace-dir",
        {},
        function (data) {
          if (data.code != "ok" && "message" in data) {
            alert(data.message);
          }
        },
        "json"
      );
    });
  } else {
    $('#projects-workspace-open').hide();
  }
  Refine.TagsManager.allProjectTags = [];
  this._buildTagsAndFetchProjects();
};

Refine.OpenProjectUI.prototype._fetchProjects = function() {
  var self = this;
  $.getJSON(
      "command/core/get-all-project-metadata",
      null,
      function(data) {
        self._renderProjects(data);
        self.resize(); // other version of this function excludes this resize
      },
      "json"
  );
};

Refine.OpenProjectUI.prototype._buildTagsAndFetchProjects = function() {
    this._buildProjectSearchPanel();
    Refine.OpenProjectUI.refreshTagsListPanel();
    this._fetchProjects();
    var tag = new URLSearchParams(window.location.search).get('tag');
    if (!tag) tag = '';
    Refine.OpenProjectUI._filterTags(tag);
};

Refine.OpenProjectUI.prototype._buildProjectSearchPanel = function(){
  var self = this;
  self._allTags = Refine.TagsManager._getAllProjectTags();
    var container = self._elmts.projectTags.empty();
  // Add search menu item
  var div = $('<div/>')
    .attr('id','divSearch')
    .appendTo(container)
  // Add form to the div on the left
    var form = $('<div/>')
    .attr('id','formSearch')
    .appendTo(div);
    // Add div for image in the form
  var divImage = $('<div/>')
    .attr('id','divImage')
    .appendTo(form)
  // Add img to the form
    $('<div/>')
    .html('<svg xmlns="http://www.w3.org/2000/svg" height="100%" viewBox="0 0 24 24" width="100%"><path d="M0 0h24v24H0z" fill="none"/><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>')
    .attr('id', 'searchIcon')
    .addClass("magnifying_glass")
    .appendTo(divImage);

    // Add div for input, in the form
  var divImage = $('<div/>')
    .attr('id','divInput')
    .appendTo(form)
    // Add input to the div
  $('<input/>')
    .attr('type', 'text')
    .attr('id','searchInProjects')
    .addClass("header-search-box").text('Search').appendTo(divImage);

    self._searchAnimation();
    self._searchInput();
}


Refine.OpenProjectUI.refreshTagsListPanel = function() {
    var allTags = Refine.TagsManager._getAllProjectTags();

    var ul = $("#tagsUl").empty();

    // Add 'all' menu item
    var li = $('<li/>').appendTo(ul);
    var a = $('<a/>').attr('href', '?tag=#open-project').text('All').appendTo(li);
    a.on('click', function(e) {
      e.preventDefault();
      Refine.OpenProjectUI._filterTags('');
    });

    $.each(allTags, function(i) {
      var li = $('<li/>').appendTo(ul);
      var a = $('<a/>').attr('href', '?tag=' + allTags[i] + '#open-project').text(allTags[i])
                      .appendTo(li);
      a.on('click', function(elm) {
        elm.preventDefault();
        Refine.OpenProjectUI._filterTags(allTags[i]);
      });
    });
};

Refine.OpenProjectUI._filterTags = function(tag) {
  // this function can run even if the open project UI is not visible to the user, so only update the URL if it is
  if (window.hash === "#open-project") window.history.pushState("", "", "?tag=" + tag + "#open-project");

  $('#tagsUl').children().each(function() {
    $(this).removeClass('current');
  });
  $('#tagsUl').find('a[href="?tag=' + tag + '#open-project"]').parent().addClass('current');

  $("#tableBody").children().each(function() {
    if ($(this).hasClass(tag) || tag === "") {
      $(this).show();
    } else {
      $(this).hide();
    }
  });
};

Refine.OpenProjectUI.prototype._searchAnimation = function() {
  var search = $('#searchIcon');
  var form = $('.header-search-box');
    var icon = $('.magnifying_glass');
  search.click(function () {
    if (form.is(':hidden'))
    {
      $("#tagsUl").hide()
      $("#divInput").show()
            icon.addClass("magnifying-glass-open")
      form.show()
            form.focus()
    }
    var widthFormOpen = Math.floor($('#right-panel-body').width() * 2 / 3);
    form.animate({
      'width': form.width() == widthFormOpen ? '0px' : widthFormOpen + "px"
    }, 'fast', function () {
      if (form.width() == 0) {
        form.hide()
                form.val('')
                icon.removeClass("magnifying-glass-open")
                $("#divInput").hide()
        $("#tagsUl").show()
      }
    });
  });
};

Refine.OpenProjectUI.prototype._searchInput = function() {
    var search = $('#searchInProjects');
    // search dynamically
    search.keypress(function (e) {
        // when enter is pressed
        if (e.keyCode == '13')
        {
            event.preventDefault();
            var text = ''
            text = search.val();
            // get the text, get back the projects that contains the text in the metadata
            $("#tableBody").filterListSearch(text);
        }

    });
};

// FIXME: This is overwriting an earlier function definition
Refine.OpenProjectUI.prototype._fetchProjects = function() {
    var self = this;
    $.ajax({
            type : 'GET',
            url : "command/core/get-all-project-metadata",
            dataType : 'json',
            success : function(data) {
                    self._renderProjects(data);
            },
            data : {},
            async : false
    });
};

Refine.OpenProjectUI.prototype._renderProjects = function(data) {
  const options  = { dateStyle: 'medium', timeStyle: 'medium' };
  const dateFormatter = new Intl.DateTimeFormat(Refine.userLang || navigator.language, options);
  var self = this;
  var projects = [];
  for (var n in data.projects) {
    if (data.projects.hasOwnProperty(n)) {
      var project = data.projects[n];
      if (project == null) {
          console.log('Project '+n+' is null. skipping...');
          continue;
      }
      project.id = n;
      if (!project.name) {
         console.log('Project '+project.id+' name is not set. skipping...');
         continue;
      }

      // project.modified is ISO 8601 format string
      const date = new Date(project.modified);
      project.date = dateFormatter.format(date);
      
      if (typeof project.userMetadata !== "undefined")  {
          for (var m in data.customMetadataColumns) {
              var found = false;
              for(var i = 0; i < project.userMetadata.length; i++) {
                  if (project.userMetadata[i].name === data.customMetadataColumns[m].name) {
                      found = true;
                      break;
                  }
                  if (!found) {
                      project.userMetadata.push({
                          name: data.customMetadataColumns[m].name,
                          display: data.customMetadataColumns[m].display,
                          value: ""
                      });
                  }
              }
          }
      }
    }
    projects.push(project);
  }

  var container = self._elmts.projectList.empty();
  if (!projects.length) {
    $("#no-project-message").clone().show().appendTo(container);
  } else {
    var projectsUl = $("<ul/>").attr('id', 'projectsUl').appendTo(container);

    var table = $(
      '<table class="tablesorter-blue list-table"><thead><tr>' +
      '<th></th>' +
      '<th></th>' +
      '<th>'+$.i18n('core-index-open/last-mod')+'</th>' +
      '<th>'+$.i18n('core-index-open/name')+'</th>' +
      '<th>'+$.i18n('core-index-open/tags')+'</th>' + 
      '<th>'+$.i18n('core-index-open/creator')+'</th>' +
      '<th>'+$.i18n('core-index-open/subject')+'</th>' +
      '<th>'+$.i18n('core-index-open/description')+'</th>' +
      '<th>'+$.i18n('core-index-open/row-count')+'</th>' + 
      (function() {
          var htmlDisplay = "";
          for (var n in data.customMetadataColumns) {
            if (data.customMetadataColumns[n].display) {
              htmlDisplay += '<th>'+ data.customMetadataColumns[n].name + '</th>';
            }
          }
          
          return htmlDisplay;
      })() +     
      '</tr></thead><tbody id="tableBody"></tbody></table>'
    ).appendTo(projectsUl)[0];

    var renderProject = function(project) {
      var tr = table.getElementsByTagName('tbody')[0].insertRow(table.rows.length - 1);
      tr.className = "project";

      var deleteLink = $('<a></a>')
      .addClass("delete-project")
      .attr("title",$.i18n('core-index-open/del-title'))
      .attr("href","")
      .html("<img src='images/close.png' />")
      .on('click',function() {
        if (window.confirm($.i18n('core-index-open/del-body', project.name))) {
          Refine.postCSRF(
            "command/core/delete-project",
            { "project" : project.id },
            function (data) {
              if (data && typeof data.code != 'undefined' && data.code == "ok") {
                Refine.TagsManager.allProjectTags = [];
                self._buildTagsAndFetchProjects();
              }
            },
            "json"
          );
        }
        return false;
      }).appendTo(
        $(tr.insertCell(tr.cells.length))
      );

      var editMetadataLink = $('<a></a>')
      .text($.i18n('core-index-open/edit-meta-data'))
      .addClass("secondary")
      .attr("href", "javascript:{}")
      .on('click',function() {
          new EditMetadataDialog(project, $(this).parent().parent());
      })
      .appendTo(
        $(tr.insertCell(tr.cells.length))
      );
      
      $('<div></div>')
      .html('<span style="display:none">' + project.modified + '</span>' + project.date)
      .addClass("last-modified")
      .appendTo($(tr.insertCell(tr.cells.length)));
      
      var nameLink = $('<a></a>')
      .addClass("project-name")
      .text(project.name)
      .attr("href", "project?project=" + project.id)
      .appendTo($(tr.insertCell(tr.cells.length)));
      
    var tagsCell = $(tr.insertCell(tr.cells.length));
    var tags = project.tags;
    tags.map(function(tag){
        $("<span/>")
        .addClass("project-tag")
        .attr("data-tag-name", tag)
        .text(tag)
        .appendTo(tagsCell);
        $(tr).addClass(tag);
    });
    
    
    var appendMetaField = function(data) {
        $('<div></div>')
        .html(data)
        .appendTo($(tr.insertCell(tr.cells.length)));
    };
    
    appendMetaField(project.creator);
    appendMetaField(project.subject);
    appendMetaField(project.description, '20%');
    appendMetaField(project.rowCount);
    
    var data = project.userMetadata;
    for(var i in data)
    {
         if (data[i].display === true) {
             appendMetaField(data[i].value); 
         }
    }
        

      };

    for (var i = 0; i < projects.length; i++) {
      renderProject(projects[i]);
    }

    $(table).tablesorter({
        headers : {
            0: { sorter: false },
            1: { sorter: false },
            2: { sorter: "text" }
        },
        sortList: [[2,1]],
        widthFixed: false
    });
    self._addTagFilter();
  }
};

Refine.OpenProjectUI.prototype._addTagFilter = function() {
  if (window.location.hash === 'open-project') {
    var tag = new URLSearchParams(window.location.search).get("tag", "");
    Refine.OpenProjectUI._filterTags(tag);
  }
};

Refine.OpenProjectUI.refreshProject = function(tr, metaData, project) {

    var refreshMetaField = function(data, index) {
        if (index === 3) {
            $('a', $('td', tr).eq(index))
            .text(data);
        } else {
            $('td', tr).eq(index).find('div')
            .text(data);
        }
    };
    
    var refreshMetaTags = function(data, index) {
        var tagCol = $('td', tr).eq(index).empty();
        if(data.constructor === Array){
            data.map(function(tag){
                var tagsCell = $("<span/>")
                .addClass("project-tag")
                .attr("data-tag-name", tag)
                .text(tag)
                .appendTo(tagCol);
                tagCol.parent().addClass(tag);
            });
   
        } else{
            data.split(",").map(function(tag){
                var tagsCell = $("<span/>")
                .addClass("project-tag")
                .attr("data-tag-name", tag)
                .text(tag)
                .appendTo(tagCol);
                tagCol.parent().addClass(tag);
            });
            
    }
   
    };
    
    var index = 3;
    refreshMetaField(metaData.name, index); index++;
    refreshMetaTags(metaData.tags, index); index++;
    refreshMetaField(metaData.creator, index); index++;
    refreshMetaField(metaData.subject,index); index++;
    refreshMetaField(metaData.description,index); index++;
    refreshMetaField(metaData.rowCount,index); index++;
    
    var updateUserMetadata = function(ob) {
        var userMetadata = ob.userMetadata;
        
        for ( var n in ob) {
            for ( var i in userMetadata) {
                if (n === userMetadata[i].name) {
                    userMetadata[i].value = ob[n];
                    break;
                }
            }
        }
        
        ob.userMetadata = userMetadata;
    };
    updateUserMetadata(metaData);
    var data = metaData.userMetadata;
    for(var i in data)
    {
         if (data[i].display === true) {
             refreshMetaField(data[i].value,index); index++; 
         }
    }

    Refine.OpenProjectUI.refreshTagsListPanel();
};

Refine.actionAreas.push({
  id: "open-project",
  label: $.i18n('core-index-open/open-proj'),
  uiClass: Refine.OpenProjectUI
});
