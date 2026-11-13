package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import application.dao.TeacherDAO;
import javafx.application.Platform;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TeacherModifyController {
    @FXML private Label lblFormTitle, lblPhotoAction;
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
    @FXML private HBox topActionBar;
    @FXML private VBox contentContainer;

    @FXML private TextField txtExcelPath;
    @FXML private VBox dropZone;

    private ObservableList<String> currentTownshipOptions;
    private FilteredList<String> filteredTownships;
    private final boolean[] isTownshipLocked = { true };

    private File selectedExcelFile = null;
    private Teacher currentTeacher;
    private boolean isUpdating = false;
    private String updatedPhotoPath = null;
    private HBox bannerBox = null;
    private final String DEFAULT_IMAGE_PATH = "/freddy.jpg";

    private final String[] VALID_DEPTS = { "C", "Mech", "EC", "CEIT", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE" };
    
    private final String[] VALID_POSITIONS = {"Professor & Head", "Professor", "Associate Professor", "Lecturer", "Assistant Lecturer", "Tutor"};

    @FXML
    public void initialize() {
        initializeNrcComponent();
        setupTeacherBidirectionalSync();
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
                        currentTownshipOptions.setAll(NrcData.getTownshipsForState(newVal));
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
            isUpdating = true;
            try {
                if (comboNrcState != null) comboNrcState.setValue(null);
                if (comboNrcTownship != null) {
                    comboNrcTownship.setValue(null);
                    if (comboNrcTownship.getEditor() != null) comboNrcTownship.getEditor().clear();
                }
                if (comboNrcType != null) comboNrcType.setValue("(N)");
                if (txtNrcNumber != null) txtNrcNumber.clear();
                isTownshipLocked[0] = false;
            } finally {
                isUpdating = false;
            }
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
                    currentTownshipOptions.setAll(NrcData.getTownshipsForState(state));
                }

                String remainder = parts[1];
                int typeIndex = remainder.indexOf("(");
                if (typeIndex != -1) {
                    String township = remainder.substring(0, typeIndex).trim();
                    if (comboNrcTownship != null) {
                        if (!currentTownshipOptions.contains(township)) currentTownshipOptions.add(township);
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

    public void initData(Teacher teacher) {
        this.currentTeacher = teacher;

        if (lblFormTitle != null) {
            lblFormTitle.setText("FACULTY / TEACHER INFORMATION FORM (MODIFY)");
        }

        if (contentContainer != null && teacher != null) {
            if (bannerBox != null && contentContainer.getChildren().contains(bannerBox)) {
                contentContainer.getChildren().remove(bannerBox);
            }
            bannerBox = new HBox();
            bannerBox.setStyle("-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 18;");

            // Use the UI row number passed from TeacherController (e.g., 7 instead of 19)
            int displayRowNo = teacher.getDisplayNo() > 0 ? teacher.getDisplayNo() : teacher.getTeacherId();

            String bannerText = String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                displayRowNo, safe(teacher.getTeacherCode()), safe(teacher.getName()), safe(teacher.getDepartment()), safe(teacher.getPosition()));

            Label bannerLabel = new Label(bannerText);
            bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
            bannerBox.getChildren().add(bannerLabel);
            contentContainer.getChildren().add(0, bannerBox);
        }

        isUpdating = true;
        try {
            if (txtTeacherId != null) txtTeacherId.setText(safe(teacher.getTeacherCode()));
            if (txtDob != null) txtDob.setText(formatToYearMonthDay(teacher.getDob()));
            if (txtName != null) txtName.setText(safe(teacher.getName()));

            if (comboGender != null) {
                comboGender.setItems(FXCollections.observableArrayList("Male", "Female"));
                comboGender.setValue((teacher.getGender() != null && teacher.getGender().equalsIgnoreCase("Female")) ? "Female" : "Male");
            }

            if (comboDept != null) {
                comboDept.setItems(FXCollections.observableArrayList(VALID_DEPTS));
                comboDept.setValue(teacher.getDepartment() != null ? teacher.getDepartment() : "CEIT");
            }

            if (txtNationality != null) txtNationality.setText(safe(teacher.getNationality(), "Myanmar"));

            if (comboPosition != null) {
                comboPosition.setItems(FXCollections.observableArrayList(VALID_POSITIONS));
                comboPosition.setValue(teacher.getPosition() != null ? teacher.getPosition() : "Tutor");
            }

            setParsedNrc(teacher.getNrc());

            if (txtPhone != null) txtPhone.setText(safe(teacher.getPhoneNo()));
            if (txtEmail != null) txtEmail.setText(safe(teacher.getEmail()));
            if (txtCurrentAddress != null) txtCurrentAddress.setText(safe(teacher.getCurrentAddress()));
            if (txtPermanentAddress != null) txtPermanentAddress.setText(safe(teacher.getPermanentAddress()));

            if (txtDegree != null) txtDegree.setText(safe(teacher.getDegree()));
            if (txtUniversity != null) txtUniversity.setText(safe(teacher.getUniversity()));
            if (txtYearCompleted != null) txtYearCompleted.setText(safe(teacher.getYearCompleted()));
            if (txtSpecialization != null) txtSpecialization.setText(safe(teacher.getSpecialization()));
            if (txtEmergency != null) txtEmergency.setText(safe(teacher.getEmergencyContact()));
        } finally {
            isUpdating = false;
        }

        updatedPhotoPath = teacher.getPhotoPath();
        loadImage(updatedPhotoPath);
    }

    private String safe(String val) {
        return safe(val, "");
    }

    private String safe(String val, String defaultVal) {
        if (val == null || val.trim().equalsIgnoreCase("null") || val.trim().equals("-")) {
            return defaultVal;
        }
        return val.trim();
    }

    private void setupTeacherBidirectionalSync() {
        if (txtTeacherId != null) {
            txtTeacherId.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null && newVal.contains("-")) {
                    String[] parts = newVal.trim().split("-");
                    if (parts.length >= 2) {
                        String deptCode = parts[1].trim();
                        for (String d : VALID_DEPTS) {
                            if (d.equalsIgnoreCase(deptCode)) {
                                isUpdating = true;
                                try {
                                    comboDept.setValue(d);
                                } finally {
                                    isUpdating = false;
                                }
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
                    String currentCode = txtTeacherId.getText() != null ? txtTeacherId.getText().trim() : "";
                    isUpdating = true;
                    try {
                        if (currentCode.contains("-")) {
                            String[] parts = currentCode.split("-");
                            String prefix = parts[0].trim().isEmpty() ? "TCH" : parts[0].trim();
                            String num = (parts.length >= 3 && !parts[parts.length - 1].trim().isEmpty())
                                    ? parts[parts.length - 1].trim()
                                    : (currentTeacher != null ? String.valueOf(currentTeacher.getTeacherId()) : "1");
                            txtTeacherId.setText(prefix + "-" + newVal + "-" + num);
                        } else if (currentTeacher != null) {
                            txtTeacherId.setText("TCH-" + newVal + "-" + currentTeacher.getTeacherId());
                        }
                    } finally {
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
                    break;
                }
            }
        }
        event.consume();
    }

    @FXML public void handleDragExited(DragEvent event) { event.consume(); }

    @FXML
    public void handleDragDropped(DragEvent event) {
        boolean success = false;
        if (event.getDragboard().hasFiles()) {
            for (File file : event.getDragboard().getFiles()) {
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
        event.consume();
    }

    @FXML
    public void handleImportExcel() {
        if (selectedExcelFile == null || !selectedExcelFile.exists()) {
            showAlert("No File Selected", "Please browse or drag and drop a valid Excel or CSV file first.", Alert.AlertType.WARNING);
            return;
        }
        try {
            List<Teacher> teachers = selectedExcelFile.getName().toLowerCase().endsWith(".csv")
                    ? parseCsvTeachers(selectedExcelFile) : parseXlsxTeachers(selectedExcelFile);
            if (teachers.isEmpty()) {
                showAlert("Import Notice", "No teacher records found.", Alert.AlertType.WARNING);
                return;
            }
            if (teachers.size() == 1) populateFormWithTeacher(teachers.get(0));
            else openSingleTeacherPickerDialog(teachers);
        } catch (Exception e) {
            showAlert("Import Error", "Failed to parse spreadsheet: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void openSingleTeacherPickerDialog(List<Teacher> teachers) {
        Dialog<Teacher> dialog = new Dialog<>();
        dialog.setTitle("Choose Teacher to Populate Form");
        dialog.setHeaderText("Select a teacher row to load into form:");
        TableView<Teacher> table = new TableView<>();
        table.setPrefHeight(250);
        table.setPrefWidth(500);

        TableColumn<Teacher, String> cCode = new TableColumn<>("Teacher ID");
        cCode.setCellValueFactory(c -> c.getValue().teacherCodeProperty());
        TableColumn<Teacher, String> cName = new TableColumn<>("Name");
        cName.setCellValueFactory(c -> c.getValue().nameProperty());
        table.getColumns().addAll(cCode, cName);
        table.setItems(FXCollections.observableArrayList(teachers));

        ButtonType btnSelect = new ButtonType("Populate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSelect, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10, table));
        dialog.setResultConverter(b -> b == btnSelect ? table.getSelectionModel().getSelectedItem() : null);

        Optional<Teacher> res = dialog.showAndWait();
        res.ifPresent(this::populateFormWithTeacher);
    }

    private void populateFormWithTeacher(Teacher t) {
        isUpdating = true;
        try {
            if (txtTeacherId != null) txtTeacherId.setText(t.getTeacherCode());
            if (txtName != null) txtName.setText(t.getName());
            if (comboDept != null) comboDept.setValue(t.getDepartment());
            if (comboPosition != null) comboPosition.setValue(t.getPosition());
            if (txtPhone != null) txtPhone.setText(t.getPhoneNo());
            if (txtEmail != null) txtEmail.setText(t.getEmail());
            setParsedNrc(t.getNrc());
        } finally {
            isUpdating = false;
        }
        showAlert("Success", "Form populated with imported data.", Alert.AlertType.INFORMATION);
    }

    private List<Teacher> parseCsvTeachers(File file) throws IOException {
        List<Teacher> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine();
            if (header == null) return list;
            String line;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                if (vals.length >= 2) {
                    list.add(new Teacher(1, vals[0].trim(), vals[1].trim(), "CEIT", "Tutor", "09-000000000", "", "Male", "Myanmar", "", "", "", "", "", "", "", "", "", DEFAULT_IMAGE_PATH));
                }
            }
        }
        return list;
    }

    private List<Teacher> parseXlsxTeachers(File file) { return new ArrayList<>(); }

    @FXML
    public void handleUploadPhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Faculty Passport Photo");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        File file = fc.showOpenDialog(txtTeacherId.getScene().getWindow());
        if (file != null) {
            updatedPhotoPath = file.toURI().toString();
            loadImage(updatedPhotoPath);
        }
    }

    private void loadImage(String path) {
        if (passportImageView == null || path == null || path.trim().isEmpty()) return;
        try {
            passportImageView.setImage(
                (path.startsWith("file:") || path.startsWith("http:"))
                    ? new Image(path, 86, 110, false, true)
                    : new Image(getClass().getResourceAsStream(path))
            );
        } catch (Exception e) {}
    }

    private String formatToYearMonthDay(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.trim().equals("-")) return "";
        String s = dateStr.trim();
        if (s.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            String[] parts = s.split("-");
            return String.format("%04d-%02d-%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        String delimiter = s.contains("-") ? "-" : (s.contains("/") ? "/" : null);
        if (delimiter != null) {
            String[] parts = s.split(Pattern.quote(delimiter));
            if (parts.length == 3 && parts[2].length() == 4) {
                return String.format("%04d-%02d-%02d", Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
        }
        return s;
    }

    private String findValidDepartment(String input) {
        if (input == null) return null;
        for (String d : VALID_DEPTS) {
            if (d.equalsIgnoreCase(input.trim())) return d;
        }
        return null;
    }

    private String findValidPosition(String input) {
        if (input == null) return null;
        for (String p : VALID_POSITIONS) {
            if (p.equalsIgnoreCase(input.trim())) return p;
        }
        return null;
    }

    @FXML
    public void handleSave() {
        if (currentTeacher == null) return;

        String rawCode = txtTeacherId != null ? txtTeacherId.getText().trim() : "";
        String rawName = txtName != null ? txtName.getText().trim() : "";
        String rawDept = comboDept != null && comboDept.getValue() != null ? comboDept.getValue().trim() : "";
        String rawPos = comboPosition != null && comboPosition.getValue() != null ? comboPosition.getValue().trim() : "";
        String rawPhone = txtPhone != null ? txtPhone.getText().trim() : "";
        String rawEmail = txtEmail != null ? txtEmail.getText().trim() : "";
        String rawDob = txtDob != null ? formatToYearMonthDay(txtDob.getText().trim()) : "";
        String rawNationality = txtNationality != null && !txtNationality.getText().trim().isEmpty()
                ? txtNationality.getText().trim() : "Myanmar";
        String rawNrc = getCombinedNrc();
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
                || rawPhone.isEmpty() || rawEmail.isEmpty() || rawDob.isEmpty()
                || rawCurrAddr.isEmpty() || rawPermAddr.isEmpty()
                || rawDegree.isEmpty() || rawUniversity.isEmpty() || rawYearComp.isEmpty()) {
            showAlert("Missing Information",
                    "Please fill in all required fields (Teacher ID, Full Name, DOB, Department, Position, NRC, Phone, Email, Addresses, Degree, University, and Year Completed).",
                    Alert.AlertType.ERROR);
            return;
        }

        if (!rawCode.matches("^[a-zA-Z]+-[a-zA-Z0-9]+-\\d+$")) {
            showAlert("Invalid Teacher ID Format", "Teacher ID must be in format: [Prefix]-[Department]-[Number] (e.g., TCH-CEIT-101).", Alert.AlertType.ERROR);
            return;
        }

        String[] parts = rawCode.split("-");
        String validatedDept = findValidDepartment(rawDept);
        String validatedPos = findValidPosition(rawPos);
        if (validatedDept == null || validatedPos == null) {
            showAlert("Invalid Selection", "Selected Department or Position is invalid.", Alert.AlertType.ERROR);
            return;
        }

        String standardizedCode = parts[0].trim() + "-" + validatedDept + "-" + parts[2].trim();

        if (!rawDob.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            showAlert("Invalid Date of Birth", "Date of Birth must be in YYYY-MM-DD format (e.g., 1985-05-15).", Alert.AlertType.ERROR);
            return;
        }
        if (!rawPhone.matches("^09-\\d+$")) {
            showAlert("Invalid Phone Number", "Phone number must be in the format 09-xxxxxxxx.", Alert.AlertType.ERROR);
            return;
        }
        if (!rawEmail.contains("@")) {
            showAlert("Invalid Email Address", "Email address must contain '@'.", Alert.AlertType.ERROR);
            return;
        }
        if (!rawEmergency.isEmpty() && !rawEmergency.matches("^09-\\d+$")) {
            showAlert("Invalid Emergency Contact", "Emergency contact must be in the format 09-xxxxxxxx.", Alert.AlertType.ERROR);
            return;
        }
        if (!rawYearComp.matches("^\\d{4}$")) {
            showAlert("Invalid Year Completed", "Year Completed must be a 4-digit year (e.g., 2012).", Alert.AlertType.ERROR);
            return;
        }

        currentTeacher.setTeacherCode(standardizedCode);
        currentTeacher.setName(rawName);
        currentTeacher.setDepartment(validatedDept);
        currentTeacher.setPosition(validatedPos);
        currentTeacher.setPhoneNo(rawPhone);
        currentTeacher.setDob(rawDob);
        currentTeacher.setGender(gender);
        currentTeacher.setNationality(rawNationality);
        currentTeacher.setNrc(rawNrc);
        currentTeacher.setEmail(rawEmail);
        currentTeacher.setCurrentAddress(rawCurrAddr);
        currentTeacher.setPermanentAddress(rawPermAddr);
        currentTeacher.setDegree(rawDegree);
        currentTeacher.setUniversity(rawUniversity);
        currentTeacher.setYearCompleted(rawYearComp);
        currentTeacher.setSpecialization(rawSpec);
        currentTeacher.setEmergencyContact(rawEmergency);
        if (updatedPhotoPath != null) currentTeacher.setPhotoPath(updatedPhotoPath);

        TeacherDAO teacherDAO = new TeacherDAO();
        boolean success = teacherDAO.modifyTeacher(currentTeacher);

        if (success) {
            if (TeacherController.masterData != null) {
                TeacherController.masterData.setAll(teacherDAO.getAllTeacher());
            }
            showAlert("Success", "Faculty record for '" + rawName + "' has been updated in the database.", Alert.AlertType.INFORMATION);
            handleReturn();
        } else {
            showAlert("Database Error", "Failed to update faculty record in the database. Please ensure the NRC is unique and valid.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleResetData() {
        if (currentTeacher != null) {
            int savedDisplayNo = currentTeacher.getDisplayNo();
            TeacherDAO teacherDAO = new TeacherDAO();
            Teacher fresh = teacherDAO.getFullTeacher(currentTeacher.getTeacherId());
            if (fresh != null) {
                fresh.setDisplayNo(savedDisplayNo);
                currentTeacher = fresh;
            }
            initData(currentTeacher);
            showAlert("Data Reset", "Form reset to original database record data.", Alert.AlertType.INFORMATION);
        }
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

    @FXML
    private void handleReturn() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("TeacherContents.fxml"));
            BorderPane mainLayout = (BorderPane) (topActionBar != null ? topActionBar.getScene() : txtTeacherId.getScene()).lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(view);
            } else {
                Parent root = txtTeacherId.getScene().getRoot();
                if (root instanceof BorderPane) ((BorderPane) root).setCenter(view);
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
        if (txtTeacherId != null && txtTeacherId.getScene() != null) {
            alert.initOwner(txtTeacherId.getScene().getWindow());
        }
        alert.showAndWait();
    }
}