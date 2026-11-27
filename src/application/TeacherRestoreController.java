package application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import application.dao.TeacherDAO;
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

public class TeacherRestoreController {

    @FXML private TextField searchField;
    @FXML private Button btnSelectAll;
    @FXML private Button btnClear;
    @FXML private Label lblSelectionStatus;

    @FXML private TableView<Teacher> deletedTable;
    @FXML private TableColumn<Teacher, Boolean> colPick;
    @FXML private TableColumn<Teacher, String> colTeacherId;
    @FXML private TableColumn<Teacher, String> colName;
    @FXML private TableColumn<Teacher, String> colDepartment;
    @FXML private TableColumn<Teacher, String> colPosition;
    @FXML private TableColumn<Teacher, String> colPhone;

    @FXML private Button btnRestore;
    @FXML private Button btnCancel;

    private final ObservableList<Teacher> deletedData = FXCollections.observableArrayList();
    private FilteredList<Teacher> filteredData;
    private final Map<Integer, BooleanProperty> selectionMap = new HashMap<>();
    private boolean recordRestored = false;

    @FXML
    public void initialize() {
        setupPickColumn();

        colTeacherId.setCellValueFactory(cellData -> cellData.getValue().teacherCodeProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colDepartment.setCellValueFactory(cellData -> cellData.getValue().departmentProperty());
        colPosition.setCellValueFactory(cellData -> cellData.getValue().positionProperty());
        colPhone.setCellValueFactory(cellData -> cellData.getValue().phoneNoProperty());

        for (TableColumn<?, ?> col : deletedTable.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
        }

        filteredData = new FilteredList<>(deletedData, t -> true);
        deletedTable.setItems(filteredData);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String q = (newVal == null) ? "" : newVal.trim().toLowerCase();
                filteredData.setPredicate(t -> {
                    if (q.isEmpty()) return true;
                    return (t.getTeacherCode() != null && t.getTeacherCode().toLowerCase().contains(q))
                            || (t.getName() != null && t.getName().toLowerCase().contains(q))
                            || (t.getDepartment() != null && t.getDepartment().toLowerCase().contains(q))
                            || (t.getPosition() != null && t.getPosition().toLowerCase().contains(q))
                            || (t.getPhoneNo() != null && t.getPhoneNo().toLowerCase().contains(q));
                });
                updateSelectionStatus();
            });
        }

        loadDeletedTeachers();
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
                    Teacher teacher = getTableRow().getItem();
                    boundProp = getSelectionProperty(teacher);
                    checkBox.selectedProperty().bindBidirectional(boundProp);
                    setGraphic(checkBox);
                }
            }
        });
    }

    private BooleanProperty getSelectionProperty(Teacher teacher) {
        return selectionMap.computeIfAbsent(teacher.getTeacherId(), id -> {
            BooleanProperty prop = new SimpleBooleanProperty(false);
            prop.addListener((obs, oldVal, newVal) -> updateSelectionStatus());
            return prop;
        });
    }

    private void loadDeletedTeachers() {
        TeacherDAO dao = new TeacherDAO();
        List<Teacher> list = dao.getDeletedTeachers();
        selectionMap.clear();
        deletedData.setAll(list);
        updateSelectionStatus();
    }

    private void updateSelectionStatus() {
        long selectedCount = deletedData.stream()
                .filter(t -> getSelectionProperty(t).get())
                .count();
        int availableCount = filteredData != null ? filteredData.size() : deletedData.size();
        if (lblSelectionStatus != null) {
            lblSelectionStatus.setText(selectedCount + " selected  •  " + availableCount + " available");
        }
    }

    @FXML
    private void handleSelectAll() {
        for (Teacher t : filteredData) {
            getSelectionProperty(t).set(true);
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
        List<Teacher> selected = deletedData.stream()
                .filter(t -> getSelectionProperty(t).get())
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            showAlert("No Selection", "Please check at least one faculty member to restore.", Alert.AlertType.WARNING);
            return;
        }

        List<Integer> idsToRestore = selected.stream()
                .map(Teacher::getTeacherId)
                .collect(Collectors.toList());

        TeacherDAO dao = new TeacherDAO();
        boolean success = dao.restoreTeacher(idsToRestore);

        if (success) {
            recordRestored = true;
            showAlert("Success", selected.size() + " faculty record(s) have been successfully restored.",
                    Alert.AlertType.INFORMATION);
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
        Window owner = (deletedTable != null && deletedTable.getScene() != null)
                ? deletedTable.getScene().getWindow() : null;
        AlertHelper.show(owner, type, title, null, message);
    }
}