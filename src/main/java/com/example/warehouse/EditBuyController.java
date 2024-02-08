package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.textfield.TextFields;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EditBuyController implements Initializable {
    @FXML
    StackPane stackPane;
    public Parent root;
    @FXML
    ComboBox<Supplier> tfSupplier;
    @FXML
    DatePicker tfDate;
    @FXML
    TextField tfInvoice;
    @FXML
    TextField tfName;
    @FXML
    TextField tfUnit;
    @FXML
    TextField tfQuantity;
    @FXML
    TextField tfPrice;
    @FXML
    TextField tfSum;
    @FXML
    TableView<Item> buyTable;
    @FXML
    MenuItem editMenu;
    List<Supplier> suppliers;
    ObservableList<Supplier> observableListSup;
    private ObservableList<Item> observableListItem;
    List<Item> editedList;
    List<Item> newList;
    List<Item> deletedList;
    List<Item> dbList;
    Buys selectedBuy;
    Buys editedBuy;
    List<Item> itemsAutoComplete;
    Item selectedProduct;
    float totalSum = 0.0F;
    String server;

    public EditBuyController(Buys selectedBuy) {
        this.selectedBuy = selectedBuy;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");
        editedList = new ArrayList<>();
        newList = new ArrayList<>();
        deletedList = new ArrayList<>();

        supplierInit();
        Supplier selectedSupplier = suppliers.stream()
                .filter(supplier -> supplier.getName().equals(selectedBuy.getName()))
                .findFirst()
                .orElse(null);
        tfSupplier.setValue(selectedSupplier);
        itemsAutoComplete = fetchDataAutoCompleteFromMySQL();
        TextFields.bindAutoCompletion(tfName, request -> {
            String filter = request.getUserText().toUpperCase();
            char[] chars = filter.toCharArray();
            IntStream.range(0, chars.length).forEach(i -> {
                Character repl = ENGLISH_TO_GREEK.get(chars[i]);
                if (repl != null) {
                    chars[i] = repl;
                } else return;
            });
            String newValToSearch = new String(chars);
            List<Item> filteredList = itemsAutoComplete.stream()
                    .filter(item -> item.getName().toUpperCase().contains(newValToSearch))
                    .collect(Collectors.toList());

            return filteredList;
        }).setPrefWidth(300);

        tfName.textProperty().addListener(this::changed);

        // Προσθήκη ακροατή κειμένου
        tfName.addEventHandler(KeyEvent.KEY_TYPED, this::handle);

        tfName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                // Αναζήτηση στη λίστα ειδών
                autocomplete();
            }
        });


        tfUnit.setOnMouseClicked(event -> {
            if (tfQuantity.isFocused()) {
                autocomplete();
            } else {
                System.out.println("Το TextField δεν είναι ενεργοποιημένο με κλικ.");
            }
        });

        applyNumericDecimalFormatter(tfQuantity);
        tfQuantity.setOnMouseClicked(event -> {
            if (tfQuantity.getText().isEmpty()) {
                autocomplete();
            } else {
                System.out.println("Το TextField δεν είναι ενεργοποιημένο με κλικ.");
            }
        });

        tfQuantity.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Αναζήτηση στη λίστα ειδών
                autocomplete();
            }
        });

        applyNumericDecimalFormatter(tfPrice);
        tfPrice.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                // Αναζήτηση στη λίστα ειδών
                addRow();
            }
        });

        tfPrice.setOnMouseClicked(event -> {
            if (tfPrice.getText().isEmpty()) {
                autocomplete();
            } else {
                System.out.println("Το TextField δεν είναι ενεργοποιημένο με κλικ.");
            }
        });

        tfInvoice.setText(selectedBuy.getInvoice());

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("item_code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Item, BigDecimal> sumColumn = new TableColumn<>("Σύνολο");
        sumColumn.setCellValueFactory(new PropertyValueFactory<>("sum"));


        // Προσθήκη των κολόνων στο TableView
        buyTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn, sumColumn);
        tableInit();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(selectedBuy.getDate(), formatter);

        tfDate.setValue(localDate);
        tfSum.setText(String.valueOf(selectedBuy.getTotal()));
        totalSum = selectedBuy.getTotal();
    }

    private void tableInit() {
        dbList = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(dbList);
        buyTable.setItems(observableListItem);
    }

    private void supplierInit() {
        suppliers = fetchSupFromMySQL();
        observableListSup = FXCollections.observableArrayList(suppliers);
        tfSupplier.setItems(observableListSup);

        tfSupplier.setCellFactory(param -> new ListCell<Supplier>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        tfSupplier.setButtonCell(new ListCell<Supplier>() {
            @Override
            protected void updateItem(Supplier item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    private List<Supplier> fetchSupFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/suppliersGetAll.php";
        List<Supplier> Suppliers = new ArrayList<>();
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
                        String phone = itemNode.get("phone").asText();
                        float turnover = Float.parseFloat(itemNode.get("turnover").asText());
                        int enable = itemNode.get("enable").asInt();
                        if (enable == 1) {
                            Supplier supplier = new Supplier(code, name, phone, turnover);
                            Suppliers.add(supplier);
                        }
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

        return Suppliers;
    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/itemsGetAllBuy.php";
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
                        Item item = new Item(code, item_code, name, quantity, unit, price, sum);
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

    void autocomplete() {
        for (Item item : itemsAutoComplete) {
            if (item.getName().equalsIgnoreCase(tfName.getText())) {
                tfUnit.setText(item.getUnit());
                if (tfPrice.getText().isEmpty())
                    tfPrice.setText(String.valueOf(item.getPrice()));
                selectedProduct = item;
            }
        }
        if (tfQuantity.getText().isEmpty())
            tfQuantity.requestFocus();
        else
            tfPrice.requestFocus();
    }

    private List<Item> fetchDataAutoCompleteFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/itemsGetAll.php";
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
                        BigDecimal quantity = BigDecimal.valueOf(itemNode.get("quantity").asDouble());
                        String unit = itemNode.get("unit").asText();
                        BigDecimal price = BigDecimal.valueOf(itemNode.get("price").asDouble());
                        int category_code = itemNode.get("category_code").asInt();
                        int enable = itemNode.get("enable").asInt();
                        if (enable == 1) {
                            Item item = new Item(code, name, quantity, unit, price, category_code);
                            Items.add(item);
                        }
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
        editMenu.setDisable(false);
        //selectedProduct.print();
        if (!tfName.getText().isEmpty() && !tfQuantity.getText().isEmpty() && !tfUnit.getText().isEmpty()) {
            autocomplete();

            String itemName = tfName.getText();
            BigDecimal quantity = new BigDecimal(tfQuantity.getText());
            BigDecimal price = new BigDecimal(tfPrice.getText());

            if (observableListItem.stream().anyMatch(item -> item.getName().equalsIgnoreCase(itemName))) {
                System.out.println("to eidos einai ston pinaka");
                Item existingItem = observableListItem.stream()
                        .filter(item -> item.getName().equalsIgnoreCase(itemName))
                        .findFirst()
                        .orElse(null);

                if (dbList.stream().anyMatch(item -> item.getName().equalsIgnoreCase(itemName))) {
                    System.out.println("to eidos einai ston pinaka kai einai palio");
                    Item insertItem = null;
                    for (Item item : dbList)
                        if (item.getName().equals(itemName))
                            insertItem = item;
                    observableListItem.remove(insertItem);
                    totalSum -= existingItem.getSum().floatValue();
                    insertItem.setQuantity(existingItem.getQuantity().add(quantity));
                    insertItem.setSum(existingItem.getQuantity().multiply(existingItem.getPrice()));
                    observableListItem.add(insertItem);
                    editedList.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                    editedList.add(insertItem);
                    totalSum += insertItem.getSum().floatValue();
                } else {
                    System.out.println("to eidos einai ston pinaka kai einai kainourio");
                    Item insertItem = null;
                    for (Item item : newList)
                        if (item.getName().equals(itemName))
                            insertItem = item;
                    observableListItem.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                    totalSum -= existingItem.getSum().floatValue();
                    insertItem.setQuantity(existingItem.getQuantity().add(quantity));
                    insertItem.setSum(existingItem.getQuantity().multiply(existingItem.getPrice()));
                    observableListItem.add(insertItem);
                    buyTable.refresh();
                    newList.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                    newList.add(insertItem);
                    totalSum += insertItem.getSum().floatValue();
                }
            } else {
                System.out.println("to eidos den einai ston pinaka");
                if (dbList.stream().anyMatch(item -> item.getName().equalsIgnoreCase(itemName))) {
                    System.out.println("to eidos den einai ston pinaka kai einai palio");
                    Item insertItem = null;
                    for (Item item : dbList)
                        if (item.getName().equals(itemName))
                            insertItem = item;
                    insertItem.setQuantity(quantity);
                    insertItem.setSum(quantity.multiply(price));
                    observableListItem.add(insertItem);
                    editedList.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                    editedList.add(insertItem);
                    totalSum += insertItem.getSum().floatValue();
                } else {
                    System.out.println("to eidos den einai ston pinaka kai einai kainourio");
                    selectedProduct.setQuantity(quantity);
                    selectedProduct.setPrice(price);
                    selectedProduct.setSum(quantity.multiply(price));
                    observableListItem.add(selectedProduct);
                    newList.add(selectedProduct);
                    selectedProduct.print();
                    totalSum += selectedProduct.getSum().floatValue();
                }
            }
            buyTable.refresh();
            tfName.setText("");
            tfName.requestFocus();
            tfQuantity.setText("");
            tfUnit.setText("");
            tfPrice.setText("");
            tfSum.setText(String.valueOf(totalSum));
        }
    }

    public void deleteRow() {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        selectedProduct = buyTable.getSelectionModel().getSelectedItem();

        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = buyTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
            deletedList.add(selectedProduct);
            editedList.remove(selectedProduct);
            totalSum -= selectedProduct.getSum().floatValue();
            tfSum.setText(String.valueOf(totalSum));
        }
    }

    public void editRow() {
        editMenu.setDisable(true);
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = buyTable.getSelectionModel().getSelectedItem();
        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            tfName.setText(selectedProduct.getName());
            tfQuantity.setText(String.valueOf(selectedProduct.getQuantity()));
            tfUnit.setText(selectedProduct.getUnit());
            tfPrice.setText(String.valueOf(selectedProduct.getPrice()));
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = buyTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
            totalSum -= selectedProduct.getSum().floatValue();
            tfSum.setText(String.valueOf(buyTable));
        }
    }

    public void saveAction(ActionEvent actionEvent) {
        if (tfSupplier.getValue() != null) {
            if (!tfInvoice.getText().isEmpty()) {
                if (!buyTable.getItems().isEmpty()) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    String newDate = dtf.format(tfDate.getValue());
                    ObservableList<Item> items = buyTable.getItems();
                    int newSupplierCode = tfSupplier.getValue().getCode();
                    String newInvoice = tfInvoice.getText();
                    //addNewRequest(items, suppliercode, date, invoice, totalSum);
                    if (!newInvoice.equals(selectedBuy.getInvoice()) || newSupplierCode != selectedBuy.getSuppliercode() || !newDate.equals(selectedBuy.getDate()))
                        updateInvoice(newInvoice, newSupplierCode, newDate, selectedBuy.getCode(),selectedBuy.getSuppliercode(),selectedBuy.getTotal());
                    else
                        updateInvoceSum(selectedBuy,totalSum);
                    if (editedList.isEmpty() && newList.isEmpty() && deletedList.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("");
                        alert.setContentText("Δεν υπάρχουν αλλαγές!");
                        Optional<ButtonType> result2 = alert.showAndWait();
                    } else {
                        if (!editedList.isEmpty()) {
                            updateRequest(editedList, newSupplierCode, newDate, newInvoice);
                            System.out.println("Λίστα επεξεργασίας");
                        }
                        if (!newList.isEmpty()) {
                            addNewRequest(newList, newSupplierCode, newDate, newInvoice);
                            System.out.println("Νέα λίστα");
                        }
                        if (!deletedList.isEmpty()) {
                            deleteRequest(deletedList);
                            System.out.println("Διαγραμμένη λίστα");
                        }
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("");
                    alert.setContentText("Το τιμολόγιο είναι κενό!");
                    Optional<ButtonType> result2 = alert.showAndWait();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("");
                alert.setContentText("Ο αριθμός τιμολογίου είναι κενός!");
                Optional<ButtonType> result2 = alert.showAndWait();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("");
            alert.setContentText("Δεν έχει επιλεγεί Προμηθευτής!");
            Optional<ButtonType> result2 = alert.showAndWait();
        }
    }

    private void updateInvoceSum(Buys selectedBuy, float totalSum) {
        String apiUrl = "http://" + server + "/warehouse/invoiceUpdateSum.php";

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

            jsonRequest.put("code", selectedBuy.getCode());
            jsonRequest.put("supplierCode", selectedBuy.getSuppliercode());
            jsonRequest.put("totalSum", totalSum);

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
                if (responseCode == 200) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("");
                    alert.setContentText(response.toString());
                    Optional<ButtonType> result2 = alert.showAndWait();
                    if (result2.get() == ButtonType.OK) {
                        buyTable.getItems().clear();
                        buyTable.refresh();
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

    private void updateInvoice(String newInvoice, int newSupplierCode, String newDate, int code, int oldSupplierCode, Float oldTotal) {
        String apiUrl = "http://" + server + "/warehouse/invoiceUpdate.php";

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

            jsonRequest.put("invoice", newInvoice);
            jsonRequest.put("supplierCode", newSupplierCode);
            jsonRequest.put("date", newDate);
            jsonRequest.put("totalSum", totalSum);
            jsonRequest.put("code", code);
            jsonRequest.put("oldSupplierCode", oldSupplierCode);
            jsonRequest.put("oldTotal", oldTotal);
            jsonRequest.putPOJO("dbList", dbList);

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
                if (responseCode == 200) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("");
                    alert.setContentText(response.toString());
                    Optional<ButtonType> result2 = alert.showAndWait();
                    if (result2.get() == ButtonType.OK) {
                        buyTable.getItems().clear();
                        buyTable.refresh();
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

    private void addNewRequest(List<Item> newList, int supplierCode, String date, String invoice) {
        String apiUrl = "http://" + server + "/warehouse/buyAdd2.php";

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
            jsonRequest.putPOJO("newList", newList);

            jsonRequest.put("date", date); // Προσαρμόστε την ημερομηνία όπως χρειάζεται
            jsonRequest.put("supplierCode", supplierCode);
            jsonRequest.put("invoice", invoice);

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
                if (responseCode == 200) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("");
                    alert.setContentText(response.toString());
                    Optional<ButtonType> result2 = alert.showAndWait();
                    if (result2.get() == ButtonType.OK) {
                        buyTable.getItems().clear();
                        buyTable.refresh();
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

    private void deleteRequest(List<Item> deletedList) {
        String apiUrl = "http://" + server + "/warehouse/buyDelete.php";

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
            jsonRequest.putPOJO("deletedList", deletedList);

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

                System.out.println("Response: " + response);
                if (responseCode == 200) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("");
                    alert.setContentText(response.toString());
                    Optional<ButtonType> result2 = alert.showAndWait();
                    if (result2.get() == ButtonType.OK) {
                        buyTable.getItems().clear();
                        buyTable.refresh();
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

    private void updateRequest(List<Item> editedList, int newSupplierCode, String newDate, String newInvoice) {
        String apiUrl = "http://" + server + "/warehouse/buyEdit.php";

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

            jsonRequest.put("date", newDate); // Προσαρμόστε την ημερομηνία όπως χρειάζεται
            jsonRequest.put("supplierCode", newSupplierCode);
            jsonRequest.put("invoice", newInvoice);

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
                if (responseCode == 200) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("");
                    alert.setContentText(response.toString());
                    Optional<ButtonType> result2 = alert.showAndWait();
                    if (result2.get() == ButtonType.OK) {
                        buyTable.getItems().clear();
                        buyTable.refresh();
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

    private static final Map<Character, Character> ENGLISH_TO_GREEK = new HashMap<>();

    static {
        ENGLISH_TO_GREEK.put('\u0041', '\u0391');  // uppercase A
        ENGLISH_TO_GREEK.put('\u0042', '\u0392');  // uppercase B
        ENGLISH_TO_GREEK.put('\u0043', '\u03A8');  // uppercase C
        ENGLISH_TO_GREEK.put('\u0044', '\u0394');  // uppercase D
        ENGLISH_TO_GREEK.put('\u0045', '\u0395');  // uppercase E
        ENGLISH_TO_GREEK.put('\u0046', '\u03A6');  // uppercase F
        ENGLISH_TO_GREEK.put('\u0047', '\u0393');  // uppercase G
        ENGLISH_TO_GREEK.put('\u0048', '\u0397');  // uppercase H
        ENGLISH_TO_GREEK.put('\u0049', '\u0399');  // uppercase I
        ENGLISH_TO_GREEK.put('\u004A', '\u039E');  // uppercase J
        ENGLISH_TO_GREEK.put('\u004B', '\u039A');  // uppercase K
        ENGLISH_TO_GREEK.put('\u004C', '\u039B');  // uppercase L
        ENGLISH_TO_GREEK.put('\u004D', '\u039C');  // uppercase M
        ENGLISH_TO_GREEK.put('\u004E', '\u039D');  // uppercase N
        ENGLISH_TO_GREEK.put('\u004F', '\u039F');  // uppercase O
        ENGLISH_TO_GREEK.put('\u0050', '\u03A0');  // uppercase P
        //ENGLISH_TO_GREEK.put('\u0051', '\u0391');  // uppercase Q
        ENGLISH_TO_GREEK.put('\u0052', '\u03A1');  // uppercase R
        ENGLISH_TO_GREEK.put('\u0053', '\u03A3');  // uppercase S
        ENGLISH_TO_GREEK.put('\u0054', '\u03A4');  // uppercase T
        ENGLISH_TO_GREEK.put('\u0055', '\u0398');  // uppercase U
        ENGLISH_TO_GREEK.put('\u0056', '\u03A9');  // uppercase V
        ENGLISH_TO_GREEK.put('\u0057', '\u03A3');  // uppercase W
        ENGLISH_TO_GREEK.put('\u0058', '\u03A7');  // uppercase X
        ENGLISH_TO_GREEK.put('\u0059', '\u03A5');  // uppercase Y
        ENGLISH_TO_GREEK.put('\u005A', '\u0396');  // uppercase Z

        // ...
    }

    private void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        // Έλεγχος αν το μήκος του κειμένου είναι μεγαλύτερο από το μήκος του TextField
        if (tfName.getText().length() > tfName.getPrefColumnCount()) {
            // Προσαρμογή του μεγέθους της γραμματοσειράς
            tfName.setStyle("-fx-font-size: 14;"); // Ορίστε το επιθυμητό μέγεθος της γραμματοσειράς
            tfName.positionCaret(0);
        } else {
            // Επαναφορά του μεγέθους της γραμματοσειράς στην προκαθορισμένη τιμή
            tfName.setStyle(""); // Επαναφορά στο default μέγεθος της γραμματοσειράς
        }
    }

    private void handle(KeyEvent event) {
        // Έλεγχος της θέσης του κέρσορα
        if (tfName.getText().length() > tfName.getPrefColumnCount()) {
            tfName.positionCaret(0);
        }
    }

    private void applyNumericDecimalFormatter(TextField textField) {
        // Ορισμός UnaryOperator για να δέχεται αριθμούς και δεκαδικούς
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();

            // Μετατροπή κόμμα σε τελεία
            text = text.replace(',', '.');

            if (Pattern.matches("[0-9]*\\.?[0-9]*", text)) {
                return change;
            }
            return null;
        };

        // Εφαρμογή του TextFormatter
        TextFormatter<String> textFormatter = new TextFormatter<>(filter);
        textField.setTextFormatter(textFormatter);
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("buys.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
