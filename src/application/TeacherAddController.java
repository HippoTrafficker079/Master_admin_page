package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import application.dao.TeacherDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

public class TeacherAddController {
    @FXML private TextField txtTeacherId, txtDob, txtName, txtNationality, txtNrc;
    @FXML private ComboBox<String> comboGender, comboDept, comboPosition;
    @FXML private TextField txtPhone, txtEmail, txtCurrentAddress, txtPermanentAddress;
    @FXML private TextField txtDegree, txtUniversity, txtYearCompleted, txtSpecialization, txtEmergency;
    @FXML private ImageView passportImageView;

    @FXML private TextField txtExcelPath;
    @FXML private VBox dropZone;

    private File selectedExcelFile = null;
    private String selectedPhotoPath = null;
    private final String DEFAULT_IMAGE_PATH = "/freddy.jpg";
    private boolean isUpdating = false;

    private final String[] VALID_DEPTS = {"CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex", "FE"};
    private final String[] VALID_POSITIONS = {"Professor & Head", "Professor", "Associate Professor", "Lecturer", "Assistant Lecturer", "Tutor"};

    @FXML
    public void initialize() {
        if (comboGender != null) comboGender.setValue("Male");
        if (comboDept != null) comboDept.setValue("CEIT");
        if (comboPosition != null) comboPosition.setValue("Tutor");
        if (txtNationality != null) txtNationality.setText("Myanmar");

        String sessionUser = Session.getUsername();
        String sessionEmail = Session.getEmail();
        if (txtName != null && sessionUser != null && !sessionUser.isEmpty()) txtName.setText(sessionUser);
        if (txtEmail != null && sessionEmail != null && !sessionEmail.isEmpty()) txtEmail.setText(sessionEmail);

        setupBidirectionalSync();
    }

