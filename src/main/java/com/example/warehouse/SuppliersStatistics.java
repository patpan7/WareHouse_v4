package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

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

public class SuppliersStatistics implements Initializable {

    @FXML
    TableView<Buys> statisticsTable;
    @FXML
    DatePicker dateFrom;
    @FXML
    DatePicker dateTo;
    @FXML
    ComboBox<Supplier> supplierField;
    ObservableList<Buys> observableList;
    String server;

    List<Supplier> suppliers;
    ObservableList<Supplier> observableListSup;
    FilteredList<Buys> filteredData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.getInstance().server;

        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        dateFrom.setValue(firstDayOfMonth);

        // Σημερινή ημερομηνία
        dateTo.setValue(LocalDate.now());

        TableColumn<Buys, String> supplierColumn = new TableColumn<>("Προμηθευτής");
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Buys, Integer> totalInvColumn = new TableColumn<>("Πλήθος Τιμολογίων");
        totalInvColumn.setCellValueFactory(new PropertyValueFactory<>("totalinvoices"));

        TableColumn<Buys, Float> totalColumn = new TableColumn<>("Σύνολο");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));

        statisticsTable.getColumns().addAll(supplierColumn, totalInvColumn, totalColumn);
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableInit();

        dateFrom.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Εδώ μπορείτε να καλέσετε τη μέθοδο που θέλετε να εκτελεστεί
            tableInit();
        });
        dateTo.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Εδώ μπορείτε να καλέσετε τη μέθοδο που θέλετε να εκτελεστεί
            tableInit();
        });

        supplierInit();
        supplierField.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilteredItems(newValue); // Καλέστε την μέθοδο updateFilteredItems() κάθε φορά που αλλάζει ο επιλεγμένος προμηθευτής
        });
    }

    private void tableInit() {
        List<Buys> buys1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(buys1);
        filteredData = new FilteredList<>(observableList, b -> true); // Δημιουργία του φιλτραρισμένου δεδομένου
        statisticsTable.setItems(filteredData); // Χρήση του φιλτραρισμένου δεδομένου για την ενημέρωση του πίνακα
    }
    /*
        private void tableInit() {
        if (supplierList == null) {
            supplierInit();
        }
        fetchDataFromMySQLIfNeeded(); // Νέα γραμμή

        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(allItems);
        filteredData = new FilteredList<>(observableListItem, b -> true);
        TreeItem<Item> root = new TreeItem<>();
        root.setExpanded(true);
        for (Supplier supplier : supplierList) {
            FilteredList<Item> itemsForSuppliers = filteredData.filtered(item -> item.getSupplier().equals(supplier.getName()));

            // Εάν ο προμηθευτής έχει είδη, προσθέστε το στο δέντρο
            if (!itemsForSuppliers.isEmpty()) {
                TreeItem<Item> supplierNode = new TreeItem<>(new Item("Προμηθευτής: "+supplier.getName()));
                for (Item item : itemsForSuppliers) {
                    supplierNode.getChildren().add(new TreeItem<>(item));
                }
                root.getChildren().add(supplierNode);
            }
        }
        statisticsTable.setRoot(root);
        root.setExpanded(true);
        statisticsTable.setShowRoot(false);
    }
     */

    private List<Buys> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/suppliersStatistics.php";
        List<Buys> buys = new ArrayList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateFrom1 = dateFrom.getValue().format(formatter);
            String dateTo1 = dateTo.getValue().format(formatter);
            URL url = new URL(API_URL + "?dateFrom=" + dateFrom1 + "&dateTo=" + dateTo1);
            System.out.println(url.toString());
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
                        int totalInvoices = itemNode.get("totalInvoices").asInt();
                        BigDecimal total = new BigDecimal(itemNode.get("total").asText()).setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);

                        Buys buy = new Buys (code, name, totalInvoices, total);
                        buys.add(buy);
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
        return buys;
    }

    private void supplierInit() {
        suppliers = fetchSupFromMySQL();
        suppliers.add(0, null);
        observableListSup = FXCollections.observableArrayList(suppliers);
        supplierField.setItems(observableListSup);

        supplierField.setCellFactory(param -> new ListCell<Supplier>() {
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

        supplierField.setButtonCell(new ListCell<Supplier>() {
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

    private void updateFilteredItems(Supplier selectedSupplier) {
        if (selectedSupplier == null) {
            // Αν δεν υπάρχει επιλεγμένη κατηγορία, εμφάνιση όλων των ειδών
            filteredData.setPredicate(item -> true);
        } else {
            // Φιλτράρισμα των ειδών με βάση την επιλεγμένη κατηγορία
            filteredData.setPredicate(item -> {
                System.out.println("itenm: " + item.getCode() + " supplier: " + selectedSupplier.getCode());
                return item.getSuppliercode() == selectedSupplier.getCode();
            });
        }
    }

}
