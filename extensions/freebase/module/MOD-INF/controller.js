function init() {
    Packages.com.google.refine.freebase.oauth.FreebaseProvider.register();
    
    var RS = Packages.com.google.refine.RefineServlet;
    RS.registerClassMapping(
        "com.google.refine.model.changes.DataExtensionChange",
        "com.google.refine.freebase.model.changes.DataExtensionChange");
    RS.registerClassMapping(
        "com.google.refine.operations.SaveProtographOperation$ProtographChange",
        "com.google.refine.freebase.operations.SaveProtographOperation$ProtographChange");
        
    // TODO(dfhuynh): Temporary hack until we know how the core module can resolve our module's classes
    RS.cacheClass(Packages.com.google.refine.freebase.model.changes.DataExtensionChange);
    RS.cacheClass(Packages.com.google.refine.freebase.operations.SaveProtographOperation$ProtographChange);
    
    RS.registerCommand(module, "extend-data",           new Packages.com.google.refine.freebase.commands.ExtendDataCommand());
    RS.registerCommand(module, "preview-extend-data",   new Packages.com.google.refine.freebase.commands.PreviewExtendDataCommand());

    RS.registerCommand(module, "preview-protograph",    new Packages.com.google.refine.freebase.commands.PreviewProtographCommand());
    RS.registerCommand(module, "save-protograph",       new Packages.com.google.refine.freebase.commands.SaveProtographCommand());

    RS.registerCommand(module, "check-authorization",   new Packages.com.google.refine.freebase.commands.auth.CheckAuthorizationCommand());
    RS.registerCommand(module, "authorize",             new Packages.com.google.refine.freebase.commands.auth.AuthorizeCommand());
    RS.registerCommand(module, "deauthorize",           new Packages.com.google.refine.freebase.commands.auth.DeAuthorizeCommand());
    RS.registerCommand(module, "user-badges",           new Packages.com.google.refine.freebase.commands.auth.GetUserBadgesCommand());

    RS.registerCommand(module, "upload-data",           new Packages.com.google.refine.freebase.commands.UploadDataCommand());
    RS.registerCommand(module, "import-qa-data",        new Packages.com.google.refine.freebase.commands.ImportQADataCommand());
    RS.registerCommand(module, "mqlread",               new Packages.com.google.refine.freebase.commands.MQLReadCommand());
    RS.registerCommand(module, "mqlwrite",              new Packages.com.google.refine.freebase.commands.MQLWriteCommand());

    var OR = Packages.com.google.refine.operations.OperationRegistry;
    
    OR.registerOperation(module, "extend-data",                     Packages.com.google.refine.freebase.operations.ExtendDataOperation);
    OR.registerOperation(module, "import-qa-data",                  Packages.com.google.refine.freebase.operations.ImportQADataOperation);
    OR.registerOperation(module, "save-protograph",                 Packages.com.google.refine.freebase.operations.SaveProtographOperation); // for backward compatibility
    OR.registerOperation(module, "save-schema-alignment-skeleton",  Packages.com.google.refine.freebase.operations.SaveProtographOperation);
    
    var RC = Packages.com.google.refine.model.recon.ReconConfig;
    
    RC.registerReconConfig(module, "strict", Packages.com.google.refine.freebase.model.recon.StrictReconConfig);
    RC.registerReconConfig(module, "extend", Packages.com.google.refine.freebase.model.recon.DataExtensionReconConfig);
    
    var ER = Packages.com.google.refine.exporters.ExporterRegistry;
    
    ER.registerExporter("tripleloader", new Packages.com.google.refine.freebase.ProtographTransposeExporter.TripleLoaderExporter());
    ER.registerExporter("mqlwrite", new Packages.com.google.refine.freebase.ProtographTransposeExporter.MqlwriteLikeExporter());
    
    Packages.com.google.refine.model.Project.
        registerOverlayModel("freebaseProtograph", Packages.com.google.refine.freebase.protograph.Protograph);
    
    ClientSideResourceManager.addPaths(
        "project/scripts",
        module,
        [
            "scripts/extension.js",
            
            "scripts/util/sign.js",
            "scripts/util/freebase.js",
            
            "scripts/dialogs/freebase-loading-dialog.js",
            "scripts/dialogs/extend-data-preview-dialog.js",
            
            "scripts/dialogs/schema-alignment/dialog.js",
            "scripts/dialogs/schema-alignment/ui-node.js",
            "scripts/dialogs/schema-alignment/ui-link.js"
        ]
    );
    
    ClientSideResourceManager.addPaths(
        "project/styles",
        module,
        [
            "styles/dialogs/freebase-loading-dialog.less",
            "styles/dialogs/extend-data-preview-dialog.less",
            "styles/dialogs/schema-alignment-dialog.less"
        ]
    );
}
