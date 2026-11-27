
package org.openrefine.launcher;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Locale userLocale = Locale.getDefault();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", userLocale);

        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        Label titleLabel = new Label(bundle.getString("title"));
        StackPane root = new StackPane(titleLabel);
        stage.setTitle(bundle.getString("title"));
        stage.setScene(scene);

        // Button to launch the standalone Java class
        Button button = new Button("Start OpenRefine!");
        button.setOnAction(e -> startOpenRefine());

        // FINALLY REALLY START THE APP
        stage.show();

        stage.setOnCloseRequest(event -> {
            // Consume the event to prevent the application from forcibly closing
            event.consume();

            // Show confirmation dialog
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle(bundle.getString("confirm_title"));
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            // Deactivate Defaultbehavior for OK Button:
            Button OK = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            OK.setDefaultButton(false);
            // Activate Defaultbehavior for CANCEL Button:
            Button CANCEL = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
            CANCEL.setDefaultButton(true);

            alert.setHeaderText(bundle.getString("confirm_header"));
            alert.setContentText(bundle.getString("confirm_content"));

            // Wait for user response
            Optional<ButtonType> response = alert.showAndWait();
            response.ifPresent(btnType -> {
                // Gracefully shutdown OpenRefine process through Controller
                // and close the application if the user confirms
                ButtonBar.ButtonData confirmBtn = btnType.getButtonData();
                if (!confirmBtn.isCancelButton()) {
//                    final Runnable runnable = () -> new Controller().onStopButtonClick();
                    stage.close();
                }
            });
        });
    }

    private void startOpenRefine() {
        try {
            com.google.refine.Refine.main(new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
