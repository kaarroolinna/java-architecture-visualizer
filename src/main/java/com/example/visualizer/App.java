package com.example.visualizer;

import com.example.visualizer.analyzer.ProjectAnalyzer;
import com.example.visualizer.model.ProjectModel;
import com.example.visualizer.ui.GraphView;
import javafx.application.Application;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Java Architecture Visualizer");
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Java Project Root or JAR");
        File root = chooser.showDialog(stage);
        if (root == null || (!root.isDirectory() && !root.getName().endsWith(".jar"))) {
            System.err.println("Invalid selection");
            stage.close();
            return;
        }
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        ProjectModel model = analyzer.analyze(root.toPath());
        model.computeCycles();
        GraphView view = new GraphView(model, root.toPath());
        view.show(stage);
    }
    public static void main(String[] args) {
        launch(args);
    }
}