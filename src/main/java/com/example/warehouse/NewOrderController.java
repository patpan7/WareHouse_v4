package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import org.controlsfx.control.textfield.TextFields;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
public class NewOrderController extends MainMenuController implements Initializable {

    @FXML
    TextField tfName;
    @FXML
    TextField tfUnit;
    @FXML
    TextField tfQuantity;
    @FXML
    TableView <Item> orderTable;

    Item selectedItem;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        List<Item> items1 = fetchDataFromMySQL();
        // Δημιουργία ObservableList από τη λίστα αντικειμένων
        List<String> itemNames = items1.stream()
                .map(Item::getName)
                .collect(Collectors.toList());

        // Ενεργοποίηση αυτόματης συμπλήρωσης στο TextField με βάση το όνομα του είδους
        TextFields.bindAutoCompletion(tfName, itemNames);

        tfName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Αναζήτηση στη λίστα ειδών
                for (Item item : items1) {
                    if (item.getName().equalsIgnoreCase(tfName.getText())) {
                        tfUnit.setText(item.getUnit());
                        selectedItem = item;
                    }
                }
                tfQuantity.requestFocus();
            }
        });

        tfQuantity.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Αναζήτηση στη λίστα ειδών
                addRow();
            }
        });

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Float> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, Float> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Προσθήκη των κολόνων στο TableView
        orderTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://localhost/wharehouse/itemsGetAll.php";
        List<Item> Items = new ArrayList<>();
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.toString());

                String status = jsonNode.get("status").asText();

                if ("success".equals(status)) {
                    JsonNode messageNode = jsonNode.get("message");

                    for (JsonNode itemNode : messageNode) {
                        int code = itemNode.get("code").asInt();
                        String name = itemNode.get("name").asText();
                        float quantity = Float.parseFloat(itemNode.get("quantity").asText());
                        String unit = itemNode.get("unit").asText();
                        float price = Float.parseFloat(itemNode.get("price").asText());

                        Item item = new Item(code, name, quantity, unit, price);
                        Items.add(item);
                    }
                } else {
                    String failMessage = jsonNode.get("message").asText();
                    System.out.println("Failed: " + failMessage);
                }
            } else {
                System.out.println("HTTP request failed with response code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Items;
    }

    public void addRow() {
        if (!tfName.getText().equals("") || !tfQuantity.getText().equals("") || !tfUnit.getText().equals("")) {
            // Πάρτε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = orderTable.getItems();
            if (!items.contains(selectedItem)) {
                selectedItem.setQuantity(Float.parseFloat(tfQuantity.getText()));

                // Προσθέστε το νέο αντικείμενο στη λίστα
                items.add(selectedItem);

                tfName.setText("");
                tfName.requestFocus();
                tfQuantity.setText("");
                tfUnit.setText("");
            } else {
                // Το selectedItem υπάρχει ήδη στη λίστα
                // Βρείτε το υπάρχον αντικείμενο στη λίστα
                Item existingItem = items.stream()
                        .filter(item -> item.equals(selectedItem))
                        .findFirst()
                        .orElse(null);

                if (existingItem != null) {
                    // Προσθέστε το quantity του υπάρχοντος αντικειμένου στο selectedItem
                    items.remove(existingItem);
                    existingItem.setQuantity(existingItem.getQuantity() + Float.parseFloat(tfQuantity.getText()));
                    items.add(existingItem);
                    tfName.setText("");
                    tfName.requestFocus();
                    tfQuantity.setText("");
                    tfUnit.setText("");
                }
            }
        }
    }

    public void deleteRow() {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = orderTable.getSelectionModel().getSelectedItem();

        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = orderTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
        }
    }

    public void editRow() {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = orderTable.getSelectionModel().getSelectedItem();

        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            tfName.setText(selectedProduct.getName());
            tfQuantity.setText(String.valueOf(selectedProduct.getQuantity()));
            tfUnit.setText(selectedProduct.getUnit());
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = orderTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
        }
    }

    public void saveAction (ActionEvent event) {
        try {
            // Χρησιμοποιήστε τον HTMLConverter για να δημιουργήσετε το PDF από τον HTML
            File outputFile = new File("tableview_to_pdf_example.pdf");
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            ConverterProperties converterProperties = new ConverterProperties();
            HtmlConverter.convertToPdf(generateHtmlFromTableView(orderTable), outputStream, converterProperties);

            System.out.println("PDF created successfully: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateHtmlFromTableView(TableView<?> tableView) {
        // Δημιουργία HTML από το TableView
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body>");
        htmlBuilder.append("<table style=\"border: 1px solid black;\n" +
                "border-collapse: collapse;" +
                "font-size: 16pt;\">");

        // Προσθήκη των επικεφαλίδων
        htmlBuilder.append("<tr style=\"border:1px solid black;\">");
        for (TableColumn<?, ?> column : tableView.getColumns()) {
            htmlBuilder.append("<th style=\"border:1px solid black;\">").append(column.getText()).append("</th>");
        }
        htmlBuilder.append("<th style=\"border:1px solid black;\">").append("Ληφθείσα Ποσότητα</th>");
        htmlBuilder.append("</tr>");
        int i = 0;
        // Προσθήκη των δεδομένων
        for (Object item : tableView.getItems()) {

            htmlBuilder.append("<tr style=\"border:1px solid black;\">");
            for (TableColumn<?, ?> column : tableView.getColumns()) {
                Object cellValue = column.getCellData(i);
                htmlBuilder.append("<td style=\"border:1px solid black;\">").append(cellValue != null ? cellValue.toString() : "").append("</td>");
            }
            htmlBuilder.append("<td style=\"border:1px solid black;\"></td>");
            i++;
            htmlBuilder.append("</tr>");
        }

        htmlBuilder.append("</table></body></html>");
        return htmlBuilder.toString();
    }
}
