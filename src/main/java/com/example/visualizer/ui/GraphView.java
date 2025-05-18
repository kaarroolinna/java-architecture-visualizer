package com.example.visualizer.ui;

import com.example.visualizer.model.*;
import com.example.visualizer.util.ExportManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GraphView {
    private final ProjectModel model;
    private final Map<String, Rectangle> pkgRects = new HashMap<>();
    private final Map<String, Label>     pkgLabels = new HashMap<>();
    private final Path projectRoot;

    private final double width = 800, height = 600;
    private Rectangle highlightedRect = null;

    private static class Delta {
        double x, y;
    }

    public GraphView(ProjectModel model, Path projectRoot) {
        this.model = model;
        this.projectRoot = projectRoot;
        model.computePackageDependencies();
        model.computeCycles();
    }

    public void show(Stage stage) {
        Pane graphPane = new Pane();
        graphPane.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

        graphPane.setOnScroll(e -> {
            double f = e.getDeltaY() > 0 ? 1.1 : 0.9;
            graphPane.setScaleX(graphPane.getScaleX() * f);
            graphPane.setScaleY(graphPane.getScaleY() * f);
            e.consume();
        });

        Delta drag = new Delta();
        graphPane.setOnMousePressed(e -> {
            drag.x = e.getSceneX() - graphPane.getTranslateX();
            drag.y = e.getSceneY() - graphPane.getTranslateY();
        });
        graphPane.setOnMouseDragged(e -> {
            graphPane.setTranslateX(e.getSceneX() - drag.x);
            graphPane.setTranslateY(e.getSceneY() - drag.y);
        });

        MenuBar menuBar = new MenuBar();
        Menu fileM = new Menu("File"), exportM = new Menu("Export");
        MenuItem toPdf   = new MenuItem("Export to PDF");
        MenuItem toGraph = new MenuItem("Export to GraphML");
        exportM.getItems().addAll(toPdf, toGraph);
        fileM.getItems().add(exportM);
        menuBar.getMenus().add(fileM);

        toPdf.setOnAction(evt -> {
            try {
                ExportManager.exportToPDF(graphPane, projectRoot);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        toGraph.setOnAction(evt -> {
            try {
                ExportManager.exportToGraphML(model, projectRoot);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        Label title = new Label("Packages and dependencies ("
                + model.getPackageNames().size() + " pkgs)");
        GridPane grid = new GridPane();
        grid.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(70);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(30);
        grid.getColumnConstraints().addAll(c1, c2);

        RowConstraints r1 = new RowConstraints();
        r1.setPercentHeight(50);
        RowConstraints r2 = new RowConstraints();
        r2.setPercentHeight(50);
        grid.getRowConstraints().addAll(r1, r2);

        grid.add(graphPane, 0, 0, 1, 2);
        GridPane.setHgrow(graphPane, Priority.ALWAYS);
        GridPane.setVgrow(graphPane, Priority.ALWAYS);
        GridPane.setHalignment(graphPane, HPos.CENTER);
        GridPane.setValignment(graphPane, VPos.CENTER);

        VBox metricsPane = new VBox(10);
        metricsPane.setPadding(new Insets(10));
        buildMetricsCharts(metricsPane);
        grid.add(metricsPane, 1, 0);
        GridPane.setHgrow(metricsPane, Priority.ALWAYS);
        GridPane.setVgrow(metricsPane, Priority.ALWAYS);

        VBox detailPane = new VBox(10);
        detailPane.setPadding(new Insets(10));
        buildDetailTree(detailPane);
        grid.add(detailPane, 1, 1);
        GridPane.setHgrow(detailPane, Priority.ALWAYS);
        GridPane.setVgrow(detailPane, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(new VBox(menuBar, title));
        root.setCenter(grid);

        Scene scene = new Scene(root, width, height);

        stage.setScene(scene);
        stage.setTitle("Java Architecture Visualizer");
        stage.setMaximized(true);
        stage.show();

        layoutPackages(graphPane);
        drawPackageDependencies(graphPane);
    }

    private void buildMetricsCharts(VBox infoPane) {

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        BarChart<String,Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setTitle("Coupling (outgoing vs incoming)");
        xAxis.setLabel("Package");
        yAxis.setLabel("Count");

        XYChart.Series<String,Number> outS = new XYChart.Series<>();
        outS.setName("Outgoing");
        model.getOutgoingCount().forEach((pkg, c) ->
                outS.getData().add(new XYChart.Data<>(pkg, c)));

        XYChart.Series<String,Number> inS = new XYChart.Series<>();
        inS.setName("Incoming");
        model.getIncomingCount().forEach((pkg, c) ->
                inS.getData().add(new XYChart.Data<>(pkg, c)));

        bar.getData().addAll(outS, inS);
        bar.setCategoryGap(10);
        bar.setPrefHeight(250);
        bar.setLegendVisible(true);
        xAxis.setTickLabelsVisible(true);
        xAxis.setTickMarkVisible(true);

        PieChart pie = new PieChart();
        pie.setTitle("Hot Spots (top 3)");
        model.getOutgoingCount().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .forEach(e ->
                        pie.getData().add(
                                new PieChart.Data(e.getKey(),
                                        e.getValue().doubleValue())));
        pie.setPrefHeight(200);
        pie.setLegendVisible(true);
        pie.setLabelsVisible(true);
        Label depth = new Label("Max depth: " + model.getMaxDependencyDepth());
        infoPane.getChildren().addAll(bar, pie, depth);
    }

    private void layoutPackages(Pane pane) {
        List<String> pkgs = new ArrayList<>(model.getPackageNames());
        double cx = width / 2,
                cy = height / 2,
                r  = Math.min(width, height) / 2 - 50;

        for (int i = 0; i < pkgs.size(); i++) {
            double angle = 2 * Math.PI * i / pkgs.size();
            double x = cx + r * Math.cos(angle) - 70;
            double y = cy + r * Math.sin(angle) - 15;

            String pkg = pkgs.get(i);

            Rectangle rect = new Rectangle(x, y, 140, 30);
            rect.setFill(Color.LIGHTBLUE);

            Label lbl = new Label(pkg + " (" + model.getClassCount(pkg) + ")");
            lbl.setLayoutX(x + 5);
            lbl.setLayoutY(y + 5);

            pane.getChildren().addAll(rect, lbl);

            double classY = y + 30;
            for (ClassNode cls : model.getPackages().stream()
                    .filter(pn -> pn.getName().equals(pkg))
                    .findAny()
                    .map(PackageNode::getClasses)
                    .orElse(Collections.emptyList())) {
                Label clsLabel = new Label("- " + cls.getName()
                        + (cls.isInterface() ? " (i)" : ""));
                clsLabel.setLayoutX(x + 10);
                clsLabel.setLayoutY(classY);
                pane.getChildren().add(clsLabel);
                classY += 15;
            }
            pkgRects.put(pkg, rect);
            pkgLabels.put(pkg, lbl);
            rect.setOnMouseClicked(e -> showPackageInfo(pkg));
        }
    }

    private void drawPackageDependencies(Pane pane) {
        for (Map.Entry<String, Set<String>> entry
                : model.getPackageDependencies().entrySet()) {
            String fromPkg = entry.getKey();
            Rectangle from = pkgRects.get(fromPkg);
            if (from == null) continue;

            for (String toPkg : entry.getValue()) {
                if (toPkg.equals(fromPkg)) continue;
                Rectangle to = pkgRects.get(toPkg);
                if (to == null) continue;

                Color c = colorForEdge(fromPkg, toPkg);
                drawArrow(pane, from, to, c);
            }
        }
    }

    private void drawArrow(Pane pane, Rectangle a, Rectangle b, Color color) {
        double sx = a.getX() + a.getWidth()  / 2,
                sy = a.getY() + a.getHeight() / 2;
        double ex = b.getX() + b.getWidth()  / 2,
                ey = b.getY() + b.getHeight() / 2;

        Line line = new Line(sx, sy, ex, ey);
        line.setStroke(color);
        line.setStrokeWidth(2);
        pane.getChildren().add(line);

        Polygon head = createArrowHead(sx, sy, ex, ey);
        head.setFill(color);
        pane.getChildren().add(head);
    }

    private Polygon createArrowHead(double sx, double sy,
                                    double ex, double ey) {
        double len = 10, ang = Math.toRadians(20);
        double theta = Math.atan2(ey - sy, ex - sx);
        double x1 = ex - len * Math.cos(theta - ang),
                y1 = ey - len * Math.sin(theta - ang);
        double x2 = ex - len * Math.cos(theta + ang),
                y2 = ey - len * Math.sin(theta + ang);
        return new Polygon(ex, ey, x1, y1, x2, y2);
    }

    private Color colorForEdge(String f, String t) {
        for (Set<String> cyc : model.getCycles()) {
            if (cyc.contains(f) && cyc.contains(t)) return Color.RED;
        }
        return Color.GRAY;
    }

    private void showPackageInfo(String pkg) {
        Stage st = new Stage();
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));

        box.getChildren().add(new Label("Package: " + pkg));
        box.getChildren().add(new Label("Classes:"));
        model.getPackages().stream()
                .filter(p -> p.getName().equals(pkg))
                .findAny()
                .ifPresent(p -> p.getClasses().forEach(c ->
                        box.getChildren().add(new Label(" - " + c.getName()))));

        st.setScene(new Scene(box, 300, 400));
        st.setTitle("Details: " + pkg);
        st.show();
    }

    private static class TreeItemData {
        enum Type {
            PACKAGE,
            CLASS,
            METHOD,
            FIELD
        }
        final Type type;
        final String display;
        final String fullName;
        TreeItemData(Type type, String display, String fullName) {
            this.type = type;
            this.display = display;
            this.fullName = fullName;
        }
        @Override public String toString() {
            return display;
        }
    }
    private void buildDetailTree(VBox infoPane) {

        TreeItem<TreeItemData> root = new TreeItem<>(
                new TreeItemData(TreeItemData.Type.PACKAGE,
                        "Project", ""));
        root.setExpanded(true);

        for (PackageNode pkg : model.getPackages()) {
            String pkgName = pkg.getName();
            TreeItem<TreeItemData> pkgItem = new TreeItem<>(
                    new TreeItemData(TreeItemData.Type.PACKAGE,
                            pkgName + " (" + pkg.getClasses().size() + ")",
                            pkgName));
            pkgItem.setExpanded(true);

            for (ClassNode cls : pkg.getClasses()) {
                String className = cls.getName();
                String classFull = pkgName + "." + className;
                TreeItem<TreeItemData> classItem = new TreeItem<>(
                        new TreeItemData(TreeItemData.Type.CLASS,
                                className, classFull));

                for (String method : cls.getMethods()) {
                    classItem.getChildren().add(
                            new TreeItem<>(new TreeItemData(
                                    TreeItemData.Type.METHOD,
                                    method + "()",
                                    classFull + "#" + method)));
                }
                for (String field : cls.getFields()) {
                    classItem.getChildren().add(
                            new TreeItem<>(new TreeItemData(
                                    TreeItemData.Type.FIELD,
                                    field,
                                    classFull + "." + field)));
                }
                pkgItem.getChildren().add(classItem);
            }
            root.getChildren().add(pkgItem);
        }

        TreeView<TreeItemData> tree = new TreeView<>(root);
        tree.setShowRoot(false);
        tree.setPrefHeight(250);

        tree.setCellFactory(tv -> {
            TreeCell<TreeItemData> cell = new TreeCell<>() {
                @Override
                protected void updateItem(TreeItemData data, boolean empty) {
                    super.updateItem(data, empty);
                    if (empty || data == null) {
                        setText(null);
                        setContextMenu(null);
                    } else {
                        setText(data.display);

                        MenuItem openFile = new MenuItem("Open Source File");
                        openFile.setOnAction(evt -> openSourceFile(data.fullName));
                        MenuItem goMethod = new MenuItem("Go to Definition...");
                        goMethod.setOnAction(evt -> navigateTo(data.fullName));
                        MenuItem copyName = new MenuItem("Copy Full Name");
                        copyName.setOnAction(evt -> {
                            ClipboardContent cc = new ClipboardContent();
                            cc.putString(data.fullName);
                            Clipboard.getSystemClipboard().setContent(cc);
                        });

                        ContextMenu menu = new ContextMenu(openFile, goMethod, copyName);
                        if (data.type == TreeItemData.Type.PACKAGE) {
                            menu.getItems().remove(goMethod);
                        }
                        setContextMenu(menu);
                    }
                }
            };
            return cell;
        });

        infoPane.getChildren().add(new Label("Structure:"));
        infoPane.getChildren().add(tree);
    }

    private void openSourceFile(String fullName) {
        Path rel = Paths.get("src", "main", "java",
                fullName.replace('.', File.separatorChar) + ".java");
        File file = projectRoot.resolve(rel).toFile();

        if (!file.exists()) {
            Alert err = new Alert(Alert.AlertType.WARNING,
                    "Automatic lookup failed:\n" + file.getAbsolutePath()
                            + "\n\nPlease locate the source file manually.");
            err.setTitle("File Not Found");
            err.setHeaderText(null);
            err.showAndWait();

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Locate " + fullName + ".java");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Java Source", "*.java"));
            Window owner = Stage.getWindows().stream()
                    .filter(Window::isShowing)
                    .findFirst().orElse(null);
            File manual = chooser.showOpenDialog(owner);
            if (manual != null && manual.exists()) {
                try {
                    Desktop.getDesktop().open(manual);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return;
        }

        List<String> options = List.of(
                "System Default", "Notepad", "VS Code",
                "IntelliJ IDEA", "Custom...");
        ChoiceDialog<String> dlg =
                new ChoiceDialog<>(options.get(0), options);
        dlg.setTitle("Open With");
        dlg.setHeaderText("Choose application to open\n" + file.getName());
        dlg.setContentText("Application:");
        Optional<String> choice = dlg.showAndWait();
        if (choice.isEmpty()) return;
        try {
            switch (choice.get()) {
                case "Notepad"       -> new ProcessBuilder("notepad",
                        file.getAbsolutePath()).start();
                case "VS Code"       -> new ProcessBuilder("code",
                        file.getAbsolutePath()).start();
                case "IntelliJ IDEA" -> new ProcessBuilder("idea",
                        file.getAbsolutePath()).start();
                case "Custom..."     -> {
                    TextInputDialog td = new TextInputDialog();
                    td.setTitle("Custom Launcher");
                    td.setHeaderText("Enter full path to executable");
                    td.setContentText("Executable:");
                    td.showAndWait().ifPresent(cmd -> {
                        try {
                            new ProcessBuilder(cmd,
                                    file.getAbsolutePath()).start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                default -> {
                    if (Desktop.isDesktopSupported())
                        Desktop.getDesktop().open(file);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateTo(String fullName) {
        String pkg = fullName.contains("#")
                ? fullName.substring(0, fullName.indexOf('#'))
                : fullName.contains(".")
                ? fullName.substring(0, fullName.lastIndexOf('.'))
                : fullName;

        Rectangle rect = pkgRects.get(pkg);
        if (rect == null) return;

        if (highlightedRect != null) {
            highlightedRect.setStrokeWidth(0);
        }
        rect.setStrokeType(StrokeType.OUTSIDE);
        rect.setStroke(Color.ORANGE);
        rect.setStrokeWidth(4);

        Label lbl = pkgLabels.get(pkg);
        if (lbl != null) lbl.toFront();
        highlightedRect = rect;
    }
}


