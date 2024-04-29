package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

public class NewBuyController implements Initializable {
    @FXML
    StackPane stackPane;
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
    TextField tfTotalPrice;
    @FXML
    TextField tfSum;
    @FXML
    TableView<Item> buyTable;
    ObservableList<Item> items;

    List<Supplier> suppliers;
    ObservableList<Supplier> observableListSup;
    List<Item> itemsAutoComplete;
    Item selectedItem;

    BigDecimal totalSum = new BigDecimal("0").setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
    String server;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.getInstance().server;

        supplierInit();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        tfDate.setValue(LocalDate.now());

        itemsAutoComplete = fetchDataFromMySQL();
        // Ενεργοποίηση αυτόματης συμπλήρωσης στο TextField με βάση το όνομα του είδους
        TextFields.bindAutoCompletion(tfName, request -> {
            String filter = request.getUserText().toUpperCase();
            char[] chars1 = filter.toCharArray();
            IntStream.range(0, chars1.length).forEach(i -> {
                Character repl = ENGLISH_TO_GREEK.get(chars1[i]);
                if (repl != null) {
                    chars1[i] = repl;
                } else return;
            });
            char[] chars2 = filter.toCharArray();
            IntStream.range(0, chars2.length).forEach(i -> {
                Character repl = GREEK_TO_ENGLISH.get(chars2[i]);
                if (repl != null) {
                    chars2[i] = repl;
                } else return;
            });

            String newValToSearch1 = new String(chars1);
            String newValToSearch2 = new String(chars2);
            System.out.println(newValToSearch1);
            System.out.println(newValToSearch2);
            List<Item> filteredList = itemsAutoComplete.stream().filter(item -> item.getName().toUpperCase().contains(newValToSearch1)).collect(Collectors.toList());
            filteredList.addAll(itemsAutoComplete.stream().filter(item -> item.getName().toUpperCase().contains(newValToSearch2)).collect(Collectors.toList()));
            return filteredList;
        }).setPrefWidth(300);

        // Προσθήκη ακροατή κειμένου
        tfName.addEventHandler(KeyEvent.KEY_TYPED, this::handle);

        tfName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                // Αναζήτηση στη λίστα ειδών
                autocomplete();
                tfQuantity.requestFocus();

