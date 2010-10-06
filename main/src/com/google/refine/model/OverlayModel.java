package com.google.refine.model;

import com.google.refine.Jsonizable;

public interface OverlayModel extends Jsonizable {
    public void onBeforeSave(Project project);
    
    public void onAfterSave(Project project);
    
    public void dispose(Project project);
}
