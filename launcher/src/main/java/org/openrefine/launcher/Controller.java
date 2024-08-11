package org.openrefine.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.hansolo.tilesfx.Command;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.beans.property.SimpleBooleanProperty;

public class Controller {

    private static final ExecutorService WATCH = Executors.newSingleThreadExecutor(r -> { var t = new Thread(r); t.setDaemon(true); return t; });
    private static SimpleBooleanProperty running = new SimpleBooleanProperty();
    private Process process;
    private Button btStart;
    private Label msgStop;
    private Label msgLog;
    private AnchorPane view;
    private AnchorPane details;

    @FXML Label msgStart;
    @FXML TextArea outputArea;
    @FXML TextArea refineOut;


    @FXML protected void onStartButtonClick() {
        outputArea.setText("OpenRefine is starting\n and will launch your browser...");

        // Path to the OpenRefine application we want to start

        String refinePath = "F:/Downloads/openrefine-3.8.2";
        // String jrePath = refinePath + "/server/target/jre";
        String classPath = refinePath + "/server/target/lib/*";
        String mainClass = "com.google.refine.Refine";
        // String javaAppPathClass = refinePath + "/server/target/lib/openrefine-3.8.2-server.jar " + mainClass;
        // String javaOptions = "-Djava.library.path=" + refinePath + "/server/target/lib/native/windows";

        ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", classPath, mainClass);

        try {
            Process process = processBuilder.start();

            refineOut.appendText("PROCESS SUPPORTS NORMAL TERMINATION? :  " + String.valueOf(process.supportsNormalTermination()) + "\n");
            refineOut.appendText("PROCESS COMMAND:  " + String.valueOf(process.info().command()) + "\n");
            refineOut.appendText("PROCESS ARGS:  " + String.valueOf(process.info().arguments()) + "\n");
            refineOut.appendText("PROCESS COMMANDLINE:  " + String.valueOf(process.info().commandLine()) + "\n");
            refineOut.appendText("PROCESS INFO:  " + String.valueOf(process.info().toString()) + "\n");
            refineOut.appendText("PROCESS PID: " + String.valueOf(process.pid()) + "\n");
            // Optionally, we can handle the process's input/output streams here
//            BufferedReader refineOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = refineOutput.readLine()) != null) {
//                try {
//                    refineOut.appendText(line + "\n");
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//
//            }

            WATCH.submit(() -> {
                runLater(() -> refineOut.clear());
                runLater(() -> refineOut.setText("Started OpenRefine:\n\n"));
                try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
                    String buffer;
                    while((buffer = br.readLine()) != null) {
                        final String bufferLine = buffer+"\n";
                        runLater(() -> refineOut.appendText(bufferLine));
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                runLater(() -> refineOut.appendText("\nOpenRefine is running."));
                runLater(() -> running.set(true));
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @FXML
    protected void onStopButtonClick() {
        outputArea.setText("OpenRefine is stopping...");
        // outputArea.appendText(String.valueOf((process.pid())));
        try {
            boolean alive = process.isAlive();
            System.out.println("IS ALIVE: " + alive);

            process.destroy();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            outputArea.appendText("OpenRefine has STOPPED.");
        }

    }
    @FXML
    protected void onLogButtonClick() {
        msgStop.setText("Showing Logs...");
    }
    @FXML
    protected void search() {
        msgStop.setText("search...");
    }
    @FXML
    protected void close() {
        msgStop.setText("close...");
    }
    @FXML
    protected void newProxy() {
        msgStop.setText("exampleText1...");
    }
    @FXML
    protected void removeProxy() {
        msgStop.setText("exampleText2...");
    }
    @FXML
    protected void settings() {
        msgStop.setText("Show Settings/Preferences?...");
    }
    @FXML
    protected void aboutBox() {
        msgStop.setText("Showing About...");
    }

    void runLater(Runnable r) {
        Platform.runLater(r::run);
    }
}

//   TODO:  Maybe instead launch via a TASK ? or SERVICE ?  I think Service() ?
//.  SEE EXAMPLES:
//   https://github.com/mynttt/UpdateTool/blob/bf4a21a2a7f63a78a1401f38969a74a37a74a9f0/updatetool-gui/src/main/java/updatetool/gui/App.java
//
//
//    @FXML
//    private Label refineRunIndicator;
//
//    public void  initialize() throws Exception {
//        refineAccessor = new OpenRefine();
//
//        // create executor that uses daemon threads:
//        exec = Executors.newCachedThreadPool(runnable -> {
//            Thread t = new Thread(runnable);
//            t.setDaemon(true);
//            return t;
//        });
//    }

//    @FXML
//    public class OpenRefineService extends Service<String> {
//        /**
//         * Constructor
//         */
//        public OpenRefineServiceStatus () {
//            // if succeeded
//            setOnSucceeded(s -> {
//                //code if Service succeeds
//            });
//
//            // if failed
//            setOnFailed(fail -> {
//                //code it Service fails
//            });
//
//            //if cancelled
//            setOnCancelled(cancelled->{
//                //code if Service get's cancelled
//            });
//        }
//
//        /**
//         * This method starts the Service
//         */
//        public void startOpenRefineService(){
//            if(!isRunning()){
//                //...
//                // reset();
//                start();
//            }
//
//        }
//
//    }



