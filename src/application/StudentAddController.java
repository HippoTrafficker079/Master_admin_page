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

public class StudentAddController {

    @FXML private TextField txtRollNo, txtDob, txtName, txtMajor, txtNationality, txtNrc;
    @FXML private ComboBox<String> comboGender;
    @FXML private ComboBox<String> comboYear;
    @FXML private ComboBox<String> comboSemester;

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

    private final String[] VALID_MAJORS = {"CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex", "FE"};
    private final String[] VALID_YEARS = {"First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"};

    @FXML
    public void initialize() {
        if (comboSemester != null && comboSemester.getValue() == null) {
            comboSemester.setValue("First Semester");
        }
        if (comboYear != null && comboYear.getValue() == null) {
            comboYear.setValue("First Year");
        }
        if (comboGender != null && comboGender.getValue() == null) {
            comboGender.setValue("Male");
        }

        setupBidirectionalSync();
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
        if (txtMajor != null) {
            txtMajor.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && !isUpdating) syncToRollNo();
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
                    if (txtMajor != null) txtMajor.setText(validMajor);
                    if (comboYear != null) comboYear.setValue(fullYear);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }

    private void syncToRollNo() {
        if (txtRollNo == null || comboYear == null || txtMajor == null) return;
        String rawYear = comboYear.getValue() != null ? comboYear.getValue().trim() : "";
        String rawMajor = txtMajor.getText() != null ? txtMajor.getText().trim() : "";

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
                txtMajor.setText(validMajor);
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
            new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

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
                    handleImportExcel();
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
                if (StudentController.masterData != null) {
                    int startingNo = StudentController.masterData.size() + 1;
                    for (Student s : students) {
                        s.setNo(startingNo++);
                        StudentController.masterData.add(s);
                    }
                }
                showAlert("Success", String.format("Successfully registered %d student(s) into the system.", students.size()), Alert.AlertType.INFORMATION);
                handleReturn();
            } else if (result.get() == btnPickOne) {
                openSingleStudentPickerDialog(students);
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
        isUpdating = true;
        try {
            if (txtRollNo != null) txtRollNo.setText(s.getRollNo());
            if (txtDob != null) txtDob.setText(s.getDob());
            if (txtName != null) txtName.setText(s.getName());
            if (comboGender != null) comboGender.setValue(s.getGender().equalsIgnoreCase("Female") ? "Female" : "Male");
            if (txtMajor != null) txtMajor.setText(s.getMajor());
            if (txtNationality != null) txtNationality.setText(s.getNationality());
            if (comboYear != null) comboYear.setValue(findValidYear(s.getYear()));
            if (comboSemester != null) comboSemester.setValue(findValidSemester(s.getSemester()));
            if (txtNrc != null) txtNrc.setText(s.getNrc());

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

        showAlert("Student Loaded", "Details for " + s.getName() + " (" + s.getRollNo() + ") have been populated into the form. You can review or edit before saving.", Alert.AlertType.INFORMATION);
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

        String semester = findValidSemester(getMapOrIndex(map, rawOrder, 4, "semester", "sem"));
        if (semester == null) semester = "First Semester";

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

        return new Student(seqNo, rollNo, name, major, year, semester, phone,
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

    private String findValidSemester(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        if (s.equals("ii") || s.equals("2") || s.equals("second semester") || s.equals("semester ii") || s.equals("semester 2")) return "Second Semester";
        if (s.equals("i") || s.equals("1") || s.equals("first semester") || s.equals("semester i") || s.equals("semester 1")) return "First Semester";
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
            new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

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
        String rawMajor = txtMajor != null ? txtMajor.getText().trim() : "";
        String rawSem = comboSemester != null && comboSemester.getValue() != null ? comboSemester.getValue().trim() : "";
        String phoneInput = txtPhone != null ? txtPhone.getText().trim() : "";
        String emailInput = txtEmail != null ? txtEmail.getText().trim() : "";
        String dobInput = txtDob != null ? txtDob.getText().trim() : "";
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
            showAlert("Invalid Major", "Entered Major '" + rawMajor + "' does not match any valid Major:\nCE, ME, EC, EP, CEIT, MC, ChE, Arch, PE, MnE, Met, Tex, FE.", Alert.AlertType.ERROR);
            return;
        }

        String validatedYear = findValidYear(rawYear);
        if (validatedYear == null) {
            showAlert("Invalid Year", "Entered Year '" + rawYear + "' is invalid.", Alert.AlertType.ERROR);
            return;
        }

        String validatedSemester = findValidSemester(rawSem);
        if (validatedSemester == null) {
            showAlert("Invalid Semester", "Please select a valid semester.", Alert.AlertType.ERROR);
            return;
        }

        String[] rollParts = rawRollNo.split("-");
        String rollYearRoman = rollParts[0].trim().toUpperCase();
        String rollMajor = rollParts[1].trim();
        String rollNumber = rollParts[2].trim();
        String expectedYearRoman = getRomanYear(validatedYear);

        boolean yearMismatch = !rollYearRoman.equalsIgnoreCase(expectedYearRoman);
        boolean majorMismatch = !rollMajor.equalsIgnoreCase(validatedMajor);

        if (yearMismatch || majorMismatch) {
            StringBuilder errorMsg = new StringBuilder("Mismatch detected between Roll No and entered student details:\n\n");
            if (yearMismatch) {
                errorMsg.append(String.format("• Year Mismatch: Roll No indicates '%s' but Year is '%s' (%s).\n", rollYearRoman, validatedYear, expectedYearRoman));
            }
            if (majorMismatch) {
                errorMsg.append(String.format("• Major Mismatch: Roll No indicates '%s' but Major is '%s'.\n", rollMajor, validatedMajor));
            }
            errorMsg.append("\nPlease correct either the Roll No or the corresponding Year / Major.");
            showAlert("Detail Mismatch Error", errorMsg.toString(), Alert.AlertType.ERROR);
            return;
        }

        String standardizedRollNo = expectedYearRoman + "-" + validatedMajor + "-" + rollNumber;
        txtRollNo.setText(standardizedRollNo);

        if (!dobInput.isEmpty() && !dobInput.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
            showAlert("Invalid Date of Birth", "Date of Birth must be in the format: Year-Month-Day (e.g., 2003-12-11) with no letters.", Alert.AlertType.ERROR);
            return;
        }

        if (!phoneInput.matches("^09-\\d+$")) {
            showAlert("Invalid Phone Number", "Student phone number must be in the format 09-xxxxxxxx (starting with 09- followed by numbers).", Alert.AlertType.ERROR);
            return;
        }
        if (!emailInput.contains("@")) {
            showAlert("Invalid Email Address", "Student email address must contain '@'.", Alert.AlertType.ERROR);
            return;
        }

        if (!fPhoneInput.isEmpty() && !fPhoneInput.matches("^09-\\d+$")) {
            showAlert("Invalid Father's Phone Number", "Father's phone number must be in the format 09-xxxxxxxx.", Alert.AlertType.ERROR);
            return;
        }
        if (!fEmailInput.isEmpty() && !fEmailInput.contains("@")) {
            showAlert("Invalid Father's Email", "Father's email address must contain '@'.", Alert.AlertType.ERROR);
            return;
        }

        if (!mPhoneInput.isEmpty() && !mPhoneInput.matches("^09-\\d+$")) {
            showAlert("Invalid Mother's Phone Number", "Mother's phone number must be in the format 09-xxxxxxxx.", Alert.AlertType.ERROR);
            return;
        }
        if (!mEmailInput.isEmpty() && !mEmailInput.contains("@")) {
            showAlert("Invalid Mother's Email", "Mother's email address must contain '@'.", Alert.AlertType.ERROR);
            return;
        }

        if (!yearCompInput.isEmpty() && !yearCompInput.matches("^\\d{4}$")) {
            showAlert("Invalid Year Completed", "Year Completed must be a 4-digit year (e.g., 2020).", Alert.AlertType.ERROR);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Addition");
        confirmAlert.setHeaderText("Add New Student");
        confirmAlert.setContentText("Are you sure you want to add this new student to the system?");
        confirmAlert.getDialogPane().setMinWidth(480);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        int newNo = StudentController.masterData != null ? StudentController.masterData.size() + 1 : 1;
        String nationality = txtNationality != null && !txtNationality.getText().trim().isEmpty() ? txtNationality.getText().trim() : "Myanmar";
        String nrc = txtNrc != null ? txtNrc.getText().trim() : "";
        String currAddr = txtCurrentAddress != null ? txtCurrentAddress.getText().trim() : "";
        String permAddr = txtPermanentAddress != null ? txtPermanentAddress.getText().trim() : "";
        String fName = txtFatherName != null ? txtFatherName.getText().trim() : "";
        String fOcc = txtFatherOcc != null ? txtFatherOcc.getText().trim() : "";
        String mName = txtMotherName != null ? txtMotherName.getText().trim() : "";
        String mOcc = txtMotherOcc != null ? txtMotherOcc.getText().trim() : "";
        String prevSchool = txtPrevSchool != null ? txtPrevSchool.getText().trim() : "";
        String addInfo = txtAdditionalInfo != null ? txtAdditionalInfo.getText().trim() : "";
        String finalPhotoPath = (selectedPhotoPath != null) ? selectedPhotoPath : DEFAULT_IMAGE_PATH;

        Student newStudent = new Student(newNo, standardizedRollNo, nameInput, validatedMajor, validatedYear, validatedSemester, phoneInput,
                dobInput, genderInput, nationality, nrc, emailInput, currAddr, permAddr,
                fName, fOcc, fPhoneInput, fEmailInput, mName, mOcc, mPhoneInput, mEmailInput,
                prevSchool, yearCompInput, addInfo, finalPhotoPath);

        if (StudentController.masterData != null) {
            StudentController.masterData.add(newStudent);
        }

        Session.clear();
        handleReturn();
    }

    @FXML
    public void handleResetData() {
        if (txtRollNo != null) txtRollNo.clear();
        if (txtDob != null) txtDob.clear();
        if (txtName != null) txtName.clear();
        if (comboGender != null) comboGender.setValue("Male");
        if (txtMajor != null) txtMajor.clear();
        if (txtNationality != null) txtNationality.clear();
        if (comboYear != null) comboYear.setValue("First Year");
        if (comboSemester != null) comboSemester.setValue("First Semester");
        if (txtNrc != null) txtNrc.clear();
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
        Session.clear();
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
}