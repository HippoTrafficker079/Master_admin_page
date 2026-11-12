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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TeacherModifyController {
    @FXML private Label lblFormTitle, lblPhotoAction;
    @FXML private TextField txtTeacherId, txtDob, txtName, txtNationality, txtNrc;
    @FXML private ComboBox<String> comboGender, comboDept, comboPosition;
    @FXML private TextField txtPhone, txtEmail, txtCurrentAddress, txtPermanentAddress;
    @FXML private TextField txtDegree, txtUniversity, txtYearCompleted, txtSpecialization, txtEmergency;
    @FXML private ImageView passportImageView;
    @FXML private HBox topActionBar;
    @FXML private VBox contentContainer;

    @FXML private TextField txtExcelPath;
    @FXML private VBox dropZone;

    private File selectedExcelFile = null;
    private Teacher currentTeacher;
    private boolean isUpdating = false;
    private String updatedPhotoPath = null;
    private HBox bannerBox = null;
    private final String DEFAULT_IMAGE_PATH = "/freddy.jpg";

    private final String[] VALID_DEPTS = {"CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex", "FE"};
    private final String[] VALID_POSITIONS = {"Professor & Head", "Professor", "Associate Professor", "Lecturer", "Assistant Lecturer", "Tutor"};

    @FXML
    public void initialize() {
        setupTeacherBidirectionalSync();
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
            
            String bannerText = String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                teacher.getTeacherId(), teacher.getTeacherCode(), teacher.getName(), teacher.getDepartment(), teacher.getPosition());

            Label bannerLabel = new Label(bannerText);
            bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
            bannerBox.getChildren().add(bannerLabel);
            contentContainer.getChildren().add(0, bannerBox);
        }

        isUpdating = true;
        try {
            if (txtTeacherId != null) txtTeacherId.setText(teacher.getTeacherCode());
            if (txtDob != null) txtDob.setText(teacher.getDob());
            if (txtName != null) txtName.setText(teacher.getName());
            
            if (comboGender != null) {
                comboGender.setItems(FXCollections.observableArrayList("Male", "Female"));
                comboGender.setValue((teacher.getGender() != null && teacher.getGender().equalsIgnoreCase("Female")) ? "Female" : "Male");
            }

            if (comboDept != null) {
                comboDept.setItems(FXCollections.observableArrayList(VALID_DEPTS));
                comboDept.setValue(teacher.getDepartment() != null ? teacher.getDepartment() : "CEIT");
            }

            if (txtNationality != null) txtNationality.setText(teacher.getNationality());

            if (comboPosition != null) {
                comboPosition.setItems(FXCollections.observableArrayList(VALID_POSITIONS));
                comboPosition.setValue(teacher.getPosition() != null ? teacher.getPosition() : "Tutor");
            }

            if (txtNrc != null) txtNrc.setText(teacher.getNrc());
            if (txtPhone != null) txtPhone.setText(teacher.getPhoneNo());
            if (txtEmail != null) txtEmail.setText(teacher.getEmail());
            if (txtCurrentAddress != null) txtCurrentAddress.setText(teacher.getCurrentAddress());
            if (txtPermanentAddress != null) txtPermanentAddress.setText(teacher.getPermanentAddress());

            if (txtDegree != null) txtDegree.setText(teacher.getDegree());
            if (txtUniversity != null) txtUniversity.setText(teacher.getUniversity());
            if (txtYearCompleted != null) txtYearCompleted.setText(teacher.getYearCompleted());
            if (txtSpecialization != null) txtSpecialization.setText(teacher.getSpecialization());
            if (txtEmergency != null) txtEmergency.setText(teacher.getEmergencyContact());
        } finally {
            isUpdating = false;
        }

        updatedPhotoPath = teacher.getPhotoPath();
        loadImage(updatedPhotoPath);
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
                                comboDept.setValue(d);
                                isUpdating = false;
                                break;
                            }
                        }
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

    @FXML
    public void handleDragExited(DragEvent event) {
        event.consume();
    }

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
            if (teachers.size() == 1) {
                populateFormWithTeacher(teachers.get(0));
            } else {
                openSingleTeacherPickerDialog(teachers);
            }
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

    private List<Teacher> parseXlsxTeachers(File file) {
        return new ArrayList<>();
    }

    @FXML
    public void handleUploadPhoto() {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(txtTeacherId.getScene().getWindow());
        if (file != null) {
            updatedPhotoPath = file.toURI().toString();
            loadImage(updatedPhotoPath);
        }
    }

    private void loadImage(String path) {
        if (passportImageView == null || path == null) return;
        try {
            passportImageView.setImage(path.startsWith("file:") ? new Image(path, 86, 110, false, true) : new Image(getClass().getResourceAsStream(path)));
        } catch (Exception e) {}
    }

    @FXML
    public void handleSave() {
        if (currentTeacher == null) return;
        currentTeacher.setTeacherCode(txtTeacherId.getText().trim());
        currentTeacher.setName(txtName.getText().trim());
        currentTeacher.setDepartment(comboDept.getValue());
        currentTeacher.setPosition(comboPosition.getValue());
        currentTeacher.setPhoneNo(txtPhone.getText().trim());
        if (updatedPhotoPath != null) currentTeacher.setPhotoPath(updatedPhotoPath);
        handleReturn();
    }

    @FXML
    public void handleResetData() {
        if (currentTeacher != null) {
            initData(currentTeacher);
            showAlert("Data Reset", "Form reset to original record data.", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleReturn() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("TeacherContents.fxml"));
            BorderPane mainLayout = (BorderPane) topActionBar.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) mainLayout.setCenter(view);
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