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
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


public class ItemsController implements Initializable {
    @FXML
    StackPane stackPane;
    String server;
    @FXML
    TableView<Item> itemsTable;
    @FXML
    TextField filterField;
    @FXML
    ComboBox<Category> categoryFiled;

    ObservableList<Category> observableListCat;
    List<Category> categories;
    ObservableList<Item> observableListItem;
    FilteredList<Item> filteredData;
    private String[] fpaList = {"6","13","24"};

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.getInstance().server;

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("item_code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Διαθέσιμη Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Item, String> fpaColumn = new TableColumn<>("ΦΠΑ");
        fpaColumn.setCellValueFactory(new PropertyValueFactory<>("fpa"));

        // Προσθήκη των κολόνων στο TableView
        itemsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn,fpaColumn);
        tableInit();

        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Wrap the ObservableList in a FilteredList (initially display all data).
        filteredData = new FilteredList<>(observableListItem, b -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(Item -> {
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


                if (Item.getName().toUpperCase().indexOf(newValToSearch1) != -1 || Item.getName().toUpperCase().indexOf(newValToSearch2) != -1) {
                    return true; // Filter matches first name.
                } else if (String.valueOf(Item.getItem_code()).indexOf(newValToSearch1) != -1 || String.valueOf(Item.getItem_code()).indexOf(newValToSearch2) != -1) {
                    return true; // Filter matches last name.
                } else if (Item.getUnit().toUpperCase().indexOf(newValToSearch1) != -1 || Item.getUnit().toUpperCase().indexOf(newValToSearch2) != -1)
                    return true;
                else
                    return false; // Does not match.
            });
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<Item> sortedData = new SortedList<>(filteredData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        // 	  Otherwise, sorting the TableView would have no effect.
        sortedData.comparatorProperty().bind(itemsTable.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        itemsTable.setItems(sortedData);


        itemsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Έλεγχος για δύο κλικ
                // Πάρτε τα δεδομένα από την επιλεγμένη γραμμή
                Item selectedProduct = itemsTable.getSelectionModel().getSelectedItem();


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

        categoryInit();
        categoryFiled.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilteredItems(newValue);
        });
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


    private void tableInit() {
        List<Item> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(items1);
        itemsTable.setItems(observableListItem);
        itemsTable.setRowFactory(tv -> new TableRow<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || item.getEnable() == 0) {
//                    ColorAdjust colorAdjust = new ColorAdjust();
//                    colorAdjust.setContrast(-1.0); // Ανενεργό εφέ
//                    setEffect(colorAdjust);
                    getStyleClass().add("inactive-row");
                } else {
//                    setEffect(null);
                    getStyleClass().removeAll("inactive-row");
                }
            }
        });
        observableListItem.sort(Comparator.comparingInt(Item::getEnable).reversed());
        itemsTable.setItems(observableListItem);
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
                        BigDecimal quantity = new BigDecimal(itemNode.get("quantity").asText()).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP);
                        String unit = itemNode.get("unit").asText();
                        BigDecimal price = new BigDecimal(itemNode.get("price").asText()).setScale(AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
                        int category_code = itemNode.get("category_code").asInt();
                        int enable = itemNode.get("enable").asInt();
                        int fpa = itemNode.get("fpa").asInt();
                        Item item = new Item(code, name, quantity, unit, price, category_code, enable,fpa);
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

    private List<Unit> fetchUnitsFromMySQL() {
        String API_URL = "http://" + server + "/warehouse/unitsGetAll.php";
        List<Unit> Units = new ArrayList<>();
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
                        String unit = itemNode.get("unit").asText();

                        Unit units = new Unit(code, unit);
                        Units.add(units);
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

        return Units;

    }

    private void openEditDialog(Item selectedProduct) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newItem.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(loader.load());

        // Ορίζετε τον τίτλο του dialog
        dialog.setTitle("Επεξεργασία Προϊόντος");

        // Παίρνετε πρόσβαση στα παιδιά του dialog box
        TextField nameField = (TextField) dialog.getDialogPane().lookup("#tfName");
        TextField priceField = (TextField) dialog.getDialogPane().lookup("#tfPrice");
        ComboBox unitField = (ComboBox) dialog.getDialogPane().lookup("#tfUnit");
        ComboBox<Category> tfCategory = (ComboBox) dialog.getDialogPane().lookup("#tfCategory");
        ComboBox tfFpa = (ComboBox) dialog.getDialogPane().lookup("#tfFpa");
        CheckBox tfEnable = (CheckBox) dialog.getDialogPane().lookup("#tfEnable");

        // Ορίζετε τιμές στα πεδία με βάση τα δεδομένα του επιλεγμένου προϊόντος
        nameField.setText(selectedProduct.getName());
        priceField.setText(String.valueOf(selectedProduct.getPrice()));

        unitField.getItems().addAll(fetchUnitsFromMySQL());
        unitField.setValue(selectedProduct.getUnit());

        AtomicInteger categoryCode = new AtomicInteger(selectedProduct.getCategory_code());
        tfCategory.getItems().addAll(observableListCat);

        StringConverter<Category> converter = new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return (category != null) ? category.getName() : "";
            }

            @Override
            public Category fromString(String string) {
                // Εδώ μπορείτε να επιστρέψετε την κατηγορία ανάλογα με το όνομα
                // αλλά στην περίπτωση επιλογής από το χρήστη, δεν χρειάζεται να κάνετε κάτι σε αυτή τη μέθοδο.
                return null;
            }
        };

        tfCategory.setConverter(converter);
        int selectedIndex = selectedProduct.getCategory_code();
        tfCategory.getSelectionModel().select(selectedIndex);
        tfCategory.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                categoryCode.set(newValue.getCode()); // Υποθέτοντας ότι η κλάση Category έχει μια μέθοδο getCode() που επιστρέφει τον κωδικό
            } else {
                categoryCode.set(selectedIndex); // Καθαρισμός του catCode αν επιλεχθεί η κενή επιλογή
            }
        });
        //tfCategory.setValue(selectedProduct.getCategory_code());

        tfFpa.getItems().addAll(fpaList);
        tfFpa.setValue(String.valueOf(selectedProduct.getFpa()));

        tfEnable.setSelected(selectedProduct.getEnable() == 1);

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

            priceField.setText(priceField.getText().replace(",", "."));
            BigDecimal price = new BigDecimal(priceField.getText()).setScale(AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
            updateRequest(selectedProduct.getItem_code(), nameField.getText(), price, unitField.getValue().toString(), categoryCode.get(), enable, Integer.parseInt(tfFpa.getValue().toString()));
            // Ενημέρωση του επιλεγμένου αντικειμένου στη λίστα
            selectedProduct.setName(nameField.getText());
            selectedProduct.setPrice(price);
            selectedProduct.setUnit(unitField.getValue().toString());
            selectedProduct.setCategory_code(categoryCode.get());
            selectedProduct.setFpa(Integer.parseInt(tfFpa.getValue().toString()));
            if (tfEnable.isSelected())
                selectedProduct.setEnable(1);
            else
                selectedProduct.setEnable(0);
            // Ανανέωση του TableView
            //itemsTable.refresh();
            //tableInit();

            // Ενημέρωση του φίλτρου με βάση την επιλεγμένη κατηγορία
            Category selectedCategory = categoryFiled.getValue();
            updateFilteredItems(selectedCategory);
        }
    }

    public void itemAddNew(ActionEvent actionEvent) throws IOException {
        try {
            // Φόρτωση του FXML αρχείου για το dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("newItem.fxml"));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.getDialogPane().setContent(loader.load());

            dialog.setTitle("Εισαγωγή Προϊόντος");

            ComboBox unitComboBox = (ComboBox) loader.getNamespace().get("tfUnit");
            unitComboBox.getItems().addAll(fetchUnitsFromMySQL());
            unitComboBox.getSelectionModel().selectFirst();

            ComboBox<Category> tfCategory = (ComboBox) dialog.getDialogPane().lookup("#tfCategory");
            ComboBox tfFpa = (ComboBox) dialog.getDialogPane().lookup("#tfFpa");
            CheckBox tfEnable = (CheckBox) dialog.getDialogPane().lookup("#tfEnable");


            AtomicInteger categotyCode = new AtomicInteger(1);
            tfCategory.getItems().addAll(observableListCat);
            tfCategory.setCellFactory(param -> new ListCell<Category>() {
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

            tfCategory.setButtonCell(new ListCell<Category>() {
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
            tfCategory.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    categotyCode.set(newValue.getCode()); // Υποθέτοντας ότι η κλάση Category έχει μια μέθοδο getCode() που επιστρέφει τον κωδικό
                } else {
                    categotyCode.set(0); // Καθαρισμός του catCode αν επιλεχθεί η κενή επιλογή
                }
            });

            tfFpa.getItems().addAll(fpaList);
            tfFpa.setValue("13");

            tfEnable.setSelected(true);


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
                tfPrice.setText(tfPrice.getText().replace(",", "."));
                String name = tfName.getText();
                BigDecimal price;
                if (tfPrice.getText().isEmpty())
                    price = new BigDecimal("0");
                else
                    price = new BigDecimal(tfPrice.getText()).setScale(AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
                String unit = unitComboBox.getValue().toString();

                int fpa = Integer.parseInt(tfFpa.getValue().toString());

                int enable = 0;
                if (tfEnable.isSelected())
                    enable = 1;
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Επιβεβαίωση εισαγωγής:");
                alert.setContentText("Όνομα: " + name + ", Τιμή: " + price + ", Μον.Μέτρησης: " + unit);
                Optional<ButtonType> result2 = alert.showAndWait();
                if (result2.isEmpty())
                    return;
                else if (result2.get() == ButtonType.OK) {
                    addNewRequest(name, price, unit, categotyCode.get(), enable, fpa);
                    // Επιλογή της κατηγορίας
                    Category selectedCategory = tfCategory.getValue();
                    int categoryCode = (selectedCategory != null) ? selectedCategory.getCode() : 0;
                    Item newItem = new Item(name, unit, price, categoryCode);
                    observableListItem.add(newItem);
                    // Ανανέωση του πίνακα
                    //itemsTable.refresh();
                    tableInit();

                    // Εφαρμογή του φίλτρου με βάση την επιλεγμένη κατηγορία
                    updateFilteredItems(selectedCategory);
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNewRequest(String name, BigDecimal price, String unit, int category_code, int enable, int fpa) {
        String apiUrl = "http://" + server + "/warehouse/itemAdd.php";

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
            Item itemData = new Item(name, unit, price, category_code, enable,fpa);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(itemData);
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
            }

            // Κλείσιμο της σύνδεσης
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void categoryAddNew(ActionEvent actionEvent) throws IOException {
        try {
            // Φόρτωση του FXML αρχείου για το dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("newCategory.fxml"));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.getDialogPane().setContent(loader.load());

            dialog.setTitle("Εισαγωγή Κατηγορίας");

            // Ορίζετε τα κουμπιά "OK" και "Cancel"
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            // Εμφάνιση του διαλόγου
            Optional<ButtonType> result = dialog.showAndWait();

            // Εδώ μπορείτε να ελέγξετε το αποτέλεσμα του διαλόγου (OK ή CANCEL)
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Επιλέγετε να κάνετε κάτι εάν πατηθεί το OK
                System.out.println("Πατήθηκε το ΟΚ");
                TextField tfName = (TextField) loader.getNamespace().get("tfName");
                String name = tfName.getText();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Επιβεβαίωση εισαγωγής:");
                alert.setContentText("Όνομα: " + name);
                Optional<ButtonType> result2 = alert.showAndWait();

                if (result2.isEmpty())
                    return;
                else if (result2.get() == ButtonType.OK) {

                    addNewCategoryRequest(name);
                    categoryInit();
                    // Ενημέρωση του φίλτρου με βάση την επιλεγμένη κατηγορία
                    Category selectedCategory = categoryFiled.getValue();
                    updateFilteredItems(selectedCategory);
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNewCategoryRequest(String name) {
        String apiUrl = "http://" + server + "/warehouse/categoryAdd.php";

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
            Category category = new Category(name);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(category);
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
            }

            // Κλείσιμο της σύνδεσης
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditOption(ActionEvent event) throws IOException {
        Item selectedItem = itemsTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Προσοχή");
            alert.setContentText("Δεν έχει επιλεγέι είδος!");
            Optional<ButtonType> result = alert.showAndWait();
            return;
        }

        openEditDialog(selectedItem);

    }

    private void updateRequest(int code, String name, BigDecimal price, String unit, int category_code, int enable, int fpa) {
        String apiUrl = "http://" + server + "/warehouse/itemUpdate.php";

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
            Item itemData = new Item(code, name, unit, price, category_code, enable, fpa);
            itemData.print();
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

    public void mainMenuClick(ActionEvent actionEvent) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
