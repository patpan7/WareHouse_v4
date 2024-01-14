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
import java.util.*;

public class EditBuyController implements Initializable {
    @FXML
    StackPane stackPane;
    public Parent root;
    @FXML
    DatePicker buyDate;
    @FXML
    Button backButton;
    @FXML
    TableView<Item> itemsTable;
    @FXML
    TextField tfSum;
    private ObservableList<Item> observableListItem;
    List<Item> editedList;
    Buys selectedBuy;

    String server;

    public EditBuyController(Buys selectedItem) {
        this.selectedBuy = selectedItem;
    }

    public EditBuyController() {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        editedList = new ArrayList<>();

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("item_code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        quantityColumn.setOnEditCommit(event -> {
            Item editedItem = event.getRowValue();
            BigDecimal newQuantity = event.getNewValue();

            // Εύρεση του editedItem στην editedList βάση του κωδικού
            Optional<Item> existingItem = editedList.stream()
                    .filter(item -> item.getCode() == editedItem.getCode())
                    .findFirst();

            if (existingItem.isPresent()) {
                // Το editedItem υπάρχει στη λίστα, ελέγχουμε αν η τιμή διαφέρει
                if (!Objects.equals(existingItem.get().getQuantity(), newQuantity)) {
                    // Η τιμή διαφέρει, ενημερώνουμε το price
                    existingItem.get().setQuantity(newQuantity);
                    existingItem.get().setSum(newQuantity.multiply(existingItem.get().getPrice()));
                }
            } else {
                // Το editedItem δεν υπάρχει στη λίστα, προσθέτουμε το editedItem με τη νέα τιμή
                editedItem.setQuantity(newQuantity);
                editedItem.setSum(newQuantity.multiply(editedItem.getPrice()));
                editedList.add(editedItem);
            }
            itemsTable.refresh();
            updateTotalSum();

        });

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
        priceColumn.setOnEditCommit(event -> {
            Item editedItem = event.getRowValue();
            BigDecimal newPrice = event.getNewValue();

            // Εύρεση του editedItem στην editedList βάση του κωδικού
            Optional<Item> existingItem = editedList.stream()
                    .filter(item -> item.getCode() == editedItem.getCode())
                    .findFirst();

            if (existingItem.isPresent()) {
                // Το editedItem υπάρχει στη λίστα, ελέγχουμε αν η τιμή διαφέρει
                if (!Objects.equals(existingItem.get().getPrice(), newPrice)) {
                    // Η τιμή διαφέρει, ενημερώνουμε το price
                    existingItem.get().setPrice(newPrice);
                    existingItem.get().setSum(newPrice.multiply(existingItem.get().getQuantity()));
                }
            } else {
                // Το editedItem δεν υπάρχει στη λίστα, προσθέτουμε το editedItem με τη νέα τιμή
                editedItem.setPrice(newPrice);
                editedItem.setSum(newPrice.multiply(editedItem.getPrice()));
                editedList.add(editedItem);
            }
            itemsTable.refresh();
            updateTotalSum();

        });

        TableColumn<Item, BigDecimal> sumColumn = new TableColumn<>("Σύνολο");
        sumColumn.setCellValueFactory(new PropertyValueFactory<>("sum"));



        // Προσθήκη των κολόνων στο TableView
        itemsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn, sumColumn);
        itemsTable.setEditable(true);
        tableInit();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(selectedBuy.getDate(), formatter);

        buyDate.setValue(localDate);
        tfSum.setText(String.valueOf(selectedBuy.getTotal()));
    }

    private void tableInit() {
        List<Item> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(items1);
        itemsTable.setItems(observableListItem);
    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/itemsGetAllBuy.php";
        List<Item> items = new ArrayList<>();
        try {
            URL url = new URL(API_URL + "?invoice=" + selectedBuy.getInvoice() + "&supplier=" + selectedBuy.getSuppliercode());
            System.out.println(url);
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
                        BigDecimal sum = BigDecimal.valueOf(itemNode.get("sum").asDouble());
                        Item item = new Item(code,item_code, name, quantity, unit,price,sum);
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

    private void updateTotalSum() {
        // Υπολογίστε το άθροισμα της στήλης "Σύνολο" και ενημερώστε το tfSum
        BigDecimal totalSum = BigDecimal.ZERO;
        for (Item item : observableListItem) {
            totalSum = totalSum.add(item.getSum());
        }
        tfSum.setText(String.valueOf(totalSum));
    }

    public void saveAction(ActionEvent actionEvent) {
        if (!editedList.isEmpty())
            updateRequest(editedList);
    }

    private void updateRequest(List<Item> editedList) {
        String apiUrl = "http://"+server+"/warehouse/buyEdit.php";

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

    public void mainMenuClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("buys.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