                event.consume();
            }
        });
        applyNumericDecimalFormatter(tfQuantity);
        tfQuantity.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                if (tfQuantity.getText().contains(",")) {
                    tfQuantity.setText(tfQuantity.getText().replace(",", "."));
                }
                // Υπολογισμός του total price
                BigDecimal quantity = new BigDecimal(tfQuantity.getText()).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP);
                BigDecimal price = new BigDecimal(tfPrice.getText()).setScale(AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
                BigDecimal totalPrice = quantity.multiply(price).setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
                tfTotalPrice.setText(totalPrice.toString());

                tfPrice.requestFocus();
                tfPrice.selectAll();

                selectedItem.setQuantity(quantity);
                selectedItem.setSum(totalPrice);
                event.consume();
            }
        });

        applyNumericDecimalFormatter(tfPrice);
        tfPrice.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                if (tfPrice.getText().contains(",")) {
                    tfPrice.setText(tfPrice.getText().replace(",", "."));
                }
                // Υπολογισμός του total price
                BigDecimal quantity = new BigDecimal(tfQuantity.getText()).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP);
                BigDecimal price = new BigDecimal(tfPrice.getText()).setScale(AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
                BigDecimal totalPrice = quantity.multiply(price).setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
                tfTotalPrice.setText(totalPrice.toString());

                tfTotalPrice.requestFocus();
                tfTotalPrice.selectAll();

                selectedItem.setPrice(price);
                selectedItem.setSum(totalPrice);
                event.consume();
            }
        });

        applyNumericDecimalFormatter(tfTotalPrice);
        tfTotalPrice.setOnKeyPressed(event ->{
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                if (tfTotalPrice.getText().contains(",")) {
                    tfTotalPrice.setText(tfTotalPrice.getText().replace(",", "."));
                }
                BigDecimal quantity = new BigDecimal(tfQuantity.getText()).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP);
                BigDecimal totalPrice = new BigDecimal(tfTotalPrice.getText()).setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
                BigDecimal price = totalPrice.divide(quantity, AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
                tfPrice.setText(price.toString());

                selectedItem.setPrice(price);
                selectedItem.setSum(totalPrice);
                addRow();
                event.consume();
            }
        });

        tfSum.setEditable(false);

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Item, BigDecimal> totalColumn = new TableColumn<>("Σύνολο");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("sum"));


        // Προσθήκη των κολόνων στο TableView
        buyTable.getColumns().addAll(nameColumn, quantityColumn, unitColumn, priceColumn, totalColumn);
        buyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    void autocomplete() {
        for (Item item : itemsAutoComplete) {
            if (item.getName().equalsIgnoreCase(tfName.getText())) {
                tfUnit.setText(item.getUnit());
                tfPrice.setText(item.getPrice().toString());

                selectedItem = item;
            }
        }
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
                        BigDecimal turnover = new BigDecimal(itemNode.get("turnover").asText()).setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
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
                        BigDecimal quantity = new BigDecimal("0").setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP);
                        String unit = itemNode.get("unit").asText();
                        BigDecimal price = new BigDecimal(itemNode.get("price").asText()).setScale(AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
                        int category_code = itemNode.get("category_code").asInt();
                        BigDecimal sum = new BigDecimal("0").setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
                        int enable = itemNode.get("enable").asInt();
                        if (enable == 1) {
                            Item item = new Item(code, name, quantity, unit, price, category_code, sum);
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
        Item insertItem = new Item(selectedItem);

        selectedItem.print();
        if (!tfName.getText().isEmpty() && !tfQuantity.getText().isEmpty() && !tfUnit.getText().isEmpty() && !tfPrice.getText().isEmpty() && new BigDecimal(tfPrice.getText()).compareTo(BigDecimal.ZERO) > 0) {
            items = buyTable.getItems();

            Item existingItem = items.stream()
                    .filter(item -> item.getName().equals(insertItem.getName()) && item.getPrice().equals(insertItem.getPrice()))
                    .findFirst()
                    .orElse(null);

            if (existingItem == null) {
                // New item, add to list
                items.add(insertItem);
                updateTotals(insertItem.getSum());
            } else {
                // Update existing item
                BigDecimal newQuantity = existingItem.getQuantity().add(insertItem.getQuantity());
                existingItem.setQuantity(newQuantity);

                BigDecimal newSum = existingItem.getPrice().multiply(newQuantity);
                existingItem.setSum(newSum);

                buyTable.refresh();
                updateTotals(insertItem.getSum());
            }

            tfName.setText("");
            tfName.requestFocus();
            tfQuantity.setText("");
            tfUnit.setText("");
            tfPrice.setText("");
            tfTotalPrice.setText("");
        }


//            Item existingItem = items.stream()
//                    .filter(item ->
//                            item.getName().equals(selectedItem.getName()) &&
//                                    item.getPrice().equals(selectedItem.getPrice()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (existingItem == null) {
//                // Δεν υπάρχει ήδη στοιχείο με το ίδιο όνομα και τιμή, οπότε προσθέτουμε νέα γραμμή
//                items.add(selectedItem);
//
//                BigDecimal total = selectedItem.getQuantity().multiply(selectedItem.getPrice());
//                totalSum = totalSum.add(total);
//                tfSum.setText(totalSum.toString());
//
//                tfName.setText("");
//                tfName.requestFocus();
//                tfQuantity.setText("");
//                tfUnit.setText("");
//                tfPrice.setText("");
//                tfTotalPrice.setText("");
//            } else {
//                // Υπάρχει ήδη στοιχείο με το ίδιο όνομα και τιμή
//                BigDecimal existingQuantity = existingItem.getQuantity();
//                BigDecimal newQuantity = existingQuantity.add(selectedItem.getQuantity());
//                existingItem.setQuantity(newQuantity);
//
//                BigDecimal newSum = existingItem.getPrice().multiply(newQuantity);
//                existingItem.setSum(newSum);
//
//                buyTable.refresh();
//                tfName.setText("");
//                tfName.requestFocus();
//                tfQuantity.setText("");
//                tfUnit.setText("");
//                tfPrice.setText("");
//                tfTotalPrice.setText("");
//                totalSum = totalSum.add(selectedItem.getSum());
//                tfSum.setText(totalSum.toString());
//            }

    }
    private void updateTotals(BigDecimal itemSum) {
        totalSum = totalSum.add(itemSum);
        tfSum.setText(totalSum.toString());
    }


    public void deleteRow(ActionEvent event) {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = buyTable.getSelectionModel().getSelectedItem();
        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = buyTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
            totalSum = totalSum.subtract(selectedItem.getSum());
            tfSum.setText(String.valueOf(totalSum));
        }
    }

    public void editRow(ActionEvent event) {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = buyTable.getSelectionModel().getSelectedItem();
        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            tfName.setText(selectedProduct.getName());
            tfQuantity.setText(selectedProduct.getQuantity().toString());
            tfUnit.setText(selectedProduct.getUnit());
            tfPrice.setText(selectedProduct.getPrice().toString());
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = buyTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
            totalSum = totalSum.subtract(selectedItem.getSum());
            tfSum.setText(totalSum.toString());
        }
    }


    public void saveAction(ActionEvent event) {
        if (tfSupplier.getValue() != null) {
            if (!tfInvoice.getText().isEmpty()) {
                if (!buyTable.getItems().isEmpty()) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String date = dtf.format(tfDate.getValue());
                    ObservableList<Item> items = buyTable.getItems();
                    int suppliercode = tfSupplier.getValue().getCode();
                    String invoice = tfInvoice.getText();
                    for (Item item : items)
                        item.print();
                    addNewRequest(items, suppliercode, date, invoice, totalSum);
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

    private void addNewRequest(ObservableList<Item> items, int supplierCode, String date, String invoice, BigDecimal totalSum) {
        String apiUrl = "http://" + server + "/warehouse/buyAdd.php";

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
            jsonRequest.putPOJO("orderTable", items);

            jsonRequest.put("date", date); // Προσαρμόστε την ημερομηνία όπως χρειάζεται
            jsonRequest.put("supplierCode", supplierCode);
            jsonRequest.put("invoice", invoice);
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
                        clean();
                    }

                }
            }
            // Κλείσιμο της σύνδεσης
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clean() {
        buyTable.getItems().clear();
        buyTable.refresh();
        totalSum = new BigDecimal("0").setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
        tfSum.setText("");
        tfSupplier.setValue(null);
        tfInvoice.setText("");
        tfDate.setValue(LocalDate.now());
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

    private static final Map<Character, Character> GREEK_TO_ENGLISH = new HashMap<>();

    static {
        GREEK_TO_ENGLISH.put('\u0391', '\u0041');  // uppercase Α
        GREEK_TO_ENGLISH.put('\u0392', '\u0042');  // uppercase Β
        GREEK_TO_ENGLISH.put('\u03A8', '\u0043');  // uppercase Ψ
        GREEK_TO_ENGLISH.put('\u0394', '\u0044');  // uppercase Δ
        GREEK_TO_ENGLISH.put('\u0395', '\u0045');  // uppercase Ε
        GREEK_TO_ENGLISH.put('\u03A6', '\u0046');  // uppercase Φ
        GREEK_TO_ENGLISH.put('\u0393', '\u0047');  // uppercase Γ
        GREEK_TO_ENGLISH.put('\u0397', '\u0048');  // uppercase Η
        GREEK_TO_ENGLISH.put('\u0399', '\u0049');  // uppercase Ι
        GREEK_TO_ENGLISH.put('\u039E', '\u004A');  // uppercase Ξ
        GREEK_TO_ENGLISH.put('\u039A', '\u004B');  // uppercase Κ
        GREEK_TO_ENGLISH.put('\u039B', '\u004C');  // uppercase Λ
        GREEK_TO_ENGLISH.put('\u039C', '\u004D');  // uppercase Μ
        GREEK_TO_ENGLISH.put('\u039D', '\u004E');  // uppercase Ν
        GREEK_TO_ENGLISH.put('\u039F', '\u004F');  // uppercase Ο
        GREEK_TO_ENGLISH.put('\u03A0', '\u0050');  // uppercase Π
        //GREEK_TO_ENGLISH.put('\u0051', '\u0391');  // uppercase Q
        GREEK_TO_ENGLISH.put('\u03A1', '\u0052');  // uppercase Ρ
        GREEK_TO_ENGLISH.put('\u03A3', '\u0053');  // uppercase Σ
        GREEK_TO_ENGLISH.put('\u03A4', '\u0054');  // uppercase Τ
        GREEK_TO_ENGLISH.put('\u0398', '\u0055');  // uppercase Θ
        GREEK_TO_ENGLISH.put('\u03A9', '\u0056');  // uppercase Ω
        GREEK_TO_ENGLISH.put('\u03A3', '\u0053');  // uppercase ς
        GREEK_TO_ENGLISH.put('\u03A7', '\u0058');  // uppercase Χ
        GREEK_TO_ENGLISH.put('\u03A5', '\u0059');  // uppercase Υ
        GREEK_TO_ENGLISH.put('\u0396', '\u005A');  // uppercase Ζ

        // ...
    }


    private void handle(KeyEvent event) {
        // Έλεγχος της θέσης του κέρσορα
        if (tfName.getText().length() > tfName.getPrefColumnCount()) {
            // Ορισμός της θέσης του κέρσορα στην αρχή
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
        Parent root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
