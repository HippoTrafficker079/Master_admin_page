package application;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import application.dao.StudentDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class StudentRemoveDialogController {

    @FXML private TableView<Student> removeTable;
    @FXML private TableColumn<Student, Integer> colNo;
    @FXML private TableColumn<Student, String> colRollNo;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colMajor;
    @FXML private TableColumn<Student, String> colYear;
    @FXML private TableColumn<Student, String> colPhone;
    @FXML private Button btnRemove;
    @FXML private Button btnCancel;

    private boolean deletionSuccessful = false;

    @FXML
    public void initialize() {
        // Enable multiple selection model programmatically
        removeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Populate sequential row number
        colNo.setCellValueFactory(
                cellData -> new ReadOnlyObjectWrapper<>(removeTable.getItems().indexOf(cellData.getValue()) + 1));
        
        // Map remaining columns to Student properties
        colRollNo.setCellValueFactory(cellData -> cellData.getValue().rollNoProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colMajor.setCellValueFactory(cellData -> cellData.getValue().majorProperty());
        colYear.setCellValueFactory(cellData -> cellData.getValue().yearProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        loadStudents();
    }

    private void loadStudents() {
        StudentDAO dao = new StudentDAO();
        List<Student> list = dao.getAllStudent();
        ObservableList<Student> data = FXCollections.observableArrayList(list);
        removeTable.setItems(data);
    }

    @FXML
    private void handleRemoveSelected() {
        ObservableList<Student> selectedStudents = removeTable.getSelectionModel().getSelectedItems();
        if (selectedStudents == null || selectedStudents.isEmpty()) {
            showAlert("No Selection", "Please select at least one student row to remove.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Are you sure you want to remove " + selectedStudents.size() + " selected student(s)?");
        confirmAlert.setContentText("This action will soft-delete the selected student records.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            StudentDAO dao = new StudentDAO();
            List<Integer> ids = selectedStudents.stream().map(Student::getNo).collect(Collectors.toList());
            boolean success = dao.removeStudents(ids);

            if (success) {
                deletionSuccessful = true;
                closeDialog();
            } else {
                showAlert("Database Error", "Failed to remove selected student(s) from database.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleCancel() {
        deletionSuccessful = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) removeTable.getScene().getWindow();
        stage.close();
    }

    public boolean isDeletionSuccessful() {
        return deletionSuccessful;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}