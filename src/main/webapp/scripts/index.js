function onClickUploadFileButton(evt) {
    var projectName = $("#project-name-input")[0].value;
    if (! $.trim(projectName).length) {
        window.alert("You must specify a project name.");
        
    } else if ($("#project-file-input")[0].files.length === 0) {
        window.alert("You must specify select a file to upload.");
        
    } else {
        $("#file-upload-form").attr("action", 
            "/command/create-project-from-upload?" + [
                "split-into-columns=" + $("#split-into-columns-input")[0].checked,
                "separator=" + $("#separator-input")[0].value,
                "ignore=" + $("#ignore-input")[0].value,
                "header-lines=" + $("#header-lines-input")[0].value,
                "skip=" + $("#skip-input")[0].value,
                "limit=" + $("#limit-input")[0].value,
                "guess-value-type=" + $("#guess-value-type-input")[0].checked
            ].join("&"));
            
        return true;
    }
    
    evt.preventDefault();
    return false;
}

function formatDate(d) {
    var d = new Date(d);
    var last_year = Date.today().add({ years: -1 });
    var last_month = Date.today().add({ months: -1 });
    var last_week = Date.today().add({ days: -7 });
    var today = Date.today();
    var tomorrow = Date.today().add({ days: 1 });

    if (d.between(today, tomorrow)) {
        return "today";
    } else if (d.between(last_week, today)) {
        var diff = today.getDayOfYear() - d.getDayOfYear();
        return (diff == 1) ? "yesterday" : diff + " days ago";
    } else if (d.between(last_month, today)) {
        var diff = (today.getDayOfYear() - d.getDayOfYear()) / 7;
        return (diff == 1) ? "a week ago" : diff.toFixed(0) + " weeks ago" ;
    } else if (d.between(last_year, today)) {
        var diff = today.getMonth() - d.getMonth();
        return (diff == 1) ? "a month ago" : diff + " months ago";
    } else {
        var diff = today.getYear() - d.getYear();
        return (diff == 1) ? "a year ago" : diff + " years ago";
    }
}

function isThereNewRelease() {
    var thisRevision = GridworksVersion.revision;
    
    var revision_pattern = /r([0-9]+)/;
    
    if (!revision_pattern.test(thisRevision)) { // probably "trunk"
        return false;
    }

    var latestRevision = GridworksReleases.releases[0].revision;
    
    var thisRev = parseInt(revision_pattern.exec(thisRevision)[1],10);
    var latestRev = parseInt(revision_pattern.exec(GridworksReleases.releases[0].revision)[1],10);
    
    return latestRev > thisRev;
}

function renderProjects(data) {
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
        $('<div>')
            .addClass("message")
            .text("No existing project. Use form on right to create.")
            .appendTo(container);
    } else {
        var table = $(
            '<table><tr>' +
                '<th></th>' +
                '<th>Name</th>' +
                '<th align="right">Last Modified</th>' +
            '</tr></table>'
        ).appendTo(container)[0];
        
        var renderProject = function(project) {
            var tr = table.insertRow(table.rows.length);
            tr.className = "project";
            
            $('<a></a>')
                .addClass("delete-project")
                .attr("title","Delete this project")
                .attr("href","")
                .html("<img src='/images/close.png' />")
                .click(function() {
                    if (window.confirm("Are you sure you want to delete project \"" + project.name + "\"?")) {
                        $.ajax({
                            type: "POST",
                            url: "/command/delete-project",
                            data: { "project" : project.id },
                            dataType: "json",
                            success: function (data) {
                                if (data && typeof data.code != 'undefined' && data.code == "ok") {
                                    fetchProjects();
                                }
                            }
                        });                    
                    }
                    return false;
                }).appendTo(tr.insertCell(tr.cells.length));
                
            $('<a></a>')
                .text(project.name)
                .attr("href", "/project.html?project=" + project.id)
                .appendTo(tr.insertCell(tr.cells.length));
                
            $('<div></div>')
                .html(formatDate(project.date))
                .addClass("last-modified")
                .appendTo(tr.insertCell(tr.cells.length));
            
        };
    
        for (var i = 0; i < projects.length; i++) {
            renderProject(projects[i]);
        }
    }
}

function fetchProjects() {
    $.getJSON(
        "/command/get-all-project-metadata",
        null,
        function(data) {
            renderProjects(data);
        },
        "json"
    );
}

function showHide(toHide, toShow) {
    $("#" + toHide).hide();
    $("#" + toShow).show();
}

function onLoad() {
    fetchProjects();
    
    $("#upload-file-button").click(onClickUploadFileButton);
    $("#more-options-link").click(function() {
        $("#more-options-controls").hide();
        $("#more-options").show();
    });
    
    $("#gridworks-version").text(
        "Version " + GridworksVersion.version + "-" + GridworksVersion.revision
    );
    if (isThereNewRelease()) {
        $('<div id="version-message">' +
            'New version "' + GridworksReleases.releases[0].description + '" <a href="' + GridworksReleases.homepage + '">available for download here</a>.' +
          '</div>').appendTo(document.body);
    }
}

$(onLoad);
