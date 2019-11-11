package org.snaccooperative.connection;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 */

public class SNACConnector {

    final static Logger logger = LoggerFactory.getLogger("snac_connection");

    public static final String PREFERENCE_STORE_KEY = "snac_apikey";

    private PreferenceStore prefStore;

    private static final SNACConnector instance = new SNACConnector();

    public static SNACConnector getInstance() {
        return instance;
    }

    private SNACConnector() {
        prefStore = ProjectManager.singleton.getPreferenceStore();
        restoreSavedKey();
    }
    
    public void saveKey(String apikey) {
        if (apikey == "") {
            final JFrame myFrame = new JFrame();
            JButton button = new JButton();
            button.setText("Key cleared! Click to exit.");
            myFrame.add(button);
            //parent.pack();
            button.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    myFrame.dispose();
                } 
            });
            myFrame.setSize(450,200);
            myFrame.setLocationRelativeTo(null);
            myFrame.setVisible(true);
            myFrame.setAlwaysOnTop(true);
        }

        logger.error("Trying to save key " + apikey);
        ArrayNode array = ParsingUtilities.mapper.createArrayNode();
        ObjectNode obj = ParsingUtilities.mapper.createObjectNode();

        obj.put("apikey", apikey);
        array.add(obj);
        prefStore.put(PREFERENCE_STORE_KEY, array);
    }

    public ObjectNode getStoredKeyData() {
        ArrayNode array = (ArrayNode) prefStore.get(PREFERENCE_STORE_KEY);
        if (array != null && array.size() > 0 && array.get(0) instanceof ObjectNode) {
            return (ObjectNode) array.get(0);
        }
        return null;
    }

    public void removeKey() {
        prefStore.put(PREFERENCE_STORE_KEY, ParsingUtilities.mapper.createArrayNode());
    }

    public String getKey() {
        String visible;
        if (getStoredKeyData() != null){
                logger.error("Returning key data " + getStoredKeyData().get("apikey").asText());
                visible = getStoredKeyData().get("apikey").asText();
                return visible;
        }else{
            return null;
        }
    }

    private void restoreSavedKey() {
        ObjectNode keys = getStoredKeyData();
    }
}
