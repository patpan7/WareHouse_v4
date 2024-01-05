package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

public class NewDistributionController implements Initializable {

    @FXML
    ComboBox<Department> tfDepartment;
    @FXML
    DatePicker tfDate;
    @FXML
    TextField tfName;
    @FXML
    TextField tfUnit;
    @FXML
    TextField tfQuantity;
    @FXML
    TextField tfTotalQuantity;
    @FXML
    TableView<Item> distributionTable;
    List<Department> departments;
    ObservableList<Department> observableListDep;
    List<Item> items1;
    Item selectedItem;
    String server;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        departmentInit();

        tfDate.setValue(LocalDate.now());
        items1 = fetchDataFromMySQL();
        // Ενεργοποίηση αυτόματης συμπλήρωσης στο TextField με βάση το όνομα του είδους
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
            List<Item> filteredList = items1.stream()
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
                addRow();
            }
        });


        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Float> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        // Προσθήκη των κολόνων στο TableView
        distributionTable.getColumns().addAll(nameColumn, quantityColumn, unitColumn);
        distributionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    }

    void autocomplete(){
        for (Item item : items1) {
            if (item.getName().equalsIgnoreCase(tfName.getText())) {
                tfUnit.setText(item.getUnit());
                tfTotalQuantity.setText(String.valueOf(item.getQuantity()));
                selectedItem = item;
            }
        }
        if(tfQuantity.getText().isEmpty())
            tfQuantity.requestFocus();
    }

    private void departmentInit() {
        departments = fetchSupFromMySQL();
        observableListDep = FXCollections.observableArrayList(departments);
        tfDepartment.setItems(observableListDep);

        tfDepartment.setCellFactory(param -> new ListCell<Department>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        tfDepartment.setButtonCell(new ListCell<Department>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    private List<Department> fetchSupFromMySQL(){
        String API_URL = "http://"+server+"/warehouse/departmentsGetAll.php";
        List<Department> Departments = new ArrayList<>();
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

                        Department department = new Department(code, name);
                        Departments.add(department);
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

        return Departments;
    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/itemsGetAll.php";
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
                        int category_code = itemNode.get("category_code").asInt();
                        float sum = 0.0F;
                        Item item = new Item(code, name, quantity, unit, price,category_code,sum);
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
        if (!tfName.getText().isEmpty() && !tfQuantity.getText().isEmpty() && !tfUnit.getText().isEmpty() && Float.parseFloat(tfQuantity.getText())>=0.01) {
            // Πάρτε τη λίστα των αντικειμένων από τον πίνακα
            autocomplete();
            ObservableList<Item> items = distributionTable.getItems();

            if (selectedItem != null) {
                float addedQuantity = Float.parseFloat(tfQuantity.getText());

                // Ελέγξτε αν το είδος υπάρχει στη λίστα του autocomplete
                if (selectedItem.getQuantity() >= addedQuantity) {
                    selectedItem.setQuantity(selectedItem.getQuantity() - addedQuantity);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("");
                    alert.setContentText("Ανεπαρκές υπόλοιπο!");
                    Optional<ButtonType> result2 = alert.showAndWait();
                    return;
                }

                // Ελέγξτε αν το είδος υπάρχει ήδη στον πίνακα
                Item existingItem = items.stream()
                        .filter(item -> item.getCode() == selectedItem.getCode())
                        .findFirst()
                        .orElse(null);

                if (existingItem != null) {
                    // Το είδος υπάρχει, ενημερώστε το quantity
                    float newQuantity = existingItem.getQuantity() + addedQuantity;
                    existingItem.setQuantity(newQuantity);

                    // Αφαιρέστε το υπάρχον αντικείμενο από τον πίνακα
                    items.remove(existingItem);

                    // Προσθήκη του ενημερωμένου αντικειμένου στον πίνακα
                    items.add(existingItem);

                    // Ενημέρωση του πίνακα
                    distributionTable.refresh();
                } else {
                    // Το είδος δεν υπάρχει, ανανεώστε το υπόλοιπο του είδους στο autocomplete
                    int index = items1.indexOf(selectedItem);
                    if (index != -1) {
                        items1.set(index, selectedItem);
                    }

                    // Δημιουργία νέου αντικειμένου για τον πίνακα
                    Item newItem = new Item();
                    newItem.setCode(selectedItem.getCode());
                    newItem.setName(selectedItem.getName());
                    newItem.setQuantity(addedQuantity);
                    newItem.setUnit(selectedItem.getUnit());
                    newItem.setPrice(selectedItem.getPrice());
                    newItem.setSum(selectedItem.getSum());

                    // Προσθήκη του νέου αντικειμένου στη λίστα του πίνακα
                    items.add(newItem);
                }

                // Καθαρισμός των πεδίων
                tfName.setText("");
                tfName.requestFocus();
                tfTotalQuantity.setText("");
                tfQuantity.setText("");
                tfUnit.setText("");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("");
            alert.setContentText("Λάθος ποσότητα!");
            Optional<ButtonType> result2 = alert.showAndWait();
        }
    }

    public void editRow(ActionEvent actionEvent) {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = distributionTable.getSelectionModel().getSelectedItem();
        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            tfName.setText(selectedProduct.getName());
            tfQuantity.setText(String.valueOf(selectedProduct.getQuantity()));
            tfUnit.setText(selectedProduct.getUnit());
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = distributionTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);

            // Βρείτε το αντίστοιχο αντικείμενο στην αρχική λίστα
            Item originalItem = items1.stream()
                    .filter(item -> item.getCode() == selectedProduct.getCode())
                    .findFirst()
                    .orElse(null);

            // Προσθέστε το quantity του επιλεγμένου αντικειμένου στην αρχική λίστα
            if (originalItem != null) {
                originalItem.setQuantity(originalItem.getQuantity() + selectedProduct.getQuantity());
                tfTotalQuantity.setText(String.valueOf(originalItem.getQuantity()));
            }
        }
    }

    public void deleteRow(ActionEvent actionEvent) {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = distributionTable.getSelectionModel().getSelectedItem();
        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = distributionTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);

            // Βρείτε το αντίστοιχο αντικείμενο στην αρχική λίστα
            Item originalItem = items1.stream()
                    .filter(item -> item.getCode() == selectedProduct.getCode())
                    .findFirst()
                    .orElse(null);

            // Προσθέστε το quantity του επιλεγμένου αντικειμένου στην αρχική λίστα
            if (originalItem != null) {
                originalItem.setQuantity(originalItem.getQuantity() + selectedProduct.getQuantity());
            }
        }
    }

    public void saveAction(ActionEvent actionEvent) {
        if (tfDepartment.getValue() != null){
            if (!distributionTable.getItems().isEmpty()){
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    String date = dtf.format(tfDate.getValue());
                    ObservableList<Item> items = distributionTable.getItems();
                    int departmentcode = tfDepartment.getValue().getCode();
                    addNewRequest(items,departmentcode,date);
            }else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("");
                alert.setContentText("Το κίνηση είναι κενή!");
                Optional<ButtonType> result2 = alert.showAndWait();
            }
        } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("");
                alert.setContentText("Δεν έχει επιλεγεί Τμήμα!");
                Optional<ButtonType> result2 = alert.showAndWait();
        }
    }

    private void addNewRequest(ObservableList<Item> items, int departmentcode, String date) {
        String apiUrl = "http://"+server+"/warehouse/distributionAdd.php";

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
            jsonRequest.putPOJO("distributionTable", items);

            jsonRequest.put("date", date); // Προσαρμόστε την ημερομηνία όπως χρειάζεται
            jsonRequest.put("departmentcode", departmentcode);

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

    public void clean(){
        distributionTable.getItems().clear();
        distributionTable.refresh();
        tfDepartment.setValue(null);
        tfDate.setValue(LocalDate.now());
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
        if (newValue.isEmpty()) {
            selectedItem = null;
            tfUnit.setText("");
            tfTotalQuantity.setText("");
            tfQuantity.setText("");
        }
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
}
