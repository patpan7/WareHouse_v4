package com.example.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ItemsStatistics2 implements Initializable {

    String server;
    @FXML
    TableView<Item> statisticsTable;
    @FXML
    TextField filterField;
    @FXML
    ComboBox<Category> categoryFiled;
    @FXML
    DatePicker dateFrom;
    @FXML
    DatePicker dateTo;

    ObservableList<Category> observableListCat;
    List<Category> categories;
    ObservableList<Item> observableListItem;
    FilteredList<Item> filteredData;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        server = AppSettings.getInstance().server;
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        dateFrom.setValue(firstDayOfMonth);

        // Σημερινή ημερομηνία
        dateTo.setValue(LocalDate.now());

        TableColumn<Item, Integer> codeColumn = new TableColumn<>("Κωδικός");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Όνομα");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, BigDecimal> quantityColumn = new TableColumn<>("Συνολική Ποσότητα");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Item, BigDecimal> totalSumColumn = new TableColumn<>("Συνολικό Κόστος");
        totalSumColumn.setCellValueFactory(new PropertyValueFactory<>("sum"));


        TableColumn<Item, String> unitColumn = new TableColumn<>("Μονάδα");
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Item, BigDecimal> priceColumn = new TableColumn<>("Μέση τιμή");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));


        statisticsTable.getColumns().addAll(codeColumn, nameColumn, quantityColumn,totalSumColumn, unitColumn, priceColumn);

        tableInit();

        categoryInit();
        categoryFiled.valueProperty().addListener((observable, oldValue, newValue) -> {
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

    private void tableInit() {
        List<Item> buys1 = fetchDataFromMySQL();

        observableListItem = FXCollections.observableArrayList(buys1);
        filteredData = new FilteredList<>(observableListItem, b -> true);

        BigDecimal sumColumn = BigDecimal.ZERO;
        for (Item item : buys1) {
            sumColumn = sumColumn.add(item.getSum());
        }

        Item totalRow = new Item("           Γενικό σύνολο:", sumColumn);
        observableListItem.add(totalRow);

        // Προσθήκη της συνολικής γραμμής και στο filteredData

        statisticsTable.setItems(filteredData);
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
        String API_URL = "http://" + server + "/warehouse/itemsStatistics2.php";
        List<Item> Items = new ArrayList<>();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateFrom1 = dateFrom.getValue().format(formatter);
            String dateTo1 = dateTo.getValue().format(formatter);

            URL url = new URL(API_URL + "?dateFrom=" + dateFrom1 + "&dateTo=" + dateTo1);
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
                        int item_code = itemNode.get("code").asInt();
                        String name = itemNode.get("item_name").asText();
                        BigDecimal total_quantity = new BigDecimal(itemNode.get("total_quantity").asText()).setScale(AppSettings.getInstance().quantityDecimals, RoundingMode.HALF_UP);
                        BigDecimal total_sum = new BigDecimal(itemNode.get("total_sum").asText()).setScale(AppSettings.getInstance().totalDecimals, RoundingMode.HALF_UP);
                        BigDecimal average_price = new BigDecimal(itemNode.get("average_price").asText()).setScale(AppSettings.getInstance().priceDecimals, RoundingMode.HALF_UP);
                        int category_code = itemNode.get("category_code").asInt();
                        String unit = itemNode.get("unit").asText();
                        Item item = new Item();
                        item.setCode(item_code);
                        item.setName(name);
                        item.setSum(total_sum);
                        item.setQuantity(total_quantity);
                        item.setPrice(average_price);
                        item.setCategory_code(category_code);
                        item.setUnit(unit);
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

    private void updateFilteredItems(Category selectedCategory) {
        List<Item> items;

        if (selectedCategory == null) {
            // Αν δεν υπάρχει επιλεγμένη κατηγορία, εμφάνιση όλων των ειδών
            items = new ArrayList<>(observableListItem);
            filteredData = new FilteredList<>(FXCollections.observableList(items), b -> true);
        } else {
            // Φιλτράρισμα των ειδών με βάση την επιλεγμένη κατηγορία
            items = statisticsTable.getItems().stream()
                    .filter(item -> item.getCategory_code() == selectedCategory.getCode())
                    .collect(Collectors.toList());
            filteredData = new FilteredList<>(FXCollections.observableList(items));
        }

        if (selectedCategory != null) {
            // Υπολογισμός συνολικού άθροισματος μόνο για τα ορατά στοιχεία
            BigDecimal filteredSumColumn = BigDecimal.ZERO;
            for (Item item : filteredData) {
                filteredSumColumn = filteredSumColumn.add(item.getSum());
            }

            // Δημιουργία της συνολικής γραμμής
            Item totalRow = new Item("           Γενικό σύνολο:", filteredSumColumn);

            // Προσθήκη της συνολικής γραμμής στο φιλτραρισμένο δεδομένο
            items.add(totalRow);
        }

        // Ενημέρωση του φιλτραρισμένου δεδομένου με την νέα λίστα στοιχείων
        filteredData = new FilteredList<>(FXCollections.observableList(items), filteredData.getPredicate());

        // Ορισμός του φιλτραρισμένου δεδομένου ως πηγή δεδομένων για τον πίνακα
        statisticsTable.setItems(filteredData);
    }



    public void saveAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("");
        alert.setContentText("Αποθήκευση αναφοράς?");
        Optional<ButtonType> result2 = alert.showAndWait();

        if (result2.get() == ButtonType.OK) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Επιλογή αρχείου για αποθήκευση");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            fileChooser.setInitialFileName("Συγκεντρωτικές Αγορές Ειδών από  "+ dtf.format(dateFrom.getValue())+" εώς "+ dtf.format(dateTo.getValue()));
            // Επιλέξτε τον τύπο αρχείου που θέλετε να αποθηκεύσετε (π.χ., PDF)
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf");
            fileChooser.getExtensionFilters().add(extFilter);

            // Πάρτε το επιλεγμένο αρχείο
            File file = fileChooser.showSaveDialog(null);
            Alert alert1 = new Alert(Alert.AlertType.INFORMATION);
            alert1.setTitle("");
            if (file != null) {
                createPDF(file.getAbsolutePath());
                alert1.setContentText("PDF created successfully: " + file.getAbsolutePath());
                System.out.println("PDF created successfully: " + file.getAbsolutePath());
            }
            else {
                alert1.setContentText("PDF creation cancelled: " + file.getAbsolutePath());
                System.out.println("PDF creation cancelled");
            }
            alert1.show();
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
            Paragraph intro = new Paragraph("Συγκεντρωτικές Αγορές Ειδών από:  "+ dtf.format(dateFrom.getValue())+" εώς: "+ dtf.format(dateTo.getValue())+ "\n\n",bfBold12);
            intro.setAlignment(Element.ALIGN_CENTER);
            if (categoryFiled.getSelectionModel().getSelectedItem() != null)
                intro.add(new Paragraph("Κατηγορία:  " + categoryFiled.getSelectionModel().getSelectedItem()+"\n\n", bfBold12));
            document.add(intro);

            // Add a table with six columns each with a specified width
            float[] columnWidths = {3.3f,2.0f,2.8f,1.5f,2.8f};
            PdfPTable orderPdf = new PdfPTable(columnWidths);
            orderPdf.setWidthPercentage(100);

            //add column titles to each
            insertCell(orderPdf, "Όνομα", Element.ALIGN_LEFT, 1, bfBold12);
            insertCell(orderPdf, "Συνολική Ποσότητα", Element.ALIGN_LEFT, 1, bfBold12);
            insertCell(orderPdf, "Συνολικό Κόστος", Element.ALIGN_LEFT, 1, bfBold12);
            insertCell(orderPdf, "Μονάδα", Element.ALIGN_LEFT, 1, bfBold12);
            insertCell(orderPdf, "Μέση Τιμή", Element.ALIGN_LEFT, 1, bfBold12);

            List<Item> items = statisticsTable.getItems();
            for (Item item : items) {
                if (item.getName().equals("           Γενικό σύνολο:")) {
                    insertCell(orderPdf, item.getName(), Element.ALIGN_LEFT, 1, bfBold12);
                    insertCell(orderPdf, "", Element.ALIGN_LEFT, 1, bfBold);
                    insertCell(orderPdf, item.getSum().toString(), Element.ALIGN_LEFT, 1, bfBold12);
                    insertCell(orderPdf, "", Element.ALIGN_LEFT, 1, bfBold);
                    insertCell(orderPdf, "", Element.ALIGN_LEFT, 1, bfBold);
                } else {
                    insertCell(orderPdf, item.getName(), Element.ALIGN_LEFT, 1, bfBold);
                    insertCell(orderPdf, item.getQuantity().toString(), Element.ALIGN_LEFT, 1, bfBold);
                    insertCell(orderPdf, item.getSum().toString(), Element.ALIGN_LEFT, 1, bfBold);
                    insertCell(orderPdf, item.getUnit(), Element.ALIGN_LEFT, 1, bfBold);
                    insertCell(orderPdf, item.getPrice().toString(), Element.ALIGN_LEFT, 1, bfBold);
                }
            }
            document.add(orderPdf);
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
}
