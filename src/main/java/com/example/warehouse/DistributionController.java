package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DistributionController implements Initializable {
    @FXML
    StackPane stackPane;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    TableView<Distribution> distributionTable;
    @FXML
    DatePicker dateFrom;
    @FXML
    DatePicker dateTo;
    @FXML
    ComboBox<Department> departmentField;

    List<Department> departmentList;
    ObservableList<Department> observableListDep;
    ObservableList<Distribution> observableList;

    FilteredList<Distribution> filteredData;
    String server;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        dateFrom.setValue(firstDayOfMonth);

        // Σημερινή ημερομηνία
        dateTo.setValue(LocalDate.now());

        TableColumn<Distribution, String> departmentColumn = new TableColumn<>("Τμήμα");
        departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));

        TableColumn<Distribution, String> dateColumn = new TableColumn<>("Ημερομηνία");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));


        distributionTable.getColumns().addAll(departmentColumn,dateColumn);
        distributionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableInit();

        filteredData = new FilteredList<>(observableList, b -> true);

        distributionTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Έλεγχος για δύο κλικ
                // Πάρτε τα δεδομένα από την επιλεγμένη γραμμή
                Distribution selectedDistribution = distributionTable.getSelectionModel().getSelectedItem();


                // Έλεγχος αν υπάρχει επιλεγμένο προϊόν
                if (selectedDistribution != null) {
                    // Ανοίξτε το dialog box για επεξεργασία
                    try {
                        openEditDialog(selectedDistribution);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        departmentInit();
        departmentField.valueProperty().addListener((observable, oldValue, newValue) -> {
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

    private void updateFilteredItems(Department selectedDepartment) {
        if (selectedDepartment == null) {
            // Αν δεν υπάρχει επιλεγμένη κατηγορία, εμφάνιση όλων των ειδών
            filteredData.setPredicate(item -> true);
        } else {
            // Φιλτράρισμα των ειδών με βάση την επιλεγμένη κατηγορία
            filteredData.setPredicate(item -> {
                return item.getName().equals(selectedDepartment.getName());
            });
        }
    }

    private void tableInit() {
        List<Distribution> orders1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(orders1);
        distributionTable.setItems(observableList);
    }

    private void departmentInit() {
        departmentList = fetchDepFromMySQL();
        departmentList.add(0,null);
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

    private List<Distribution> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/distributionGetAll.php";
        List<Distribution> distributions = new ArrayList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
                        String department = itemNode.get("department").asText();
                        String date = itemNode.get("date").asText();

                        Distribution distribution = new Distribution(department,date);
                        distributions.add(distribution);
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
        return distributions;
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

    public void destributionAddNew(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("newDistribution.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void departmentAddNew(ActionEvent actionEvent) throws IOException {
        try {
            // Φόρτωση του FXML αρχείου για το dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("newDepartment.fxml"));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.getDialogPane().setContent(loader.load());

            dialog.setTitle("Εισαγωγή Τμήματος");

            // Ορίζετε τα κουμπιά "OK" και "Cancel"
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            // Εμφάνιση του διαλόγου
            Optional<ButtonType> result = dialog.showAndWait();

            // Εδώ μπορείτε να ελέγξετε το αποτέλεσμα του διαλόγου (OK ή CANCEL)
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Επιλέγετε να κάνετε κάτι εάν πατηθεί το OK
                System.out.println("Πατήθηκε το ΟΚ");
                TextField tfDepartment = (TextField) loader.getNamespace().get("tfDepartment");
                String department = tfDepartment.getText();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Επιβεβαίωση εισαγωγής:");
                alert.setContentText("Τμήμα: " + department);
                Optional<ButtonType> result2 = alert.showAndWait();

                if (result2.isEmpty())
                    return;
                else if (result2.get() == ButtonType.OK) {

                    addNewDepartmentRequest(department);
                    departmentInit();
                    // Ενημέρωση του φίλτρου με βάση την επιλεγμένη κατηγορία
                    Department selectedDepartment = departmentField.getValue();
                    updateFilteredItems(selectedDepartment);
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addNewDepartmentRequest(String name) {
        String apiUrl = "http://"+server+"/warehouse/departmentAdd.php";

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
            Department department = new Department(name);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(department);
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

    private void openEditDialog(Distribution selectedItem) throws IOException {
        // Δημιουργία νέου FXMLLoader
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("editDistribution.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}