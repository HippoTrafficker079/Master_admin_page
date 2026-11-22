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
import javafx.stage.Stage;
import javafx.stage.Window;

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
        deletedTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        UIAnimator.attachTableRowHover(deletedTable);
        UIAnimator.attachButtonHover(btnRestore);
        UIAnimator.attachButtonHover(btnCancel);

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
            UIAnimator.playShake(btnRestore);
            showAlert("No Selection", "Please select at least one faculty row to restore.", Alert.AlertType.WARNING);
            return;
        }

        UIAnimator.playTapPulse(btnRestore);
        List<Integer> idsToRestore = selected.stream()
                .map(Teacher::getTeacherId)
                .collect(Collectors.toList());

        TeacherDAO dao = new TeacherDAO();
        boolean success = dao.restoreTeacher(idsToRestore);

        if (success) {
            recordRestored = true;
            closeWindow();
        } else {
            showAlert("Database Error", "Failed to restore selected faculty records.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCancel() {
        UIAnimator.playTapPulse(btnCancel);
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
        Window owner = (deletedTable.getScene() != null) ? deletedTable.getScene().getWindow() : null;
        AlertHelper.show(owner, type, title, null, message);
    }
}