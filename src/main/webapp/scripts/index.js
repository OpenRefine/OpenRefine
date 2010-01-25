function onLoad() {
    $("#upload-file-button").click(onClickUploadFileButton);
}
$(onLoad);

function onClickUploadFileButton(evt) {
    if ($("#project-name-input")[0].value.trim().length == 0) {
        window.alert("You must specify a project name.");
        
        evt.preventDefault();
        return false;
    }
}