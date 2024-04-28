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
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class BuysController implements Initializable {

    public MenuItem editOption;
    @FXML
    StackPane stackPane;
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
        server = AppSettings.getInstance().server;

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

        buysTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Έλεγχος για δύο κλικ
                // Πάρτε τα δεδομένα από την επιλεγμένη γραμμή
                Buys selectedBuy = buysTable.getSelectionModel().getSelectedItem();


                // Έλεγχος αν υπάρχει επιλεγμένο προϊόν
                if (selectedBuy != null) {
                    // Ανοίξτε το dialog box για επεξεργασία
                    try {
                        openEditDialog(selectedBuy);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
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
        List<Buys> buys1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(buys1);
        buysTable.setItems(observableList);
    }

    private List<Buys> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/buysGetAll.php";
        List<Buys> buys = new ArrayList<>();
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
                        String date = itemNode.get("date").asText();
                        Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(date);
                        String date2 = new SimpleDateFormat("dd/MM/yyyy").format(date1);
                        String invoice = itemNode.get("invoice").asText();
                        BigDecimal total = new BigDecimal(itemNode.get("total").asText()).setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
                        int suppliercode = itemNode.get("suppliercode").asInt();

                        Buys buy = new Buys (code, name,date2, invoice, total,suppliercode);
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
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return buys;
    }

    public void buyAddNew(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newBuy.fxml"));
        loader.setController(new NewBuyController());
        root = loader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void handleEditOption(ActionEvent event) throws IOException {
        Buys selectedOrder = buysTable.getSelectionModel().getSelectedItem();

        if(selectedOrder == null){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Προσοχή");
            alert.setContentText("Δεν έχει επιλεγεί αγορά!");
            Optional<ButtonType> result = alert.showAndWait();
            return;
        }

        openEditDialog(selectedOrder);
    }

    private void openEditDialog(Buys selectedItem) throws IOException {
        // Δημιουργία νέου FXMLLoader
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newBuy.fxml"));
        loader.setController(new EditBuyController(selectedItem));
        root = loader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
}
