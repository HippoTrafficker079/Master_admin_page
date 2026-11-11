package application;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import application.dao.StudentDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

public class StudentController {

    @FXML
    private VBox rootPane;
    @FXML
    private TableView<Student> studentTable;
    @FXML
    private TableColumn<Student, Integer> colNo;
    @FXML
    private TableColumn<Student, String> colRollNo;
    @FXML
    private TableColumn<Student, String> colName;
    @FXML
    private TableColumn<Student, String> colMajor;
    @FXML
    private TableColumn<Student, String> colYear;
    @FXML
    private TableColumn<Student, String> colSemester;
    @FXML
    private TableColumn<Student, String> colPhone;

    @FXML
    private Button modify;
    @FXML
    private Button add;
    @FXML
    private Button remove;

    @FXML
    private TextField searchField;

    public static ObservableList<Student> masterData = null;
    private FilteredList<Student> filteredData;

    private ComboBox<String> majorHeaderCombo;
    private ComboBox<String> yearHeaderCombo;
    private ComboBox<String> semesterHeaderCombo;

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                studentTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });
        }

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
                "All Majors", "Pending", "CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex",
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
        
        // ALWAYS fetch fresh data from the database so newly imported 
        // students get their real auto-incremented database IDs immediately.
        refreshDataFromDatabase();

        majorHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        yearHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        semesterHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        studentTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && studentTable.getSelectionModel().getSelectedItem() != null) {
                openStudentDetail(studentTable.getSelectionModel().getSelectedItem());
            }
        });
        
     // 1. Lock everything from user sorting, reordering, and resizing
        for (TableColumn<?, ?> col : studentTable.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
//            col.setResizable(false);
        }

        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
    }

    public void refreshDataFromDatabase() {
        StudentDAO studentDAO = new StudentDAO();
        List<Student> list = studentDAO.getAllStudent();
        masterData = FXCollections.observableArrayList(list);
        setupTableData();
    }

    private void setupTableData() {
        filteredData = new FilteredList<>(masterData, b -> true);
        SortedList<Student> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(studentTable.comparatorProperty());
        studentTable.setItems(sortedData);
    }

    @FXML
    public void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentAdd.fxml"));
            Parent addStudentView = loader.load();

            BorderPane mainLayout = (BorderPane) studentTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(addStudentView);
            } else {
                Parent root = studentTable.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(addStudentView);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openStudentDetail(Student student) {
        try {
            int rowIndex = studentTable.getItems().indexOf(student) + 1;
            StudentDAO studentDAO = new StudentDAO();
            Student fullStudent = studentDAO.getStudentbyId(student.getNo());
            Student targetStudent = (fullStudent != null) ? fullStudent : student;
            targetStudent.setDisplayNo(rowIndex > 0 ? rowIndex : student.getNo());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentDetail.fxml"));
            Parent detailView = loader.load();

            StudentDetailController controller = loader.getController();
            controller.initData(targetStudent);

            BorderPane mainLayout = (BorderPane) studentTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(detailView);
            } else {
                Parent root = studentTable.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(detailView);
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
            try {
                // Calculate the 1-based index from the table view
                int rowIndex = studentTable.getItems().indexOf(selected) + 1;
                
                StudentDAO studentDAO = new StudentDAO();
                Student fullStudent = studentDAO.getStudentbyId(selected.getNo());
                Student targetStudent = (fullStudent != null) ? fullStudent : selected;
                
                // Pass the calculated UI row number
                targetStudent.setDisplayNo(rowIndex > 0 ? rowIndex : selected.getNo());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("StudentModify.fxml"));
                Parent modifyView = loader.load();

                StudentModifyController controller = loader.getController();
                controller.initData(targetStudent);

                BorderPane mainLayout = (BorderPane) studentTable.getScene().lookup("#mainBorderPane");
                if (mainLayout != null) {
                    mainLayout.setCenter(modifyView);
                } else {
                    Parent root = studentTable.getScene().getRoot();
                    if (root instanceof BorderPane) {
                        ((BorderPane) root).setCenter(modifyView);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("No Selection", "Please select a student row from the table to modify.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    public void handleRemove() {
        ObservableList<Student> selectedStudents = studentTable.getSelectionModel().getSelectedItems();
        if (selectedStudents != null && !selectedStudents.isEmpty()) {
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

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                StudentDAO studentDAO = new StudentDAO();
                List<Integer> idsToRemove = selectedStudents.stream()
                        .map(Student::getNo)
                        .collect(Collectors.toList());

                boolean success = studentDAO.removeStudents(idsToRemove);
                if (success) {
                    masterData.removeAll(selectedStudents);
                    List<Student> freshList = studentDAO.getAllStudent();
                    masterData.setAll(freshList);
                    studentTable.getSelectionModel().clearSelection();
                    studentTable.refresh();
                } else {
                    showAlert("Database Error", "Failed to remove selected student(s) from the database.", Alert.AlertType.ERROR);
                }
            }
        } else {
            showAlert("No Selection", "Please select one or more student rows to remove.", Alert.AlertType.WARNING);
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

        masterData = FXCollections.observableArrayList(list);
        setupTableData();
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
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        if (studentTable != null && studentTable.getScene() != null) {
            alert.initOwner(studentTable.getScene().getWindow());
        }
        alert.showAndWait();
    }
    
    private void disableTableSortingAndReordering(TableView<?> table) {
        for (TableColumn<?, ?> col : table.getColumns()) {
            col.setSortable(false);
            col.setResizable(false);
            col.setReorderable(false); // Disables dragging column headers to reorder
        }
    }
}