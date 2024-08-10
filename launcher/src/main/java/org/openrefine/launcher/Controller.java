package org.openrefine.launcher;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javafx.concurrent.Service;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class Controller {

//    private OpenRefine refineAccessor;

    // java.util.concurrent.Executor typically provides a pool of threads
    private Executor exec;

    public Button btStart;
    private Label msgStart;
    private Label msgStop;
    private Label msgLog;
    private AnchorPane view;
    private AnchorPane details;

    @FXML
    protected void onStartButtonClick() {
        msgStart.setText("OpenRefine is starting\n and will launch your browser...");
    }
    @FXML
    protected void onStopButtonClick() {
        msgStop.setText("OpenRefine is shutting down...");
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
}

//   TODO:  Launch via a TASK ? or SERVICE ?  I think Service() ?
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



