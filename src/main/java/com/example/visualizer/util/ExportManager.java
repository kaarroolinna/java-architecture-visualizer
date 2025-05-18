package com.example.visualizer.util;

import com.example.visualizer.model.ProjectModel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

public class ExportManager {
    public static void exportToPDF(Pane graphPane, Path projectRoot) throws IOException {
        SnapshotParameters params = new SnapshotParameters();
        WritableImage img = graphPane.snapshot(params, null);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save diagram as PDF");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf")
        );
        File out = chooser.showSaveDialog(graphPane.getScene().getWindow());
        if (out == null) return;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle((float)img.getWidth(), (float)img.getHeight()));
            doc.addPage(page);

            var awtImage = SwingFXUtils.fromFXImage(img, null);
            var pdImage  = LosslessFactory.createFromImage(doc, awtImage);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(pdImage, 0, 0, (float)img.getWidth(), (float)img.getHeight());
            }

            if (!out.getName().toLowerCase().endsWith(".pdf"))
                out = new File(out.getAbsolutePath() + ".pdf");
            doc.save(out);
        }
    }
    public static void exportToGraphML(ProjectModel model, Path projectRoot) throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save as GraphML");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("GraphML files", "*.graphml")
        );
        File out = chooser.showSaveDialog(null);
        if (out == null) return;

        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            w.write("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n");
            w.write("  <graph id=\"G\" edgedefault=\"directed\">\n");
            for (String pkg : model.getPackageNames()) {
                w.write("    <node id=\"" + pkg + "\"/>\n");
            }
            for (Map.Entry<String, java.util.Set<String>> e : model.getPackageDependencies().entrySet()) {
                String from = e.getKey();
                for (String to : e.getValue()) {
                    if (to.equals(from)) continue;
                    w.write("    <edge source=\"" + from + "\" target=\"" + to + "\"/>\n");
                }
            }
            w.write("  </graph>\n</graphml>\n");
        }
    }
}
