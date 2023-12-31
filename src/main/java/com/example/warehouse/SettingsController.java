package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController extends MainMenuController implements Initializable {

    @FXML
    TextField tfServer;
    @FXML
    ListView <Unit> listUnits;
    ObservableList<Unit> observableList;
    String server;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");
        // Φόρτωση παραμέτρου
        String server = AppSettings.loadSetting("server");
        tfServer.setText(server);
        listInit();
    }

    private void listInit() {
        List<Unit> units = fetchDataFromMySQL();
        // Προσθήκη των προϊόντων στο ObservableList για την παρακολούθηση των αλλαγών
        observableList = FXCollections.observableArrayList(units);
        listUnits.setItems(observableList);
    }

    private List<Unit> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/unitsGetAll.php";
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

    public void saveSettings(ActionEvent event) throws IOException {
        AppSettings.saveSetting("server", tfServer.getText());
        mainMenuClick(event);
    }

    public void unitAddNew(ActionEvent actionEvent) {
        try {
            // Φόρτωση του FXML αρχείου για το dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("newUnit.fxml"));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.getDialogPane().setContent(loader.load());

            dialog.setTitle("Εισαγωγή Μονάδας μέτρησης");

            // Ορίζετε τα κουμπιά "OK" και "Cancel"
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            // Εμφάνιση του διαλόγου
            Optional<ButtonType> result = dialog.showAndWait();

            // Εδώ μπορείτε να ελέγξετε το αποτέλεσμα του διαλόγου (OK ή CANCEL)
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Επιλέγετε να κάνετε κάτι εάν πατηθεί το OK
                System.out.println("Πατήθηκε το ΟΚ");
                TextField tfUnit = (TextField) loader.getNamespace().get("tfUnit");
                String unit = tfUnit.getText();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Επιβεβαίωση εισαγωγής:");
                alert.setContentText("Μονάδας μέτρησης: " + unit);
                Optional<ButtonType> result2 = alert.showAndWait();

                if (result2.isEmpty())
                    return;
                else if (result2.get() == ButtonType.OK) {

                    addNewCategoryRequest(unit);
                    listInit();
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addNewCategoryRequest(String unit) {
        String apiUrl = "http://"+server+"/warehouse/unitAdd.php";

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
            Unit units = new Unit(unit);

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(units);
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
}
