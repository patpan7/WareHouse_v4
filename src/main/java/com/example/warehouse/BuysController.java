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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class BuysController extends MainMenuController implements Initializable {
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    TableView<Buys> buysTable;
    @FXML
    DatePicker dateFrom;
    @FXML
    DatePicker dateTo;
    ObservableList<Buys> observableList;
    String server;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        dateFrom.setValue(firstDayOfMonth);

        // Σημερινή ημερομηνία
        dateTo.setValue(LocalDate.now());

        TableColumn<Buys, String> supplierColumn = new TableColumn<>("Προμηθευτής");
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Buys, String> dateColumn = new TableColumn<>("Ημερομηνία");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Buys, String> invColumn = new TableColumn<>("Αρ. Τιμολογίου");
        invColumn.setCellValueFactory(new PropertyValueFactory<>("invoice"));

        TableColumn<Buys, Float> totalColumn = new TableColumn<>("Σύνολο");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));

        buysTable.getColumns().addAll(supplierColumn,dateColumn, invColumn, totalColumn);
        tableInit();
        buysTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void tableInit() {
        List<Buys> orders1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(orders1);
        buysTable.setItems(observableList);
    }

    private List<Buys> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/buysGetAll.php";
        List<Buys> buys = new ArrayList<>();
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
                        String name = itemNode.get("name").asText();
                        String date = itemNode.get("date").asText();
                        String invoice = itemNode.get("invoice").asText();
                        Float total = Float.parseFloat(itemNode.get("total").asText());


                        Buys buy = new Buys (name,date, invoice, total);
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

    public void buyAddNew(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("newBuy.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void handleEditOption(ActionEvent event) throws IOException {
        Buys selectedOrder = buysTable.getSelectionModel().getSelectedItem();

        if(selectedOrder == null){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Προσοχή");
            alert.setContentText("Δεν έχει επιλεγέι είδος!");
            Optional<ButtonType> result = alert.showAndWait();
            return;
        }

        openEditDialog(selectedOrder);
    }

    private void openEditDialog(Buys selectedItem) throws IOException {
        // Δημιουργία νέου FXMLLoader
        FXMLLoader loader = new FXMLLoader(getClass().getResource("editBuy.fxml"));

        // Προσθήκη προσαρμοσμένου κατασκευαστή
        loader.setController(new EditBuyController(selectedItem));

        // Φόρτωση του FXML
        Parent root = loader.load();

        // Δημιουργία νέου παραθύρου
        Stage stage = new Stage();
        stage.setTitle("Λεπτομέρειες Παραγγελίας");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
