package org.openrefine.launcher;

import java.io.IOException;
import java.util.Optional;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setTitle("OpenRefine Launcher");
        stage.setScene(scene);


        // FINALLY REALLY START THE APP
        stage.show();

        stage.setOnCloseRequest(event -> {
            // Consume the event to prevent the application from forcibly closing
            event.consume();

            // Show confirmation dialog
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Confirm");
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            //Deactivate Defaultbehavior for yes-Button:
            Button OK = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            OK.setDefaultButton( false );
            //Activate Defaultbehavior for no-Button:
            Button CANCEL = (Button) alert.getDialogPane().lookupButton( ButtonType.CANCEL );
            CANCEL.setDefaultButton( true );

            alert.setHeaderText("Close OpenRefine and save projects");
            alert.setContentText("Are you sure?");

            // Wait for user response
            Optional<ButtonType> response = alert.showAndWait();
            response.ifPresent(btnType -> {
                // Gracefully shutdown OpenRefine process through Controller
                // and close the application if the user confirms
                ButtonBar.ButtonData confirm = btnType.getButtonData();
                if(!confirm.isCancelButton()) {
                    final Runnable runnable = () -> new Controller().onStopButtonClick();
                    stage.close();
                }

            });

        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
