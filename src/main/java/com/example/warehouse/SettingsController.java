package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {
    @FXML
    StackPane stackPane;
    @FXML
    TextField tfServer;
    @FXML
    TextField tfQuantity;
    @FXML
    TextField tfPrice;
    @FXML
    TextField tfTotal;
    @FXML
    ListView <Unit> listUnits;
    @FXML
    Button btnUpdate;

    ObservableList<Unit> observableList;
    String server;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.loadSetting("server");
        int quantity = Integer.parseInt(AppSettings.loadSetting("QuantityDecimals"));
        int price = Integer.parseInt(AppSettings.loadSetting("PriceDecimals"));
        int total = Integer.parseInt(AppSettings.loadSetting("TotalDecimals"));
        // Φόρτωση παραμέτρου
        //String server = AppSettings.loadSetting("server");
        tfServer.setText(server);
        tfQuantity.setText(String.valueOf(quantity));
        tfPrice.setText(String.valueOf(price));
        tfTotal.setText(String.valueOf(total));
        listInit();

        btnUpdate.setOnAction(event -> showUpdateWindow());
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
        AppSettings.saveSetting("QuantityDecimals", tfQuantity.getText());
        AppSettings.saveSetting("PriceDecimals", tfPrice.getText());
        AppSettings.saveSetting("TotalDecimals", tfTotal.getText());
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

    public void mainMenuClick(ActionEvent event) throws IOException {
        MainMenuController mainMenuController = new MainMenuController();
        mainMenuController.mainMenuClick(stackPane);
    }
    private void showUpdateWindow() {
        Stage updateStage = new Stage();
        updateStage.initModality(Modality.APPLICATION_MODAL);
        updateStage.setTitle("Updating Application");

        Label updateLabel = new Label("Updating...");
        ProgressBar progressBar = new ProgressBar();
        Button restartButton = new Button("Restart");
        restartButton.setDisable(true);
        restartButton.setOnAction(e -> {
            updateStage.close();
            restartApplication();
        });

        VBox vbox = new VBox(10, updateLabel, progressBar, restartButton);
        vbox.setAlignment(Pos.CENTER);
        Scene scene = new Scene(vbox, 300, 150);
        updateStage.setScene(scene);
        updateStage.show();

        Task<Void> updateTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                runUpdateScript(progressBar);
                return null;
            }

            @Override
            protected void succeeded() {
                updateLabel.setText("Update completed");
                progressBar.setProgress(1.0);
                restartButton.setDisable(false);
            }

            @Override
            protected void failed() {
                updateLabel.setText("Update failed");
            }
        };

        new Thread(updateTask).start();
    }

    private void runUpdateScript(ProgressBar progressBar) throws IOException {
        // Create a temporary batch file to replace the executable and restart the application
        String tempBatchFile = "C:\\Warehouse\\tempUpdate.bat";
        try (FileWriter writer = new FileWriter(tempBatchFile)) {
            writer.write("@echo off\n");
            writer.write("timeout /t 5 /nobreak\n"); // Wait for 5 seconds to ensure the application is closed
            writer.write("del \"C:\\Warehouse\\Warehouse.exe\"\n"); // Wait for 5 seconds to ensure the application is closed
            writer.write("ren \"C:\\Warehouse\\new_Warehouse.exe\" \"C:\\Warehouse\\Warehouse.exe\"\n");
            writer.write("start \"C:\\Warehouse\\Warehouse.exe\"\n");
            writer.write("del \"%~f0\"\n"); // Delete this temporary batch file after execution
        }

        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "C:\\Warehouse\\updateApplication.bat");
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                // Update progress bar based on the output of the script
                // For simplicity, we can increment progress here
                Platform.runLater(() -> progressBar.setProgress(progressBar.getProgress() + 0.1));
            }
        }

        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                System.err.println(line);
            }
        }

        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Script exited with code: " + exitCode);
    }

    private void restartApplication() {
        // Close the application
        Platform.exit();

        // Start the temporary batch file to replace the executable and restart the application
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait for the JavaFX application to exit
                new ProcessBuilder("cmd.exe", "/c", "C:\\Warehouse\\tempUpdate.bat").start();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
