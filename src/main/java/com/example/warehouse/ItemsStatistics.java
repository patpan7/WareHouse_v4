package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ItemsStatistics implements Initializable {

    String server;
    @FXML
    TreeTableView<Item> statisticsTable;
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
    List<Supplier> supplierList;
    List<Item> allItems;
    List<Item> itemSuppliersStatistics;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        dateFrom.setValue(firstDayOfMonth);

        // Σημερινή ημερομηνία
        dateTo.setValue(LocalDate.now());

        TreeTableColumn<Item, Integer> codeColumn = new TreeTableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("code"));
        codeColumn.setCellFactory(column -> new TreeTableCell<Item, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                // Ελέγξτε αν η τιμή του κωδικού είναι μηδενική και αποκρύψτε το κελί αν είναι
                if (empty || item == null || item == 0) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });


        TreeTableColumn<Item, String> nameColumn = new TreeTableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));

        TreeTableColumn<Item, BigDecimal> quantityColumn = new TreeTableColumn<>("Συνολική Ποσότητα");
        quantityColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("quantity"));

        TreeTableColumn<Item, BigDecimal> totalSumColumn = new TreeTableColumn<>("Συνολικό Κόστος");
        totalSumColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("sum"));
        totalSumColumn.setCellFactory(column -> new TreeTableCell<Item, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);

                // Ελέγξτε αν η τιμή είναι μηδενική ή κενή και αποκρύψτε το κελί αν είναι
                if (empty || item == null || item.equals(BigDecimal.ZERO)) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.toString());
                }
            }
        });

        TreeTableColumn<Item, String> unitColumn = new TreeTableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("unit"));

        TreeTableColumn<Item, BigDecimal> priceColumn = new TreeTableColumn<>("Μέση τιμή");
        priceColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("price"));


        statisticsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn,totalSumColumn, unitColumn, priceColumn);

        supplierInit();

        tableInit();

