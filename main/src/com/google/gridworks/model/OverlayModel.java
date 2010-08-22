package com.google.gridworks.model;

import com.google.gridworks.Jsonizable;

public interface OverlayModel extends Jsonizable {
    public void onBeforeSave();
    
    public void onAfterSave();
    
    public void dispose();
}
