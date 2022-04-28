/**
 * define the system-info module
 * this module will be used to display system information and bind to the system-info.html template
 * requests will be made to the server to get the system information using ajax
 */

Refine.SystemInfo = function (elmt) {
    elmt.html(DOM.loadHTML("core", "scripts/index/system-info.html"));

    this._elmts = DOM.bind(elmt);

    let domElements = this._elmts;

    const systemInfo = {
        date: "",
        osVersion: "",
        availableMemory: "",
        totalMemory: "",
        usedMemory: "",
        hostname: "",
    };

    const byteToMB = (bytes) => {
        return (bytes / 1024 / 1024).toFixed(2);
    };

    const memoryRatio = (sysInfo) => {
        return (sysInfo.usedMemory / sysInfo.totalMemory * 100).toFixed(2);
    };

    const memoryRatioDisplay = (sysInfo) => {
        return `${byteToMB(sysInfo.usedMemory)}MB / ${byteToMB(sysInfo.totalMemory)}MB`;
    };

    const sysInfoCall = function() {
        $.ajax({
            url: "command/core/get-system-info?",
            type: "GET",
            async: true,
            data: {
            },
            success: function(data) {
                systemInfo.date = data["date"];
                systemInfo.osVersion = data["os_version"];
                systemInfo.availableMemory = data["available_memory"];
                systemInfo.totalMemory = data["total_memory"];
                systemInfo.hostname = data["hostname"];
                systemInfo.usedMemory = systemInfo.totalMemory - systemInfo.availableMemory;

                // assign systemInfo.name to this._elmts.os_info
                domElements.os_info.text(systemInfo.osVersion);
                domElements.memory_info.text(memoryRatioDisplay(systemInfo));
                domElements.hostname_info.text(systemInfo.hostname);
                domElements.date_info.text(systemInfo.date);
            }
        }).fail(function(jqXhr, textStatus, errorThrown ) {
            console.log(errorThrown);
            clearInterval(intervalId)
            alert( textStatus + ':' + errorThrown );
        });

        console.log(systemInfo);
    }

    let interval = 100;
    intervalId = setInterval(sysInfoCall, interval)
}

Refine.SetLanguageUI.prototype.resize = function() {
};

Refine.actionAreas.push({
    id: "system-info",
    label: "System Info",
    uiClass: Refine.SystemInfo,
})
