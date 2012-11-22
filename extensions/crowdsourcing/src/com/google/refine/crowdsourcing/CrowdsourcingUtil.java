package com.google.refine.crowdsourcing;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;


public class CrowdsourcingUtil {

    public static Object getPreference(String prefName) {
        PreferenceStore ps = ProjectManager.singleton.getPreferenceStore();
        Object pref = ps.get(prefName);
        
        return pref;
    }

    //special case are cml fields/agg-s
    public static ArrayList<String> parseCmlFields(String cml) {
     
        ArrayList<String> field_names = new ArrayList<String>();
           
        //"(?<=\\{)([^}]*)(?=\\})"        
        Pattern regex = Pattern.compile("(?<=(\\{){2})([^}]*)(?=\\})", Pattern.DOTALL);
        
        Matcher regexMatcher = regex.matcher(cml);
        while (regexMatcher.find()) 
        {
          field_names.add(regexMatcher.group());
        } 
            
        return field_names;
    }
    
    
}
