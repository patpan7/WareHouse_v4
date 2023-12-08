package com.example.warehouse;

import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class PrintUtil {
    public static void printTableView(TableView<?> tableView, String title) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob != null && printerJob.showPrintDialog(tableView.getScene().getWindow())) {
            Node printNode = createPrintNode(tableView, title);
            boolean success = printerJob.printPage(printNode);

            if (success) {
                printerJob.endJob();
            }
        }
    }

    private static Node createPrintNode(TableView<?> tableView, String title) {
        // Customize the appearance of the printed page using CSS
        String css = "" +
                "table {\n" +
                "    -fx-font-size: 12px;\n" +
                "    -fx-table-cell-border-color: black;\n" +
                "    -fx-border-width: 1px;\n" +
                "    -fx-border-color: black;\n" +
                "}\n" +
                ".column-header {\n" +
                "    -fx-font-weight: bold;\n" +
                "}\n" +
                ".table-cell {\n" +
                "    -fx-alignment: CENTER;\n" +
                "}\n";

        tableView.getStylesheets().add("data:text/css," + css);

        // Create a VBox to hold the title and the table
        VBox printNode = new VBox();
        printNode.setAlignment(Pos.CENTER);

        // Create a Text node for the title
        Text titleText = new Text(title);
        titleText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        titleText.setUnderline(true);
        titleText.setStyle("-fx-fill: black;");
        printNode.getChildren().add(titleText);

        // Create a snapshot of the table
        WritableImage snapshot = tableView.snapshot(new SnapshotParameters(), null);

        // Create an ImageView to display the snapshot
        ImageView imageView = new ImageView(snapshot);
        printNode.getChildren().add(imageView);

        return printNode;
    }
}