    private void setupBidirectionalSync() {
        if (txtTeacherId != null) {
            txtTeacherId.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null && newVal.contains("-")) {
                    String[] parts = newVal.trim().split("-");
                    if (parts.length >= 2) {
                        for (String d : VALID_DEPTS) {
                            if (d.equalsIgnoreCase(parts[1].trim())) {
                                isUpdating = true;
                                comboDept.setValue(d);
                                isUpdating = false;
                                break;
                            }
                        }
                    }
                }
            });
        }

        if (comboDept != null) {
            comboDept.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null && txtTeacherId != null) {
                    String code = txtTeacherId.getText().trim();
                    if (code.contains("-")) {
                        String[] parts = code.split("-");
                        isUpdating = true;
                        txtTeacherId.setText(parts[0] + "-" + newVal + "-" + (parts.length > 2 ? parts[2] : "101"));
                        isUpdating = false;
                    }
                }
            });
        }
    }

    @FXML
    public void handleBrowseExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel / CSV File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Spreadsheet & CSV Files (*.xlsx, *.csv)", "*.xlsx", "*.csv"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));

        Stage stage = (Stage) txtTeacherId.getScene().getWindow();
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
            List<Teacher> importedTeachers;
            if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                importedTeachers = parseCsvTeachers(selectedExcelFile);
            } else if (fileName.endsWith(".xlsx")) {
                importedTeachers = parseXlsxTeachers(selectedExcelFile);
            } else {
                showAlert("Unsupported File", "Please select a supported file (.xlsx or .csv).", Alert.AlertType.ERROR);
                return;
            }

            if (importedTeachers == null || importedTeachers.isEmpty()) {
                showAlert("Import Notice", "No teacher records were found in the file.", Alert.AlertType.WARNING);
                return;
            }

            promptBatchConfirmation(importedTeachers);

        } catch (Exception e) {
            showAlert("Import Error", "Failed to parse spreadsheet: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void promptBatchConfirmation(List<Teacher> teachers) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Data Successfully Imported");
        alert.setHeaderText("Data are successfully imported from " + selectedExcelFile.getName());
        alert.setContentText(String.format("Found %d teacher record(s).\n\nChoose an action below:", teachers.size()));

        ButtonType btnAddAll = new ButtonType("Add All Teachers", ButtonBar.ButtonData.YES);
        ButtonType btnPickOne = new ButtonType("Select a Teacher to Fill Form", ButtonBar.ButtonData.OTHER);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnAddAll, btnPickOne, btnCancel);
        alert.getDialogPane().setMinWidth(520);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnAddAll) {
                if (TeacherController.masterData != null) {
                    TeacherController.masterData.addAll(teachers);
                }
                showAlert("Success", String.format("Successfully registered %d teacher(s) into the system.", teachers.size()), Alert.AlertType.INFORMATION);
                handleReturn();
            } else if (result.get() == btnPickOne) {
                openSingleTeacherPickerDialog(teachers);
            } else {
                handleReturn();
            }
        }
    }

    private void openSingleTeacherPickerDialog(List<Teacher> teachers) {
        Dialog<Teacher> dialog = new Dialog<>();
        dialog.setTitle("Choose Teacher to Populate Form");
        dialog.setHeaderText("Select a teacher row to load into the form blanks below:");

        TableView<Teacher> previewTable = new TableView<>();
        previewTable.setPrefHeight(300);
        previewTable.setPrefWidth(640);

        TableColumn<Teacher, String> colCode = new TableColumn<>("Teacher ID");
        colCode.setCellValueFactory(c -> c.getValue().teacherCodeProperty());
        colCode.setPrefWidth(120);

        TableColumn<Teacher, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colName.setPrefWidth(160);

        TableColumn<Teacher, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(c -> c.getValue().departmentProperty());
        colDept.setPrefWidth(90);

        TableColumn<Teacher, String> colPos = new TableColumn<>("Position");
        colPos.setCellValueFactory(c -> c.getValue().positionProperty());
        colPos.setPrefWidth(130);

        TableColumn<Teacher, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(c -> c.getValue().phoneNoProperty());
        colPhone.setPrefWidth(130);

        previewTable.getColumns().addAll(colCode, colName, colDept, colPos, colPhone);
        previewTable.setItems(FXCollections.observableArrayList(teachers));
        if (!teachers.isEmpty()) previewTable.getSelectionModel().selectFirst();

        ButtonType btnSelect = new ButtonType("Populate Form", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSelect, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10, previewTable));

        dialog.setResultConverter(btn -> btn == btnSelect ? previewTable.getSelectionModel().getSelectedItem() : null);

        previewTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && previewTable.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(previewTable.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        Optional<Teacher> res = dialog.showAndWait();
        if (res.isPresent() && res.get() != null) {
            populateFormWithTeacher(res.get());
        }
    }

    private void populateFormWithTeacher(Teacher t) {
        isUpdating = true;
        try {
            if (txtTeacherId != null) txtTeacherId.setText(t.getTeacherCode());
            if (txtName != null) txtName.setText(t.getName());
            if (comboDept != null) comboDept.setValue(t.getDepartment());
            if (comboPosition != null) comboPosition.setValue(t.getPosition());
            if (txtPhone != null) txtPhone.setText(t.getPhoneNo());
            if (txtDob != null) txtDob.setText(t.getDob());
            if (comboGender != null) comboGender.setValue(t.getGender());
            if (txtNationality != null) txtNationality.setText(t.getNationality());
            if (txtNrc != null) txtNrc.setText(t.getNrc());
            if (txtEmail != null) txtEmail.setText(t.getEmail());
            if (txtCurrentAddress != null) txtCurrentAddress.setText(t.getCurrentAddress());
            if (txtPermanentAddress != null) txtPermanentAddress.setText(t.getPermanentAddress());
            if (txtDegree != null) txtDegree.setText(t.getDegree());
            if (txtUniversity != null) txtUniversity.setText(t.getUniversity());
            if (txtYearCompleted != null) txtYearCompleted.setText(t.getYearCompleted());
            if (txtSpecialization != null) txtSpecialization.setText(t.getSpecialization());
            if (txtEmergency != null) txtEmergency.setText(t.getEmergencyContact());
        } finally {
            isUpdating = false;
        }
        showAlert("Teacher Loaded", "Details for " + t.getName() + " have been loaded into the form.", Alert.AlertType.INFORMATION);
    }

    private List<Teacher> parseCsvTeachers(File file) throws IOException {
        List<Teacher> teachers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) return teachers;
            String delimiter = headerLine.contains(",") ? "," : "\t";
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
                Teacher t = buildTeacherFromMap(map, values, counter++);
                if (t != null) teachers.add(t);
            }
        }
        return teachers;
    }

    private List<Teacher> parseXlsxTeachers(File file) {
        List<Teacher> teachers = new ArrayList<>();
        try (ZipFile zip = new ZipFile(file)) {
            List<String> sharedStrings = new ArrayList<>();
            ZipEntry sstEntry = zip.getEntry("xl/sharedStrings.xml");
            if (sstEntry != null) {
                String sstXml = new String(zip.getInputStream(sstEntry).readAllBytes(), StandardCharsets.UTF_8);
                Matcher siMatcher = Pattern.compile("<si>(.*?)</si>", Pattern.DOTALL).matcher(sstXml);
                while (siMatcher.find()) {
                    Matcher tMatcher = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL).matcher(siMatcher.group(1));
                    StringBuilder sb = new StringBuilder();
                    while (tMatcher.find()) sb.append(tMatcher.group(1));
                    sharedStrings.add(sb.toString());
                }
            }
            ZipEntry sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml");
            if (sheetEntry == null) return teachers;
            String sheetXml = new String(zip.getInputStream(sheetEntry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher rowMatcher = Pattern.compile("<row[^>]*>(.*?)</row>", Pattern.DOTALL).matcher(sheetXml);
            List<String> headers = null;
            int counter = 1;
            while (rowMatcher.find()) {
                List<String> rowCells = new ArrayList<>();
                Matcher cMatcher = Pattern.compile("<c\\b[^>]*>(.*?)</c>", Pattern.DOTALL).matcher(rowMatcher.group(1));
                while (cMatcher.find()) {
                    Matcher vMatcher = Pattern.compile("<v>(.*?)</v>").matcher(cMatcher.group(1));
                    if (vMatcher.find()) {
                        String val = vMatcher.group(1);
                        try {
                            int idx = Integer.parseInt(val);
                            if (idx >= 0 && idx < sharedStrings.size()) val = sharedStrings.get(idx);
                        } catch (Exception e) {}
                        rowCells.add(val);
                    } else {
                        rowCells.add("");
                    }
                }
                if (headers == null) headers = rowCells;
                else {
                    if (rowCells.isEmpty() || String.join("", rowCells).trim().isEmpty()) continue;
                    Map<String, String> map = new HashMap<>();
                    String[] raw = new String[headers.size()];
                    for (int i = 0; i < headers.size(); i++) {
                        String h = headers.get(i).trim().toLowerCase();
                        String v = i < rowCells.size() ? rowCells.get(i).trim() : "";
                        map.put(h, v);
                        raw[i] = v;
                    }
                    Teacher t = buildTeacherFromMap(map, raw, counter++);
                    if (t != null) teachers.add(t);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teachers;
    }

    private Teacher buildTeacherFromMap(Map<String, String> map, String[] rawOrder, int seqNo) {
        String code = getMapOrIndex(map, rawOrder, 0, "teacher id", "teacherid", "id", "code");
        String name = getMapOrIndex(map, rawOrder, 1, "name", "full name", "fullname");
        String dept = getMapOrIndex(map, rawOrder, 2, "department", "dept");
        String pos = getMapOrIndex(map, rawOrder, 3, "position", "title");
        String phone = getMapOrIndex(map, rawOrder, 4, "phone", "phone number");
        if (code.isEmpty() || name.isEmpty()) return null;

        return new Teacher(
            seqNo, code, name, dept.isEmpty() ? "CEIT" : dept, pos.isEmpty() ? "Tutor" : pos,
            phone.isEmpty() ? "09-000000000" : phone,
            getMapOrIndex(map, rawOrder, 5, "dob"), getMapOrIndex(map, rawOrder, 6, "gender"),
            getMapOrIndex(map, rawOrder, 7, "nationality"), getMapOrIndex(map, rawOrder, 8, "nrc"),
            getMapOrIndex(map, rawOrder, 9, "email"), getMapOrIndex(map, rawOrder, 10, "current address"),
            getMapOrIndex(map, rawOrder, 11, "permanent address"), getMapOrIndex(map, rawOrder, 12, "degree"),
            getMapOrIndex(map, rawOrder, 13, "university"), getMapOrIndex(map, rawOrder, 14, "year completed"),
            getMapOrIndex(map, rawOrder, 15, "specialization"), getMapOrIndex(map, rawOrder, 16, "emergency contact"),
            DEFAULT_IMAGE_PATH
        );
    }

    private String getMapOrIndex(Map<String, String> map, String[] rawOrder, int index, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k) && !map.get(k).trim().isEmpty()) return map.get(k).trim();
        }
        if (index < rawOrder.length && rawOrder[index] != null) return rawOrder[index].trim();
        return "";
    }

    @FXML
    public void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Photo");
        Stage stage = (Stage) txtTeacherId.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedPhotoPath = file.toURI().toString();
            try { passportImageView.setImage(new Image(selectedPhotoPath, 86, 110, false, true)); } catch (Exception e) {}
        }
    }

    @FXML
    public void handleSave() {
        String rawCode = txtTeacherId != null ? txtTeacherId.getText().trim() : "";
        String rawName = txtName != null ? txtName.getText().trim() : "";
        if (rawCode.isEmpty() || rawName.isEmpty()) {
            showAlert("Error", "Teacher ID and Name are required.", Alert.AlertType.ERROR);
            return;
        }
        Teacher newTeacher = new Teacher(
            TeacherController.masterData != null ? TeacherController.masterData.size() + 1 : 1,
            rawCode, rawName, comboDept.getValue(), comboPosition.getValue(), txtPhone.getText().trim(),
            txtDob.getText().trim(), comboGender.getValue(), txtNationality.getText().trim(),
            txtNrc.getText().trim(), txtEmail.getText().trim(), txtCurrentAddress.getText().trim(), txtPermanentAddress.getText().trim(),
            txtDegree.getText().trim(), txtUniversity.getText().trim(), txtYearCompleted.getText().trim(), txtSpecialization.getText().trim(),
            txtEmergency.getText().trim(), selectedPhotoPath != null ? selectedPhotoPath : DEFAULT_IMAGE_PATH
        );
        if (TeacherController.masterData != null) TeacherController.masterData.add(newTeacher);
        handleReturn();
    }

    @FXML
    public void handleResetData() {
        if (txtTeacherId != null) txtTeacherId.clear();
        if (txtName != null) txtName.clear();
        showAlert("Reset", "Form cleared.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleReturn() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("TeacherContents.fxml"));
            BorderPane mainLayout = (BorderPane) txtTeacherId.getScene().getRoot();
            mainLayout.setCenter(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(520);
        alert.showAndWait();
    }
}