//        // Wrap the ObservableList in a FilteredList (initially display all data).
//        filteredData = new FilteredList<>(observableListItem, b -> true);
//
//        // 2. Set the filter Predicate whenever the filter changes.
//        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
//            filteredData.setPredicate(Item -> {
//                // If filter text is empty, display all persons.
//
//                if (newValue == null || newValue.isEmpty()) {
//                    return true;
//                }
//
//                String ValToSearch = newValue.toUpperCase();
//                char[] chars = ValToSearch.toCharArray();
//                for (int i = 0; i < chars.length; i++) {
//                    Character repl = ENGLISH_TO_GREEK.get(chars[i]);
//                    if (repl != null) {
//                        chars[i] = repl;
//                    }
//                }
//                String newValToSearch = new String(chars);
//
//                // Compare first name and last name of every person with filter text.
//                String lowerCaseFilter = newValToSearch.toLowerCase();
//
//                if (Item.getName().toLowerCase().indexOf(lowerCaseFilter) != -1) {
//                    return true; // Filter matches first name.
//                } else if (String.valueOf(Item.getItem_code()).indexOf(lowerCaseFilter) != -1) {
//                    return true; // Filter matches last name.
//                } else if (Item.getUnit().toLowerCase().indexOf(lowerCaseFilter) != -1)
//                    return true;
//                else
//                    return false; // Does not match.
//            });
//        });
//
//        // 3. Wrap the FilteredList in a SortedList.
//        SortedList<Item> sortedData = new SortedList<>(filteredData);
//
//        // 4. Bind the SortedList comparator to the TableView comparator.
//        // 	  Otherwise, sorting the TableView would have no effect.
//        sortedData.comparatorProperty().bind((ObservableValue<? extends Comparator<? super Item>>) statisticsTable.comparatorProperty());
//
//        // 5. Add sorted (and filtered) data to the table.
//        statisticsTable.setItems(sortedData);

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

        allItems = fetchItemsFromMySQL();
        itemSuppliersStatistics = fetchitemsStatisticsFromMySQL();

        observableListItem = FXCollections.observableArrayList(allItems);
        filteredData = new FilteredList<>(observableListItem, b -> true);

        TreeItem<Item> root = new TreeItem<>();
        root.setExpanded(true);

        for (Item item : allItems) {
            // Έλεγχος αν υπάρχουν αγορές για το συγκεκριμένο είδος
            boolean hasPurchases = false;
            for (Item purchase : itemSuppliersStatistics) {
                if (purchase.getItem_code() == item.getCode()) {
                    hasPurchases = true;
                    break;
                }
            }

            // Προσθήκη του είδους στο δέντρο μόνο αν υπάρχουν αγορές για αυτό
            if (hasPurchases) {
                TreeItem<Item> itemNode = new TreeItem<>(item);

                for (Item purchase : itemSuppliersStatistics) {
                    if (purchase.getItem_code() == item.getCode()) {
                        purchase.setName(purchase.name);
                        TreeItem<Item> purchaseNode = new TreeItem<>(purchase);
                        itemNode.getChildren().add(purchaseNode);
                    }
                }

                root.getChildren().add(itemNode);
            }
        }

        statisticsTable.setRoot(root);
        statisticsTable.setShowRoot(false);
        for (TreeItem<Item> item : root.getChildren()) {
            // Ανοίγει τα παιδιά του κάθε τμήματος
            item.setExpanded(true);
        }
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

    private void supplierInit() {
        supplierList = fetchSupFromMySQL();
        if (supplierList.isEmpty()) {
            System.out.println("Η λίστα των Προμηθευτών είναι κενή.");
        }
    }


    private List<Item> fetchItemsFromMySQL() {
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
                        int category_code = itemNode.get("category_code").asInt();
                        Item item = new Item(code, name,category_code);
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

    private List<Item> fetchitemsStatisticsFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/itemsStatistics.php";
        List<Item> Items = new ArrayList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateFrom1 = dateFrom.getValue().format(formatter);
            String dateTo1 = dateTo.getValue().format(formatter);

            URL url = new URL(API_URL + "?dateFrom=" + dateFrom1 + "&dateTo=" + dateTo1);
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
                        int item_code = itemNode.get("code").asInt();
                        String supplier_name = "    " + itemNode.get("supplier_name").asText();
                        BigDecimal total_quantity = BigDecimal.valueOf(itemNode.get("total_quantity").asDouble());
                        BigDecimal total_sum = BigDecimal.valueOf(itemNode.get("total_sum").asDouble()).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal average_price = BigDecimal.valueOf(itemNode.get("average_price").asDouble()).setScale(2, RoundingMode.HALF_UP);
                        String unit = itemNode.get("unit").asText();
                        Item item = new Item(item_code, supplier_name, total_quantity, total_sum, average_price, unit);
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

    private List<Supplier> fetchSupFromMySQL(){
        String API_URL = "http://"+server+"/warehouse/suppliersGetAll.php";
        List<Supplier> suppliers = new ArrayList<>();
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
                        String name = itemNode.get("name").asText();

                        Supplier supplier = new Supplier(name);
                        suppliers.add(supplier);
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

        return suppliers;
    }

    private void updateFilteredItems(Category selectedCategory) {
        if (selectedCategory == null) {
            // Αν δεν υπάρχει επιλεγμένη κατηγορία, εμφάνιση όλων των ειδών
            filteredData.setPredicate(item -> true);
        } else {
            // Φιλτράρισμα των ειδών με βάση την επιλεγμένη κατηγορία
            filteredData.setPredicate(item -> {
                System.out.println("Filtering " + item.getCategory_code() + " with " + selectedCategory.getCode());
                return item.getCategory_code() == selectedCategory.getCode();
            });
        }
        TreeItem<Item> root = statisticsTable.getRoot();
        root.getChildren().clear();
        tableInitFiltered();
    }

    private void tableInitFiltered() {
        TreeItem<Item> root = new TreeItem<>();
        root.setExpanded(true);

        for (Item item : filteredData) {
            // Έλεγχος αν υπάρχουν αγορές για το συγκεκριμένο είδος
            boolean hasPurchases = false;
            for (Item purchase : itemSuppliersStatistics) {
                if (purchase.getItem_code() == item.getCode()) {
                    hasPurchases = true;
                    break;
                }
            }

            // Προσθήκη του είδους στο δέντρο μόνο αν υπάρχουν αγορές για αυτό
            if (hasPurchases) {
                TreeItem<Item> itemNode = new TreeItem<>(item);

                for (Item purchase : itemSuppliersStatistics) {
                    if (purchase.getItem_code() == item.getCode()) {
                        purchase.setName(purchase.name);
                        TreeItem<Item> purchaseNode = new TreeItem<>(purchase);
                        itemNode.getChildren().add(purchaseNode);
                    }
                }

                root.getChildren().add(itemNode);
            }
        }

        statisticsTable.setRoot(root);
        statisticsTable.setShowRoot(false);
        for (TreeItem<Item> item : root.getChildren()) {
            // Ανοίγει τα παιδιά του κάθε τμήματος
            item.setExpanded(true);
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
