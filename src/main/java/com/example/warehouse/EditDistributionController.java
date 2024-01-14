package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EditDistributionController implements Initializable {
    @FXML
    StackPane stackPane;
    @FXML
    DatePicker buyDate;
    @FXML
    ComboBox<Department> departmentField;
    @FXML
    Button backButton;
    @FXML
    TableView<Item> itemsTable;
    private ObservableList<Item> observableListItem;

    List<Department> departmentList;
    ObservableList<Department> observableListDep;
    Distribution selectedDistribution;

    String server;

    public EditDistributionController(Distribution selectedItem) {
        this.selectedDistribution = selectedItem;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));


        // Προσθήκη των κολόνων στο TableView
        itemsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn);
        tableInit();
        departmentInit();

        Department selectedDepartment = departmentList.stream()
                .filter(department -> department.getName().equals(selectedDistribution.getDepartment()))
                .findFirst()
                .orElse(null);
        departmentField.setValue(selectedDepartment);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(selectedDistribution.getDate(), formatter);

        buyDate.setValue(localDate);
    }
    private void departmentInit() {
        departmentList = fetchDepFromMySQL();
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

    private void tableInit() {
        List<Item> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(items1);
        itemsTable.setItems(observableListItem);
    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/itemsGetAllDistribution.php";
        List<Item> items = new ArrayList<>();
        try {
            URL url = new URL(API_URL + "?date=" + selectedDistribution.getDate() + "&department=" + selectedDistribution.getDepartment());
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
                        String name = itemNode.get("name").asText();
                        BigDecimal quantity = BigDecimal.valueOf(Long.parseLong(itemNode.get("quantity").asText()));
                        String unit = itemNode.get("unit").asText();
                        Item item = new Item(code, name, quantity, unit);
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

    public void handlebackButton(ActionEvent event){
        // Κλείσιμο του παραθύρου
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    public void changeDepartment(ActionEvent actionEvent) {
        departmentField.setEditable(true);
        departmentField.setDisable(false);
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
