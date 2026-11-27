package application;

import java.io.IOException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import application.dao.TeacherDAO;
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
                teacherTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
            });

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
        UIAnimator.attachTableRowHover(teacherTable);
        
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

        startLiveDatabaseSync();
        
        UIAnimator.setEmptyTablePlaceholder(
                teacherTable,
                "\uD83D\uDC68\u200D\uD83C\uDFEB",
                "No Faculty Found",
                "No faculty records match your current search or filters. Click Reset or press Ctrl+R to view all.");

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
                if (!teacherTable.getSelectionModel().getSelectedItems().isEmpty()) {
                    handleRemove();
                    event.consume();
                }
            } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                teacherTable.getSelectionModel().clearSelection();
                rootPane.requestFocus();
                event.consume();
            }
        });
    }

    private void startLiveDatabaseSync() {
        if (liveSyncTimeline != null) {
            liveSyncTimeline.stop();
        }
        liveSyncTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            if (rootPane != null && rootPane.getScene() != null
                    && rootPane.getScene().getWindow() != null
                    && rootPane.getScene().getWindow().isShowing()
                    && rootPane.getParent() != null) {
                syncFromDatabaseSilently();
            }
        }));
        liveSyncTimeline.setCycleCount(Timeline.INDEFINITE);
        liveSyncTimeline.play();
    }

    private void syncFromDatabaseSilently() {
        // Skip if a previous database sync is still in progress
        if (!isSyncing.compareAndSet(false, true)) {
            return;
        }

        String searchText = (searchField != null && searchField.getText() != null) ? searchField.getText().trim() : "";
        String selectedDept = deptHeaderCombo != null ? deptHeaderCombo.getValue() : "All Departments";
        String selectedPos = posHeaderCombo != null ? posHeaderCombo.getValue() : "All Positions";

        Task<List<Teacher>> fetchTask = new Task<>() {
            @Override
            protected List<Teacher> call() {
                TeacherDAO teacherDAO = new TeacherDAO();
                return teacherDAO.getFilteredTeachers(searchText, selectedDept, selectedPos);
            }
        };

        fetchTask.setOnSucceeded(e -> {
            isSyncing.set(false);
            List<Teacher> latestList = fetchTask.getValue();
            if (latestList == null) return;

            String newSignature = buildTeacherSignature(latestList);
            if (!newSignature.equals(lastDataSignature)) {
                lastDataSignature = newSignature;

                List<Integer> selectedIds = teacherTable.getSelectionModel().getSelectedItems().stream()
                        .filter(Objects::nonNull)
                        .map(Teacher::getTeacherId)
                        .collect(Collectors.toList());
                int focusedIndex = teacherTable.getFocusModel().getFocusedIndex();

                masterData = FXCollections.observableArrayList(latestList);
                setupTableData();

                if (!selectedIds.isEmpty()) {
                    int firstMatchedRow = -1;
                    for (int i = 0; i < teacherTable.getItems().size(); i++) {
                        if (selectedIds.contains(teacherTable.getItems().get(i).getTeacherId())) {
                            teacherTable.getSelectionModel().select(i);
                            if (firstMatchedRow == -1) firstMatchedRow = i;
                        }
                    }
                    if (firstMatchedRow >= 0) {
                        teacherTable.scrollTo(Math.max(0, firstMatchedRow - 2));
                    }
                } else if (focusedIndex >= 0 && focusedIndex < teacherTable.getItems().size()) {
                    teacherTable.scrollTo(focusedIndex);
                }

                DashboardController.refreshDashboard();
            }
        });

        fetchTask.setOnFailed(e -> isSyncing.set(false));
        fetchTask.setOnCancelled(e -> isSyncing.set(false));

        Thread bgThread = new Thread(fetchTask, "TeacherLiveSyncThread");
        bgThread.setDaemon(true);
        bgThread.start();
    }

    private String buildTeacherSignature(List<Teacher> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(list.size()).append("|");
        for (Teacher t : list) {
            sb.append(t.getTeacherId()).append(":")
              .append(t.getTeacherCode()).append(":")
              .append(t.getName()).append(":")
              .append(t.getDepartment()).append(":")
              .append(t.getPosition()).append(":")
              .append(t.getPhoneNo()).append(";");
        }
        return sb.toString();
    }

    public void refreshDataFromDatabase() {
        String searchText = (searchField != null && searchField.getText() != null) ? searchField.getText().trim() : "";
        String selectedDept = deptHeaderCombo != null ? deptHeaderCombo.getValue() : "All Departments";
        String selectedPos = posHeaderCombo != null ? posHeaderCombo.getValue() : "All Positions";

        TeacherDAO teacherDAO = new TeacherDAO();
        List<Teacher> list = teacherDAO.getFilteredTeachers(searchText, selectedDept, selectedPos);
        lastDataSignature = buildTeacherSignature(list);
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
        UIAnimator.playTapPulse(add);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherAdd.fxml"));
            Parent addView = loader.load();
            UIAnimator.attachAllButtonAnimations(addView);

            BorderPane mainLayout = (BorderPane) teacherTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(addView);
                UIAnimator.playViewEntrance(addView);
            } else {
                Parent root = teacherTable.getScene().getRoot();
                if (root instanceof BorderPane) {
                    ((BorderPane) root).setCenter(addView);
                    UIAnimator.playViewEntrance(addView);
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
            UIAnimator.attachAllButtonAnimations(detailView);

            TeacherDetailController controller = loader.getController();
            controller.initData(targetTeacher);

            BorderPane mainLayout = (BorderPane) teacherTable.getScene().lookup("#mainBorderPane");
            if (mainLayout != null) {
                mainLayout.setCenter(detailView);
                UIAnimator.playViewEntrance(detailView);
            } else {
                Parent root = teacherTable.getScene().getRoot();
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
        Teacher selected = teacherTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            UIAnimator.playTapPulse(modify);
            try {
                int uiRowNumber = teacherTable.getItems().indexOf(selected) + 1;

                TeacherDAO teacherDAO = new TeacherDAO();
                Teacher fullTeacher = teacherDAO.getFullTeacher(selected.getTeacherId());
                Teacher targetTeacher = (fullTeacher != null) ? fullTeacher : selected;
                targetTeacher.setDisplayNo(uiRowNumber > 0 ? uiRowNumber : selected.getTeacherId());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherModify.fxml"));
                Parent modifyView = loader.load();
                UIAnimator.attachAllButtonAnimations(modifyView);

                TeacherModifyController controller = loader.getController();
                controller.initData(targetTeacher);

                BorderPane mainLayout = (BorderPane) teacherTable.getScene().lookup("#mainBorderPane");
                if (mainLayout != null) {
                    mainLayout.setCenter(modifyView);
                    UIAnimator.playViewEntrance(modifyView);
                } else {
                    Parent root = teacherTable.getScene().getRoot();
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
            showAlert("No Selection", "Please select a teacher row from the table to modify.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    public void handleRemove() {
        ObservableList<Teacher> selectedTeachers = teacherTable.getSelectionModel().getSelectedItems();
        if (selectedTeachers != null && !selectedTeachers.isEmpty()) {
            UIAnimator.playTapPulse(remove);
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
                    UIAnimator.playTableFilterFade(teacherTable);
                    AttendanceController.refreshLive();
                    teacherTable.getSelectionModel().clearSelection();
                    teacherTable.refresh();
                } else {
                    showAlert("Database Error", "Failed to remove selected faculty member(s) from the database.", Alert.AlertType.ERROR);
                }
            }
        } else {
            UIAnimator.playShake(remove);
            showAlert("No Selection", "Please select one or more teacher rows to remove.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    public void handleRestore() {
        UIAnimator.playTapPulse(restore);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TeacherRestore.fxml"));
            Parent root = loader.load();
            UIAnimator.attachAllButtonAnimations(root);

            TeacherRestoreController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Restore Deleted Teachers");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (teacherTable != null && teacherTable.getScene() != null) {
                stage.initOwner(teacherTable.getScene().getWindow());
            }

            stage.setScene(new Scene(root));
            stage.setResizable(false);
            UIAnimator.playViewEntrance(root);
            stage.showAndWait();

            if (controller.isRecordRestored()) {
                refreshDataFromDatabase();
                UIAnimator.playTableFilterFade(teacherTable);
                AttendanceController.refreshLive();
                UIAnimator.showToast(rootPane, "Selected faculty record(s) restored successfully");
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
        lastDataSignature = buildTeacherSignature(list);

        masterData = FXCollections.observableArrayList(list);
        setupTableData();
        UIAnimator.playTableFilterFade(teacherTable);
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

    public static Parent getView() {
        if (instance != null && instance.rootPane != null) {
            instance.syncFromDatabaseSilently(); // Keeps filters, scroll position & selection intact!
            return instance.rootPane;
        }
        try {
            return FXMLLoader.load(TeacherController.class.getResource("TeacherContents.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}