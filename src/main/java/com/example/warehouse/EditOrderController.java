package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class EditOrderController implements Initializable {
    @FXML
    StackPane stackPane;
    @FXML
    DatePicker orderDate;
    @FXML
    Button backButton;
    @FXML
    TableView <Item> itemsTable;
    private ObservableList<Item> observableListItem;
    Order selectedOrder;

    String server;
    public EditOrderController(Order selectedOrder) {
        this.selectedOrder = selectedOrder;
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

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
        itemsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn);
        tableInit();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(selectedOrder.getDate(), formatter);

        orderDate.setValue(localDate);
    }

    private void tableInit() {
        List<Item> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(items1);
        itemsTable.setItems(observableListItem);
    }


    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/itemsGetAllOrder.php";
        List<Item> items = new ArrayList<>();
        try {
            URL url = new URL(API_URL + "?date=" + selectedOrder.getDate());
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

                        Item item = new Item(code, name, quantity, unit);
                        items.add(item);
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
        return items;
    }

    public void saveAction(ActionEvent actionEvent) {
        try {
            if (!itemsTable.getItems().isEmpty()) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Επιλογή αρχείου για αποθήκευση");
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                fileChooser.setInitialFileName("Παραγγελία "+ dtf.format(orderDate.getValue()));
                // Επιλέξτε τον τύπο αρχείου που θέλετε να αποθηκεύσετε (π.χ., PDF)
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf");
                fileChooser.getExtensionFilters().add(extFilter);

                // Πάρτε το επιλεγμένο αρχείο
                File file = fileChooser.showSaveDialog(null);

                if (file != null) {
                    // Χρησιμοποιήστε τον HTMLConverter για να δημιουργήσετε το PDF από τον HTML
                    File outputFile = file;
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    ConverterProperties converterProperties = new ConverterProperties();
                    HtmlConverter.convertToPdf(generateHtmlFromTableView(itemsTable), outputStream, converterProperties);

                    System.out.println("PDF created successfully: " + outputFile.getAbsolutePath());
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("");
                alert.setContentText("Η παραγγελία είναι άδεια!");
                Optional<ButtonType> result2 = alert.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateHtmlFromTableView(TableView<?> tableView) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        // Δημιουργία HTML από το TableView
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body>");
        htmlBuilder.append("<h1><center>Παραγγελία "+dtf.format(orderDate.getValue())+"<P>");
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
        for (Object ignored : tableView.getItems()) {

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

    public void handlebackButton(ActionEvent event){
        // Κλείσιμο του παραθύρου
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
