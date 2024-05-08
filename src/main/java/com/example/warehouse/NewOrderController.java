package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.controlsfx.control.textfield.TextFields;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NewOrderController implements Initializable {
    @FXML
    StackPane stackPane;
    @FXML
    TextField tfName;
    @FXML
    TextField tfUnit;
    @FXML
    TextField tfQuantity;
    @FXML
    TableView <Item> orderTable;
    @FXML
    TextArea taComment;
    @FXML
    DatePicker orderDate;
    List<Item> itemsAutoComplete;
    Item selectedItem;

    String server;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.getInstance().server;

        itemsAutoComplete = fetchDataFromMySQL();

        // Ενεργοποίηση αυτόματης συμπλήρωσης στο TextField με βάση το όνομα του είδους
        TextFields.bindAutoCompletion(tfName, request -> {
            String filter = request.getUserText().toUpperCase();
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
            System.out.println(newValToSearch1);
            System.out.println(newValToSearch2);
            List<Item> filteredList = itemsAutoComplete.stream()
                    .filter(item -> item.getName().toUpperCase().contains(newValToSearch1))
                    .collect(Collectors.toList());
            filteredList.addAll(itemsAutoComplete.stream()
                    .filter(item -> item.getName().toUpperCase().contains(newValToSearch2))
                    .collect(Collectors.toList()));
            return filteredList;
        }).setPrefWidth(300);

        //tfName.textProperty().addListener(this::changed);

        // Προσθήκη ακροατή κειμένου
        tfName.addEventHandler(KeyEvent.KEY_TYPED, this::handle);

        tfName.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                // Αναζήτηση στη λίστα ειδών
                autocomplete();
            }
        });


        tfUnit.setOnMouseClicked(event -> {
            if (tfQuantity.isFocused()) {
                autocomplete();
            } else {
                System.out.println("Το TextField δεν είναι ενεργοποιημένο με κλικ.");
            }
        });

        tfQuantity.setOnMouseClicked(event -> {
            if (tfQuantity.isFocused()) {
                autocomplete();
            } else {
                System.out.println("Το TextField δεν είναι ενεργοποιημένο με κλικ.");
            }
        });

        tfQuantity.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // Αναζήτηση στη λίστα ειδών
                addRow();
            }
        });

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Προσθήκη των κολόνων στο TableView
        orderTable.getColumns().addAll(nameColumn, quantityColumn, unitColumn, priceColumn);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        orderDate.setValue(LocalDate.now());
    }

    void autocomplete(){
        for (Item item : itemsAutoComplete) {
            if (item.getName().equalsIgnoreCase(tfName.getText())) {
                tfUnit.setText(item.getUnit());
                selectedItem = item;
            }
        }
        tfQuantity.requestFocus();
    }

    private List<Item> fetchDataFromMySQL() {
        String API_URL = "http://"+server+"/warehouse/itemsGetAll.php";
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
                        if (enable == 1){
                            Item item = new Item(code, name, quantity, unit, price,category_code);
                            Items.add(item);
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
        return Items;
    }

    public void addRow() {
        if (!tfName.getText().isEmpty() && !tfQuantity.getText().isEmpty() && !tfUnit.getText().isEmpty()) {
            // Πάρτε τη λίστα των αντικειμένων από τον πίνακα
            autocomplete();
            ObservableList<Item> items = orderTable.getItems();
            if (!items.contains(selectedItem)) {
                selectedItem.setQuantity(new BigDecimal(tfQuantity.getText()).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP));

                // Προσθέστε το νέο αντικείμενο στη λίσταitem
                items.add(selectedItem);

                tfName.setText("");
                tfName.requestFocus();
                tfQuantity.setText("");
                tfUnit.setText("");
            } else {
                // Το selectedItem υπάρχει ήδη στη λίστα
                // Βρείτε το υπάρχον αντικείμενο στη λίστα
                Item existingItem = items.stream()
                        .filter(item -> item.equals(selectedItem))
                        .findFirst()
                        .orElse(null);

                if (existingItem != null) {
                    // Προσθέστε το quantity του υπάρχοντος αντικειμένου στο selectedItem
                    items.remove(existingItem);
                    existingItem.setQuantity(existingItem.getQuantity().add(new BigDecimal(tfQuantity.getText())).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP));
                    items.add(existingItem);
                    tfName.setText("");
                    tfName.requestFocus();
                    tfQuantity.setText("");
                    tfUnit.setText("");
                }
            }
        }
    }

    public void deleteRow() {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = orderTable.getSelectionModel().getSelectedItem();

        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = orderTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
        }
    }

    public void editRow() {
        // Λάβετε την επιλεγμένη γραμμή από τον πίνακα
        Item selectedProduct = orderTable.getSelectionModel().getSelectedItem();

        // Αν έχει επιλεγεί γραμμή
        if (selectedProduct != null) {
            tfName.setText(selectedProduct.getName());
            tfQuantity.setText(String.valueOf(selectedProduct.getQuantity()));
            tfUnit.setText(selectedProduct.getUnit());
            // Λάβετε τη λίστα των αντικειμένων από τον πίνακα
            ObservableList<Item> items = orderTable.getItems();

            // Διαγράψτε την επιλεγμένη γραμμή από τη λίστα
            items.remove(selectedProduct);
        }
    }

    public void saveAction () {
        if (!orderTable.getItems().isEmpty()) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Επιλογή αρχείου για αποθήκευση");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            fileChooser.setInitialFileName("Παραγγελία "+ dtf.format(orderDate.getValue()));
            // Επιλέξτε τον τύπο αρχείου που θέλετε να αποθηκεύσετε (π.χ., PDF)
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf");
            fileChooser.getExtensionFilters().add(extFilter);

            // Πάρτε το επιλεγμένο αρχείο
            File file = fileChooser.showSaveDialog(null);

            if (file != null) {
                createPDF(file.getAbsolutePath());
                System.out.println("PDF created successfully: " + file.getAbsolutePath());

            }
            dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String date = dtf.format(orderDate.getValue());
            System.out.println(date);
            ObservableList<Item> items = orderTable.getItems();
            addNewRequest(items, date);
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("");
            alert.setContentText("Η παραγγελία είναι άδεια!");
            Optional<ButtonType> result2 = alert.showAndWait();
        }
    }


    private void addNewRequest(ObservableList<Item> tableView, String date) {
        String apiUrl = "http://"+server+"/warehouse/orderAdd.php";

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
            ObjectNode jsonRequest = objectMapper.createObjectNode();

            // Προσθήκη της λίστας με τα είδη στο JSON
            jsonRequest.putPOJO("orderTable", tableView);

            // Προσθήκη της ημερομηνίας στο JSON
            jsonRequest.put("date", date); // Προσαρμόστε την ημερομηνία όπως χρειάζεται

            // Μετατροπή του JSON αντικειμένου σε JSON string
            String parameters = objectMapper.writeValueAsString(jsonRequest);
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
                if (responseCode == 200){
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("");
                    alert.setContentText(response.toString());
                    Optional<ButtonType> result2 = alert.showAndWait();
                    if (result2.get() == ButtonType.OK) {
                        orderTable.getItems().clear();
                        orderTable.refresh();
                    }

                }
            }
            // Κλείσιμο της σύνδεσης
            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createPDF(String filename) throws RuntimeException {

        // create a pdf
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);

        try {

            // fonts nature to be used in the pdf document
            BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            bf.setSubset(true);
            Font bfBold12 = new Font(bf, 14, Font.BOLD, BaseColor.BLACK);
            Font bfBold = new Font(bf, 12);

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));

            document.open();

            //Add Title on top of the page
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            Paragraph intro = new Paragraph("Παραγγελία "+ dtf.format(orderDate.getValue())+"\n\n",bfBold12);
            intro.setAlignment(Element.ALIGN_CENTER);
            document.add(intro);

            // Add a table with six columns each with a specified width
            float[] columnWidths = {3.3f,2.0f,1.5f,2.8f};
            PdfPTable orderPdf = new PdfPTable(columnWidths);
            orderPdf.setWidthPercentage(90f);

            //add column titles to each
            insertCell(orderPdf, "Όνομα", Element.ALIGN_LEFT, 1, bfBold12);
            insertCell(orderPdf, "Ποσότητα", Element.ALIGN_LEFT, 1, bfBold12);
            insertCell(orderPdf, "Μονάδα", Element.ALIGN_LEFT, 1, bfBold12);
            insertCell(orderPdf, "Τιμή", Element.ALIGN_LEFT, 1, bfBold12);

            for (Item item : orderTable.getItems()) {
                insertCell(orderPdf, item.getName(), Element.ALIGN_LEFT, 1, bfBold);
                insertCell(orderPdf, item.getQuantity().toString(), Element.ALIGN_LEFT, 1, bfBold);
                insertCell(orderPdf, item.getUnit(), Element.ALIGN_LEFT, 1, bfBold);
                insertCell(orderPdf, item.getPrice().toString(), Element.ALIGN_LEFT, 1, bfBold);
            }

            document.add(orderPdf);

            Paragraph outro = new Paragraph("\n\n\nΣχόλιο παραγγελίας: \n"+ taComment.getText(),bfBold12);
            outro.setAlignment(Element.ALIGN_LEFT);
            outro.setSpacingBefore(20f);
            document.add(outro);

            document.close();

        } catch (DocumentException | IOException ex) {
            throw new RuntimeException(ex);
        }

    }
    // method to format table cell with data
    private void insertCell(PdfPTable table,String text,int align,int colspan,Font font){

        PdfPCell cell = new PdfPCell(new Phrase(text.trim(),font));
        cell.setHorizontalAlignment(align);
        cell.setColspan(colspan);
        cell.setMinimumHeight(20f);
        if (text.trim().equalsIgnoreCase("")) {
            cell.setMinimumHeight(10f);
        }
        table.addCell(cell);
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

    private void handle(KeyEvent event) {
        // Έλεγχος της θέσης του κέρσορα
        if (tfName.getText().length() > tfName.getPrefColumnCount()) {
            // Ορισμός της θέσης του κέρσορα στην αρχή
            tfName.positionCaret(0);
        }
    }

    public void mainMenuClick(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("orders.fxml"));
        Parent root = fxmlLoader.load();
        stackPane.getChildren().clear();
        stackPane.getChildren().add(root);
    }
}
