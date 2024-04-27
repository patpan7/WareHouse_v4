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
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EditDistributionController implements Initializable {
    @FXML
    StackPane stackPane;
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
    @FXML
    MenuItem editMenu;
    List<Department> departments;
    ObservableList<Department> observableListDep;
    ObservableList<Item> observableListItem;
    Item selectedItem;
    List<Item> editedList;
    List<Item> newList;
    List<Item> deletedList;
    List<Item> dbList;
    List<Item> itemsAutoComplete;
    String server;
    Distribution selectedDistribution;

    public EditDistributionController(Distribution selectedItem) {
        this.selectedDistribution = selectedItem;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");
        editedList = new ArrayList<>();
        newList = new ArrayList<>();
        deletedList = new ArrayList<>();
        departmentInit();
        Department selectedDepartment = departments.stream()
                .filter(department -> department.getName().equals(selectedDistribution.getDepartment()))
                .findFirst()
                .orElse(null);
        tfDepartment.setValue(selectedDepartment);

        itemsAutoComplete = fetchDataAutoCompleteFromMySQL();
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

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("item_code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));


        // Προσθήκη των κολόνων στο TableView
        distributionTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn);
        tableInit();


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(selectedDistribution.getDate(), formatter);

        tfDate.setValue(localDate);
    }

    private void tableInit() {
        dbList = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(dbList);
        distributionTable.setItems(observableListItem);
    }

    private void departmentInit() {
        departments = fetchDepFromMySQL();
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

    void autocomplete() {
        for (Item item : itemsAutoComplete) {
            if (item.getName().equalsIgnoreCase(tfName.getText())) {
                tfUnit.setText(item.getUnit());
                tfTotalQuantity.setText(String.valueOf(item.getQuantity()));
                selectedItem = item;
            }
        }
        if (tfQuantity.getText().isEmpty())
            tfQuantity.requestFocus();
    }
    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/itemsGetAllDistribution.php";
        List<Item> items = new ArrayList<>();
        try {
            Date date1 = new SimpleDateFormat("dd/MM/yyyy").parse(selectedDistribution.getDate());
            String date2 = new SimpleDateFormat("yyyy-MM-dd").format(date1);
            URL url = new URL(API_URL + "?date=" + date2 + "&department=" + selectedDistribution.getDepartment());
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
                        BigDecimal quantity = BigDecimal.valueOf(Long.parseLong(itemNode.get("quantity").asText()));
                        String unit = itemNode.get("unit").asText();
                        Item item = new Item(code, item_code, name, quantity, unit);
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
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return items;
    }

    private List<Department> fetchDepFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/departmentsGetAll.php";
        List<Department> departments = new ArrayList<>();
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
                        departments.add(department);
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

        return departments;
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
        if (!tfName.getText().isEmpty() && !tfQuantity.getText().isEmpty() && !tfUnit.getText().isEmpty() && BigDecimal.valueOf(Long.parseLong(tfQuantity.getText())).compareTo(BigDecimal.valueOf(0.01)) > 0) {
            // Πάρτε τη λίστα των αντικειμένων από τον πίνακα
            //autocomplete();

            String itemName = tfName.getText();
            BigDecimal quantity = new BigDecimal(tfTotalQuantity.getText());
            BigDecimal addedQuantity = BigDecimal.valueOf(Long.parseLong(tfQuantity.getText()));

            if (quantity.compareTo(addedQuantity) > 0) {
                //quantity = quantity.subtract(addedQuantity);
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
                        insertItem.setQuantity(existingItem.getQuantity().add(addedQuantity));
                        observableListItem.add(insertItem);
                        editedList.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                        editedList.add(insertItem);
                    } else {
                        System.out.println("to eidos einai ston pinaka kai einai kainourio");
                        Item insertItem = null;
                        for (Item item : newList)
                            if (item.getName().equals(itemName))
                                insertItem = item;
                        observableListItem.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                        insertItem.setQuantity(existingItem.getQuantity().add(addedQuantity));
                        observableListItem.add(insertItem);
                        distributionTable.refresh();
                        newList.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                        newList.add(insertItem);
                    }
                } else {
                    System.out.println("to eidos den einai ston pinaka");
                    if (dbList.stream().anyMatch(item -> item.getName().equalsIgnoreCase(itemName))) {
                        System.out.println("to eidos den einai ston pinaka kai einai palio");
                        Item insertItem = null;
                        for (Item item : dbList)
                            if (item.getName().equals(itemName))
                                insertItem = item;
                        insertItem.setQuantity(addedQuantity);
                        observableListItem.add(insertItem);
                        editedList.removeIf(item -> item.getName().equalsIgnoreCase(itemName));
                        editedList.add(insertItem);
                    } else {
                        System.out.println("to eidos den einai ston pinaka kai einai kainourio");
                        selectedItem.setQuantity(addedQuantity);
                        observableListItem.add(selectedItem);
                        newList.add(selectedItem);
                        selectedItem.print();
                    }
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("");
                alert.setContentText("Λάθος ποσότητα!");
                Optional<ButtonType> result2 = alert.showAndWait();
            }
            distributionTable.refresh();
            tfName.setText("");
            tfName.requestFocus();
            tfQuantity.setText("");
            tfUnit.setText("");
        }
    }

    public void editRow(ActionEvent actionEvent) {
        editMenu.setDisable(true);
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
            Item originalItem = itemsAutoComplete.stream()
                    .filter(item -> item.getItem_code() == selectedProduct.getItem_code())
                    .findFirst()
                    .orElse(null);

            // Προσθέστε το quantity του επιλεγμένου αντικειμένου στην αρχική λίστα
            if (originalItem != null) {
                originalItem.print();
                originalItem.setQuantity(originalItem.getQuantity().add(selectedProduct.getQuantity()));
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
            deletedList.add(selectedProduct);
            editedList.remove(selectedProduct);
//            // Βρείτε το αντίστοιχο αντικείμενο στην αρχική λίστα
//            Item originalItem = dbList.stream()
//                    .filter(item -> item.getItem_code() == selectedProduct.getItem_code())
//                    .findFirst()
//                    .orElse(null);
//
//            // Προσθέστε το quantity του επιλεγμένου αντικειμένου στην αρχική λίστα
//            if (originalItem != null) {
//                originalItem.setQuantity(originalItem.getQuantity().add(selectedProduct.getQuantity()));
//            }
        }
    }

    public void saveAction(ActionEvent actionEvent) {
        if (tfDepartment.getValue() != null){
            if (!distributionTable.getItems().isEmpty()){
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String date = dtf.format(tfDate.getValue());
                ObservableList<Item> items = distributionTable.getItems();
                int departmentcode = tfDepartment.getValue().getCode();
                //addNewRequest(items,departmentcode,date);
                if (!tfDepartment.getValue().equals(selectedDistribution.getDepartment()) || !date.equals(selectedDistribution.getDate()))
                    updateDepartment(departmentcode, date);
                if (editedList.isEmpty() && newList.isEmpty() && deletedList.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("");
                    alert.setContentText("Δεν υπάρχουν αλλαγές!");
                    Optional<ButtonType> result2 = alert.showAndWait();
                } else {
                    if (!editedList.isEmpty()) {
                        updateRequest(editedList, departmentcode, date);
                        System.out.println("Λίστα επεξεργασίας");
                    }
                    if (!newList.isEmpty()) {
                        addNewRequest(newList, departmentcode, date);
                        System.out.println("Νέα λίστα");
                    }
                    if (!deletedList.isEmpty()) {
                        deleteRequest(deletedList);
                        System.out.println("Διαγραμμένη λίστα");
                    }
                }
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

    private void updateDepartment(int departmentCode, String date) {
        String apiUrl = "http://" + server + "/warehouse/departmentUpdate.php";

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

            jsonRequest.put("departmentCode", departmentCode);
            jsonRequest.put("date", date);
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
                        distributionTable.getItems().clear();
                        distributionTable.refresh();
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

    private void updateRequest(List<Item> editedList, int departmentCode, String date) {
        String apiUrl = "http://" + server + "/warehouse/distributionEdit.php";

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

            jsonRequest.put("date", date); // Προσαρμόστε την ημερομηνία όπως χρειάζεται
            jsonRequest.put("departmentCode", departmentCode);

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
                        distributionTable.getItems().clear();
                        distributionTable.refresh();
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

    private void addNewRequest(List<Item> items, int departmentcode, String date) {
        String apiUrl = "http://"+server+"/warehouse/distributionAdd2.php";

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

    private void deleteRequest(List<Item> deletedList) {
        String apiUrl = "http://" + server + "/warehouse/distributionDelete.php";

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
                        distributionTable.getItems().clear();
                        distributionTable.refresh();
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("distribution.fxml"));
        Parent root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
