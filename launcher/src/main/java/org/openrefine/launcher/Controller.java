package org.openrefine.launcher;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class Controller {
    public Button btStart;

    @FXML
    private Label msgStart;

    @FXML
    protected void onStartButtonClick() {
        msgStart.setText("OpenRefine is starting\n and will launch your browser...");
    }
}
