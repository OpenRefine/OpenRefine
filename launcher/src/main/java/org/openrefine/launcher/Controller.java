
package org.openrefine.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

public class Controller {

    private static final ExecutorService WATCH = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    private static SimpleBooleanProperty running = new SimpleBooleanProperty();
    private Process process;
    private Button btStart;
    private Label msgStop;
    private Label msgLog;
    private AnchorPane view;
    private AnchorPane details;

    @FXML
    Label msgStart;
    @FXML
    TextArea outputArea;
    @FXML
    TextArea refineOut;

    @FXML
    protected void onStartButtonClick() {
        this.outputArea.clear();

        if (process != null && process.isAlive()) {
            this.outputArea.setText("OpenRefine is already running.\n");

        } else {

            runLater(() -> outputArea.setText("OpenRefine is starting\n and will launch your browser...\n"));

            // Path to the OpenRefine application we want to start

            String refinePath = "F:/Downloads/openrefine-3.8.2";
            // String jrePath = refinePath + "/server/target/jre";
            String classPath = refinePath + "/server/target/lib/*";
            String mainClass = "com.google.refine.Refine";
            // String javaAppPathClass = refinePath + "/server/target/lib/openrefine-3.8.2-server.jar " + mainClass;
            // String javaOptions = "-Djava.library.path=" + refinePath + "/server/target/lib/native/windows";

            ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", classPath, mainClass);
            // redirect errors from process to the standard output stream of the process.
            processBuilder.redirectErrorStream(true);

            try {
                Boolean error = false;
                process = processBuilder.start();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));) {
                    String ebuffer;
                    while ((ebuffer = br.readLine()) != null) {
                        final String ebufferLine = ebuffer + "\n";
                        runLater(() -> refineOut.appendText(ebufferLine));
                        // Check if we had an error during start
                        if (ebufferLine.contains("Exception")) {
                            error = true;
                        }
                    }
                    if (error) {
                        runLater(() -> outputArea.appendText("ERROR OCCURRED WHILE STARTING.\nCheck the logs."));
                    }
                    runLater(() -> running.set(false));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // We handle the process's input/output streams here
                WATCH.submit(() -> {
                    runLater(() -> refineOut.clear());

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
                        String buffer;
                        while ((buffer = br.readLine()) != null) {
                            final String bufferLine = buffer + "\n";
                            runLater(() -> refineOut.appendText(bufferLine));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        runLater(() -> running.set(true));
    }

    @FXML
    public void onStopButtonClick() {
        if (!(process == null)) {
            if (process.isAlive()) {
                runLater(() -> outputArea.setText("OpenRefine is stopping...\n"));

                try {
                    runLater(() -> process.destroy());
                    runLater(() -> outputArea.appendText("OpenRefine STOPPED.\n"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            } else
                runLater(() -> outputArea.setText(
                        "No running OpenRefine process found.\nThere might have been an error.\nCheck Linux `ps` or Windows Task Manager for `OpenJDK openrefine` and kill the PID manually.\n"));
        }
    }

    void runLater(Runnable r) {
        Platform.runLater(r::run);
    }

    @FXML
    protected void onLogButtonClick() {
        msgStop.setText("Showing Logs...");
    }
    //
    // @FXML
    // protected void search() {
    // msgStop.setText("search...");
    // }
    //
    // @FXML
    // protected void close() {
    // msgStop.setText("close...");
    // }
    //
    // @FXML
    // protected void newProxy() {
    // msgStop.setText("exampleText1...");
    // }
    //
    // @FXML
    // protected void removeProxy() {
    // msgStop.setText("exampleText2...");
    // }
    //
    // @FXML
    // protected void settings() {
    // msgStop.setText("Show Settings/Preferences?...");
    // }
    //
    // @FXML
    // protected void aboutBox() {
    // msgStop.setText("Showing About...");
    // }

}
