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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class DepartmentStatistics implements Initializable {

    @FXML
    TreeTableView<Item> statisticsTable;
    @FXML
    DatePicker dateFrom;
    @FXML
    DatePicker dateTo;
    @FXML
    ComboBox<Department> departmentField;
    ObservableList<Item> observableListItem;
    String server;
    List<Department> departmentList;
    ObservableList<Department> observableListDep;
    FilteredList<Item> filteredData;
    private List<Item> allItems;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.getInstance().server;

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

        TreeTableColumn<Item, String> unitColumn = new TreeTableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("unit"));

        TreeTableColumn<Item, String> departmentColumn = new TreeTableColumn<>("Τμήμα");
        departmentColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("department"));

        statisticsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn);
        //statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        departmentInit();
        departmentField.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilteredItems(newValue); // Κανονική ενημέρωση με βάση τη νέα επιλογή
        });


        tableInit();

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
        if (departmentList == null) {
            departmentInit();
        }
        fetchDataFromMySQLIfNeeded();
        observableListItem = FXCollections.observableArrayList(allItems);
        filteredData = new FilteredList<>(observableListItem, b -> true);
        TreeItem<Item> root = new TreeItem<>();
        root.setExpanded(true);
        // Δημιουργία ενός tree item για κάθε τμήμα που έχει είδη
        for (Department department : departmentList) {
            FilteredList<Item> itemsForDepartment = filteredData.filtered(item -> item.getDepartment().equals(department.getName()));

            // Εάν το τμήμα έχει είδη, προσθέστε το στο δέντρο
            if (!itemsForDepartment.isEmpty()) {
                TreeItem<Item> departmentNode = new TreeItem<>(new Item("Τμήμα: "+department.getName()));

                for (Item item : itemsForDepartment) {
                    departmentNode.getChildren().add(new TreeItem<>(item));
                }

                root.getChildren().add(departmentNode);
            }

        }


        statisticsTable.setRoot(root);
        // Ανοίγει τα παιδιά του κάθε τμήματος
        root.setExpanded(true);
        statisticsTable.setShowRoot(false);

        for (TreeItem<Item> item : root.getChildren()) {
            // Ανοίγει τα παιδιά του κάθε τμήματος
            item.setExpanded(true);
        }
    }

    private void departmentInit() {
        departmentList = fetchDepFromMySQL();
        if (departmentList.isEmpty()) {
            System.out.println("Η λίστα των τμημάτων είναι κενή.");
        } else {
            departmentList.add(0, new Department(""));
            observableListDep = FXCollections.observableArrayList(departmentList);
            departmentField.setItems(observableListDep);

            departmentField.setCellFactory(param -> new ListCell<Department>() {
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

            departmentField.setButtonCell(new ListCell<Department>() {
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
    }

    // Μέθοδος που φορτώνει τα δεδομένα από το API
    private void fetchDataFromMySQLIfNeeded() {
        if (allItems == null) {
            allItems = fetchDataFromMySQL();
        }
    }


    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/departmentStatistics.php";
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
                        int code = itemNode.get("code").asInt();
                        String name = "    "+itemNode.get("name").asText();
                        BigDecimal quantity = new BigDecimal(itemNode.get("quantity").asText()).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP);
                        String unit = itemNode.get("unit").asText();
                        String department = itemNode.get("depname").asText();
                        Item item = new Item(code, name, quantity, unit,department);
                        Items.add(item);
                        item.print();
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

    private List<Department> fetchDepFromMySQL(){
        String API_URL = "http://"+server+"/warehouse/departmentsGetAll.php";
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

    private void updateFilteredItems(Department selectedDepartment) {
        //fetchDataFromMySQLIfNeeded();
        if (selectedDepartment.getName().equals("")) {
            // Αν δεν υπάρχει επιλεγμένη κατηγορία, εμφάνιση όλων των ειδών
            filteredData.setPredicate(item -> true);
        } else {
            // Φιλτράρισμα των ειδών με βάση την επιλεγμένη κατηγορία
            filteredData.setPredicate(item -> {
                System.out.println("Filtering " + item.getDepartment() + " with " + selectedDepartment.getName());
                return item.getDepartment().equals(selectedDepartment.getName());
            });
        }
        System.out.println("Filtered data size: " + filteredData.size());

        TreeItem<Item> root = statisticsTable.getRoot();
        root.getChildren().clear();
        tableInitFiltered();
    }

    private void tableInitFiltered() {
        TreeItem<Item> root = new TreeItem<>();
        root.setExpanded(true);

        for (Department department : departmentList) {
            FilteredList<Item> itemsForDepartment = filteredData.filtered(item -> item.getDepartment().equals(department.getName()));

            if (!itemsForDepartment.isEmpty()) {
                TreeItem<Item> departmentNode = new TreeItem<>(new Item("Τμήμα: "+department.getName()));

                for (Item item : itemsForDepartment) {
                    departmentNode.getChildren().add(new TreeItem<>(item));
                }

                root.getChildren().add(departmentNode);
            }
        }

        statisticsTable.setRoot(root);
        root.setExpanded(true);
        statisticsTable.setShowRoot(false);
        for (TreeItem<Item> item : root.getChildren()) {
            item.setExpanded(true);
        }
    }

}
