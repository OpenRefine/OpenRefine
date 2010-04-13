function onClickUploadFileButton(evt) {
    var projectName = $("#project-name-input")[0].value;
    if (! $.trim(projectName).length) {
        window.alert("You must specify a project name.");
        
        evt.preventDefault();
        return false;
    } else {
        $("#file-upload-form").attr("action", 
            "/command/create-project-from-upload?" + [
                "separator=" + $("#separator-input")[0].value,
                "ignore=" + $("#ignore-input")[0].value,
                "header-lines=" + $("#header-lines-input")[0].value,
                "skip=" + $("#skip-input")[0].value,
                "limit=" + $("#limit-input")[0].value,
                "guess-value-type=" + $("#guess-value-type-input")[0].checked
            ].join("&"));
    }
}

function formatDate(d) {
    var yesterday = Date.today().add({ days: -1 });
    var today = Date.today();
    var tomorrow = Date.today().add({ days: 1 });
    if (d.between(today, tomorrow)) {
        return "Today " + d.toString("h:mm tt");
    } else if (d.between(yesterday, today)) {
        return "Yesterday " + d.toString("h:mm tt");
    } else {
        return d.toString("ddd, MMM d, yyyy");
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
    
    if (!projects.length) {
        $('#body-empty').show();
        $('#create-project-panel').remove().appendTo($('#body-empty-create-project-panel-container'));
    } else {
        $('#body-nonempty').show();
        $('#create-project-panel').remove().appendTo($('#body-nonempty-create-project-panel-container'));

        projects.sort(function(a, b) { return b.date.getTime() - a.date.getTime(); });
        if (projects.length > 10) {
            $('#body-nonempty-logo-container').css("vertical-align", "top");
            $('#body-nonempty-create-project-panel-container').css("vertical-align", "top");
        }
        
        var container = $("#projects").empty().show();
        $('<h1>').text("Projects").appendTo(container);
        
        var renderProject = function(project) {
            var div = $('<div>').addClass("project").appendTo(container);
        
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
                                    window.location.reload();
                                }
                            }
                        });                    
                    }
                    return false;
                }).appendTo(div);
            
            $('<a></a>').text(project.name).attr("href", "/project.html?project=" + project.id).appendTo(div);
            $('<span></span>').text(formatDate(project.date)).addClass("last-modified").appendTo(div);
        };
        
        for (var i = 0; i < projects.length; i++) {
            renderProject(projects[i]);
        }
    }
}

function onLoad() {
    $.getJSON(
        "/command/get-all-project-metadata",
        null,
        function(data) {
            renderProjects(data);
            $("#upload-file-button").click(onClickUploadFileButton);
        },
        "json"
    );
    
    if (isThereNewRelease()) {
        $('<div id="version-message">' +
            'New version "' + GridworksReleases.releases[0].description + '" <a href="' + GridworksReleases.homepage + '">available for download here</a>.' +
          '</div>').appendTo(document.body);
    }
}

$(onLoad);
