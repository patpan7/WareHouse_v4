package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class ProductsController extends MainMenuController implements Initializable {
    String[] UNITS = {"ΤΕΜ", "ΚΙΛ", "ΛΙΤ", "ΔΟΧ"};
    @FXML
    TableView <item> itemsTable;
    @FXML
    TextField filterField;

    ObservableList<item> observableList;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        TableColumn<item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<item, Float> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<item, Float> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Προσθήκη των κολόνων στο TableView
        itemsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn);
        tableInit();

        // Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<item> filteredData = new FilteredList<>(observableList, b -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                // If filter text is empty, display all persons.

                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String ValToSearch = newValue.toUpperCase();
                char[] chars = ValToSearch.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                    Character repl = ENGLISH_TO_GREEK.get(chars[i]);
                    if (repl != null) {
                        chars[i] = repl;
                    }
                }
                String newValToSearch = new String(chars);

                // Compare first name and last name of every person with filter text.
                String lowerCaseFilter = newValToSearch.toLowerCase();

                if (item.getName().toLowerCase().indexOf(lowerCaseFilter) != -1 ) {
                    return true; // Filter matches first name.
                } else if (String.valueOf(item.getCode()).indexOf(lowerCaseFilter) != -1) {
                    return true; // Filter matches last name.
                }
                else if (item.getUnit().toLowerCase().indexOf(lowerCaseFilter)!=-1)
                    return true;
                else
                    return false; // Does not match.
            });
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<item> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        // 	  Otherwise, sorting the TableView would have no effect.
        sortedData.comparatorProperty().bind(itemsTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        itemsTable.setItems(sortedData);


        itemsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Έλεγχος για δύο κλικ
                // Πάρτε τα δεδομένα από την επιλεγμένη γραμμή
                item selectedProduct = itemsTable.getSelectionModel().getSelectedItem();


                // Έλεγχος αν υπάρχει επιλεγμένο προϊόν
                if (selectedProduct != null) {
                    // Ανοίξτε το dialog box για επεξεργασία
                    try {
                        openEditDialog(selectedProduct);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void tableInit() {
        List<item> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(items1);
        itemsTable.setItems(observableList);
    }

    private List<item> fetchDataFromMySQL() {
        String API_URL = "http://localhost/wharehouse/itemsGetAll.php";
            List<item> items = new ArrayList<>();
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

                            item item = new item(code, name, quantity, unit, price);
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

    private void openEditDialog(item selectedProduct) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newItem.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(loader.load());

        // Ορίζετε τον τίτλο του dialog
        dialog.setTitle("Επεξεργασία Προϊόντος");

        // Παίρνετε πρόσβαση στα παιδιά του dialog box
        TextField nameField = (TextField) dialog.getDialogPane().lookup("#tfName");
        TextField priceField = (TextField) dialog.getDialogPane().lookup("#tfPrice");
        ComboBox unitField = (ComboBox) dialog.getDialogPane().lookup("#tfUnit");


        // Ορίζετε τιμές στα πεδία με βάση τα δεδομένα του επιλεγμένου προϊόντος
        nameField.setText(selectedProduct.getName());
        priceField.setText(String.valueOf(selectedProduct.getPrice()));
        unitField.getItems().addAll(UNITS);
        unitField.setValue(selectedProduct.getUnit());

        // Προσθήκη κουμπιών στον διάλογο
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        // Ορίζετε τη συμπεριφορά του κουμπιού "OK" σε περίπτωση πατήματος
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Επιστρέφετε το αντικείμενο με τις ενημερωμένες τιμές
                System.out.println("Πατήθηκε το ΟΚ");
                updateRequest(selectedProduct.getCode(),nameField.getText(),Float.parseFloat(priceField.getText()), unitField.getValue().toString());
                tableInit();
            }
    }

    public void itemAddNew(ActionEvent actionEvent) throws IOException {
        try {
            // Φόρτωση του FXML αρχείου για το dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("newItem.fxml"));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.getDialogPane().setContent(loader.load());

            dialog.setTitle("Εισαγωγή Προϊόντος");

            ComboBox<String> categoryComboBox = (ComboBox<String>) loader.getNamespace().get("tfUnit");
            categoryComboBox.getItems().addAll(UNITS);
            categoryComboBox.getSelectionModel().selectFirst();

            // Ορίζετε τα κουμπιά "OK" και "Cancel"
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            // Εμφάνιση του διαλόγου
            Optional<ButtonType> result = dialog.showAndWait();

            // Εδώ μπορείτε να ελέγξετε το αποτέλεσμα του διαλόγου (OK ή CANCEL)
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Επιλέγετε να κάνετε κάτι εάν πατηθεί το OK
                System.out.println("Πατήθηκε το ΟΚ");
                TextField tfName = (TextField) loader.getNamespace().get("tfName");
                TextField tfPrice = (TextField) loader.getNamespace().get("tfPrice");
                String name = tfName.getText();
                Float price = Float.parseFloat(tfPrice.getText());
                String unit = categoryComboBox.getValue();

                addNewRequest(name, price, unit);
                tableInit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNewRequest(String name, Float price, String unit) {
        String apiUrl = "http://localhost/wharehouse/itemAdd.php";

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
            item itemData = new item(name,unit,price);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(itemData);

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
            }

            // Κλείσιμο της σύνδεσης
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateRequest(int code, String name, Float price, String unit) {
        String apiUrl = "http://localhost/wharehouse/itemUpdate.php";

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
            item itemData = new item(code,name,unit,price);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(itemData);

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


}
