package application;

import java.io.IOException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import application.dao.StudentDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class StudentController {

    @FXML private VBox rootPane;
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Integer> colNo;
    @FXML private TableColumn<Student, String> colRollNo;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colMajor;
    @FXML private TableColumn<Student, String> colYear;
    @FXML private TableColumn<Student, String> colSemester;
    @FXML private TableColumn<Student, String> colPhone;

    @FXML private Button modify;
    @FXML private Button add;
    @FXML private Button remove;
    @FXML private Button restore;

    @FXML private TextField searchField;

    private static StudentController instance;
    public static ObservableList<Student> masterData = null;
    private FilteredList<Student> filteredData;

    private ComboBox<String> majorHeaderCombo;
    private ComboBox<String> yearHeaderCombo;
    private ComboBox<String> semesterHeaderCombo;

    private static Timeline liveSyncTimeline;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    private String lastDataSignature = "";

    public static void refreshLive() {
        if (instance != null) {
            Platform.runLater(() -> instance.syncFromDatabaseSilently());
        }
    }
    
    @FXML
    public void initialize() {
        instance = this;

        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                studentTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });

            // Immediately sync whenever Manage Student tab becomes visible
            rootPane.parentProperty().addListener((obs, oldParent, newParent) -> {
                if (newParent != null) {
                    syncFromDatabaseSilently();
                }
            });
        }

        // Attach subtle hover & focus microinteractions
        UIAnimator.attachButtonHover(add);
        UIAnimator.attachButtonHover(modify);
        UIAnimator.attachButtonHover(remove);
        UIAnimator.attachButtonHover(restore);
        UIAnimator.attachFocusGlow(searchField);
        UIAnimator.attachAllButtonAnimations(rootPane);
        UIAnimator.attachTableRowHover(studentTable);

        // Enable multiple selection for batch operations directly in the main table
        studentTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colNo.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(studentTable.getItems().indexOf(cellData.getValue()) + 1));
        colRollNo.setCellValueFactory(cellData -> cellData.getValue().rollNoProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colMajor.setCellValueFactory(cellData -> cellData.getValue().majorProperty());
        colYear.setCellValueFactory(cellData -> cellData.getValue().yearProperty());
        colSemester.setCellValueFactory(cellData -> cellData.getValue().semesterProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        ObservableList<String> majorOptions = FXCollections.observableArrayList(
                "All Majors", "Pending", "C", "Mech", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex",
                "FE");

        ObservableList<String> yearOptions = FXCollections.observableArrayList(
                "All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year");

        ObservableList<String> semesterOptions = FXCollections.observableArrayList(
                "All Semesters", "Pending", "First Semester", "Second Semester");

        majorHeaderCombo = new ComboBox<>(majorOptions);
        majorHeaderCombo.setPromptText("Major");
        majorHeaderCombo.setMaxWidth(Double.MAX_VALUE);
        majorHeaderCombo.setValue("All Majors");
        colMajor.setGraphic(majorHeaderCombo);

        yearHeaderCombo = new ComboBox<>(yearOptions);
        yearHeaderCombo.setPromptText("Year");
        yearHeaderCombo.setMaxWidth(Double.MAX_VALUE);
        yearHeaderCombo.setValue("All Years");
        colYear.setGraphic(yearHeaderCombo);

        semesterHeaderCombo = new ComboBox<>(semesterOptions);
        semesterHeaderCombo.setPromptText("Semester");
        semesterHeaderCombo.setMaxWidth(Double.MAX_VALUE);
        semesterHeaderCombo.setValue("All Semesters");
        colSemester.setGraphic(semesterHeaderCombo);

        refreshDataFromDatabase();

        majorHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        yearHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        semesterHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        studentTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && studentTable.getSelectionModel().getSelectedItem() != null) {
                openStudentDetail(studentTable.getSelectionModel().getSelectedItem());
            }
        });

        for (TableColumn<?, ?> col : studentTable.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
        }

        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        startLiveDatabaseSync();
        
        UIAnimator.setEmptyTablePlaceholder(
                studentTable,
                "\uD83C\uDF93",
                "No Students Found",
                "No student records match your current search or filters. Click Reset or press Ctrl+R to view all.");

        UIAnimator.attachShortcutHandler(rootPane, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case F -> {
                        if (searchField != null) { searchField.requestFocus(); searchField.selectAll(); }
                        event.consume();
                    }
                    case N -> { handleAdd(); event.consume(); }
                    case E -> { handleModify(); event.consume(); }
                    case R -> { handleReset(); event.consume(); }
                    default -> {}
                }
            } else if (event.getCode() == javafx.scene.input.KeyCode.DELETE
                    && !UIAnimator.isTypingInTextInput(rootPane.getScene())) {
                if (!studentTable.getSelectionModel().getSelectedItems().isEmpty()) {
                    handleRemove();
                    event.consume();
                }
            } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                studentTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
                event.consume();
            }
        });
    }

    private void startLiveDatabaseSync() {
        if (liveSyncTimeline != null) {
            liveSyncTimeline.stop();
        }
        liveSyncTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            if (rootPane != null && rootPane.getScene() != null && rootPane.getParent() != null) {
                syncFromDatabaseSilently();
            }
        }));
        liveSyncTimeline.setCycleCount(Timeline.INDEFINITE);
        liveSyncTimeline.play();
    }

    private void syncFromDatabaseSilently() {
        String searchText = (searchField != null && searchField.getText() != null) ? searchField.getText().trim() : "";
        String selectedMajor = majorHeaderCombo != null ? majorHeaderCombo.getValue() : "All Majors";
        String selectedYear = yearHeaderCombo != null ? yearHeaderCombo.getValue() : "All Years";
        String selectedSemester = semesterHeaderCombo != null ? semesterHeaderCombo.getValue() : "All Semesters";

        Task<List<Student>> fetchTask = new Task<>() {
            @Override
            protected List<Student> call() {
                StudentDAO studentDAO = new StudentDAO();
                return studentDAO.getFilteredStudents(searchText, selectedMajor, selectedYear, selectedSemester);
            }
        };

        fetchTask.setOnSucceeded(e -> {
            List<Student> latestList = fetchTask.getValue();
            if (latestList == null) return;

            String newSignature = buildStudentSignature(latestList);
            if (!newSignature.equals(lastDataSignature)) {
                lastDataSignature = newSignature;

                // Preserve currently selected student IDs & focused row index
                List<Integer> selectedIds = studentTable.getSelectionModel().getSelectedItems().stream()
                        .filter(Objects::nonNull)
                        .map(Student::getNo)
                        .collect(Collectors.toList());
                int focusedIndex = studentTable.getFocusModel().getFocusedIndex();

                masterData = FXCollections.observableArrayList(latestList);
                setupTableData();

                // Restore selection and scroll position if rows still exist
                if (!selectedIds.isEmpty()) {
                    int firstMatchedRow = -1;
                    for (int i = 0; i < studentTable.getItems().size(); i++) {
                        if (selectedIds.contains(studentTable.getItems().get(i).getNo())) {
                            studentTable.getSelectionModel().select(i);
                            if (firstMatchedRow == -1) firstMatchedRow = i;
                        }
                    }
                    if (firstMatchedRow >= 0) {
                        studentTable.scrollTo(Math.max(0, firstMatchedRow - 2));
                    }
                } else if (focusedIndex >= 0 && focusedIndex < studentTable.getItems().size()) {
                    studentTable.scrollTo(focusedIndex);
                }

                DashboardController.refreshDashboard();
            }
        });

        Thread bgThread = new Thread(fetchTask, "StudentLiveSyncThread");
        bgThread.setDaemon(true);
        bgThread.start();
    }

    private String buildStudentSignature(List<Student> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(list.size()).append("|");
        for (Student s : list) {
            sb.append(s.getNo()).append(":")
              .append(s.getRollNo()).append(":")
              .append(s.getName()).append(":")
              .append(s.getMajor()).append(":")
              .append(s.getYear()).append(":")
              .append(s.getSemester()).append(":")
              .append(s.getPhoneNo()).append(";");
        }
        return sb.toString();
    }

    public void refreshDataFromDatabase() {
        String searchText = (searchField != null && searchField.getText() != null) ? searchField.getText().trim() : "";
        String selectedMajor = majorHeaderCombo != null ? majorHeaderCombo.getValue() : "All Majors";
        String selectedYear = yearHeaderCombo != null ? yearHeaderCombo.getValue() : "All Years";
        String selectedSemester = semesterHeaderCombo != null ? semesterHeaderCombo.getValue() : "All Semesters";

        StudentDAO studentDAO = new StudentDAO();
        List<Student> list = studentDAO.getFilteredStudents(searchText, selectedMajor, selectedYear, selectedSemester);
        lastDataSignature = buildStudentSignature(list);
        masterData = FXCollections.observableArrayList(list);
        setupTableData();
        DashboardController.refreshDashboard();
    }

    private void setupTableData() {
        filteredData = new FilteredList<>(masterData, b -> true);
        SortedList<Student> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(studentTable.comparatorProperty());
        studentTable.setItems(sortedData);
    }

    @FXML
    public void handleAdd() {
        UIAnimator.playTapPulse(add);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentAdd.fxml"));
            Parent addStudentView = loader.load();
            UIAnimator.attachAllButtonAnimations(addStudentView);

            BorderPane mainLayout = (BorderPane) studentTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(addStudentView);
                UIAnimator.playViewEntrance(addStudentView);
            } else {
                Parent root = studentTable.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(addStudentView);
                    UIAnimator.playViewEntrance(addStudentView);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openStudentDetail(Student student) {
        try {
            int uiRowNumber = studentTable.getItems().indexOf(student) + 1;

            StudentDAO studentDAO = new StudentDAO();
            Student fullStudent = studentDAO.getStudentbyId(student.getNo());
            Student targetStudent = (fullStudent != null) ? fullStudent : student;

            targetStudent.setDisplayNo(uiRowNumber > 0 ? uiRowNumber : student.getNo());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentDetail.fxml"));
            Parent detailView = loader.load();
            UIAnimator.attachAllButtonAnimations(detailView);
            
            StudentDetailController controller = loader.getController();
            controller.initData(targetStudent);

            BorderPane mainLayout = (BorderPane) studentTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(detailView);
                UIAnimator.playViewEntrance(detailView);
            } else {
                Parent root = studentTable.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(detailView);
                    UIAnimator.playViewEntrance(detailView);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleModify() {
        Student selected = studentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            UIAnimator.playTapPulse(modify);
            
            try {
                int uiRowNumber = studentTable.getItems().indexOf(selected) + 1;

                StudentDAO studentDAO = new StudentDAO();
                Student fullStudent = studentDAO.getStudentbyId(selected.getNo());
                Student targetStudent = (fullStudent != null) ? fullStudent : selected;

                targetStudent.setDisplayNo(uiRowNumber > 0 ? uiRowNumber : selected.getNo());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentModify.fxml"));
                Parent modifyView = loader.load();
                UIAnimator.attachAllButtonAnimations(modifyView);
                
                StudentModifyController controller = loader.getController();
                controller.initData(targetStudent);

                BorderPane mainLayout = (BorderPane) studentTable.getScene().lookup("#mainBorderPane");
                if (mainLayout != null) {
                    mainLayout.setCenter(modifyView);
                    UIAnimator.playViewEntrance(modifyView);
                } else {
                    Parent root = studentTable.getScene().getRoot();
                    if (root instanceof BorderPane) {
                        ((BorderPane) root).setCenter(modifyView);
                        UIAnimator.playViewEntrance(modifyView);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            UIAnimator.playShake(modify);
            showAlert("No Selection", "Please select a student row from the table to modify.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    public void handleRemove() {
        ObservableList<Student> selectedStudents = studentTable.getSelectionModel().getSelectedItems();
        if (selectedStudents != null && !selectedStudents.isEmpty()) {
            UIAnimator.playTapPulse(remove);
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");

            if (selectedStudents.size() == 1) {
                Student selected = selectedStudents.get(0);
                confirmAlert.setHeaderText("Are you sure you want to remove this student?");
                confirmAlert.setContentText("Roll No: " + selected.getRollNo() + "\nName: " + selected.getName());
            } else {
                confirmAlert.setHeaderText("Are you sure you want to remove " + selectedStudents.size() + " selected students?");
                confirmAlert.setContentText("This action will soft-delete all selected student records.");
            }

            confirmAlert.getDialogPane().setMinWidth(450);
            confirmAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            AlertHelper.styleAlert(confirmAlert, true);
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                StudentDAO studentDAO = new StudentDAO();
                List<Integer> idsToRemove = selectedStudents.stream()
                        .map(Student::getNo)
                        .collect(Collectors.toList());

                boolean success = studentDAO.removeStudents(idsToRemove);
                if (success) {
                    refreshDataFromDatabase();
                    UIAnimator.playTableFilterFade(studentTable);
                    AttendanceController.refreshLive();
                    studentTable.getSelectionModel().clearSelection();
                    studentTable.refresh();
                } else {
                    showAlert("Database Error", "Failed to remove selected student(s) from the database.", Alert.AlertType.ERROR);
                }
            }
        } else {
            UIAnimator.playShake(remove);
            showAlert("No Selection", "Please select one or more student rows to remove.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    public void handleRestore() {
        UIAnimator.playTapPulse(restore);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentRestore.fxml"));
            Parent root = loader.load();
            UIAnimator.attachAllButtonAnimations(root);

            StudentRestoreController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Restore Deleted Students");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (studentTable != null && studentTable.getScene() != null) {
                stage.initOwner(studentTable.getScene().getWindow());
            }

            stage.setScene(new Scene(root));
            stage.setResizable(false);
            UIAnimator.playViewEntrance(root);
            stage.showAndWait();

            if (controller.isRecordRestored()) {
                refreshDataFromDatabase();
                UIAnimator.playTableFilterFade(studentTable);
                AttendanceController.refreshLive();
                UIAnimator.showToast(rootPane, "Selected student record(s) restored successfully");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open the restore dialog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleSearch() {
        applyFilters();
    }

    @FXML
    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim();
        String selectedMajor = majorHeaderCombo.getValue();
        String selectedYear = yearHeaderCombo.getValue();
        String selectedSemester = semesterHeaderCombo.getValue();

        StudentDAO studentDAO = new StudentDAO();
        List<Student> list = studentDAO.getFilteredStudents(searchText, selectedMajor, selectedYear, selectedSemester);
        lastDataSignature = buildStudentSignature(list);

        masterData = FXCollections.observableArrayList(list);
        setupTableData();
        UIAnimator.playTableFilterFade(studentTable);
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        majorHeaderCombo.setValue("All Majors");
        yearHeaderCombo.setValue("All Years");
        semesterHeaderCombo.setValue("All Semesters");
        studentTable.getSelectionModel().clearSelection();

        applyFilters();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Window owner = (studentTable != null && studentTable.getScene() != null)
                ? studentTable.getScene().getWindow() : null;
        AlertHelper.show(owner, type, title, null, message);
    }

    private void disableTableSortingAndReordering(TableView<?> table) {
        for (TableColumn<?, ?> col : table.getColumns()) {
            col.setSortable(false);
            col.setResizable(false);
            col.setReorderable(false);
        }
    }

    public static Parent getView() {
        if (instance != null && instance.rootPane != null) {
            instance.syncFromDatabaseSilently(); // Keeps filters, scroll position & selection intact!
            return instance.rootPane;
        }
        try {
            return FXMLLoader.load(StudentController.class.getResource("StudentContent.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}