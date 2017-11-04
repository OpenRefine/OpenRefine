package com.google.refine.importers.tree;


public class ImportParameters {
    protected boolean trimStrings;
    protected boolean storeEmptyStrings;
    protected boolean guessDataType;
    protected boolean includeFileSources;
    protected String fileSource;
    
    public ImportParameters(boolean trimStrings, boolean storeEmptyStrings, boolean guessCellValueTypes,
            boolean includeFileSources, String fileSource) {
        this.trimStrings = trimStrings;
        this.storeEmptyStrings = storeEmptyStrings;
        this.guessDataType = guessCellValueTypes;
        this.includeFileSources = includeFileSources;
        this.fileSource = fileSource;
    }

    public ImportParameters(boolean trimStrings, boolean storeEmptyStrings, boolean guessCellValueTypes) {
        this(trimStrings, storeEmptyStrings, guessCellValueTypes, false, "");
    }
    
}
