package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
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
import java.util.ResourceBundle;

public class EditBuyController implements Initializable {
    @FXML
    StackPane stackPane;
    public Parent root;
    @FXML
    DatePicker buyDate;
    @FXML
    Button backButton;
    @FXML
    TableView<Item> itemsTable;
    private ObservableList<Item> observableListItem;
    Buys selectedBuy;

    String server;

    public EditBuyController(Buys selectedItem) {
        this.selectedBuy = selectedItem;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");

        TableColumn<Item, String> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Float> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, Float> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Item, Float> sumColumn = new TableColumn<>("Σύνολο");
        sumColumn.setCellValueFactory(new PropertyValueFactory<>("sum"));



        // Προσθήκη των κολόνων στο TableView
        itemsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn, unitColumn, priceColumn, sumColumn);
        tableInit();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate localDate = LocalDate.parse(selectedBuy.getDate(), formatter);

        buyDate.setValue(localDate);
    }

    private void tableInit() {
        List<Item> items1 = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableListItem = FXCollections.observableArrayList(items1);
        itemsTable.setItems(observableListItem);
    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/itemsGetAllBuy.php";
        List<Item> items = new ArrayList<>();
        try {
            URL url = new URL(API_URL + "?invoice=" + selectedBuy.getInvoice() + "&supplier=" + selectedBuy.getSuppliercode());
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
                        float quantity = Float.parseFloat(itemNode.get("quantity").asText());
                        String unit = itemNode.get("unit").asText();
                        float price = Float.parseFloat(itemNode.get("price").asText());
                        float sum = Float.parseFloat(itemNode.get("sum").asText());
                        Item item = new Item(code, name, quantity, unit,price,sum);
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

    public void handlebackButton(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-menu.fxml"));
        root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
