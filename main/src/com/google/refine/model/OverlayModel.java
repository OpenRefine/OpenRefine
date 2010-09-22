package com.google.refine.model;

import com.google.refine.Jsonizable;

public interface OverlayModel extends Jsonizable {
    public void onBeforeSave();
    
    public void onAfterSave();
    
    public void dispose();
}
