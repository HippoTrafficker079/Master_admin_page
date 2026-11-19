package application;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import application.dao.TeacherDAO;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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

public class TeacherController {

    @FXML private VBox rootPane;
    @FXML private TableView<Teacher> teacherTable;
    @FXML private TableColumn<Teacher, Integer> colNo;
    @FXML private TableColumn<Teacher, String> colTeacherId;
    @FXML private TableColumn<Teacher, String> colName;
    @FXML private TableColumn<Teacher, String> colDepartment;
    @FXML private TableColumn<Teacher, String> colPosition;
    @FXML private TableColumn<Teacher, String> colPhone;

    @FXML private Button modify;
    @FXML private Button add;
    @FXML private Button remove;
    @FXML private Button restore;

    @FXML private TextField searchField;

    private static TeacherController instance;
    public static ObservableList<Teacher> masterData = null;
    private FilteredList<Teacher> filteredData;

    private ComboBox<String> deptHeaderCombo;
    private ComboBox<String> posHeaderCombo;

    public static void refreshLive() {
        if (instance != null) {
            Platform.runLater(() -> instance.refreshDataFromDatabase());
        }
    }

    @FXML
    public void initialize() {
        instance = this;

        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                teacherTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });
        }

        // Enable multiple selection for batch soft-deletion
        teacherTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Display sequential UI row numbers (1, 2, 3...) instead of raw DB primary keys
        colNo.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(teacherTable.getItems().indexOf(cellData.getValue()) + 1));

        if (colTeacherId != null) {
            colTeacherId.setCellValueFactory(cellData -> cellData.getValue().teacherCodeProperty());
        }

        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colDepartment.setCellValueFactory(cellData -> cellData.getValue().departmentProperty());
        colPosition.setCellValueFactory(cellData -> cellData.getValue().positionProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        ObservableList<String> departmentOptions = FXCollections.observableArrayList(
                "All Departments", "C", "Mech", "EC", "CEIT", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE"
        );

        ObservableList<String> positionOptions = FXCollections.observableArrayList(
                "All Positions", "Professor & Head", "Professor", "Associate Professor", "Lecturer", "Assistant Lecturer", "Tutor"
        );

        deptHeaderCombo = new ComboBox<>(departmentOptions);
        deptHeaderCombo.setPromptText("Department");
        deptHeaderCombo.setMaxWidth(Double.MAX_VALUE);
        deptHeaderCombo.setValue("All Departments");
        colDepartment.setGraphic(deptHeaderCombo);

        posHeaderCombo = new ComboBox<>(positionOptions);
        posHeaderCombo.setPromptText("Position");
        posHeaderCombo.setMaxWidth(Double.MAX_VALUE);
        posHeaderCombo.setValue("All Positions");
        colPosition.setGraphic(posHeaderCombo);

        refreshDataFromDatabase();

        deptHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        posHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        teacherTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && teacherTable.getSelectionModel().getSelectedItem() != null) {
                openTeacherDetail(teacherTable.getSelectionModel().getSelectedItem());
            }
        });

        for (TableColumn<?, ?> col : teacherTable.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
        }
    }

    public void refreshDataFromDatabase() {
        TeacherDAO teacherDAO = new TeacherDAO();
        List<Teacher> list = teacherDAO.getAllTeacher();
        masterData = FXCollections.observableArrayList(list);
        setupTableData();
        DashboardController.refreshDashboard();
    }

    private void setupTableData() {
        filteredData = new FilteredList<>(masterData, b -> true);
        SortedList<Teacher> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(teacherTable.comparatorProperty());
        teacherTable.setItems(sortedData);
    }

    @FXML
    public void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherAdd.fxml"));
            Parent addView = loader.load();

            BorderPane mainLayout = (BorderPane) teacherTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(addView);
            } else {
                Parent root = teacherTable.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(addView);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openTeacherDetail(Teacher teacher) {
        try {
            int uiRowNumber = teacherTable.getItems().indexOf(teacher) + 1;

            TeacherDAO teacherDAO = new TeacherDAO();
            Teacher fullTeacher = teacherDAO.getFullTeacher(teacher.getTeacherId());
            Teacher targetTeacher = (fullTeacher != null) ? fullTeacher : teacher;
            targetTeacher.setDisplayNo(uiRowNumber > 0 ? uiRowNumber : teacher.getTeacherId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherDetail.fxml"));
            Parent detailView = loader.load();

            TeacherDetailController controller = loader.getController();
            controller.initData(targetTeacher);

            BorderPane mainLayout = (BorderPane) teacherTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(detailView);
            } else {
                Parent root = teacherTable.getScene().getRoot();
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
        Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                int uiRowNumber = teacherTable.getItems().indexOf(selected) + 1;

                TeacherDAO teacherDAO = new TeacherDAO();
                Teacher fullTeacher = teacherDAO.getFullTeacher(selected.getTeacherId());
                Teacher targetTeacher = (fullTeacher != null) ? fullTeacher : selected;
                targetTeacher.setDisplayNo(uiRowNumber > 0 ? uiRowNumber : selected.getTeacherId());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherModify.fxml"));
                Parent modifyView = loader.load();

                TeacherModifyController controller = loader.getController();
                controller.initData(targetTeacher);

                BorderPane mainLayout = (BorderPane) teacherTable.getScene().lookup("#mainBorderPane");
                if (mainLayout != null) {
                    mainLayout.setCenter(modifyView);
                } else {
                    Parent root = teacherTable.getScene().getRoot();
                    if (root instanceof BorderPane) {
                        ((BorderPane) root).setCenter(modifyView);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert("No Selection", "Please select a teacher row from the table to modify.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    public void handleRemove() {
        ObservableList<Teacher> selectedTeachers = teacherTable.getSelectionModel().getSelectedItems();
        if (selectedTeachers != null && !selectedTeachers.isEmpty()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");

            if (selectedTeachers.size() == 1) {
                Teacher selected = selectedTeachers.get(0);
                confirmAlert.setHeaderText("Are you sure you want to remove this faculty member?");
                confirmAlert.setContentText("ID: " + selected.getTeacherCode() + "\nName: " + selected.getName());
            } else {
                confirmAlert.setHeaderText("Are you sure you want to remove " + selectedTeachers.size() + " selected faculty members?");
                confirmAlert.setContentText("This action will soft-delete all selected faculty records.");
            }

            confirmAlert.getDialogPane().setMinWidth(450);
            confirmAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            
            AlertHelper.styleAlert(confirmAlert, true);
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                TeacherDAO teacherDAO = new TeacherDAO();
                List<Integer> idsToRemove = selectedTeachers.stream()
                        .map(Teacher::getTeacherId)
                        .collect(Collectors.toList());

                boolean success = teacherDAO.removeBatchTeacher(idsToRemove);
                if (success) {
                    refreshDataFromDatabase();
                    AttendanceController.refreshLive();
                    teacherTable.getSelectionModel().clearSelection();
                    teacherTable.refresh();
                } else {
                    showAlert("Database Error", "Failed to remove selected faculty member(s) from the database.", Alert.AlertType.ERROR);
                }
            }
        } else {
            showAlert("No Selection", "Please select one or more teacher rows to remove.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    public void handleRestore() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherRestore.fxml"));
            Parent root = loader.load();

            TeacherRestoreController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Restore Deleted Teachers");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (teacherTable != null && teacherTable.getScene() != null) {
                stage.initOwner(teacherTable.getScene().getWindow());
            }

            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isRecordRestored()) {
                refreshDataFromDatabase();
                AttendanceController.refreshLive();
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
        String selectedDept = deptHeaderCombo.getValue();
        String selectedPos = posHeaderCombo.getValue();

        TeacherDAO teacherDAO = new TeacherDAO();
        List<Teacher> list = teacherDAO.getFilteredTeachers(searchText, selectedDept, selectedPos);

        masterData = FXCollections.observableArrayList(list);
        setupTableData();
    }

    @FXML
    public void handleReset() {
        searchField.clear();
        deptHeaderCombo.setValue("All Departments");
        posHeaderCombo.setValue("All Positions");
        teacherTable.getSelectionModel().clearSelection();

        applyFilters();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Window owner = (teacherTable != null && teacherTable.getScene() != null)
                ? teacherTable.getScene().getWindow() : null;
        AlertHelper.show(owner, type, title, null, message);
    }
}