Refine.ManageExtensionsUI = function(elmt) {
    elmt.html(DOM.loadHTML("core", "scripts/index/manage-extensions-ui.html"));

    document.querySelector("#openExtensionDirectory").textContent = $.i18n('core-index-extensions/open-extension-directory');
    document.querySelector("#listExtensionsHeader").textContent = $.i18n('core-index-extensions/list-extensions-header');
    document.querySelector("#extensionsDocumentationButton").textContent = $.i18n('core-index-extensions/extensions-documentation-button');
    document.querySelector("#extensionsDiscoveryButton").textContent = $.i18n('core-index-extensions/extensions-discovery-button');
    document.querySelector("#extensionsTableName").textContent = $.i18n('core-index-extensions/extensions-table-name');
    document.querySelector("#extensionsTableBundled").textContent = $.i18n('core-index-extensions/extensions-table-bundled');

    document.querySelector("#openExtensionDirectory").addEventListener("click", Refine.ManageExtensionsUI._openExtensionDirectory);

    if (!Host.isLocalhost()) {
        document.querySelector("#openExtensionDirectory").style.display = "none";
    }

    Refine.ManageExtensionsUI._renderExtensions();
};

// Use relative command URLs (no leading "/") so installs under a path prefix
// such as /openrefine still resolve correctly. See #7387.
Refine.ManageExtensionsUI._fetchExtensions = function() {
    return new Promise(function(resolve, reject) {
        $.getJSON("command/core/get-version", null)
            .done(function(data) {
                resolve(data.module_names);
            })
            .fail(function(jqXHR, textStatus, errorThrown) {
                reject(errorThrown || textStatus);
            });
    });
};

Refine.ManageExtensionsUI._openExtensionDirectory = function() {
    Refine.postCSRF(
        "command/core/open-extensions-dir",
        {},
        function(data) {
            if (data && data.code != "ok" && "message" in data) {
                alert(data.message);
            }
        },
        "json",
        function(jqXHR, textStatus, errorThrown) {
            console.error("Failed to open extension directory", errorThrown || textStatus);
        }
    );
};

Refine.ManageExtensionsUI._renderExtensions = function() {
    const coreExtensions = ["core", "database", "gdata", "jython", "pc-axis", "wikidata"];

    Refine.ManageExtensionsUI._fetchExtensions().then(extensions => {
        const extensionContainer = document.querySelector("tbody#extensionList");
        extensions.forEach(extension => {
            const extensionRow = document.createElement("tr");
            extensionRow.innerHTML = `
                <td>${extension}</td>
                <td>${coreExtensions.includes(extension) ? "true" : "false"}</td>
            `;
            extensionContainer.appendChild(extensionRow);
        });
    });
};

Refine.actionAreas.push({
    id: "manage-extensions",
    label: $.i18n('core-index-extensions/extensions'),
    uiClass: Refine.ManageExtensionsUI,
});
