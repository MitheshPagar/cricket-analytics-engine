package com.cricket.engine;

import javafx.application.Application;
import javafx.stage.Stage;

public class BowlingAllocatorApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        BowlingAllocatorController controller =
                new BowlingAllocatorController(primaryStage);
        controller.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}