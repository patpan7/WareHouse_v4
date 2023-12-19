package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OrdersController extends MainMenuController implements Initializable {

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    TableView <Order> ordersTable;
    ObservableList<Order> observableList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TableColumn<Order, String> dateColumn = new TableColumn<>("Ημερομηνία");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Order, Integer> totalProductsColumn = new TableColumn<>("Σύνολο ειδών");
        totalProductsColumn.setCellValueFactory(new PropertyValueFactory<>("totalProducts"));

        // Προσθήκη των κολόνων στο TableView
        ordersTable.getColumns().addAll(dateColumn,totalProductsColumn);
        tableInit();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

    }

    private void tableInit() {
        List<Order> orders1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(orders1);
        ordersTable.setItems(observableList);
    }

    private List<Order> fetchDataFromMySQL() {
        String API_URL = "http://localhost/wharehouse/ordersGetAll.php";
        List<Order> orders = new ArrayList<>();
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
                        int totalProducts = itemNode.get("totalProducts").asInt();
                        String date = itemNode.get("date").asText();

                        Order order = new Order(date, totalProducts);
                        orders.add(order);
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
        return orders;
    }

    public void orderAddNew(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("newOrder.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


    public void handleEditOption(ActionEvent actionEvent) throws IOException {
        Order selectedItem = ordersTable.getSelectionModel().getSelectedItem();
        System.out.println(selectedItem.getDate());
        if(selectedItem == null){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Προσοχή");
            alert.setContentText("Δεν έχει επιλεγέι είδος!");
            Optional<ButtonType> result = alert.showAndWait();
            return;
        }

        openEditDialog(actionEvent, selectedItem);
    }

    private void openEditDialog(ActionEvent event, Order selectedItem) throws IOException {
        root = FXMLLoader.load(getClass().getResource("editOrder.fxml"));
        Scene newScene = new Scene(root);

        // Δημιουργία ενός κόμβου Node για το MenuItem
        MenuItem menuItem = (MenuItem) event.getSource();
        Node sourceNode = menuItem.getParentPopup().getOwnerNode();

        // Παίρνουμε το παράθυρο από τον τρέχοντα κόμβο
        Stage currentStage = (Stage) sourceNode.getScene().getWindow();

        currentStage.setScene(newScene);
        currentStage.show();
    }
}
