package application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import application.dao.StudentDAO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

public class StudentRestoreController {

    @FXML private TextField searchField;
    @FXML private Button btnSelectAll;
    @FXML private Button btnClear;
    @FXML private Label lblSelectionStatus;

    @FXML private TableView<Student> deletedTable;
    @FXML private TableColumn<Student, Boolean> colPick;
    @FXML private TableColumn<Student, String> colRollNo;
    @FXML private TableColumn<Student, String> colName;
    @FXML private TableColumn<Student, String> colMajor;
    @FXML private TableColumn<Student, String> colYear;
    @FXML private TableColumn<Student, String> colPhone;

    @FXML private Button btnRestore;
    @FXML private Button btnCancel;

    private final ObservableList<Student> deletedData = FXCollections.observableArrayList();
    private FilteredList<Student> filteredData;
    private final Map<Integer, BooleanProperty> selectionMap = new HashMap<>();
    private boolean recordRestored = false;

    @FXML
    public void initialize() {
        setupPickColumn();

        colRollNo.setCellValueFactory(cellData -> cellData.getValue().rollNoProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colMajor.setCellValueFactory(cellData -> cellData.getValue().majorProperty());
        colYear.setCellValueFactory(cellData -> cellData.getValue().yearProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        for (TableColumn<?, ?> col : deletedTable.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
        }

        filteredData = new FilteredList<>(deletedData, s -> true);
        deletedTable.setItems(filteredData);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String q = (newVal == null) ? "" : newVal.trim().toLowerCase();
                filteredData.setPredicate(s -> {
                    if (q.isEmpty()) return true;
                    return (s.getRollNo() != null && s.getRollNo().toLowerCase().contains(q))
                            || (s.getName() != null && s.getName().toLowerCase().contains(q))
                            || (s.getMajor() != null && s.getMajor().toLowerCase().contains(q))
                            || (s.getYear() != null && s.getYear().toLowerCase().contains(q))
                            || (s.getPhoneNo() != null && s.getPhoneNo().toLowerCase().contains(q));
                });
                updateSelectionStatus();
            });
        }

        loadDeletedStudents();
        UIAnimator.setEmptyTablePlaceholder(
                deletedTable,
                "\u2705",
                "No Deleted Records",
                "There are currently no archived records matching your search.");

        UIAnimator.attachShortcutHandler(deletedTable, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == javafx.scene.input.KeyCode.F && searchField != null) {
                    searchField.requestFocus();
                    searchField.selectAll();
                    event.consume();
                } else if (event.getCode() == javafx.scene.input.KeyCode.A
                        && !UIAnimator.isTypingInTextInput(deletedTable.getScene())) {
                    handleSelectAll();
                    event.consume();
                }
            } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                handleCancel();
                event.consume();
            }
        });
    }

    private void setupPickColumn() {
        colPick.setCellValueFactory(cellData -> getSelectionProperty(cellData.getValue()));
        colPick.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private BooleanProperty boundProp = null;

            {
                setAlignment(Pos.CENTER);
                checkBox.setOnAction(e -> updateSelectionStatus());
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (boundProp != null) {
                    checkBox.selectedProperty().unbindBidirectional(boundProp);
                    boundProp = null;
                }
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Student student = getTableRow().getItem();
                    boundProp = getSelectionProperty(student);
                    checkBox.selectedProperty().bindBidirectional(boundProp);
                    setGraphic(checkBox);
                }
            }
        });
    }

    private BooleanProperty getSelectionProperty(Student student) {
        return selectionMap.computeIfAbsent(student.getNo(), id -> {
            BooleanProperty prop = new SimpleBooleanProperty(false);
            prop.addListener((obs, oldVal, newVal) -> updateSelectionStatus());
            return prop;
        });
    }

    private void loadDeletedStudents() {
        StudentDAO dao = new StudentDAO();
        List<Student> list = dao.getDeletedStudents();
        selectionMap.clear();
        deletedData.setAll(list);
        updateSelectionStatus();
    }

    private void updateSelectionStatus() {
        long selectedCount = deletedData.stream()
                .filter(s -> getSelectionProperty(s).get())
                .count();
        int availableCount = filteredData != null ? filteredData.size() : deletedData.size();
        if (lblSelectionStatus != null) {
            lblSelectionStatus.setText(selectedCount + " selected  •  " + availableCount + " available");
        }
    }

    @FXML
    private void handleSelectAll() {
        for (Student s : filteredData) {
            getSelectionProperty(s).set(true);
        }
        deletedTable.refresh();
        updateSelectionStatus();
    }

    @FXML
    private void handleClear() {
        if (searchField != null) {
            searchField.clear();
        }
        for (BooleanProperty prop : selectionMap.values()) {
            prop.set(false);
        }
        deletedTable.getSelectionModel().clearSelection();
        deletedTable.refresh();
        updateSelectionStatus();
    }

    @FXML
    private void handleRestoreSelected() {
        List<Student> selected = deletedData.stream()
                .filter(s -> getSelectionProperty(s).get())
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showAlert("No Selection", "Please check at least one student to restore.", Alert.AlertType.WARNING);
            return;
        }

        List<Integer> idsToRestore = selected.stream()
                .map(Student::getNo)
                .collect(Collectors.toList());

        StudentDAO dao = new StudentDAO();
        boolean success = dao.restoreStudents(idsToRestore);

        if (success) {
            recordRestored = true;
            showAlert("Success", selected.size() + " student record(s) have been successfully restored.",
                    Alert.AlertType.INFORMATION);
            closeWindow();
        } else {
            showAlert("Database Error", "Failed to restore selected records.", Alert.AlertType.ERROR);
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
        Window owner = (deletedTable != null && deletedTable.getScene() != null)
                ? deletedTable.getScene().getWindow() : null;
        AlertHelper.show(owner, type, title, null, message);
    }
}