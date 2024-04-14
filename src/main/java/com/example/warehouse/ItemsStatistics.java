package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ItemsStatistics implements Initializable {

    String server;
    @FXML
    TableView<Item> statisticsTable;
    @FXML
    TextField filterField;
    @FXML
    ComboBox<Category> categoryFiled;
    @FXML
    DatePicker dateFrom;
    @FXML
    DatePicker dateTo;

    ObservableList<Category> observableListCat;
    List<Category> categories;
    ObservableList<Item> observableListItem;
    FilteredList<Item> filteredData;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        dateFrom.setValue(firstDayOfMonth);

        // Σημερινή ημερομηνία
        dateTo.setValue(LocalDate.now());

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("item_code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Συνολική Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        statisticsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn);
        tableInit();

        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(observableListItem, b -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(Item -> {
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

                if (Item.getName().toLowerCase().indexOf(lowerCaseFilter) != -1) {
                    return true; // Filter matches first name.
                } else if (String.valueOf(Item.getItem_code()).indexOf(lowerCaseFilter) != -1) {
                    return true; // Filter matches last name.
                } else if (Item.getUnit().toLowerCase().indexOf(lowerCaseFilter) != -1)
                    return true;
                else
                    return false; // Does not match.
            });
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<Item> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        // 	  Otherwise, sorting the TableView would have no effect.
        sortedData.comparatorProperty().bind(statisticsTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        statisticsTable.setItems(sortedData);

        categoryInit();
        categoryFiled.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilteredItems(newValue);
        });

        dateFrom.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Εδώ μπορείτε να καλέσετε τη μέθοδο που θέλετε να εκτελεστεί
            tableInit();
        });
        dateTo.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Εδώ μπορείτε να καλέσετε τη μέθοδο που θέλετε να εκτελεστεί
            tableInit();
        });
    }

    private void tableInit() {
        List<Item> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(items1);
        statisticsTable.setItems(observableListItem);
        categoryFiled.getSelectionModel().select(0);
    }

    private void categoryInit() {
        categories = fetchCatFromMySQL();
        categories.add(0, null);
        observableListCat = FXCollections.observableArrayList(categories);
        categoryFiled.setItems(observableListCat);

        categoryFiled.setCellFactory(param -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        categoryFiled.setButtonCell(new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/itemsStatistics.php";
        List<Item> Items = new ArrayList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateFrom1 = dateFrom.getValue().format(formatter);
            String dateTo1 = dateTo.getValue().format(formatter);

            URL url = new URL(API_URL + "?dateFrom=" + dateFrom1 + "&dateTo=" + dateTo1);
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
                        Item item = new Item(code, name, quantity, unit, price, category_code);
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

    private List<Category> fetchCatFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/categoryGetAll.php";
        List<Category> categories = new ArrayList<>();
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

                        Category category = new Category(code, name);
                        categories.add(category);
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

        return categories;
    }

    private void updateFilteredItems(Category selectedCategory) {
        if (selectedCategory == null) {
            // Αν δεν υπάρχει επιλεγμένη κατηγορία, εμφάνιση όλων των ειδών
            filteredData.setPredicate(item -> true);
        } else {
            // Φιλτράρισμα των ειδών με βάση την επιλεγμένη κατηγορία
            filteredData.setPredicate(item -> {
                return item.getCategory_code() == selectedCategory.getCode();
            });
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
