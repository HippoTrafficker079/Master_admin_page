package application;

import java.util.List;
import java.util.stream.Collectors;

import application.dao.StudentDAO;
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

public class StudentRestoreController {

    @FXML private TableView<Student> deletedTable;
    @FXML private TableColumn<Student, String> colRollNo;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colMajor;
    @FXML private TableColumn<Student, String> colYear;
    @FXML private TableColumn<Student, String> colPhone;

    @FXML private Button btnRestore;
    @FXML private Button btnCancel;

    private final ObservableList<Student> deletedData = FXCollections.observableArrayList();
    private boolean recordRestored = false;

    @FXML
    public void initialize() {
        deletedTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        UIAnimator.attachTableRowHover(deletedTable);
        UIAnimator.attachButtonHover(btnRestore);
        UIAnimator.attachButtonHover(btnCancel);

        colRollNo.setCellValueFactory(cellData -> cellData.getValue().rollNoProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colMajor.setCellValueFactory(cellData -> cellData.getValue().majorProperty());
        colYear.setCellValueFactory(cellData -> cellData.getValue().yearProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        loadDeletedStudents();
    }

    private void loadDeletedStudents() {
        StudentDAO dao = new StudentDAO();
        List<Student> list = dao.getDeletedStudents();
        deletedData.setAll(list);
        deletedTable.setItems(deletedData);
    }

    @FXML
    private void handleRestoreSelected() {
        ObservableList<Student> selected = deletedTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            UIAnimator.playShake(btnRestore);
            showAlert("No Selection", "Please select at least one student row to restore.", Alert.AlertType.WARNING);
            return;
        }

        UIAnimator.playTapPulse(btnRestore);
        List<Integer> idsToRestore = selected.stream()
                .map(Student::getNo)
                .collect(Collectors.toList());

        StudentDAO dao = new StudentDAO();
        boolean success = dao.restoreStudents(idsToRestore);

        if (success) {
            recordRestored = true;
            closeWindow();
        } else {
            showAlert("Database Error", "Failed to restore selected records.", Alert.AlertType.ERROR);
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