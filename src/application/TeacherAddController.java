package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TeacherAddController {
    @FXML private TextField txtTeacherId, txtDob, txtName, txtNationality;

    // Structured NRC Fields
    @FXML private ComboBox<String> comboNrcState;
    @FXML private ComboBox<String> comboNrcTownship;
    @FXML private ComboBox<String> comboNrcType;
    @FXML private TextField txtNrcNumber;

    @FXML private ComboBox<String> comboGender, comboDept, comboPosition;
    @FXML private TextField txtPhone, txtEmail, txtCurrentAddress, txtPermanentAddress;
    @FXML private TextField txtDegree, txtUniversity, txtYearCompleted, txtSpecialization, txtEmergency;
    @FXML private ImageView passportImageView;

    @FXML private TextField txtExcelPath;
    @FXML private VBox dropZone;

    private ObservableList<String> currentTownshipOptions;
    private FilteredList<String> filteredTownships;
    private final boolean[] isTownshipLocked = { false };

    private File selectedExcelFile = null;
    private String selectedPhotoPath = null;
    private final String DEFAULT_IMAGE_PATH = null;
    private boolean isUpdating = false;

    private final String[] VALID_DEPTS = {"C", "Mech", "EC", "CEIT", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE"};
    
    private final String[] VALID_POSITIONS = { "Professor & Head", "Professor", "Associate Professor", "Lecturer", "Assistant Lecturer", "Tutor" };
    
    @FXML
    public void initialize() {
        initializeNrcComponent();

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
            comboNrcTownship.setVisibleRowCount(12);
        }

        if (comboNrcState != null) {
            comboNrcState.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdating) return;
                if (newVal != null && !newVal.equals(oldVal)) {
                    isUpdating = true;
                    try {
                        ObservableList<String> stateTownships = NrcData.getTownshipsForState(newVal);
                        currentTownshipOptions.setAll(stateTownships);
                        isTownshipLocked[0] = false;
                        filteredTownships.setPredicate(s -> true);
                        comboNrcTownship.getSelectionModel().clearSelection();
                        comboNrcTownship.setValue(null);
                        if (comboNrcTownship.getEditor() != null) comboNrcTownship.getEditor().clear();
                        if (comboNrcTownship.isShowing()) comboNrcTownship.hide();
                    } finally {
                        isUpdating = false;
                    }
                }
            });
        }

        if (comboNrcTownship != null && comboNrcTownship.getEditor() != null) {
            comboNrcTownship.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (isUpdating) return;
                if (comboNrcTownship.getValue() != null && comboNrcTownship.getValue().equals(newValue)) return;

                if (newValue == null || newValue.trim().isEmpty() || newValue.equalsIgnoreCase("Township")) {
                    isTownshipLocked[0] = false;
                    Platform.runLater(() -> filteredTownships.setPredicate(s -> true));
                    return;
                }

                if (!isTownshipLocked[0]) {
                    Platform.runLater(() -> {
                        String query = newValue.toLowerCase().trim();
                        filteredTownships.setPredicate(item -> item != null && item.toLowerCase().contains(query));
                        if (!comboNrcTownship.isShowing() && !filteredTownships.isEmpty()) {
                            comboNrcTownship.show();
                        }
                    });
                }
            });

            comboNrcTownship.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdating) return;
                if (newVal != null && !newVal.trim().isEmpty()) {
                    isTownshipLocked[0] = true;
                    if (comboNrcTownship.getEditor() != null && !comboNrcTownship.getEditor().getText().equals(newVal)) {
                        isUpdating = true;
                        comboNrcTownship.getEditor().setText(newVal);
                        isUpdating = false;
                    }

                    String matchedState = NrcData.getStateForTownship(newVal);
                    if (matchedState != null && comboNrcState != null && !matchedState.equals(comboNrcState.getValue())) {
                        isUpdating = true;
                        try {
                            comboNrcState.setValue(matchedState);
                            currentTownshipOptions.setAll(NrcData.getTownshipsForState(matchedState));
                        } finally {
                            isUpdating = false;
                        }
                    }

                    String associatedCode = NrcData.getCodeForTownship(newVal);
                    if (associatedCode != null && txtNrcNumber != null
                            && (txtNrcNumber.getText() == null || txtNrcNumber.getText().trim().isEmpty())) {
                        isUpdating = true;
                        try {
                            txtNrcNumber.setText(associatedCode);
                        } finally {
                            isUpdating = false;
                        }
                    }
                } else {
                    isTownshipLocked[0] = false;
                }
            });

            comboNrcTownship.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (isUpdating) return;
                if (isTownshipLocked[0]) {
                    KeyCode code = event.getCode();
                    if (code == KeyCode.BACK_SPACE || code == KeyCode.DELETE) {
                        isUpdating = true;
                        try {
                            isTownshipLocked[0] = false;
                            comboNrcTownship.setValue(null);
                            comboNrcTownship.getEditor().clear();
                            if (comboNrcState != null && comboNrcState.getValue() != null) {
                                currentTownshipOptions.setAll(NrcData.getTownshipsForState(comboNrcState.getValue()));
                            } else {
                                currentTownshipOptions.setAll(NrcData.getAllTownships());
                            }
                            filteredTownships.setPredicate(s -> true);
                        } finally {
                            isUpdating = false;
                        }
                        event.consume();
                        return;
                    }

                    event.consume();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Action Restricted");
                        alert.setHeaderText(null);
                        alert.setContentText("Please press backspace once to clear first.");
                        alert.getDialogPane().setMinWidth(380);
                        alert.showAndWait();
                    });
                }
            });
        }

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

        if (state.isEmpty() || township.isEmpty() || number.isEmpty()) return "";
        return state + "/" + township + type + number;
    }

    private void setParsedNrc(String fullNrc) {
        if (fullNrc == null || fullNrc.trim().isEmpty() || fullNrc.trim().equals("-")) {
            isTownshipLocked[0] = false;
            return;
        }
        isUpdating = true;
        try {
            String[] parts = fullNrc.split("/");
            if (parts.length >= 2) {
                String state = parts[0].trim();
                if (comboNrcState != null) {
                    if (!comboNrcState.getItems().contains(state)) comboNrcState.getItems().add(state);
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
                        if (comboNrcTownship.getEditor() != null) comboNrcTownship.getEditor().setText(township);
                        isTownshipLocked[0] = true;
                    }
                    int closingIndex = remainder.indexOf(")");
                    if (closingIndex != -1) {
                        String type = remainder.substring(typeIndex, closingIndex + 1).trim();
                        if (comboNrcType != null) {
                            if (!comboNrcType.getItems().contains(type)) comboNrcType.getItems().add(type);
                            comboNrcType.setValue(type);
                        }
                        String number = remainder.substring(closingIndex + 1).trim();
                        if (txtNrcNumber != null) txtNrcNumber.setText(number);
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
            if (txtExcelPath != null) txtExcelPath.setText(file.getAbsolutePath());
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
                    if (txtExcelPath != null) txtExcelPath.setText(file.getAbsolutePath());
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
        Alert choiceAlert = new Alert(Alert.AlertType.CONFIRMATION);
        choiceAlert.setTitle("Data Successfully Imported");
        choiceAlert.setHeaderText("Data are successfully imported from " + selectedExcelFile.getName());
        choiceAlert.setContentText("Found " + teachers.size() + " teacher record(s).\n\nChoose an action below:");

        ButtonType btnSelectOne = new ButtonType("Select a Teacher to Fill Form");
        ButtonType btnAddAll = new ButtonType("Add All Teachers", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        choiceAlert.getButtonTypes().setAll(btnSelectOne, btnAddAll, btnCancel);

        // Apply style.css & consistent button styling
        DialogPane dialogPane = choiceAlert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: #f8fafc;");
        dialogPane.setMinWidth(540);

        Button selectOneBtn = (Button) dialogPane.lookupButton(btnSelectOne);
        if (selectOneBtn != null) {
            selectOneBtn.getStyleClass().add("search-button");
            selectOneBtn.setPrefHeight(34);
            selectOneBtn.setStyle("-fx-padding: 0 14;");
        }

        Button addAllBtn = (Button) dialogPane.lookupButton(btnAddAll);
        if (addAllBtn != null) {
            addAllBtn.getStyleClass().add("search-button");
            addAllBtn.setPrefHeight(34);
            addAllBtn.setStyle("-fx-padding: 0 16;");
        }

        Button cancelBtn = (Button) dialogPane.lookupButton(btnCancel);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("reset-button");
            cancelBtn.setPrefHeight(34);
            cancelBtn.setPrefWidth(95);
        }
        
        AlertHelper.styleAlert(choiceAlert, false);
        Optional<ButtonType> result = choiceAlert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnSelectOne) {
                openSingleTeacherPickerDialog(teachers);
            } else if (result.get() == btnAddAll) {
                TeacherDAO teacherDAO = new TeacherDAO();
                int addedCount = teacherDAO.insertBatchTeachers(teachers);
                String dupErrors = teacherDAO.getLastBatchError();

                if (addedCount > 0) {
                    TeacherController.refreshLive();
                    AttendanceController.refreshLive();
                    DashboardController.refreshDashboard();

                    if (dupErrors != null && !dupErrors.isEmpty()) {
                        showAlert("Partial Import Completed",
                                "Successfully added " + addedCount + " teacher(s) to the database.\n\n"
                                + "The following duplicate record(s) were skipped:\n" + dupErrors,
                                Alert.AlertType.WARNING);
                    } else {
                        showAlert("Batch Import Successful",
                                "Successfully added " + addedCount + " teacher(s) to the database.",
                                Alert.AlertType.INFORMATION);
                    }
                    handleReturn();
                } else {
                    String reason = (dupErrors != null && !dupErrors.isEmpty())
                            ? "No new teachers were added because duplicate record(s) were found:\n\n" + dupErrors
                            : "Failed to insert imported teachers into the database. Please check for duplicate Teacher IDs, NRCs, or Emails.";
                    showAlert("Batch Import Failed", reason, Alert.AlertType.ERROR);
                }
            }
        }
    }

    private void openSingleTeacherPickerDialog(List<Teacher> teachers) {
        Dialog<Teacher> dialog = new Dialog<>();
        dialog.setTitle("Choose Teacher to Populate Form");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogPane.setStyle("-fx-background-color: #f8fafc; -fx-padding: 16;");

        Label promptLbl = new Label("Select a teacher row to load into the form blanks below:");
        promptLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        TableView<Teacher> previewTable = new TableView<>();
        previewTable.setPrefHeight(320);
        previewTable.setPrefWidth(640);
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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
        for (TableColumn<?, ?> col : previewTable.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
        }

        previewTable.setItems(FXCollections.observableArrayList(teachers));
        if (!teachers.isEmpty()) previewTable.getSelectionModel().selectFirst();

        ButtonType btnSelect = new ButtonType("Populate Form", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(btnSelect, ButtonType.CANCEL);
        dialogPane.setContent(new VBox(10, promptLbl, previewTable));

        Button populateBtn = (Button) dialogPane.lookupButton(btnSelect);
        if (populateBtn != null) {
            populateBtn.getStyleClass().add("search-button");
            populateBtn.setPrefHeight(34);
            populateBtn.setPrefWidth(130);
        }

        Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.getStyleClass().add("reset-button");
            cancelBtn.setPrefHeight(34);
            cancelBtn.setPrefWidth(95);
        }

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
            setParsedNrc(t.getNrc());
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
                    String cellXml = cMatcher.group(1);
                    Matcher vMatcher = Pattern.compile("<v>(.*?)</v>").matcher(cellXml);
                    if (vMatcher.find()) {
                        String val = vMatcher.group(1);
                        if (cMatcher.group(0).contains("t=\"s\"")) {
                            try {
                                int idx = Integer.parseInt(val);
                                if (idx >= 0 && idx < sharedStrings.size())
                                    val = sharedStrings.get(idx);
                            } catch (Exception e) {
                            }
                        }
                        rowCells.add(val);
                    } else if (cellXml.contains("<is>")) {
                        Matcher isMatcher = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL).matcher(cellXml);
                        StringBuilder sb = new StringBuilder();
                        while (isMatcher.find())
                            sb.append(isMatcher.group(1));
                        rowCells.add(sb.toString());
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
        String dob = normalizeDateToYMD(getMapOrIndex(map, rawOrder, 5, "dob", "date of birth"));
        
        if (code.isEmpty() || name.isEmpty()) return null;

        return new Teacher(
                seqNo, code, name, dept.isEmpty() ? "CEIT" : dept, pos.isEmpty() ? "Tutor" : pos,
                phone.isEmpty() ? "09-000000000" : phone,
                dob, getMapOrIndex(map, rawOrder, 6, "gender"),
                getMapOrIndex(map, rawOrder, 7, "nationality"), getMapOrIndex(map, rawOrder, 8, "nrc"),
                getMapOrIndex(map, rawOrder, 9, "email"), getMapOrIndex(map, rawOrder, 10, "current address"),
                getMapOrIndex(map, rawOrder, 11, "permanent address"), getMapOrIndex(map, rawOrder, 12, "degree", "highest degree"),
                getMapOrIndex(map, rawOrder, 13, "university"), getMapOrIndex(map, rawOrder, 14, "year completed"),
                getMapOrIndex(map, rawOrder, 15, "specialization"),
                getMapOrIndex(map, rawOrder, 16, "emergency contact"),
                DEFAULT_IMAGE_PATH);
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
            try {
                passportImageView.setImage(new Image(selectedPhotoPath, 86, 111, false, true));
                passportImageView.setFitWidth(86.0);
                passportImageView.setFitHeight(111.0);
                passportImageView.setPreserveRatio(false);
            } catch (Exception e) {}
        }
    }

    @FXML
    public void handleSave() {
        String rawCode = txtTeacherId != null ? txtTeacherId.getText().trim() : "";
        String rawName = txtName != null ? txtName.getText().trim() : "";
        String rawDept = comboDept != null && comboDept.getValue() != null ? comboDept.getValue().trim() : "";
        String rawPos = comboPosition != null && comboPosition.getValue() != null ? comboPosition.getValue().trim() : "";
        String rawPhone = txtPhone != null ? txtPhone.getText().trim() : "";
        String rawEmail = txtEmail != null ? txtEmail.getText().trim() : "";
        String rawDob = txtDob != null ? txtDob.getText().trim() : "";
        String rawNrc = getCombinedNrc();
        String rawNationality = txtNationality != null && !txtNationality.getText().trim().isEmpty()
                ? txtNationality.getText().trim() : "Myanmar";
        String rawCurrAddr = txtCurrentAddress != null ? txtCurrentAddress.getText().trim() : "";
        String rawPermAddr = txtPermanentAddress != null ? txtPermanentAddress.getText().trim() : "";
        String rawDegree = txtDegree != null ? txtDegree.getText().trim() : "";
        String rawUniversity = txtUniversity != null ? txtUniversity.getText().trim() : "";
        String rawYearComp = txtYearCompleted != null ? txtYearCompleted.getText().trim() : "";
        String rawSpec = txtSpecialization != null ? txtSpecialization.getText().trim() : "";
        String rawEmergency = txtEmergency != null ? txtEmergency.getText().trim() : "";
        String gender = comboGender != null && comboGender.getValue() != null ? comboGender.getValue().trim() : "Male";

        if (rawNrc.isEmpty()) {
            showAlert("Incomplete NRC", "Please complete all structured NRC fields (State, Township, Type, and 6-digit Number).", Alert.AlertType.ERROR);
            return;
        }

        if (rawCode.isEmpty() || rawName.isEmpty() || rawDept.isEmpty() || rawPos.isEmpty()
                || rawPhone.isEmpty() || rawDob.isEmpty()
                || rawCurrAddr.isEmpty() || rawPermAddr.isEmpty()
                || rawDegree.isEmpty() || rawUniversity.isEmpty() || rawYearComp.isEmpty()) {
            showAlert("Missing Information",
                    "Please fill in all required fields (Teacher ID, Full Name, DOB, Department, Position, NRC, Phone, Addresses, Degree, University, and Year Completed).",
                    Alert.AlertType.ERROR);
            return;
        }

        if (!rawCode.matches("^[a-zA-Z]+-[a-zA-Z0-9]+-\\d+$")) {
            showAlert("Invalid Teacher ID Format", "Teacher ID must be in format: [Prefix]-[Department]-[Number] (e.g., TCH-CEIT-101).", Alert.AlertType.ERROR);
            return;
        }

        String[] parts = rawCode.split("-");
        String deptFromCode = findValidDepartment(parts[1].trim());
        if (deptFromCode == null || !deptFromCode.equalsIgnoreCase(rawDept)) {
            showAlert("Department Mismatch", String.format("Teacher ID department '%s' does not match selected Department '%s'.", parts[1].trim(), rawDept), Alert.AlertType.ERROR);
            return;
        }

        String standardizedCode = parts[0].trim() + "-" + deptFromCode + "-" + parts[2].trim();

        if (!rawDob.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            showAlert("Invalid Date of Birth", "Date of Birth must be in YYYY-MM-DD format (e.g., 1985-05-15).", Alert.AlertType.ERROR);
            return;
        }
        if (!rawPhone.matches("^09-\\d+$")) {
            showAlert("Invalid Phone Number", "Phone number must start with 09- followed by digits.", Alert.AlertType.ERROR);
            return;
        }
        if (!rawEmail.isEmpty() && !rawEmail.contains("@")) {
            showAlert("Invalid Email Address", "Email address must contain '@'.", Alert.AlertType.ERROR);
            return;
        }

        if (!rawYearComp.matches("^\\d{4}$")) {
            showAlert("Invalid Year Completed", "Year Completed must be a 4-digit year (e.g., 2015).", Alert.AlertType.ERROR);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Addition");
        confirmAlert.setHeaderText("Add New Teacher");
        confirmAlert.setContentText("Are you sure you want to add this new teacher to the system?");
        if (txtTeacherId != null && txtTeacherId.getScene() != null) {
            confirmAlert.initOwner(txtTeacherId.getScene().getWindow());
        }
        AlertHelper.styleAlert(confirmAlert, false);
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String finalPhoto = selectedPhotoPath != null ? selectedPhotoPath : DEFAULT_IMAGE_PATH;
        Teacher newTeacher = new Teacher(
                0, standardizedCode, rawName, deptFromCode, rawPos, rawPhone,
                rawDob, gender, rawNationality, rawNrc, rawEmail,
                rawCurrAddr, rawPermAddr, rawDegree, rawUniversity,
                rawYearComp, rawSpec, rawEmergency, finalPhoto);

        TeacherDAO teacherDAO = new TeacherDAO();
        int generatedId = teacherDAO.insertTecher(newTeacher);

        if (generatedId > 0) {
            newTeacher.setTeacherId(generatedId);
            TeacherController.refreshLive();
            AttendanceController.refreshLive();
            DashboardController.refreshDashboard();
            Session.clear();
            showAlert("Success", "Teacher '" + rawName + "' has been successfully added to the database.", Alert.AlertType.INFORMATION);
            handleReturn();
        } else {
            showAlert("Database Error", "Failed to save teacher to the database. Please check that the NRC or Email is not already registered.", Alert.AlertType.ERROR);
        }
    }

    private String findValidDepartment(String input) {
        if (input == null) return null;
        for (String d : VALID_DEPTS) {
            if (d.equalsIgnoreCase(input.trim())) return d;
        }
        return null;
    }

    @FXML
    public void handleResetData() {
        isUpdating = true;
        try {
            if (txtTeacherId != null) txtTeacherId.clear();
            if (txtName != null) txtName.clear();
            if (comboDept != null) comboDept.setValue("CEIT");
            if (comboPosition != null) comboPosition.setValue("Tutor");
            if (txtPhone != null) txtPhone.clear();
            if (txtDob != null) txtDob.clear();
            if (comboGender != null) comboGender.setValue("Male");
            if (txtNationality != null) txtNationality.setText("Myanmar");
            if (comboNrcState != null) comboNrcState.setValue(null);
            if (comboNrcTownship != null) {
                comboNrcTownship.setValue(null);
                if (comboNrcTownship.getEditor() != null) comboNrcTownship.getEditor().clear();
            }
            isTownshipLocked[0] = false;
            if (comboNrcType != null) comboNrcType.setValue("(N)");
            if (txtNrcNumber != null) txtNrcNumber.clear();
            if (txtEmail != null) txtEmail.clear();
            if (txtCurrentAddress != null) txtCurrentAddress.clear();
            if (txtPermanentAddress != null) txtPermanentAddress.clear();
            if (txtDegree != null) txtDegree.clear();
            if (txtUniversity != null) txtUniversity.clear();
            if (txtYearCompleted != null) txtYearCompleted.clear();
            if (txtSpecialization != null) txtSpecialization.clear();
            if (txtEmergency != null) txtEmergency.clear();
            if (txtExcelPath != null) txtExcelPath.clear();
        } finally {
            isUpdating = false;
        }
        selectedExcelFile = null;
        selectedPhotoPath = null;
        if (passportImageView != null) {
            passportImageView.setImage(null);
        }
        showAlert("Reset", "Form cleared.", Alert.AlertType.INFORMATION);
    }

    @FXML
    public void handleNrcHelp() {
        javafx.stage.Window owner = (txtTeacherId != null && txtTeacherId.getScene() != null)
                ? txtTeacherId.getScene().getWindow() : null;
        AlertHelper.showInfo(owner, "NRC Selection Guide", "How to fill out the NRC Field",
            "• State / Region Code (1-14): Selects the regional prefix.\n\n" +
            "• Township ComboBox: You can type any township name globally across all states! Selecting a township will automatically update the State code and number prefix for you.\n\n" +
            "• NRC Number: Enter the remaining 6-digit identification number."
        );
    }

    @FXML
    private void handleReturn() {
        Session.clear();
        if (txtTeacherId == null || txtTeacherId.getScene() == null) {
            return;
        }
        Parent teacherView = TeacherController.getView();
        if (teacherView != null) {
            BorderPane mainLayout = (BorderPane) txtTeacherId.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(teacherView);
            } else if (txtTeacherId.getScene().getRoot() instanceof BorderPane) {
                ((BorderPane) txtTeacherId.getScene().getRoot()).setCenter(teacherView);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        javafx.stage.Window owner = (txtTeacherId != null && txtTeacherId.getScene() != null)
                ? txtTeacherId.getScene().getWindow() : null;
        AlertHelper.show(owner, type, title, null, message);
    }
    
    private String normalizeDateToYMD(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "";
        }
        String cleaned = rawDate.trim();

        // 1. Handle Excel numeric serial dates (e.g., 30177 or 30177.0)
        if (cleaned.matches("^\\d+(\\.0+)?$")) {
            try {
                long serialDays = (long) Double.parseDouble(cleaned);
                LocalDate date = LocalDate.of(1899, 12, 30).plusDays(serialDays);
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {
            }
        }

        // 2. Try common date patterns (day-month-year & year-month-day with '-', '/', or '.')
        String[] patterns = {
            "d-M-yyyy",
            "dd-MM-yyyy",
            "d/M/yyyy",
            "dd/MM/yyyy",
            "d.M.yyyy",
            "dd.MM.yyyy",
            "yyyy-M-d",
            "yyyy-MM-dd",
            "yyyy/M/d",
            "yyyy/MM/dd"
        };

        for (String pattern : patterns) {
            try {
                LocalDate parsed = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern));
                return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }

        return cleaned;
    }
}