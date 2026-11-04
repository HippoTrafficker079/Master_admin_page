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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TeacherDetailController {

    @FXML private Label lblFormTitle, lblPhotoAction;
    @FXML private Label lblTeacherId, lblDob, lblName, lblGender, lblDept, lblNationality, lblPosition, lblNrc;
    @FXML private Label lblPhone, lblEmail, lblCurrentAddress, lblPermanentAddress;
    @FXML private Label lblDegree, lblUniversity, lblYearCompleted, lblSpecialization, lblEmergency;

    @FXML private GridPane gridTeacherDetails;
    @FXML private GridPane gridContactInfo;
    @FXML private GridPane gridAcademicInfo;

    private TextField txtTeacherId, txtDob, txtName, txtNationality, txtNrc;
    private ComboBox<String> comboGender;
    private ComboBox<String> comboDept;
    private ComboBox<String> comboPosition;

    private TextField txtPhone, txtEmail, txtCurrentAddress, txtPermanentAddress;
    private TextField txtDegree, txtUniversity, txtYearCompleted, txtSpecialization, txtEmergency;

    @FXML private ImageView passportImageView;
    @FXML private Button btnSave;
    @FXML private Button btnReset;
    @FXML private HBox topActionBar;
    @FXML private HBox bottomActionBar;
    @FXML private VBox contentContainer;

    @FXML private VBox importBox;
    @FXML private TextField txtExcelPath;
    @FXML private VBox dropZone;

    private Teacher currentTeacher;
    private boolean isEditMode = false;
    private boolean isAddMode = false;
    private boolean isUpdating = false;
    private File selectedExcelFile = null;
    private String updatedPhotoPath = null;
    private HBox bannerBox = null;
    private final String DEFAULT_IMAGE_PATH = "/freddy.jpg";

    private final String[] VALID_DEPTS = {"CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex", "FE"};
    private final String[] VALID_POSITIONS = {"Professor & Head", "Professor", "Associate Professor", "Lecturer", "Assistant Lecturer", "Tutor"};

    public void initData(Teacher teacher) {
        initData(teacher, false);
    }

    public void initData(Teacher teacher, boolean editMode) {
        this.currentTeacher = teacher;
        this.isEditMode = editMode;
        this.isAddMode = false;

        if (importBox != null) {
            importBox.setVisible(false);
            importBox.setManaged(false);
        }

        if (lblFormTitle != null) {
            lblFormTitle.setText(editMode ? "FACULTY / TEACHER INFORMATION FORM (MODIFY)" : "FACULTY / TEACHER INFORMATION FORM (VIEW)");
        }
        if (lblPhotoAction != null) {
            lblPhotoAction.setVisible(editMode);
            lblPhotoAction.setManaged(editMode);
        }

        if (contentContainer != null && teacher != null) {
            if (bannerBox != null && contentContainer.getChildren().contains(bannerBox)) {
                contentContainer.getChildren().remove(bannerBox);
            }
            bannerBox = new HBox();
            bannerBox.setStyle("-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 18;");
            
            String bannerText = String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                teacher.getTeacherId(),
                teacher.getTeacherCode(),
                teacher.getName(),
                teacher.getDepartment(),
                teacher.getPosition()
            );

            Label bannerLabel = new Label(bannerText);
            bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
            bannerBox.getChildren().add(bannerLabel);
            contentContainer.getChildren().add(0, bannerBox);
        }

        if (!isEditMode) {
            lblTeacherId.setText(teacher.getTeacherCode());
            lblDob.setText(formatToYearMonthDay(teacher.getDob()));
            lblName.setText(teacher.getName());
            lblGender.setText(teacher.getGender());
            lblDept.setText(teacher.getDepartment());
            lblNationality.setText(teacher.getNationality());
            lblPosition.setText(teacher.getPosition());
            lblNrc.setText(teacher.getNrc());
            
            lblPhone.setText(teacher.getPhoneNo());
            lblEmail.setText(teacher.getEmail());
            lblCurrentAddress.setText(teacher.getCurrentAddress());
            lblPermanentAddress.setText(teacher.getPermanentAddress());
            
            lblDegree.setText(teacher.getDegree());
            lblUniversity.setText(teacher.getUniversity());
            lblYearCompleted.setText(teacher.getYearCompleted());
            lblSpecialization.setText(teacher.getSpecialization());
            lblEmergency.setText(teacher.getEmergencyContact());

            if (bottomActionBar != null) { bottomActionBar.setVisible(false); bottomActionBar.setManaged(false); }
            if (btnReset != null) { btnReset.setVisible(false); btnReset.setManaged(false); }
        } else {
            createTextFields(teacher);
            setupTeacherBidirectionalSync();

            if (bottomActionBar != null) { bottomActionBar.setVisible(true); bottomActionBar.setManaged(true); }
            if (btnSave != null) {
                btnSave.setText("Save Changes");
                btnSave.setVisible(true);
                btnSave.setManaged(true);
            }
            if (btnReset != null) {
                btnReset.setVisible(true);
                btnReset.setManaged(true);
            }
        }

        loadImage(teacher.getPhotoPath());
    }

    private void setupTeacherBidirectionalSync() {
        if (!isEditMode && !isAddMode) return;

        if (txtTeacherId != null) {
            txtTeacherId.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && !isUpdating) syncDeptFromTeacherId();
            });
            txtTeacherId.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null && newVal.contains("-")) {
                    String[] parts = newVal.trim().split("-");
                    if (parts.length >= 2) {
                        String deptCode = findValidDepartment(parts[1].trim());
                        if (deptCode != null && comboDept != null) {
                            isUpdating = true;
                            try {
                                comboDept.setValue(deptCode);
                            } finally {
                                isUpdating = false;
                            }
                        }
                    }
                }
            });
        }

        if (comboDept != null) {
            comboDept.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdating && newVal != null) syncTeacherIdFromDept();
            });
        }
    }

    private void syncDeptFromTeacherId() {
        if (txtTeacherId == null || txtTeacherId.getText() == null) return;
        String raw = txtTeacherId.getText().trim();
        String[] parts = raw.split("-");
        if (parts.length >= 2) {
            String deptCode = findValidDepartment(parts[1].trim());
            if (deptCode != null && comboDept != null) {
                isUpdating = true;
                try {
                    comboDept.setValue(deptCode);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }

    private void syncTeacherIdFromDept() {
        if (comboDept == null || txtTeacherId == null) return;
        String validDept = findValidDepartment(comboDept.getValue());
        if (validDept != null) {
            String currentCode = txtTeacherId.getText() != null ? txtTeacherId.getText().trim() : "";
            if (currentCode.contains("-")) {
                String[] parts = currentCode.split("-");
                String prefix = parts[0].trim();
                String num = parts[parts.length - 1].trim();

                isUpdating = true;
                try {
                    txtTeacherId.setText(prefix + "-" + validDept + "-" + num);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }

    public void initAddMode() {
        this.currentTeacher = null;
        this.isEditMode = false;
        this.isAddMode = true;

        if (importBox != null) {
            importBox.setVisible(true);
            importBox.setManaged(true);
        }

        if (lblFormTitle != null) {
            lblFormTitle.setText("FACULTY / TEACHER INFORMATION FORM (ADD NEW TEACHER)");
        }
        if (lblPhotoAction != null) {
            lblPhotoAction.setText("📷 Upload Photo");
            lblPhotoAction.setVisible(true);
            lblPhotoAction.setManaged(true);
        }

        Teacher blankTeacher = new Teacher(
            TeacherController.masterData != null ? TeacherController.masterData.size() + 1 : 1,
            "TCH-CEIT-101", "", "CEIT", "Tutor", "",
            "", "Male", "Myanmar", "", "",
            "", "", "", "", "", "", "", DEFAULT_IMAGE_PATH
        );

        createTextFields(blankTeacher);
        setupTeacherBidirectionalSync();

        txtTeacherId.setText("");
        txtTeacherId.setPromptText("e.g. TCH-CEIT-101");

        String sessionUser = Session.getUsername();
        String sessionEmail = Session.getEmail();
        txtName.setText(sessionUser != null && !sessionUser.isEmpty() ? sessionUser : "");
        txtName.setPromptText("Faculty Full Name");
        txtEmail.setText(sessionEmail != null && !sessionEmail.isEmpty() ? sessionEmail : "");
        txtEmail.setPromptText("teacher@ytu.edu.mm");

        txtPhone.setText("");
        txtPhone.setPromptText("09-231231231");
        txtEmergency.setText("");
        txtEmergency.setPromptText("09-231231231");

        if (bottomActionBar != null) { bottomActionBar.setVisible(true); bottomActionBar.setManaged(true); }
        if (btnSave != null) {
            btnSave.setText("+ Add New Teacher");
            btnSave.setVisible(true);
            btnSave.setManaged(true);
        }
        if (btnReset != null) {
            btnReset.setText("Reset Data");
            btnReset.setVisible(true);
            btnReset.setManaged(true);
        }

        loadImage(DEFAULT_IMAGE_PATH);
    }

    private void createTextFields(Teacher teacher) {
        replaceLabelWithControl(lblTeacherId, txtTeacherId = new TextField(teacher != null ? teacher.getTeacherCode() : ""));
        replaceLabelWithControl(lblDob, txtDob = new TextField(teacher != null ? formatToYearMonthDay(teacher.getDob()) : ""));
        replaceLabelWithControl(lblName, txtName = new TextField(teacher != null ? teacher.getName() : ""));

        comboGender = new ComboBox<>(FXCollections.observableArrayList("Male", "Female"));
        comboGender.setMaxWidth(Double.MAX_VALUE);
        String g = (teacher != null && teacher.getGender() != null && teacher.getGender().equalsIgnoreCase("Female")) ? "Female" : "Male";
        comboGender.setValue(g);
        replaceLabelWithControl(lblGender, comboGender);

        comboDept = new ComboBox<>(FXCollections.observableArrayList(VALID_DEPTS));
        comboDept.setMaxWidth(Double.MAX_VALUE);
        String d = teacher != null ? findValidDepartment(teacher.getDepartment()) : "CEIT";
        comboDept.setValue(d != null ? d : "CEIT");
        replaceLabelWithControl(lblDept, comboDept);

        replaceLabelWithControl(lblNationality, txtNationality = new TextField(teacher != null ? teacher.getNationality() : "Myanmar"));

        comboPosition = new ComboBox<>(FXCollections.observableArrayList(VALID_POSITIONS));
        comboPosition.setMaxWidth(Double.MAX_VALUE);
        String pos = teacher != null ? findValidPosition(teacher.getPosition()) : "Tutor";
        comboPosition.setValue(pos != null ? pos : "Tutor");
        replaceLabelWithControl(lblPosition, comboPosition);

        replaceLabelWithControl(lblNrc, txtNrc = new TextField(teacher != null ? teacher.getNrc() : ""));

        replaceLabelWithControl(lblPhone, txtPhone = new TextField(teacher != null ? teacher.getPhoneNo() : ""));
        replaceLabelWithControl(lblEmail, txtEmail = new TextField(teacher != null ? teacher.getEmail() : ""));
        replaceLabelWithControl(lblCurrentAddress, txtCurrentAddress = new TextField(teacher != null ? teacher.getCurrentAddress() : ""));
        replaceLabelWithControl(lblPermanentAddress, txtPermanentAddress = new TextField(teacher != null ? teacher.getPermanentAddress() : ""));

        replaceLabelWithControl(lblDegree, txtDegree = new TextField(teacher != null ? teacher.getDegree() : ""));
        replaceLabelWithControl(lblUniversity, txtUniversity = new TextField(teacher != null ? teacher.getUniversity() : ""));
        replaceLabelWithControl(lblYearCompleted, txtYearCompleted = new TextField(teacher != null ? teacher.getYearCompleted() : ""));
        replaceLabelWithControl(lblSpecialization, txtSpecialization = new TextField(teacher != null ? teacher.getSpecialization() : ""));
        replaceLabelWithControl(lblEmergency, txtEmergency = new TextField(teacher != null ? teacher.getEmergencyContact() : ""));

        txtPhone.setPromptText("09-231231231");
        txtEmail.setPromptText("teacher@ytu.edu.mm");
        txtEmergency.setPromptText("09-231231231");
        txtDob.setPromptText("YYYY-MM-DD");
        txtYearCompleted.setPromptText("e.g. 2015");
    }

    private void replaceLabelWithControl(Label label, javafx.scene.Node control) {
        if (label != null && label.getParent() instanceof GridPane) {
            GridPane grid = (GridPane) label.getParent();
            Integer col = GridPane.getColumnIndex(label);
            Integer row = GridPane.getRowIndex(label);

            grid.getChildren().remove(label);
            control.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
            grid.add(control, col != null ? col : 0, row != null ? row : 0);
        }
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

    @FXML
    public void handleBrowseExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Faculty Excel / CSV File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Spreadsheet & CSV Files (*.xlsx, *.csv)", "*.xlsx", "*.csv"),
            new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        Stage stage = (Stage) topActionBar.getScene().getWindow();
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
        if (dropZone != null && event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
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
        if (dropZone != null) {
            dropZone.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1.5; -fx-border-style: dashed; -fx-border-radius: 6; -fx-background-color: #f8fafc; -fx-background-radius: 6; -fx-cursor: hand;");
        }
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
                    handleImportExcel();
                    break;
                }
            }
        }

        event.setDropCompleted(success);
        if (dropZone != null) {
            dropZone.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1.5; -fx-border-style: dashed; -fx-border-radius: 6; -fx-background-color: #f8fafc; -fx-cursor: hand;");
        }
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
            if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                importFromCsv(selectedExcelFile);
            } else if (fileName.endsWith(".xlsx")) {
                boolean success = importFromNativeXlsx(selectedExcelFile);
                if (!success) {
                    showAlert("Import Notice", "Could not parse data from .xlsx file. Please try the .csv template instead.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Unsupported File", "Please select a supported file (.xlsx or .csv).", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Import Error", "Failed to parse spreadsheet: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void importFromCsv(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                showAlert("Empty File", "The selected file is empty.", Alert.AlertType.WARNING);
                return;
            }

            String delimiter = headerLine.contains(",") ? "," : (headerLine.contains("\t") ? "\t" : ";");
            String[] headers = headerLine.split(delimiter);

            String dataLine = br.readLine();
            if (dataLine == null) {
                showAlert("No Data Row", "The selected file does not contain a row of faculty data below the header.", Alert.AlertType.WARNING);
                return;
            }

            String[] values = dataLine.split(delimiter, -1);
            Map<String, String> map = new HashMap<>();

            for (int i = 0; i < headers.length && i < values.length; i++) {
                String cleanHeader = headers[i].replace("\"", "").trim().toLowerCase();
                String cleanVal = values[i].replace("\"", "").trim();
                map.put(cleanHeader, cleanVal);
            }

            populateTeacherFieldsFromMap(map, values);
            showAlert("Import Successful", "Teacher data from " + file.getName() + " has been loaded into the form.", Alert.AlertType.INFORMATION);
        }
    }

    private boolean importFromNativeXlsx(File file) {
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
            if (sheetEntry == null) return false;

            String sheetXml = new String(zip.getInputStream(sheetEntry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher rowMatcher = Pattern.compile("<row[^>]*>(.*?)</row>", Pattern.DOTALL).matcher(sheetXml);

            List<String> headers = null;
            List<String> values = null;

            if (rowMatcher.find()) {
                headers = extractCellsFromRow(rowMatcher.group(1), sharedStrings);
            }
            if (rowMatcher.find()) {
                values = extractCellsFromRow(rowMatcher.group(1), sharedStrings);
            }

            if (headers == null || values == null || headers.isEmpty()) return false;

            Map<String, String> map = new HashMap<>();
            String[] rawValues = new String[headers.size()];

            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i).trim().toLowerCase();
                String v = i < values.size() ? values.get(i).trim() : "";
                map.put(h, v);
                rawValues[i] = v;
            }

            populateTeacherFieldsFromMap(map, rawValues);
            showAlert("Import Successful", "Faculty data from Excel file (" + file.getName() + ") has been loaded into the form.", Alert.AlertType.INFORMATION);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
            if (rMatcher.find()) {
                colIndex = getColIndex(rMatcher.group(1));
            }

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
                if (isMatcher.find()) {
                    val = isMatcher.group(1).trim();
                }
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

    private void populateTeacherFieldsFromMap(Map<String, String> map, String[] rawOrder) {
        if (txtTeacherId != null) txtTeacherId.setText(getMapOrIndex(map, rawOrder, 0, "teacher id", "id", "teacherid", "employee id", "teacher code"));
        if (txtName != null) txtName.setText(getMapOrIndex(map, rawOrder, 1, "name", "full name", "fullname", "teacher name"));
        
        String importedDept = getMapOrIndex(map, rawOrder, 2, "department", "dept");
        String normDept = findValidDepartment(importedDept);
        if (comboDept != null && normDept != null) comboDept.setValue(normDept);

        String importedPos = getMapOrIndex(map, rawOrder, 3, "position", "rank", "title");
        String normPos = findValidPosition(importedPos);
        if (comboPosition != null && normPos != null) comboPosition.setValue(normPos);

        if (txtPhone != null) txtPhone.setText(getMapOrIndex(map, rawOrder, 4, "phone", "phone number", "phone-no", "mobile"));
        if (txtDob != null) txtDob.setText(formatToYearMonthDay(getMapOrIndex(map, rawOrder, 5, "dob", "date of birth", "birthdate")));
        
        String sex = getMapOrIndex(map, rawOrder, 6, "gender", "sex");
        if (comboGender != null) {
            comboGender.setValue(sex.equalsIgnoreCase("Female") ? "Female" : "Male");
        }
        
        String nat = getMapOrIndex(map, rawOrder, 7, "nationality", "country");
        if (txtNationality != null) txtNationality.setText(!nat.isEmpty() ? nat : "Myanmar");
        
        if (txtNrc != null) txtNrc.setText(getMapOrIndex(map, rawOrder, 8, "nrc", "nrc / id no.", "nrc number"));
        if (txtEmail != null) txtEmail.setText(getMapOrIndex(map, rawOrder, 9, "email", "email address"));
        if (txtCurrentAddress != null) txtCurrentAddress.setText(getMapOrIndex(map, rawOrder, 10, "current address", "address"));
        if (txtPermanentAddress != null) txtPermanentAddress.setText(getMapOrIndex(map, rawOrder, 11, "permanent address"));
        
        if (txtDegree != null) txtDegree.setText(getMapOrIndex(map, rawOrder, 12, "highest degree", "degree"));
        if (txtUniversity != null) txtUniversity.setText(getMapOrIndex(map, rawOrder, 13, "university", "institution", "college"));
        if (txtYearCompleted != null) txtYearCompleted.setText(getMapOrIndex(map, rawOrder, 14, "year completed", "graduation year"));
        if (txtSpecialization != null) txtSpecialization.setText(getMapOrIndex(map, rawOrder, 15, "specialization", "area", "major"));
        if (txtEmergency != null) txtEmergency.setText(getMapOrIndex(map, rawOrder, 16, "emergency contact", "emergency phone", "emergency"));
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
    public void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Faculty Passport Photo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        if (passportImageView != null && passportImageView.getScene() != null) {
            Stage stage = (Stage) passportImageView.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                updatedPhotoPath = file.toURI().toString();
                loadImage(updatedPhotoPath);
            }
        }
    }

    private void loadImage(String path) {
        if (passportImageView == null) return;
        if (path != null && !path.trim().isEmpty()) {
            try {
                Image img = (path.startsWith("file:") || path.startsWith("http:"))
                        ? new Image(path, 86, 110, false, true)
                        : new Image(getClass().getResourceAsStream(path));
                if (img != null) {
                    passportImageView.setImage(img);
                }
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
        String rawEmergency = txtEmergency != null ? txtEmergency.getText().trim() : "";
        String rawYearComp = txtYearCompleted != null ? txtYearCompleted.getText().trim() : "";
        String gender = comboGender != null && comboGender.getValue() != null ? comboGender.getValue().trim() : "Male";

        if (rawCode.isEmpty() || rawName.isEmpty() || rawDept.isEmpty() || rawPos.isEmpty() || rawPhone.isEmpty()) {
            showAlert("Missing Information", "Please fill in Teacher ID, Full Name, Department, Position, and Phone Number.", Alert.AlertType.ERROR);
            return;
        }

        if (!rawCode.matches("^[a-zA-Z]+-[a-zA-Z0-9]+-\\d+$")) {
            showAlert("Invalid Teacher ID Format", "Teacher ID must be in the format: [Prefix]-[Department]-[Number] (e.g., TCH-CEIT-101).", Alert.AlertType.ERROR);
            return;
        }

        String[] parts = rawCode.split("-");
        String idPrefix = parts[0].trim();
        String idDeptCode = parts[1].trim();
        String idNum = parts[2].trim();

        String deptFromCode = findValidDepartment(idDeptCode);
        if (deptFromCode == null) {
            showAlert("Invalid Department in Teacher ID", "The department code '" + idDeptCode + "' in Teacher ID is invalid.", Alert.AlertType.ERROR);
            return;
        }

        String validatedDept = findValidDepartment(rawDept);
        if (validatedDept == null) {
            showAlert("Invalid Department", "Selected department is invalid.", Alert.AlertType.ERROR);
            return;
        }

        String validatedPos = findValidPosition(rawPos);
        if (validatedPos == null) {
            showAlert("Invalid Position", "Selected position is invalid.", Alert.AlertType.ERROR);
            return;
        }

        String standardizedCode = rawCode;
        if (isAddMode) {
            if (!deptFromCode.equalsIgnoreCase(validatedDept)) {
                showAlert("Detail Mismatch Error", 
                    String.format("Mismatch detected between Teacher ID and Department:\n\n• Teacher ID indicates Department '%s' but selected Department is '%s'.\n\nPlease correct either the Teacher ID or Department.", idDeptCode, validatedDept), 
                    Alert.AlertType.ERROR);
                return;
            }
            standardizedCode = idPrefix + "-" + validatedDept + "-" + idNum;
        } else if (isEditMode) {
            if (!deptFromCode.equalsIgnoreCase(validatedDept)) {
                validatedDept = deptFromCode;
            }
            standardizedCode = idPrefix + "-" + validatedDept + "-" + idNum;
        }

        isUpdating = true;
        try {
            txtTeacherId.setText(standardizedCode);
            comboDept.setValue(validatedDept);
            comboPosition.setValue(validatedPos);
        } finally {
            isUpdating = false;
        }

        if (!rawDob.isEmpty() && !rawDob.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            showAlert("Invalid Date of Birth", "Date of Birth must be in the format: Year-Month-Day (e.g., 1985-05-15) with no letters.", Alert.AlertType.ERROR);
            return;
        }

        if (!rawPhone.matches("^09-\\d+$")) {
            showAlert("Invalid Phone Number", "Teacher phone number must be in the format 09-xxxxxxxx (starting with 09- followed by numbers).", Alert.AlertType.ERROR);
            return;
        }
        if (!rawEmail.isEmpty() && !rawEmail.contains("@")) {
            showAlert("Invalid Email Address", "Email address must contain '@'.", Alert.AlertType.ERROR);
            return;
        }
        if (!rawEmergency.isEmpty() && !rawEmergency.matches("^09-\\d+$")) {
            showAlert("Invalid Emergency Contact", "Emergency contact must be in the format 09-xxxxxxxx (starting with 09- followed by numbers).", Alert.AlertType.ERROR);
            return;
        }
        if (!rawYearComp.isEmpty() && !rawYearComp.matches("^\\d{4}$")) {
            showAlert("Invalid Year Completed", "Year Completed must be a 4-digit year (e.g., 2012).", Alert.AlertType.ERROR);
            return;
        }

        String finalPhoto = updatedPhotoPath != null ? updatedPhotoPath : (currentTeacher != null ? currentTeacher.getPhotoPath() : DEFAULT_IMAGE_PATH);

        if (isAddMode) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Addition");
            confirmAlert.setHeaderText("Add New Faculty Member");
            confirmAlert.setContentText("Are you sure you want to register this teacher into the system?");
            confirmAlert.getDialogPane().setMinWidth(480);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) return;

            int newId = TeacherController.masterData != null ? TeacherController.masterData.size() + 1 : 1;
            Teacher newTeacher = new Teacher(
                newId, standardizedCode, rawName, validatedDept, validatedPos, rawPhone,
                rawDob, gender, txtNationality.getText().trim(),
                txtNrc.getText().trim(), rawEmail, txtCurrentAddress.getText().trim(), txtPermanentAddress.getText().trim(),
                txtDegree.getText().trim(), txtUniversity.getText().trim(), rawYearComp, txtSpecialization.getText().trim(),
                rawEmergency, finalPhoto
            );

            if (TeacherController.masterData != null) {
                TeacherController.masterData.add(newTeacher);
            }
            Session.clear();
        } else if (isEditMode && currentTeacher != null) {
            currentTeacher.setTeacherCode(standardizedCode);
            currentTeacher.setName(rawName);
            currentTeacher.setDepartment(validatedDept);
            currentTeacher.setPosition(validatedPos);
            currentTeacher.setPhoneNo(rawPhone);
            currentTeacher.setDob(rawDob);
            currentTeacher.setGender(gender);
            currentTeacher.setNationality(txtNationality.getText().trim());
            currentTeacher.setNrc(txtNrc.getText().trim());
            currentTeacher.setEmail(rawEmail);
            currentTeacher.setCurrentAddress(txtCurrentAddress.getText().trim());
            currentTeacher.setPermanentAddress(txtPermanentAddress.getText().trim());
            currentTeacher.setDegree(txtDegree.getText().trim());
            currentTeacher.setUniversity(txtUniversity.getText().trim());
            currentTeacher.setYearCompleted(rawYearComp);
            currentTeacher.setSpecialization(txtSpecialization.getText().trim());
            currentTeacher.setEmergencyContact(rawEmergency);
            if (updatedPhotoPath != null) {
                currentTeacher.photoPathProperty().set(updatedPhotoPath);
            }
        }

        handleReturn();
    }

    @FXML
    public void handleResetData() {
        if (isAddMode) {
            if (txtTeacherId != null) txtTeacherId.clear();
            if (txtName != null) txtName.clear();
            if (comboDept != null) comboDept.setValue("CEIT");
            if (comboPosition != null) comboPosition.setValue("Tutor");
            if (txtPhone != null) txtPhone.clear();
            if (txtDob != null) txtDob.clear();
            if (comboGender != null) comboGender.setValue("Male");
            if (txtNationality != null) txtNationality.setText("Myanmar");
            if (txtNrc != null) txtNrc.clear();
            if (txtEmail != null) txtEmail.clear();
            if (txtCurrentAddress != null) txtCurrentAddress.clear();
            if (txtPermanentAddress != null) txtPermanentAddress.clear();
            if (txtDegree != null) txtDegree.clear();
            if (txtUniversity != null) txtUniversity.clear();
            if (txtYearCompleted != null) txtYearCompleted.clear();
            if (txtSpecialization != null) txtSpecialization.clear();
            if (txtEmergency != null) txtEmergency.clear();
            if (txtExcelPath != null) txtExcelPath.clear();
            selectedExcelFile = null;
            updatedPhotoPath = null;
            loadImage(DEFAULT_IMAGE_PATH);
            showAlert("Data Reset", "Teacher form inputs have been cleared.", Alert.AlertType.INFORMATION);
        } else if (currentTeacher != null) {
            TeacherDAO teacherDAO = new TeacherDAO();
            Teacher fresh = teacherDAO.getFullTeacher(currentTeacher.getTeacherId());
            if (fresh != null) {
                currentTeacher = fresh;
            } else {
                currentTeacher.resetToOriginal();
            }
            initData(currentTeacher, isEditMode);
            showAlert("Data Reset", "Faculty record has been reset to its database state.", Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    private void handleReturn() {
        Session.clear();
        try {
            Parent teacherView = FXMLLoader.load(getClass().getResource("TeacherContents.fxml"));
            BorderPane mainLayout = (BorderPane) topActionBar.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(teacherView);
            } else {
                Parent root = topActionBar.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(teacherView);
                }
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
        if (topActionBar != null && topActionBar.getScene() != null) {
            alert.initOwner(topActionBar.getScene().getWindow());
        }
        alert.showAndWait();
    }
}