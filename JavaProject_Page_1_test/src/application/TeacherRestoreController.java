package application;

import java.util.List;
import java.util.stream.Collectors;

import application.dao.TeacherDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class TeacherRestoreController {

    @FXML private TableView<Teacher> deletedTable;
    @FXML private TableColumn<Teacher, String> colTeacherId;
    @FXML private TableColumn<Teacher, String> colName;
    @FXML private TableColumn<Teacher, String> colDepartment;
    @FXML private TableColumn<Teacher, String> colPosition;
    @FXML private TableColumn<Teacher, String> colPhone;

    @FXML private Button btnRestore;
    @FXML private Button btnCancel;

    private final ObservableList<Teacher> deletedData = FXCollections.observableArrayList();
    private boolean recordRestored = false;

    @FXML
    public void initialize() {
        // Enable multiple selection (Ctrl / Shift)
        deletedTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colTeacherId.setCellValueFactory(cellData -> cellData.getValue().teacherCodeProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colDepartment.setCellValueFactory(cellData -> cellData.getValue().departmentProperty());
        colPosition.setCellValueFactory(cellData -> cellData.getValue().positionProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        loadDeletedTeachers();
    }

    private void loadDeletedTeachers() {
        TeacherDAO dao = new TeacherDAO();
        List<Teacher> list = dao.getDeletedTeachers();
        deletedData.setAll(list);
        deletedTable.setItems(deletedData);
    }

    @FXML
    private void handleRestoreSelected() {
        ObservableList<Teacher> selected = deletedTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showAlert("No Selection", "Please select at least one faculty row to restore.", Alert.AlertType.WARNING);
            return;
        }

        List<Integer> idsToRestore = selected.stream()
                .map(Teacher::getTeacherId)
                .collect(Collectors.toList());

        TeacherDAO dao = new TeacherDAO();
        boolean success = dao.restoreTeacher(idsToRestore);

        if (success) {
            recordRestored = true;
            showAlert("Success", selected.size() + " faculty record(s) have been successfully restored.", Alert.AlertType.INFORMATION);
            closeWindow();
        } else {
            showAlert("Database Error", "Failed to restore selected faculty records.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    public boolean isRecordRestored() {
        return recordRestored;
    }

    private void closeWindow() {
        Stage stage = (Stage) deletedTable.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(400);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        if (deletedTable.getScene() != null && deletedTable.getScene().getWindow() != null) {
            alert.initOwner(deletedTable.getScene().getWindow());
        }
        alert.showAndWait();
    }
}