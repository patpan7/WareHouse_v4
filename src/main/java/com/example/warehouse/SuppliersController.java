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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class SuppliersController implements Initializable {
    @FXML
    StackPane stackPane;
    @FXML
    TableView<Supplier> supplierTable;
    @FXML
    TextField filterField;

    ObservableList<Supplier> observableList;

    String server;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        TableColumn<Supplier, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<Supplier, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Supplier, String> phoneColumn = new TableColumn<>("Τηλέφωνο");
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Supplier, Float> turnoverColumn = new TableColumn<>("Τζίρος");
        turnoverColumn.setCellValueFactory(new PropertyValueFactory<>("turnover"));

        // Προσθήκη των κολόνων στο TableView
        supplierTable.getColumns().addAll(codeColumn, nameColumn, phoneColumn, turnoverColumn);
        tableInit();
        supplierTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        FilteredList<Supplier> filteredData = new FilteredList<>(observableList, b -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(Supplier -> {
                // If filter text is empty, display all persons.

                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String filter = newValue.toUpperCase();
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

                if (Supplier.getName().toUpperCase().indexOf(newValToSearch1) != -1 || Supplier.getName().toUpperCase().indexOf(newValToSearch2) != -1) {
                    return true; // Filter matches first name.
                } else if (String.valueOf(Supplier.getCode()).indexOf(newValToSearch1) != -1 || String.valueOf(Supplier.getCode()).indexOf(newValToSearch2) != -1) {
                    return true; // Filter matches last name.
                }
                else if (Supplier.getPhone().toLowerCase().indexOf(newValToSearch1)!=-1 || Supplier.getPhone().toLowerCase().indexOf(newValToSearch2)!=-1)
                    return true;
                else
                    return false; // Does not match.
            });
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<Supplier> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        // 	  Otherwise, sorting the TableView would have no effect.
        sortedData.comparatorProperty().bind(supplierTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        supplierTable.setItems(sortedData);


        supplierTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Έλεγχος για δύο κλικ
                // Πάρτε τα δεδομένα από την επιλεγμένη γραμμή
                Supplier selectedProduct = supplierTable.getSelectionModel().getSelectedItem();


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
        List<Supplier> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(items1);
        supplierTable.setItems(observableList);
        supplierTable.setRowFactory(tv -> new TableRow<Supplier>() {
            @Override
            protected void updateItem(Supplier supplier, boolean empty) {
                super.updateItem(supplier, empty);
                if (supplier == null || supplier.getEnable() == 0) {
                    ColorAdjust colorAdjust = new ColorAdjust();
                    colorAdjust.setSaturation(-1.0); // Ανενεργό εφέ
                    setEffect(colorAdjust);
                } else {
                    setEffect(null);
                }
            }
        });
        observableList.sort(Comparator.comparingInt(Supplier::getEnable).reversed());
        supplierTable.setItems(observableList);
    }

    private List<Supplier> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/suppliersGetAll.php";
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
                        Supplier supplier = new Supplier(code, name, phone,turnover,enable);
                        Suppliers.add(supplier);
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

    private void openEditDialog(Supplier selectedProduct) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newSupplier.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(loader.load());

        // Ορίζετε τον τίτλο του dialog
        dialog.setTitle("Επεξεργασία Προϊόντος");

        // Παίρνετε πρόσβαση στα παιδιά του dialog box
        TextField nameField = (TextField) dialog.getDialogPane().lookup("#tfName");
        TextField phoneField = (TextField) dialog.getDialogPane().lookup("#tfPhone");
        CheckBox tfEnable = (CheckBox) dialog.getDialogPane().lookup("#tfEnable");

        // Ορίζετε τιμές στα πεδία με βάση τα δεδομένα του επιλεγμένου προϊόντος
        nameField.setText(selectedProduct.getName());
        phoneField.setText(selectedProduct.getPhone());
        if (selectedProduct.getEnable() == 1)
            tfEnable.setSelected(true);
        else
            tfEnable.setSelected(false);

        // Προσθήκη κουμπιών στον διάλογο
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        // Ορίζετε τη συμπεριφορά του κουμπιού "OK" σε περίπτωση πατήματος
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Επιστρέφετε το αντικείμενο με τις ενημερωμένες τιμές
            System.out.println("Πατήθηκε το ΟΚ");
            int enable = 0;
            if (tfEnable.isSelected())
                enable = 1;
            updateRequest(selectedProduct.getCode(),nameField.getText(),phoneField.getText(),enable);
            tableInit();
        }
    }

    public void supplierAddNew(ActionEvent actionEvent) throws IOException {
        try {
            // Φόρτωση του FXML αρχείου για το dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("newSupplier.fxml"));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.getDialogPane().setContent(loader.load());

            dialog.setTitle("Εισαγωγή Προμηθευτή");

            // Ορίζετε τα κουμπιά "OK" και "Cancel"
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            // Εμφάνιση του διαλόγου
            Optional<ButtonType> result = dialog.showAndWait();

            // Εδώ μπορείτε να ελέγξετε το αποτέλεσμα του διαλόγου (OK ή CANCEL)
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Επιλέγετε να κάνετε κάτι εάν πατηθεί το OK
                System.out.println("Πατήθηκε το ΟΚ");
                TextField tfName = (TextField) loader.getNamespace().get("tfName");
                TextField tfPhone = (TextField) loader.getNamespace().get("tfPhone");
                String name = tfName.getText();
                String phone = tfPhone.getText();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Επιβεβαίωση εισαγωγής:");
                alert.setContentText("Όνομα: " + name+", Τηλέφωνο: "+phone);
                Optional<ButtonType> result2 = alert.showAndWait();

                if (result2.isEmpty())
                    return;
                else if (result2.get() == ButtonType.OK) {
                    addNewRequest(name, phone);
                    tableInit();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNewRequest(String name, String phone) {
        String apiUrl = "http://"+server+"/warehouse/supplierAdd.php";

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
            Supplier supplierData = new Supplier(name,phone);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(supplierData);

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

    @FXML
    private void handleEditOption (ActionEvent event) throws IOException {
        Supplier selectedSupplier = supplierTable.getSelectionModel().getSelectedItem();

        if(selectedSupplier == null){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Προσοχή");
            alert.setContentText("Δεν έχει επιλεγεί προμηθευτής!");
            Optional<ButtonType> result = alert.showAndWait();
            return;
        }

        openEditDialog(selectedSupplier);

    }

    private void updateRequest(int code, String name, String phone, int enable) {
        String apiUrl = "http://"+server+"/warehouse/supplierUpdate.php";

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
            Supplier supplierData = new Supplier(code,name,phone,enable);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(supplierData);

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

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
