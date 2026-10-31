package application;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import application.dao.TeacherDAO;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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

    @FXML private TextField searchField;

    public static ObservableList<Teacher> masterData = null;
    private FilteredList<Teacher> filteredData;

    private ComboBox<String> deptHeaderCombo;
    private ComboBox<String> posHeaderCombo;

    @FXML
    public void initialize() {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                teacherTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });
        }

        colNo.setCellValueFactory(cellData -> cellData.getValue().teacherIdProperty().asObject());
        
        if (colTeacherId != null) {
            colTeacherId.setCellValueFactory(cellData -> cellData.getValue().teacherCodeProperty());
        }

        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colDepartment.setCellValueFactory(cellData -> cellData.getValue().departmentProperty());
        colPosition.setCellValueFactory(cellData -> cellData.getValue().positionProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        ObservableList<String> departmentOptions = FXCollections.observableArrayList(
            "All Departments", "CE", "ME", "EC", "EP", "CEIT", "MC", "ChE", "Arch", "PE", "MnE", "Met", "Tex", "FE"
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

        if (masterData == null) {
            refreshDataFromDatabase();
        } else {
            setupTableData();
        }

        deptHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        posHeaderCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        teacherTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && teacherTable.getSelectionModel().getSelectedItem() != null) {
                openTeacherDetail(teacherTable.getSelectionModel().getSelectedItem(), false);
            }
        });
    }

    private void refreshDataFromDatabase() {
        TeacherDAO teacherDAO = new TeacherDAO();
        List<Teacher> list = teacherDAO.getAllTeacher();
        masterData = FXCollections.observableArrayList(list);
        setupTableData();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherCreatePrompt.fxml"));
            Parent createPromptView = loader.load();

            BorderPane mainLayout = (BorderPane) teacherTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(createPromptView);
            } else {
                Parent root = teacherTable.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(createPromptView);
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
            openTeacherDetail(selected, true);
        } else {
            showAlert("No Selection", "Please select a teacher row from the table to modify.", Alert.AlertType.WARNING);
        }
    }

    private void openTeacherDetail(Teacher teacher, boolean editMode) {
        try {
            TeacherDAO teacherDAO = new TeacherDAO();
            Teacher fullTeacher = teacherDAO.getFullTeacher(teacher.getTeacherId());
            Teacher targetTeacher = (fullTeacher != null) ? fullTeacher : teacher;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherDetail.fxml"));
            Parent detailView = loader.load();

            TeacherDetailController controller = loader.getController();
            controller.initData(targetTeacher, editMode);

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
    public void handleRemove() {
        Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Deletion");
            confirmAlert.setHeaderText("Are you sure you want to remove this faculty member?");
            confirmAlert.setContentText("ID: " + selected.getTeacherCode() + "\nName: " + selected.getName());
            confirmAlert.getDialogPane().setMinWidth(450);
            confirmAlert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                masterData.remove(selected);
                int index = 1;
                for (Teacher t : masterData) {
                    t.setTeacherId(index++);
                }
                teacherTable.getSelectionModel().clearSelection();
                teacherTable.refresh();
            }
        } else {
            showAlert("No Selection", "Please select a teacher row to remove.", Alert.AlertType.WARNING);
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
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(520);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        if (teacherTable != null && teacherTable.getScene() != null) {
            alert.initOwner(teacherTable.getScene().getWindow());
        }
        alert.showAndWait();
    }
}