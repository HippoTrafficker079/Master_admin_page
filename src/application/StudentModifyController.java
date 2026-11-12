package application;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import application.dao.StudentDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class StudentModifyController {
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
    @FXML private VBox contentContainer;

    private Student currentStudent;
    private HBox bannerBox = null;
    private String updatedPhotoPath = null;
    private boolean isUpdating = false;



    private final String[] VALID_MAJORS = { "CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex", "FE" };
    private final String[] VALID_YEARS = { "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year" };

    @FXML
    public void initialize() {
        initializeNrcComponent();
        setupBidirectionalSync();
    }

 // --- Add these field declarations if not already present ---
    private ObservableList<String> currentTownshipOptions;
    private FilteredList<String> filteredTownships;


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

        // Track whether a township selection is locked in. 
        // Set to true by default on load/modify so it requires Backspace to clear first!
        final boolean[] isTownshipLocked = {true};

        if (comboNrcState != null) {
            comboNrcState.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (isUpdating) return;
                
                if (newVal != null && !newVal.equals(oldVal)) {
                    isUpdating = true;
                    try {
                        // Fetch and set only the townships associated with this specific State code
                        ObservableList<String> stateTownships = NrcData.getTownshipsForState(newVal);
                        if (currentTownshipOptions != null) {
                            currentTownshipOptions.setAll(stateTownships);
                        } else {
                            currentTownshipOptions = stateTownships;
                            filteredTownships = new FilteredList<>(currentTownshipOptions, s -> true);
                            if (comboNrcTownship != null) {
                                comboNrcTownship.setItems(filteredTownships);
                            }
                        }
                        
                        isTownshipLocked[0] = false;
                        filteredTownships.setPredicate(s -> true);
                        comboNrcTownship.getSelectionModel().clearSelection();
                        comboNrcTownship.setValue(null);
                        if (comboNrcTownship.getEditor() != null) {
                            comboNrcTownship.getEditor().clear();
                        }
                        if (comboNrcTownship.isShowing()) {
                            comboNrcTownship.hide();
                        }
                    } finally {
                        isUpdating = false;
                    }
                }
            });
        }

        if (comboNrcTownship != null && comboNrcTownship.getEditor() != null) {
            // Type-ahead autocomplete listener (allows free searching when unlocked)
            comboNrcTownship.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
                if (isUpdating) return;

                if (comboNrcTownship.getValue() != null && comboNrcTownship.getValue().equals(newValue)) {
                    return;
                }

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

            // Lock selection when a specific township is chosen from the dropdown list
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
                    if (matchedState != null && comboNrcState != null) {
                        if (!matchedState.equals(comboNrcState.getValue())) {
                            isUpdating = true;
                            try {
                                comboNrcState.setValue(matchedState);
                                // Ensure dropdown options match the newly synced state
                                ObservableList<String> stateTownships = NrcData.getTownshipsForState(matchedState);
                                if (currentTownshipOptions != null) {
                                    currentTownshipOptions.setAll(stateTownships);
                                }
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
                } else {
                    isTownshipLocked[0] = false;
                }
            });

            // Key Filter: Restrict typing over a selected township unless Backspace is pressed first
            comboNrcTownship.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (isUpdating) return;

                if (isTownshipLocked[0]) {
                    javafx.scene.input.KeyCode code = event.getCode();

                    // Allow Backspace or Delete to clear the selection and unlock typing on the FIRST press
                    if (code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.DELETE) {
                        isUpdating = true;
                        try {
                            isTownshipLocked[0] = false;
                            comboNrcTownship.setValue(null);
                            comboNrcTownship.getEditor().clear();
                            
                            // Re-load correct townships for current state upon clearing
                            if (comboNrcState != null && comboNrcState.getValue() != null) {
                                ObservableList<String> stateTownships = NrcData.getTownshipsForState(comboNrcState.getValue());
                                if (currentTownshipOptions != null) {
                                    currentTownshipOptions.setAll(stateTownships);
                                }
                            }
                            filteredTownships.setPredicate(s -> true);
                        } finally {
                            isUpdating = false;
                        }
                        event.consume();
                        return;
                    }

                    // Block any other button and trigger the warning error message popup
                 // Block any other button and trigger the warning error message popup
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
                
                // 1. Set State ComboBox
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
                    
                    // 2. Set Township ComboBox & Editor text
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
                        
                        // 3. Set Type ComboBox
                        if (comboNrcType != null) {
                            if (!comboNrcType.getItems().contains(type)) {
                                comboNrcType.getItems().add(type);
                            }
                            comboNrcType.setValue(type);
                        }

                        // 4. Set Number TextField
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

    // UPDATE method signature to accept the row number instead of student ID
    public void initData(Student student) {
        this.currentStudent = student;
        if (student != null) {
            if (contentContainer != null) {
                if (bannerBox != null && contentContainer.getChildren().contains(bannerBox)) {
                    contentContainer.getChildren().remove(bannerBox);
                } else if (!contentContainer.getChildren().isEmpty()) {
                    Node firstNode = contentContainer.getChildren().get(0);
                    if (firstNode instanceof HBox && firstNode.getStyle() != null && firstNode.getStyle().contains("-fx-background-color: #dbeafe;")) {
                        contentContainer.getChildren().remove(firstNode);
                    }
                }

                bannerBox = new HBox();
                bannerBox.setStyle("-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 18;");

                // Retrieve the displayNo passed from the Main Controller
                int displayRowNumber = student.getDisplayNo() > 0 ? student.getDisplayNo() : student.getNo();

                String bannerText = String.format("⭐     %d     |     %s     |     %s     |     %s     |     %s",
                        displayRowNumber, safe(student.getRollNo()), safe(student.getName()), safe(student.getMajor()), safe(student.getYear()));

                Label bannerLabel = new Label(bannerText);
                bannerLabel.setStyle("-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 15px;");
                bannerBox.getChildren().add(bannerLabel);
                contentContainer.getChildren().add(0, bannerBox);
            }

            isUpdating = true;
            try {
                if (txtRollNo != null) txtRollNo.setText(safe(student.getRollNo()));

                if (dpDob != null && student.getDob() != null && !student.getDob().trim().isEmpty()) {
                    try {
                        dpDob.setValue(LocalDate.parse(student.getDob().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    } catch (Exception ex) {
                        dpDob.setValue(null);
                    }
                }

                if (txtName != null) txtName.setText(safe(student.getName()));

                String g = safe(student.getGender());
                if (comboGender != null) {
                    comboGender.setValue(g.equalsIgnoreCase("Female") ? "Female" : "Male");
                }

                String m = findValidMajor(safe(student.getMajor()));
                if (comboMajor != null && m != null) {
                    comboMajor.setValue(m);
                }

                if (txtNationality != null) txtNationality.setText(safe(student.getNationality(), "Myanmar"));

                String y = findValidYear(safe(student.getYear()));
                if (comboYear != null && y != null) {
                    comboYear.setValue(y);
                }

                // Auto-select NRC parts correctly in Modify mode
                setParsedNrc(student.getNrc());

                if (txtPhone != null) txtPhone.setText(safe(student.getPhoneNo()));
                if (txtEmail != null) txtEmail.setText(safe(student.getEmail()));
                if (txtCurrentAddress != null) txtCurrentAddress.setText(safe(student.getCurrentAddress()));
                if (txtPermanentAddress != null) txtPermanentAddress.setText(safe(student.getPermanentAddress()));

                if (txtFatherName != null) txtFatherName.setText(safe(student.getFatherName()));
                if (txtFatherOcc != null) txtFatherOcc.setText(safe(student.getFatherOccupation()));
                if (txtFatherPhone != null) txtFatherPhone.setText(safe(student.getFatherPhone()));
                if (txtFatherEmail != null) txtFatherEmail.setText(safe(student.getFatherEmail()));

                if (txtMotherName != null) txtMotherName.setText(safe(student.getMotherName()));
                if (txtMotherOcc != null) txtMotherOcc.setText(safe(student.getMotherOccupation()));
                if (txtMotherPhone != null) txtMotherPhone.setText(safe(student.getMotherPhone()));
                if (txtMotherEmail != null) txtMotherEmail.setText(safe(student.getMotherEmail()));

                if (txtPrevSchool != null) txtPrevSchool.setText(safe(student.getPrevSchool()));
                if (txtYearCompleted != null) txtYearCompleted.setText(safe(student.getYearCompleted()));
                if (txtAdditionalInfo != null) txtAdditionalInfo.setText(safe(student.getAdditionalInfo()));
            } finally {
                isUpdating = false;
            }

            updatedPhotoPath = student.getPhotoPath();
            loadImage(updatedPhotoPath);
        }
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
        fileChooser.setTitle("Replace Passport Size Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.bmp"));

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
        if (currentStudent != null) {
            String rawRollNo = txtRollNo != null ? txtRollNo.getText().trim() : "";
            String rawYear = comboYear != null && comboYear.getValue() != null ? comboYear.getValue().trim() : "";
            String rawMajor = comboMajor != null && comboMajor.getValue() != null ? comboMajor.getValue().trim() : "";
            String dobInput = (dpDob != null && dpDob.getValue() != null) ? dpDob.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
            String phoneInput = txtPhone != null ? txtPhone.getText().trim() : "";
            String emailInput = txtEmail != null ? txtEmail.getText().trim() : "";
            String genderInput = comboGender != null && comboGender.getValue() != null ? comboGender.getValue().trim() : "Male";
            String yearCompInput = txtYearCompleted != null ? txtYearCompleted.getText().trim() : "";

            if (!rawRollNo.matches("^[a-zA-Z]+-[a-zA-Z0-9]+-\\d+$")) {
                showAlert("Invalid Roll No Format", "Roll No must be in valid format.", Alert.AlertType.ERROR);
                return;
            }

            String validatedMajor = findValidMajor(rawMajor);
            String validatedYear = findValidYear(rawYear);
            if (validatedMajor == null || validatedYear == null) {
                showAlert("Invalid Major or Year", "Selected Major or Year is invalid.", Alert.AlertType.ERROR);
                return;
            }

            String standardizedRollNo = getRomanYear(validatedYear) + "-" + validatedMajor + "-" + rawRollNo.split("-")[2].trim();
            txtRollNo.setText(standardizedRollNo);

            String nrcInput = getCombinedNrc();
            if (nrcInput.isEmpty()) {
                showAlert("Incomplete NRC", "Please complete all structured NRC fields.", Alert.AlertType.ERROR);
                return;
            }

            if (!phoneInput.matches("^09-\\d+$")) {
                showAlert("Invalid Phone Number", "Phone number must be in format 09-xxxxxxxx.", Alert.AlertType.ERROR);
                return;
            }
            if (!emailInput.contains("@")) {
                showAlert("Invalid Email", "Email must contain '@'.", Alert.AlertType.ERROR);
                return;
            }

            currentStudent.setRollNo(standardizedRollNo);
            if (dpDob != null && dpDob.getValue() != null) currentStudent.setDob(dobInput);
            if (txtName != null) currentStudent.setName(txtName.getText().trim());
            currentStudent.setGender(genderInput);
            currentStudent.setMajor(validatedMajor);
            if (txtNationality != null) currentStudent.setNationality(txtNationality.getText().trim());
            currentStudent.setYear(validatedYear);
            currentStudent.setNrc(nrcInput);
            currentStudent.setPhoneNo(phoneInput);
            currentStudent.setEmail(emailInput);
            if (txtCurrentAddress != null) currentStudent.setCurrentAddress(txtCurrentAddress.getText().trim());
            if (txtPermanentAddress != null) currentStudent.setPermanentAddress(txtPermanentAddress.getText().trim());
            if (txtFatherName != null) currentStudent.setFatherName(txtFatherName.getText().trim());
            if (txtFatherOcc != null) currentStudent.setFatherOccupation(txtFatherOcc.getText().trim());
            if (txtMotherName != null) currentStudent.setMotherName(txtMotherName.getText().trim());
            if (txtMotherOcc != null) currentStudent.setMotherOccupation(txtMotherOcc.getText().trim());
            if (txtPrevSchool != null) currentStudent.setPrevSchool(txtPrevSchool.getText().trim());
            currentStudent.setYearCompleted(yearCompInput);
            if (txtAdditionalInfo != null) currentStudent.setAdditionalInfo(txtAdditionalInfo.getText().trim());

            if (updatedPhotoPath != null) {
                currentStudent.setPhotoPath(updatedPhotoPath);
            }
            StudentDAO studentDAO = new StudentDAO();
            boolean success = studentDAO.modifyStudent(currentStudent);
            
            if (!success) {
                showAlert("Database Error", "Failed to save changes to the database", Alert.AlertType.ERROR);
                return;
            }
            
            if (StudentController.masterData != null) {
                for (int i = 0; i < StudentController.masterData.size(); i++) {
                    if (StudentController.masterData.get(i).getNo() == currentStudent.getNo()) {
                        StudentController.masterData.set(i, currentStudent);
                        break;
                    }
                }
            }
            
        }
        
        handleReturn();
        DashboardController.refreshDashboard();
    }

    @FXML
    public void handleResetData() {
        if (currentStudent != null) {
            StudentDAO studentDAO = new StudentDAO();
            Student fresh = studentDAO.getStudentbyId(currentStudent.getNo());
            if (fresh != null) {
                currentStudent = fresh;
            } else {
                currentStudent.resetToOriginal();
            }
            
            // Populate form data back with fresh database values
            initData(currentStudent);

            // --- FIX NRC STATE & TOWNSHIP RESET SYNC ---
            if (currentStudent.getNrc() != null && !currentStudent.getNrc().isEmpty()) {
                String fullNrc = currentStudent.getNrc();
                // Example format: 12/DaGaNa(N)123456 or similar
                try {
                    String stateCode = fullNrc.split("/")[0].trim();
                    String remainder = fullNrc.split("/")[1];
                    String townshipName = remainder.split("\\(")[0].trim();

                    isUpdating = true;
                    try {
                        if (comboNrcState != null) {
                            comboNrcState.setValue(stateCode);
                            // Repopulate townships strictly for this state code
                            ObservableList<String> stateTownships = NrcData.getTownshipsForState(stateCode);
                            if (currentTownshipOptions != null) {
                                currentTownshipOptions.setAll(stateTownships);
                            }
                        }
                        if (comboNrcTownship != null) {
                            comboNrcTownship.setValue(townshipName);
                            if (comboNrcTownship.getEditor() != null) {
                                comboNrcTownship.getEditor().setText(townshipName);
                            }
                        }
                    } finally {
                        isUpdating = false;
                    }
                } catch (Exception e) {
                    // Fallback if NRC parsing format varies
                }
            }

            showAlert("Data Reset", "This student's record has been reset to its database state.",
                    Alert.AlertType.INFORMATION);
        }
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