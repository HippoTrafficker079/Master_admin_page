package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javafx.application.Platform;
import application.dao.StudentDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class StudentAddController {
    @FXML private TextField txtRollNo, txtName, txtNationality;
    
    // Structured NRC Fields
    @FXML private ComboBox<String> comboNrcState;
    @FXML private ComboBox<String> comboNrcTownship;
    @FXML private ComboBox<String> comboNrcType;
    @FXML private TextField txtNrcNumber;

    @FXML private DatePicker dpDob;
    @FXML private ComboBox<String> comboGender;
    @FXML private ComboBox<String> comboMajor;
    @FXML private ComboBox<String> comboYear;

    @FXML private TextField txtPhone, txtEmail, txtCurrentAddress, txtPermanentAddress;
    @FXML private TextField txtFatherName, txtFatherOcc, txtFatherPhone, txtFatherEmail;
    @FXML private TextField txtMotherName, txtMotherOcc, txtMotherPhone, txtMotherEmail;
    @FXML private TextField txtPrevSchool, txtYearCompleted, txtAdditionalInfo;
    @FXML private ImageView passportImageView;

    @FXML private TextField txtExcelPath;
    @FXML private VBox dropZone;

    private File selectedExcelFile = null;
    private String selectedPhotoPath = null;
    private final String DEFAULT_IMAGE_PATH = "/freddy.jpg";
    private boolean isUpdating = false;

    private FilteredList<String> filteredTownships;
    private ObservableList<String> currentTownshipOptions;

    private final String[] VALID_MAJORS = { "CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex", "FE" };
    private final String[] VALID_YEARS = { "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year" };

    @FXML
    public void initialize() {
        if (comboYear != null && comboYear.getValue() == null) comboYear.setValue("First Year");
        if (comboGender != null && comboGender.getValue() == null) comboGender.setValue("Male");
        if (comboMajor != null && comboMajor.getValue() == null) comboMajor.setValue("CEIT");

        initializeNrcComponent();
        setupBidirectionalSync();
    }

    private void initializeNrcComponent() {
        if (comboNrcState != null) {
            comboNrcState.setItems(NrcData.STATES);
        }
        if (comboNrcType != null) {
            comboNrcType.setItems(NrcData.TYPES);
            comboNrcType.setValue("(N)");
        }

        currentTownshipOptions = NrcData.getAllTownships();
        filteredTownships = new FilteredList<>(currentTownshipOptions, s -> true);
        if (comboNrcTownship != null) {
            comboNrcTownship.setItems(filteredTownships);
            comboNrcTownship.setVisibleRowCount(10);
        }

        // 1. State change listener
        if (comboNrcState != null) {
            comboNrcState.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdating) return;
                
                if (newVal != null && !newVal.equals(oldVal)) {
                    ObservableList<String> townships = NrcData.getTownshipsForState(newVal);
                    
                    isUpdating = true;
                    try {
                        filteredTownships.setPredicate(s -> true);
                        currentTownshipOptions.setAll(townships);
                        comboNrcTownship.getSelectionModel().clearSelection();
                        comboNrcTownship.setValue(null);
                        if (comboNrcTownship.getEditor() != null) {
                            comboNrcTownship.getEditor().clear();
                        }
                    } finally {
                        isUpdating = false;
                    }
                }
            });
        }

        // 2. Universal Searchable Autocomplete Filter Listener
        if (comboNrcTownship != null && comboNrcTownship.getEditor() != null) {
            comboNrcTownship.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (isUpdating) return;

                if (comboNrcTownship.getValue() != null && comboNrcTownship.getValue().equals(newValue)) {
                    return;
                }

                Platform.runLater(() -> {
                    if (newValue == null || newValue.trim().isEmpty()) {
                        filteredTownships.setPredicate(s -> true);
                    } else {
                        String query = newValue.toLowerCase().trim();
                        filteredTownships.setPredicate(item -> item != null && item.toLowerCase().contains(query));
                        if (!comboNrcTownship.isShowing() && !filteredTownships.isEmpty()) {
                            comboNrcTownship.show();
                        }
                    }
                });
            });

            // 3. Township Selection Listener: Automatically updates State code & NRC number!
            comboNrcTownship.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdating) return;
                
                if (newVal != null && !newVal.trim().isEmpty()) {
                    if (comboNrcTownship.getEditor() != null && !comboNrcTownship.getEditor().getText().equals(newVal)) {
                        isUpdating = true;
                        comboNrcTownship.getEditor().setText(newVal);
                        isUpdating = false;
                    }

                    String matchedState = NrcData.getStateForTownship(newVal);
                    if (matchedState != null && comboNrcState != null) {
                        if (!matchedState.equals(comboNrcState.getValue())) {
                            isUpdating = true;
                            try {
                                ObservableList<String> townships = NrcData.getTownshipsForState(matchedState);
                                currentTownshipOptions.setAll(townships);
                                comboNrcState.setValue(matchedState);
                                comboNrcTownship.setValue(newVal);
                            } finally {
                                isUpdating = false;
                            }
                        }
                    }

                    String associatedCode = NrcData.getCodeForTownship(newVal);
                    if (associatedCode != null && txtNrcNumber != null && (txtNrcNumber.getText() == null || txtNrcNumber.getText().trim().isEmpty())) {
                        isUpdating = true;
                        try {
                            txtNrcNumber.setText(associatedCode);
                        } finally {
                            isUpdating = false;
                        }
                    }
                }
            });
        }

        // Restrict NRC Number to strictly 6 digits max
        if (txtNrcNumber != null) {
            txtNrcNumber.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.matches("\\d*")) {
                    txtNrcNumber.setText(newVal.replaceAll("[^\\d]", ""));
                }
                if (txtNrcNumber.getText().length() > 6) {
                    txtNrcNumber.setText(txtNrcNumber.getText().substring(0, 6));
                }
            });
        }
    }

    private String getCombinedNrc() {
        String state = comboNrcState != null && comboNrcState.getValue() != null ? comboNrcState.getValue().trim() : "";
        String township = "";
        if (comboNrcTownship != null) {
            township = comboNrcTownship.getValue() != null ? comboNrcTownship.getValue().trim() : "";
            if (township.isEmpty() && comboNrcTownship.getEditor() != null) {
                township = comboNrcTownship.getEditor().getText().trim();
            }
        }
        String type = comboNrcType != null && comboNrcType.getValue() != null ? comboNrcType.getValue().trim() : "(N)";
        String number = txtNrcNumber != null ? txtNrcNumber.getText().trim() : "";

        if (state.isEmpty() || township.isEmpty() || number.isEmpty()) {
            return "";
        }
        return state + "/" + township + type + number;
    }

    private void setParsedNrc(String fullNrc) {
        if (fullNrc == null || fullNrc.trim().isEmpty()) return;
        isUpdating = true;
        try {
            String[] parts = fullNrc.split("/");
            if (parts.length >= 2) {
                String state = parts[0].trim();
                
                if (comboNrcState != null) {
                    if (!comboNrcState.getItems().contains(state)) {
                        comboNrcState.getItems().add(state);
                    }
                    comboNrcState.setValue(state);
                }

                String remainder = parts[1];
                int typeIndex = remainder.indexOf("(");
                if (typeIndex != -1) {
                    String township = remainder.substring(0, typeIndex).trim();
                    
                    if (comboNrcTownship != null) {
                        if (currentTownshipOptions != null && !currentTownshipOptions.contains(township)) {
                            currentTownshipOptions.add(township);
                        }
                        comboNrcTownship.setValue(township);
                        if (comboNrcTownship.getEditor() != null) {
                            comboNrcTownship.getEditor().setText(township);
                        }
                    }

                    int closingIndex = remainder.indexOf(")");
                    if (closingIndex != -1) {
                        String type = remainder.substring(typeIndex, closingIndex + 1).trim();
                        if (comboNrcType != null) {
                            if (!comboNrcType.getItems().contains(type)) {
                                comboNrcType.getItems().add(type);
                            }
                            comboNrcType.setValue(type);
                        }

                        String number = remainder.substring(closingIndex + 1).trim();
                        if (txtNrcNumber != null) {
                            txtNrcNumber.setText(number);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (txtNrcNumber != null) txtNrcNumber.setText(fullNrc);
        } finally {
            isUpdating = false;
        }
    }

    private void setupBidirectionalSync() {
        if (txtRollNo != null) {
            txtRollNo.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && !isUpdating) syncFromRollNo();
            });
            txtRollNo.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null && newVal.matches("^[a-zA-Z]+-[a-zA-Z0-9]+-\\d+$")) {
                    syncFromRollNo();
                }
            });
        }
        if (comboMajor != null) {
            comboMajor.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null) syncToRollNo();
            });
        }
        if (comboYear != null) {
            comboYear.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null) syncToRollNo();
            });
        }
    }

    private void syncFromRollNo() {
        if (txtRollNo == null || txtRollNo.getText() == null) return;
        String raw = txtRollNo.getText().trim();
        String[] parts = raw.split("-");
        if (parts.length >= 3 && parts[2].matches("\\d+")) {
            String fullYear = getYearFromRoman(parts[0].trim());
            String validMajor = findValidMajor(parts[1].trim());

            if (fullYear != null && validMajor != null) {
                isUpdating = true;
                try {
                    if (comboMajor != null) comboMajor.setValue(validMajor);
                    if (comboYear != null) comboYear.setValue(fullYear);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }

    private void syncToRollNo() {
        if (txtRollNo == null || comboYear == null || comboMajor == null) return;
        String rawYear = comboYear.getValue() != null ? comboYear.getValue().trim() : "";
        String rawMajor = comboMajor.getValue() != null ? comboMajor.getValue().trim() : "";

        String validYear = findValidYear(rawYear);
        String validMajor = findValidMajor(rawMajor);

        if (validYear != null && validMajor != null) {
            String roman = getRomanYear(validYear);
            String currentRoll = txtRollNo.getText() != null ? txtRollNo.getText().trim() : "";
            String[] parts = currentRoll.split("-");
            String rollNum = (parts.length >= 3 && parts[parts.length - 1].matches("\\d+")) ? parts[parts.length - 1] : "1";

            isUpdating = true;
            try {
                txtRollNo.setText(roman + "-" + validMajor + "-" + rollNum);
                comboYear.setValue(validYear);
                comboMajor.setValue(validMajor);
            } finally {
                isUpdating = false;
            }
        }
    }

    @FXML
    public void handleBrowseExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel / CSV File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Spreadsheet & CSV Files (*.xlsx, *.csv)", "*.xlsx", "*.csv"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));

        Stage stage = (Stage) txtRollNo.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedExcelFile = file;
            if (txtExcelPath != null) {
                txtExcelPath.setText(file.getAbsolutePath());
            }
        }
    }

    @FXML
    public void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            for (File file : event.getDragboard().getFiles()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".xlsx") || name.endsWith(".csv") || name.endsWith(".txt")) {
                    event.acceptTransferModes(TransferMode.COPY);
                    dropZone.setStyle("-fx-border-color: #2563eb; -fx-border-width: 2.0; -fx-border-style: dashed; -fx-border-radius: 6; -fx-background-color: #eff6ff; -fx-background-radius: 6; -fx-cursor: hand;");
                    break;
                }
            }
        }
        event.consume();
    }

    @FXML
    public void handleDragExited(DragEvent event) {
        dropZone.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1.5; -fx-border-style: dashed; -fx-border-radius: 6; -fx-background-color: #f8fafc; -fx-background-radius: 6; -fx-cursor: hand;");
        event.consume();
    }

    @FXML
    public void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            for (File file : db.getFiles()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".xlsx") || name.endsWith(".csv") || name.endsWith(".txt")) {
                    selectedExcelFile = file;
                    if (txtExcelPath != null) {
                        txtExcelPath.setText(file.getAbsolutePath());
                    }
                    success = true;
                    Platform.runLater(this::handleImportExcel);
                    break;
                }
            }
        }
        event.setDropCompleted(success);
        dropZone.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1.5; -fx-border-style: dashed; -fx-border-radius: 6; -fx-background-color: #f8fafc; -fx-cursor: hand;");
        event.consume();
    }

    @FXML
    public void handleImportExcel() {
        if (selectedExcelFile == null || !selectedExcelFile.exists()) {
            showAlert("No File Selected", "Please browse or drag and drop a valid Excel or CSV file first.", Alert.AlertType.WARNING);
            return;
        }

        String fileName = selectedExcelFile.getName().toLowerCase();
        try {
            List<Student> importedStudents;
            if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                importedStudents = parseCsvStudents(selectedExcelFile);
            } else if (fileName.endsWith(".xlsx")) {
                importedStudents = parseXlsxStudents(selectedExcelFile);
            } else {
                showAlert("Unsupported File", "Please select a supported file (.xlsx or .csv).", Alert.AlertType.ERROR);
                return;
            }

            if (importedStudents == null || importedStudents.isEmpty()) {
                showAlert("Import Notice", "No student records were found in the file.", Alert.AlertType.WARNING);
                return;
            }

            promptBatchConfirmation(importedStudents);

        } catch (Exception e) {
            showAlert("Import Error", "Failed to parse spreadsheet: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void promptBatchConfirmation(List<Student> students) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Data Successfully Imported");
        alert.setHeaderText("Data are successfully imported from " + selectedExcelFile.getName());
        alert.setContentText(String.format("Found %d student record(s).\n\nChoose an action below:", students.size()));

        ButtonType btnAddAll = new ButtonType("Add All Students", ButtonBar.ButtonData.YES);
        ButtonType btnPickOne = new ButtonType("Select a Student to Fill Form", ButtonBar.ButtonData.OTHER);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnAddAll, btnPickOne, btnCancel);
        alert.getDialogPane().setMinWidth(520);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnAddAll) {
                StudentDAO studentDAO = new StudentDAO();
                int importedRow = studentDAO.insertBatchStudents(students);
                if (importedRow > 0) {
                    if (StudentController.masterData != null) {
                        List<Student> freshList = studentDAO.getAllStudent();
                        StudentController.masterData.setAll(freshList);
                    }
                    showAlert("Success", String.format("Successfully registered %d student(s) into the system.", students.size()), Alert.AlertType.INFORMATION);
                    handleReturn();
                }
            } else if (result.get() == btnPickOne) {
                if (students != null && !students.isEmpty()) {
                    openSingleStudentPickerDialog(students);
                } else {
                    showAlert("Empty List", "No student records were found in the imported file.", Alert.AlertType.WARNING);
                }
            } else {
                handleReturn();
            }
        }
    }

    private void openSingleStudentPickerDialog(List<Student> students) {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle("Choose Student to Populate Form");
        dialog.setHeaderText("Select a student row to load into the form blanks below:");

        TableView<Student> previewTable = new TableView<>();
        previewTable.setPrefHeight(300);
        previewTable.setPrefWidth(640);

        TableColumn<Student, String> colRoll = new TableColumn<>("Roll No");
        colRoll.setCellValueFactory(c -> c.getValue().rollNoProperty());
        colRoll.setPrefWidth(110);

        TableColumn<Student, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colName.setPrefWidth(160);

        TableColumn<Student, String> colMajor = new TableColumn<>("Major");
        colMajor.setCellValueFactory(c -> c.getValue().majorProperty());
        colMajor.setPrefWidth(80);

        TableColumn<Student, String> colYear = new TableColumn<>("Year");
        colYear.setCellValueFactory(c -> c.getValue().yearProperty());
        colYear.setPrefWidth(120);

        TableColumn<Student, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(c -> c.getValue().phoneNoProperty());
        colPhone.setPrefWidth(140);

        previewTable.getColumns().addAll(colRoll, colName, colMajor, colYear, colPhone);
        previewTable.setItems(FXCollections.observableArrayList(students));
        if (!students.isEmpty()) {
            previewTable.getSelectionModel().selectFirst();
        }

        ButtonType btnSelect = new ButtonType("Populate Form", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSelect, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10, previewTable));

        dialog.setResultConverter(btn -> {
            if (btn == btnSelect) {
                return previewTable.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        previewTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && previewTable.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(previewTable.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        Optional<Student> res = dialog.showAndWait();
        if (res.isPresent() && res.get() != null) {
            populateFormWithStudent(res.get());
        }
    }

    private void populateFormWithStudent(Student s) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                .appendOptional(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                .appendOptional(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                .toFormatter();
        isUpdating = true;
        try {
            if (txtRollNo != null) txtRollNo.setText(s.getRollNo());

            if (dpDob != null && s.getDob() != null && !s.getDob().trim().isEmpty()) {
                try {
                    dpDob.setValue(LocalDate.parse(s.getDob().trim(), formatter));
                } catch (Exception ex) {
                    dpDob.setValue(null);
                }
            }

            if (txtName != null) txtName.setText(s.getName());
            if (comboGender != null) comboGender.setValue(s.getGender().equalsIgnoreCase("Female") ? "Female" : "Male");

            String normMajor = findValidMajor(s.getMajor());
            if (comboMajor != null && normMajor != null) comboMajor.setValue(normMajor);

            if (txtNationality != null) txtNationality.setText(s.getNationality());
            if (comboYear != null) comboYear.setValue(findValidYear(s.getYear()));
            
            setParsedNrc(s.getNrc());

            if (txtPhone != null) txtPhone.setText(s.getPhoneNo());
            if (txtEmail != null) txtEmail.setText(s.getEmail());
            if (txtCurrentAddress != null) txtCurrentAddress.setText(s.getCurrentAddress());
            if (txtPermanentAddress != null) txtPermanentAddress.setText(s.getPermanentAddress());

            if (txtFatherName != null) txtFatherName.setText(s.getFatherName());
            if (txtFatherOcc != null) txtFatherOcc.setText(s.getFatherOccupation());
            if (txtFatherPhone != null) txtFatherPhone.setText(s.getFatherPhone());
            if (txtFatherEmail != null) txtFatherEmail.setText(s.getFatherEmail());

            if (txtMotherName != null) txtMotherName.setText(s.getMotherName());
            if (txtMotherOcc != null) txtMotherOcc.setText(s.getMotherOccupation());
            if (txtMotherPhone != null) txtMotherPhone.setText(s.getMotherPhone());
            if (txtMotherEmail != null) txtMotherEmail.setText(s.getMotherEmail());

            if (txtPrevSchool != null) txtPrevSchool.setText(s.getPrevSchool());
            if (txtYearCompleted != null) txtYearCompleted.setText(s.getYearCompleted());
            if (txtAdditionalInfo != null) txtAdditionalInfo.setText(s.getAdditionalInfo());
        } finally {
            isUpdating = false;
        }

        showAlert("Student Loaded", "Details for " + s.getName() + " (" + s.getRollNo() + ") have been populated into the form.", Alert.AlertType.INFORMATION);
    }

    private List<Student> parseCsvStudents(File file) throws IOException {
        List<Student> students = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) return students;

            String delimiter = headerLine.contains(",") ? "," : (headerLine.contains("\t") ? "\t" : ";");
            String[] headers = headerLine.split(delimiter);

            String line;
            int counter = 1;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(delimiter, -1);
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    map.put(headers[i].replace("\"", "").trim().toLowerCase(), values[i].replace("\"", "").trim());
                }
                Student student = buildStudentFromMap(map, values, counter++);
                if (student != null) students.add(student);
            }
        }
        return students;
    }

    private List<Student> parseXlsxStudents(File file) {
        List<Student> students = new ArrayList<>();
        try (ZipFile zip = new ZipFile(file)) {
            List<String> sharedStrings = new ArrayList<>();
            ZipEntry sstEntry = zip.getEntry("xl/sharedStrings.xml");
            if (sstEntry != null) {
                String sstXml = new String(zip.getInputStream(sstEntry).readAllBytes(), StandardCharsets.UTF_8);
                Matcher siMatcher = Pattern.compile("<si>(.*?)</si>", Pattern.DOTALL).matcher(sstXml);
                while (siMatcher.find()) {
                    String siContent = siMatcher.group(1);
                    StringBuilder sb = new StringBuilder();
                    Matcher tMatcher = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL).matcher(siContent);
                    while (tMatcher.find()) {
                        sb.append(tMatcher.group(1));
                    }
                    sharedStrings.add(unescapeXml(sb.toString()));
                }
            }

            ZipEntry sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml");
            if (sheetEntry == null) return students;

            String sheetXml = new String(zip.getInputStream(sheetEntry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher rowMatcher = Pattern.compile("<row[^>]*>(.*?)</row>", Pattern.DOTALL).matcher(sheetXml);

            List<String> headers = null;
            int counter = 1;
            while (rowMatcher.find()) {
                List<String> rowCells = extractCellsFromRow(rowMatcher.group(1), sharedStrings);
                if (headers == null) {
                    headers = rowCells;
                } else {
                    if (rowCells.isEmpty() || String.join("", rowCells).trim().isEmpty()) continue;
                    Map<String, String> map = new HashMap<>();
                    String[] rawValues = new String[headers.size()];
                    for (int i = 0; i < headers.size(); i++) {
                        String h = headers.get(i).trim().toLowerCase();
                        String v = i < rowCells.size() ? rowCells.get(i).trim() : "";
                        map.put(h, v);
                        rawValues[i] = v;
                    }
                    Student student = buildStudentFromMap(map, rawValues, counter++);
                    if (student != null) students.add(student);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return students;
    }

    private Student buildStudentFromMap(Map<String, String> map, String[] rawOrder, int seqNo) {
        String rollNo = getMapOrIndex(map, rawOrder, 0, "roll no", "rollno", "roll_no", "id", "studentid");
        String name = getMapOrIndex(map, rawOrder, 1, "name", "full name", "fullname", "student name");
        String major = findValidMajor(getMapOrIndex(map, rawOrder, 2, "major", "programme", "department"));
        if (major == null) major = "CEIT";

        String year = findValidYear(getMapOrIndex(map, rawOrder, 3, "year", "year / level", "level"));
        if (year == null) year = "First Year";

        String phone = getMapOrIndex(map, rawOrder, 5, "phone", "phone number", "phone no", "phoneno");
        if (phone.isEmpty()) phone = "09-000000000";

        String dob = getMapOrIndex(map, rawOrder, 6, "dob", "date of birth", "birthdate");
        String gender = getMapOrIndex(map, rawOrder, 7, "gender", "sex");
        if (!gender.equalsIgnoreCase("Female") && !gender.equalsIgnoreCase("Male")) gender = "Male";

        String nationality = getMapOrIndex(map, rawOrder, 8, "nationality", "country");
        if (nationality.isEmpty()) nationality = "Myanmar";

        String nrc = getMapOrIndex(map, rawOrder, 9, "nrc", "nrc / id no.", "id no", "nrc number");
        String email = getMapOrIndex(map, rawOrder, 10, "email", "email address");
        String currAddr = getMapOrIndex(map, rawOrder, 11, "current address", "address");
        String permAddr = getMapOrIndex(map, rawOrder, 12, "permanent address");

        String fName = getMapOrIndex(map, rawOrder, 13, "father name", "father's name", "father");
        String fOcc = getMapOrIndex(map, rawOrder, 14, "father occupation", "father occ");
        String fPhone = getMapOrIndex(map, rawOrder, 15, "father phone", "father's phone");
        String fEmail = getMapOrIndex(map, rawOrder, 16, "father email", "father's email");

        String mName = getMapOrIndex(map, rawOrder, 17, "mother name", "mother's name", "mother");
        String mOcc = getMapOrIndex(map, rawOrder, 18, "mother occupation", "mother occ");
        String mPhone = getMapOrIndex(map, rawOrder, 19, "mother phone", "mother's phone");
        String mEmail = getMapOrIndex(map, rawOrder, 20, "mother email", "mother's email");

        String prevSchool = getMapOrIndex(map, rawOrder, 21, "previous school", "school");
        String yearComp = getMapOrIndex(map, rawOrder, 22, "year completed", "matriculation year");
        String addInfo = getMapOrIndex(map, rawOrder, 23, "additional info", "remarks");

        if (rollNo.isEmpty() || name.isEmpty()) return null;

        return new Student(seqNo, rollNo, name, major, year, "Pending", phone,
                dob, gender, nationality, nrc, email, currAddr, permAddr,
                fName, fOcc, fPhone, fEmail, mName, mOcc, mPhone, mEmail,
                prevSchool, yearComp, addInfo, DEFAULT_IMAGE_PATH);
    }

    private List<String> extractCellsFromRow(String rowContent, List<String> sharedStrings) {
        List<String> rowCells = new ArrayList<>();
        Matcher cMatcher = Pattern.compile("<c\\b([^>]*)>(.*?)</c>", Pattern.DOTALL).matcher(rowContent);
        int nextExpectedCol = 0;

        while (cMatcher.find()) {
            String attrs = cMatcher.group(1);
            String body = cMatcher.group(2);

            Matcher rMatcher = Pattern.compile("r=\"([A-Z]+)[0-9]+\"").matcher(attrs);
            int colIndex = nextExpectedCol;
            if (rMatcher.find()) colIndex = getColIndex(rMatcher.group(1));

            while (nextExpectedCol < colIndex) {
                rowCells.add("");
                nextExpectedCol++;
            }

            boolean isShared = attrs.contains("t=\"s\"");
            String val = "";

            Matcher vMatcher = Pattern.compile("<v>(.*?)</v>").matcher(body);
            if (vMatcher.find()) {
                val = vMatcher.group(1).trim();
                if (isShared) {
                    try {
                        int sIndex = Integer.parseInt(val);
                        if (sIndex >= 0 && sIndex < sharedStrings.size()) {
                            val = sharedStrings.get(sIndex);
                        }
                    } catch (Exception ex) {}
                }
            } else {
                Matcher isMatcher = Pattern.compile("<is>.*?<t[^>]*>(.*?)</t>.*?</is>", Pattern.DOTALL).matcher(body);
                if (isMatcher.find()) val = isMatcher.group(1).trim();
            }

            rowCells.add(unescapeXml(val));
            nextExpectedCol = colIndex + 1;
        }
        return rowCells;
    }

    private int getColIndex(String colStr) {
        int col = 0;
        for (int i = 0; i < colStr.length(); i++) {
            col = col * 26 + (colStr.charAt(i) - 'A' + 1);
        }
        return Math.max(0, col - 1);
    }

    private String unescapeXml(String text) {
        if (text == null) return "";
        return text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'");
    }

    private String getMapOrIndex(Map<String, String> map, String[] rawOrder, int index, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k) && !map.get(k).trim().isEmpty()) {
                return map.get(k).trim();
            }
        }
        if (index < rawOrder.length && rawOrder[index] != null && !rawOrder[index].trim().isEmpty()) {
            return rawOrder[index].trim();
        }
        return "";
    }

    private String findValidMajor(String input) {
        if (input == null) return null;
        for (String m : VALID_MAJORS) {
            if (m.equalsIgnoreCase(input.trim())) return m;
        }
        return null;
    }

    private String findValidYear(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        if (s.contains("first") || s.equals("1") || s.equals("i")) return "First Year";
        if (s.contains("second") || s.equals("2") || s.equals("ii")) return "Second Year";
        if (s.contains("third") || s.equals("3") || s.equals("iii")) return "Third Year";
        if (s.contains("fourth") || s.equals("4") || s.equals("iv")) return "Fourth Year";
        if (s.contains("fifth") || s.equals("5") || s.equals("v")) return "Fifth Year";
        for (String y : VALID_YEARS) {
            if (y.equalsIgnoreCase(input.trim())) return y;
        }
        return null;
    }

    private String getYearFromRoman(String roman) {
        if (roman == null) return null;
        String r = roman.trim().toUpperCase();
        switch (r) {
            case "I": return "First Year";
            case "II": return "Second Year";
            case "III": return "Third Year";
            case "IV": return "Fourth Year";
            case "V": return "Fifth Year";
            default: return null;
        }
    }

    private String getRomanYear(String yearStr) {
        if (yearStr == null) return "";
        String y = yearStr.trim().toLowerCase();
        if (y.contains("first") || y.equals("i") || y.equals("1")) return "I";
        if (y.contains("second") || y.equals("ii") || y.equals("2")) return "II";
        if (y.contains("third") || y.equals("iii") || y.equals("3")) return "III";
        if (y.contains("fourth") || y.equals("iv") || y.equals("4")) return "IV";
        if (y.contains("fifth") || y.equals("v") || y.equals("5")) return "V";
        return "";
    }

    @FXML
    public void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Passport Size Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.bmp"));

        if (passportImageView != null && passportImageView.getScene() != null) {
            Stage stage = (Stage) passportImageView.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                try {
                    selectedPhotoPath = file.toURI().toString();
                    Image image = new Image(selectedPhotoPath, 86, 110, false, true);
                    passportImageView.setImage(image);
                } catch (Exception e) {
                    showAlert("Image Error", "Could not load image: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        }
    }

    @FXML
    public void handleAddStudent() {
        String rawRollNo = txtRollNo != null ? txtRollNo.getText().trim() : "";
        String rawYear = comboYear != null && comboYear.getValue() != null ? comboYear.getValue().trim() : "";
        String rawMajor = comboMajor != null && comboMajor.getValue() != null ? comboMajor.getValue().trim() : "";
        String phoneInput = txtPhone != null ? txtPhone.getText().trim() : "";
        String emailInput = txtEmail != null ? txtEmail.getText().trim() : "";
        String dobInput = (dpDob != null && dpDob.getValue() != null) ? dpDob.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
        String nameInput = txtName != null ? txtName.getText().trim() : "";
        String genderInput = comboGender != null && comboGender.getValue() != null ? comboGender.getValue().trim() : "Male";
        String yearCompInput = txtYearCompleted != null ? txtYearCompleted.getText().trim() : "";

        String fPhoneInput = txtFatherPhone != null ? txtFatherPhone.getText().trim() : "";
        String fEmailInput = txtFatherEmail != null ? txtFatherEmail.getText().trim() : "";
        String mPhoneInput = txtMotherPhone != null ? txtMotherPhone.getText().trim() : "";
        String mEmailInput = txtMotherEmail != null ? txtMotherEmail.getText().trim() : "";

        if (rawRollNo.isEmpty() || nameInput.isEmpty() || rawMajor.isEmpty() ||
                rawYear.isEmpty() || phoneInput.isEmpty() || emailInput.isEmpty() || dobInput.isEmpty()) {
            showAlert("Missing Information", "Please fill in all required student details before saving.", Alert.AlertType.ERROR);
            return;
        }

        if (!rawRollNo.matches("^[a-zA-Z]+-[a-zA-Z0-9]+-\\d+$")) {
            showAlert("Invalid Roll No Format", "Roll No must be in the format [Year]-[Major]-[Number] (e.g., II-CEIT-2).", Alert.AlertType.ERROR);
            return;
        }

        String validatedMajor = findValidMajor(rawMajor);
        if (validatedMajor == null) {
            showAlert("Invalid Major", "Selected Major '" + rawMajor + "' does not match any valid Major.", Alert.AlertType.ERROR);
            return;
        }

        String validatedYear = findValidYear(rawYear);
        if (validatedYear == null) {
            showAlert("Invalid Year", "Selected Year '" + rawYear + "' is invalid.", Alert.AlertType.ERROR);
            return;
        }

        String standardizedRollNo = getRomanYear(validatedYear) + "-" + validatedMajor + "-" + rawRollNo.split("-")[2].trim();
        txtRollNo.setText(standardizedRollNo);

        String nrcInput = getCombinedNrc();
        if (nrcInput.isEmpty()) {
            showAlert("Incomplete NRC", "Please complete all structured NRC fields (State, Township, Type, Number).", Alert.AlertType.ERROR);
            return;
        }

        if (!phoneInput.matches("^09-\\d+$")) {
            showAlert("Invalid Phone Number", "Student phone number must be in the format 09-xxxxxxxx.", Alert.AlertType.ERROR);
            return;
        }
        if (!emailInput.contains("@")) {
            showAlert("Invalid Email Address", "Student email address must contain '@'.", Alert.AlertType.ERROR);
            return;
        }

        int newNo = StudentController.masterData != null ? StudentController.masterData.size() + 1 : 1;
        String nationality = txtNationality != null && !txtNationality.getText().trim().isEmpty() ? txtNationality.getText().trim() : "Myanmar";
        String currAddr = txtCurrentAddress != null ? txtCurrentAddress.getText().trim() : "";
        String permAddr = txtPermanentAddress != null ? txtPermanentAddress.getText().trim() : "";
        String fName = txtFatherName != null ? txtFatherName.getText().trim() : "";
        String fOcc = txtFatherOcc != null ? txtFatherOcc.getText().trim() : "";
        String mName = txtMotherName != null ? txtMotherName.getText().trim() : "";
        String mOcc = txtMotherOcc != null ? txtMotherOcc.getText().trim() : "";
        String prevSchool = txtPrevSchool != null ? txtPrevSchool.getText().trim() : "";
        String addInfo = txtAdditionalInfo != null ? txtAdditionalInfo.getText().trim() : "";
        String finalPhotoPath = (selectedPhotoPath != null) ? selectedPhotoPath : DEFAULT_IMAGE_PATH;

        Student newStudent = new Student(newNo, standardizedRollNo, nameInput, validatedMajor, validatedYear,
                "Pending", phoneInput, dobInput, genderInput, nationality, nrcInput, emailInput, currAddr, permAddr,
                fName, fOcc, fPhoneInput, fEmailInput, mName, mOcc, mPhoneInput, mEmailInput,
                prevSchool, yearCompInput, addInfo, finalPhotoPath);

        StudentDAO studentDAO = new StudentDAO();
        int insertResult = studentDAO.insertStudent(newStudent);
        if (insertResult <= 0) {
            showAlert("Database Error", "Insertion Failed. Transaction has rolled back.", AlertType.ERROR);
            return;
        }

        if (StudentController.masterData != null) {
            StudentController.masterData.add(newStudent);
        }

        handleReturn();
    }

    @FXML
    public void handleResetData() {
        if (txtRollNo != null) txtRollNo.clear();
        if (comboNrcState != null) comboNrcState.setValue(null);
        if (comboNrcTownship != null) {
            comboNrcTownship.setValue(null);
            if (comboNrcTownship.getEditor() != null) comboNrcTownship.getEditor().clear();
        }
        if (comboNrcType != null) comboNrcType.setValue("(N)");
        if (txtNrcNumber != null) txtNrcNumber.clear();

        if (dpDob != null) dpDob.setValue(null);
        if (txtName != null) txtName.clear();
        if (comboGender != null) comboGender.setValue("Male");
        if (comboMajor != null) comboMajor.setValue("CEIT");
        if (txtNationality != null) txtNationality.clear();
        if (comboYear != null) comboYear.setValue("First Year");
        if (txtPhone != null) txtPhone.clear();
        if (txtEmail != null) txtEmail.clear();
        if (txtCurrentAddress != null) txtCurrentAddress.clear();
        if (txtPermanentAddress != null) txtPermanentAddress.clear();
        if (txtFatherName != null) txtFatherName.clear();
        if (txtFatherOcc != null) txtFatherOcc.clear();
        if (txtFatherPhone != null) txtFatherPhone.clear();
        if (txtFatherEmail != null) txtFatherEmail.clear();
        if (txtMotherName != null) txtMotherName.clear();
        if (txtMotherOcc != null) txtMotherOcc.clear();
        if (txtMotherPhone != null) txtMotherPhone.clear();
        if (txtMotherEmail != null) txtMotherEmail.clear();
        if (txtPrevSchool != null) txtPrevSchool.clear();
        if (txtYearCompleted != null) txtYearCompleted.clear();
        if (txtAdditionalInfo != null) txtAdditionalInfo.clear();
        if (txtExcelPath != null) txtExcelPath.clear();
        selectedExcelFile = null;
        selectedPhotoPath = null;
        try {
            passportImageView.setImage(new Image(getClass().getResourceAsStream(DEFAULT_IMAGE_PATH)));
        } catch (Exception e) {}

        showAlert("Data Reset", "Form inputs have been cleared.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleReturn() {
        try {
            Parent root = txtRollNo.getScene().getRoot();
            if (root instanceof BorderPane) {
                BorderPane mainLayout = (BorderPane) root;
                Parent studentView = FXMLLoader.load(getClass().getResource("StudentContent.fxml"));
                mainLayout.setCenter(studentView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    @FXML
    public void handleNrcHelp() {
        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("NRC Selection Guide");
        helpAlert.setHeaderText("How to fill out the NRC Field");
        helpAlert.setContentText(
            "• State / Region Code (1-14): Selects the regional prefix.\n\n" +
            "• Township ComboBox: You can type any township name globally across all states! Selecting a township will automatically update the State code and number prefix for you.\n\n" +
            "• NRC Number: Enter the remaining 6-digit identification number."
        );
        helpAlert.getDialogPane().setMinWidth(450);
        helpAlert.showAndWait();
    }
}