package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NewOrderController implements Initializable {
    @FXML
    StackPane stackPane;
    @FXML
    TextField tfName;
    @FXML
    TextField tfUnit;
    @FXML
    TextField tfQuantity;
    @FXML
    TableView <Item> orderTable;
    @FXML
    DatePicker orderDate;
    List<Item> itemsAutoComplete;
    Item selectedItem;

    String server;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");
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
            List<Item> filteredList = itemsAutoComplete.stream()
                    .filter(item -> item.getName().toUpperCase().contains(newValToSearch1))
                    .collect(Collectors.toList());
            filteredList.addAll(itemsAutoComplete.stream()
                    .filter(item -> item.getName().toUpperCase().contains(newValToSearch2))
                    .collect(Collectors.toList()));
            return filteredList;
        }).setPrefWidth(300);

        //tfName.textProperty().addListener(this::changed);

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

        tfQuantity.setOnMouseClicked(event -> {
            if (tfQuantity.isFocused()) {
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

/*
        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
*/

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Προσθήκη των κολόνων στο TableView
        //orderTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn);
        orderTable.getColumns().addAll(nameColumn, quantityColumn, unitColumn, priceColumn);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        orderDate.setValue(LocalDate.now());
    }

    void autocomplete(){
        for (Item item : itemsAutoComplete) {
            if (item.getName().equalsIgnoreCase(tfName.getText())) {
                tfUnit.setText(item.getUnit());
                selectedItem = item;
            }
        }
        tfQuantity.requestFocus();
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
                        BigDecimal quantity = BigDecimal.valueOf(itemNode.get("quantity").asDouble());
                        String unit = itemNode.get("unit").asText();
                        BigDecimal price = BigDecimal.valueOf(itemNode.get("price").asDouble());
                        int category_code = itemNode.get("category_code").asInt();
                        int enable = itemNode.get("enable").asInt();
                        if (enable == 1){
                            Item item = new Item(code, name, quantity, unit, price,category_code);
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
        if (!tfName.getText().isEmpty() && !tfQuantity.getText().isEmpty() && !tfUnit.getText().isEmpty()) {
            // Πάρτε τη λίστα των αντικειμένων από τον πίνακα
            autocomplete();
            ObservableList<Item> items = orderTable.getItems();
            if (!items.contains(selectedItem)) {
                selectedItem.setQuantity(new BigDecimal(tfQuantity.getText()));

                // Προσθέστε το νέο αντικείμενο στη λίσταitem
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
                    existingItem.setQuantity(existingItem.getQuantity().add(new BigDecimal(tfQuantity.getText())));
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

    public void saveAction () {
        try {
            if (!orderTable.getItems().isEmpty()) {
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
                    HtmlConverter.convertToPdf(generateHtmlFromTableView(orderTable), outputStream, converterProperties);

                    System.out.println("PDF created successfully: " + outputFile.getAbsolutePath());
                }
                dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String date = dtf.format(orderDate.getValue());
                System.out.println(date);
                ObservableList<Item> items = orderTable.getItems();
                addNewRequest(items, date);
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

    private void addNewRequest(ObservableList<Item> tableView, String date) {
        String apiUrl = "http://"+server+"/warehouse/orderAdd.php";

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
            jsonRequest.putPOJO("orderTable", tableView);

            // Προσθήκη της ημερομηνίας στο JSON
            jsonRequest.put("date", date); // Προσαρμόστε την ημερομηνία όπως χρειάζεται

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
                        orderTable.getItems().clear();
                        orderTable.refresh();
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
            // Ορισμός της θέσης του κέρσορα στην αρχή
            tfName.positionCaret(0);
        }
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("orders.fxml"));
        Parent root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
