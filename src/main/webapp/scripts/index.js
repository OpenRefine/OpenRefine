function onLoad() {
    $("#upload-file-button").click(onClickUploadFileButton);
    
    $.getJSON(
        "/command/get-all-project-metadata",
        null,
        function(data) {
            renderProjects(data);
        },
        "json"
    );
}
$(onLoad);

function onClickUploadFileButton(evt) {
    if ($("#project-name-input")[0].value.trim().length == 0) {
        window.alert("You must specify a project name.");
        
        evt.preventDefault();
        return false;
    } else {
        $("#file-upload-form").attr("action", 
            "/command/create-project-from-upload?" + [
                "skip=" + $("#skip-input")[0].value,
                "limit=" + $("#limit-input")[0].value
            ].join("&"));
    }
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
    
    if (projects.length > 0) {
        projects.sort(function(a, b) { return b.date.getTime() - a.date.getTime(); });
        
        var container = $("#projects").empty().show();

        $('<h2></h2>').text("Projects").appendTo(container);

        var table = $('<table><tr><td></td><td>last modified</td><td></td></tr></table>').attr("cellspacing", "5")[0];

        for (var i = 0; i < projects.length; i++) {
            var project = projects[i];
            var tr = table.insertRow(table.rows.length);
            var td0 = tr.insertCell(0);
            var td1 = tr.insertCell(1);
            var td2 = tr.insertCell(2);
        
            $('<a></a>').text(project.name).attr("href", "/project.html?project=" + project.id).appendTo(td0);
            $('<span></span>').text(formatDate(project.date)).appendTo(td1);
            $('<a></a>').attr("title","Delete this project").attr("href","").html("<img src='/images/close.png' />").click(function() {
                if (window.confirm("Are you sure you want to delete this project?")) {
                    $.ajax({
                        type: "POST",
                        url: "/command/delete-project",
                        data: { "project" : project.id },
                        dataType: "json",
                        success: function (data) {
                            if (data && typeof data['code'] != 'undefined' && data.code == "ok") {
                                window.location.reload()
                            }
                        }
                    });                    
                }
                return false;
            }).appendTo(td2);
        }
        
        $(table).appendTo(container);
    }
}

function formatDate(d) {
    var yesterday = Date.today().add({ days: -1 });
    var today = Date.today();
    var tomorrow = Date.today().add({ days: 1 });
    if (d.between(today, tomorrow)) {
        return "Today " + d.toString("HH:mm");
    } else if (d.between(yesterday, today)) {
        return "Yesterday " + d.toString("HH:mm");
    } else {
        return d.toString("M-ddd-yyyy");
    }
}