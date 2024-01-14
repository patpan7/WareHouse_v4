package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.BigDecimalStringConverter;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    List <Item> editedList;
    Order selectedOrder;

    String server;

    public EditOrderController(Order selectedOrder) {
        this.selectedOrder = selectedOrder;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");
        editedList = new ArrayList<>();

        TableColumn<Item, Integer> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("item_code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        quantityColumn.setOnEditCommit(event -> {
            Item editedItem = event.getRowValue();
            BigDecimal newQuantity = event.getNewValue();

            if (newQuantity != editedItem.getQuantity()){
                editedItem.setQuantity(newQuantity);
                editedList.add(editedItem);
            }
        });

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Προσθήκη των κολόνων στο TableView
        itemsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn);
        itemsTable.setEditable(true);


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
                        int item_code = itemNode.get("item_code").asInt();
                        String name = itemNode.get("name").asText();
                        BigDecimal quantity = BigDecimal.valueOf(itemNode.get("quantity").asDouble());
                        String unit = itemNode.get("unit").asText();
                        BigDecimal price = BigDecimal.valueOf(itemNode.get("price").asDouble());

                        Item item = new Item(code, item_code, name, unit, quantity,price);
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

            if (!editedList.isEmpty()){
                updateRequest(editedList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateRequest(List<Item> editedList) {
        String apiUrl = "http://"+server+"/warehouse/orderEdit.php";

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            // Ορισμός του content type ως JSON
            connection.setRequestProperty("Content-Type", "application/json");

            // Ενεργοποίηση εξόδου
            connection.setDoOutput(true);

            // Δημιουργία του JSON αντικειμένου με τις αντίστοιχες ιδιότητες
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode jsonRequest = objectMapper.createObjectNode();

            // Προσθήκη της λίστας με τα είδη στο JSON
            jsonRequest.putPOJO("editedList", editedList);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(jsonRequest);
            System.out.println(parameters);

            // Αποστολή των παραμέτρων
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = parameters.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Λήψη του HTTP response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Διάβασμα της απάντησης
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                StringBuilder response = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                System.out.println("Response: " + response.toString());
                if (responseCode == 200){
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("");
                    alert.setContentText(response.toString());
                    Optional<ButtonType> result2 = alert.showAndWait();
                    if (result2.get() == ButtonType.OK) {
                        itemsTable.getItems().clear();
                        itemsTable.refresh();
                        mainMenuClick(new ActionEvent());
                    }

                }
            }
            // Κλείσιμο της σύνδεσης
            connection.disconnect();

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

    public void mainMenuClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("orders.fxml"));
        Parent root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
