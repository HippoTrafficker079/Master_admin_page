package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import application.dao.AttendanceDAO;
import application.dao.CourseDAO;
import application.dao.StudentDAO;
import application.dto.StudentProfile;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

public class AttendanceController {

    // =========================================================================
    // 1. FXML UI COMPONENTS
    // =========================================================================

    @FXML private VBox rootPane, matrixFilterBox, dailyFilterBox;
    @FXML private HBox matrixFilterRow1, matrixFilterRow2;
    @FXML private Button btnClassView, btnIndividualView, btnMatrixSummary, btnDailyLog;
    @FXML private Button btnResetDailyStatus, btnToggleStatus, btnMarkAllPresent, btnSaveChanges;
    @FXML private Button btnBackToList;
    @FXML private HBox subModeBar, hintBoxMatrix, hintBoxDaily, hintBoxIndiv, saveChangesBox;
    @FXML private ScrollPane individualDashboardScroll;
    @FXML private VBox classTablesContainer, individualDashboardContainer;
    @FXML private Label studentHeaderLabel;

    @FXML private ComboBox<String> comboDepartment, comboYear, comboSubject;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private TextField searchField;
    @FXML private CheckBox chkLowAttendance;

    @FXML private DatePicker dailyDatePicker;
    @FXML private ComboBox<String> dailyPeriodCombo;
    @FXML private TextField dailySearchField;
    @FXML private CheckBox chkPresent, chkAbsent, chkExcuse;

    // Matrix Summary Table
    @FXML private TableView<AttendanceRecord> matrixTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoMatrix;
    @FXML private TableColumn<AttendanceRecord, String> colRollMatrix, colNameMatrix, colDeptMatrix, colYearMatrix, colOverallMatrix;

    @FXML private TableView<AttendanceRecord> dailyLogTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoLog;
    @FXML private TableColumn<AttendanceRecord, String> colRollLog, colNameLog, colDateLog, colSubjLog, colStatusLog, colMarkedByLog, colRemarksLog;
    @FXML private TableColumn<AttendanceRecord, Integer> colPeriodLog;

    @FXML private ImageView indivProfileImage;
    @FXML private Label lblIndivName, lblIndivStudentId, lblIndivDept, lblIndivYearSec, lblIndivStatusBadge;
    @FXML private Label lblTotalAttended, lblTotalClasses, lblAcademicStanding;
    @FXML private Label lblIndivEmail, lblIndivPhone, lblIndivAddress;
    @FXML private Label lblDeptTotalAttended, lblDeptTotalRate, lblDeptTotalStanding, lblPaginationInfo;

    @FXML private TableView<AttendanceRecord> individualTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoIndiv;
    @FXML private TableColumn<AttendanceRecord, String> colSubjIndiv, colCourseCodeIndiv, colAttendedIndiv, colPercentageIndiv, colRemarksIndiv;

    @FXML private ComboBox<String> comboIndivLogCourse, comboIndivLogStatus;
    @FXML private DatePicker indivLogStartDate, indivLogEndDate;
    @FXML private TableView<AttendanceRecord> chronologicalLogTable;
    @FXML private TableColumn<AttendanceRecord, Integer> colNoChrono;
    @FXML private TableColumn<AttendanceRecord, String> colDateChrono, colCourseChrono, colCourseCodeChrono, colStatusChrono, colMarkedByChrono, colRemarksChrono;

    @FXML private Button btnPage1, btnPage2, btnPage3, btnPrevPage, btnNextPage;


    // =========================================================================
    // 2. DATA LISTS & CONTROLLER STATE
    // =========================================================================

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final CourseDAO courseDAO = new CourseDAO();

    private ObservableList<AttendanceRecord> matrixData;
    private ObservableList<AttendanceRecord> dailyLogData;
    private ObservableList<AttendanceRecord> individualData;
    private ObservableList<AttendanceRecord> chronoLogData;

    private FilteredList<AttendanceRecord> filteredMatrixData;
    private FilteredList<AttendanceRecord> filteredDailyData;
    private FilteredList<AttendanceRecord> filteredIndivData;
    private FilteredList<AttendanceRecord> filteredChronoData;

    private final Map<String, AttendanceRecord> modifiedDailyRecords = new HashMap<>();

    private boolean isMatrixMode = true;
    private boolean isClassViewActive = true;
    private boolean isStudentLocked = false;
    private int selectedStudentId = -1;
    private String selectedStudentRollNo = null;
    private String selectedStudentName = null;

    private int currentPage = 1;
    private final int rowsPerPage = 6;
    private int currentPaginationWindow = 1;
    private static AttendanceController instance;

    // =========================================================================
    // 3. INITIALIZATION & TABLE SETUP
    // =========================================================================

    @FXML
    public void initialize() {
        setupRootFocusAndTables();
        setupResponsiveFilterBar();
        setupDatePickers();
        setupTableColumns();
        setupFilterComboBoxes();
        setupDataLists();
        setupSubjectSearchComboBox();
        setupTableRowAndCellFactories();

        applyFilters();
    }

    private void setupRootFocusAndTables() {
        if (rootPane != null) {
            rootPane.setOnMouseClicked(event -> {
                matrixTable.getSelectionModel().clearSelection();
                dailyLogTable.getSelectionModel().clearSelection();
                individualTable.getSelectionModel().clearSelection();
                if (chronologicalLogTable != null) {
                    chronologicalLogTable.getSelectionModel().clearSelection();
                }
                rootPane.requestFocus();
            });

            rootPane.visibleProperty().addListener((obs, wasVisible, isNowVisible) -> {
                if (isNowVisible && !isClassViewActive && isStudentLocked) {
                    refreshSelectedStudentDashboard();
                }
            });

            rootPane.parentProperty().addListener((obs, oldParent, newParent) -> {
                if (newParent != null && !isClassViewActive && isStudentLocked) {
                    refreshSelectedStudentDashboard();
                }
            });

            Platform.runLater(() -> {
                if (rootPane.getParent() instanceof javafx.scene.layout.Region parentRegion) {
                    rootPane.prefWidthProperty().bind(parentRegion.widthProperty());
                    rootPane.maxWidthProperty().bind(parentRegion.widthProperty());
                }
            });
        }

        matrixTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        dailyLogTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        individualTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        if (chronologicalLogTable != null) {
            chronologicalLogTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }

        disableTableSortingAndReordering(matrixTable);
        disableTableSortingAndReordering(dailyLogTable);
        disableTableSortingAndReordering(individualTable);
        if (chronologicalLogTable != null) {
            disableTableSortingAndReordering(chronologicalLogTable);
        }
    }

    private void setupResponsiveFilterBar() {
        if (matrixFilterBox != null) {
            matrixFilterBox.widthProperty().addListener((obs, oldW, newW) -> {
                double w = newW.doubleValue();
                if (w <= 0) return;

                double scale = Math.max(0.72, Math.min(1.0, (w - 40.0) / 900.0));

                if (comboDepartment != null) {
                    comboDepartment.setMinWidth(110.0);
                    comboDepartment.setPrefWidth(155.0 * scale);
                }
                if (comboYear != null) {
                    comboYear.setMinWidth(95.0);
                    comboYear.setPrefWidth(135.0 * scale);
                }
                if (comboSubject != null) {
                    comboSubject.setMinWidth(150.0);
                    comboSubject.setPrefWidth(275.0 * scale);
                }
                if (startDatePicker != null) {
                    startDatePicker.setMinWidth(115.0);
                    startDatePicker.setPrefWidth(145.0 * scale);
                }
                if (endDatePicker != null) {
                    endDatePicker.setMinWidth(115.0);
                    endDatePicker.setPrefWidth(145.0 * scale);
                }
            });
        }
    }

    private void moveSearchFieldToRow1(boolean toRow1) {
        if (matrixFilterRow1 == null || matrixFilterRow2 == null || searchField == null) return;

        matrixFilterRow1.getChildren().remove(searchField);
        matrixFilterRow2.getChildren().remove(searchField);

        if (toRow1) {
            HBox.setHgrow(searchField, Priority.ALWAYS);
            searchField.setMaxWidth(Double.MAX_VALUE);

            int yearIdx = matrixFilterRow1.getChildren().indexOf(comboYear);
            matrixFilterRow1.getChildren().add(yearIdx >= 0 ? yearIdx + 1 : matrixFilterRow1.getChildren().size(), searchField);
        } else {
            HBox.setHgrow(searchField, Priority.NEVER);
            searchField.setPrefWidth(260.0);
            searchField.setMaxWidth(260.0);

            matrixFilterRow2.getChildren().add(0, searchField);
        }
    }

    private void disableTableSortingAndReordering(TableView<?> table) {
        for (TableColumn<?, ?> col : table.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
        }
    }

    private void setupDatePickers() {
        StringConverter<LocalDate> dateFormatter = new StringConverter<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            @Override
            public String toString(LocalDate date) {
                if (date != null) return formatter.format(date);
                return "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) {
                    try { return LocalDate.parse(string, formatter); } catch (Exception e) { return null; }
                }
                return null;
            }
        };

        if (startDatePicker != null) {
            startDatePicker.setConverter(dateFormatter);
            startDatePicker.setPromptText("Start Date");
            startDatePicker.setTooltip(new Tooltip("Start Date (dd-MM-yyyy)"));
        }
        if (endDatePicker != null) {
            endDatePicker.setConverter(dateFormatter);
            endDatePicker.setPromptText("End Date");
            endDatePicker.setTooltip(new Tooltip("End Date (defaults to today)"));
            endDatePicker.setValue(LocalDate.now());
        }
        if (dailyDatePicker != null) {
            dailyDatePicker.setConverter(dateFormatter);
            dailyDatePicker.setPromptText("Log Date");
            dailyDatePicker.setTooltip(new Tooltip("Daily Log Date (defaults to today)"));
            dailyDatePicker.setValue(LocalDate.now());
        }
        if (indivLogStartDate != null) {
            indivLogStartDate.setConverter(dateFormatter);
            indivLogStartDate.setPromptText("Start Date");
            indivLogStartDate.setTooltip(new Tooltip("Start Date (dd-MM-yyyy)"));
        }
        if (indivLogEndDate != null) {
            indivLogEndDate.setConverter(dateFormatter);
            indivLogEndDate.setPromptText("End Date");
            indivLogEndDate.setTooltip(new Tooltip("End Date (defaults to today)"));
            indivLogEndDate.setValue(LocalDate.now());
        }
    }

    private void setupTableColumns() {
        colNoMatrix.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
        setupSequentialRowNumberCellFactory(colNoMatrix);
        colRollMatrix.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        colNameMatrix.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        colDeptMatrix.setCellValueFactory(cell -> cell.getValue().departmentProperty());
        colYearMatrix.setCellValueFactory(cell -> cell.getValue().yearProperty());
        colOverallMatrix.setCellValueFactory(cell -> cell.getValue().percentageProperty().asString("%.1f%%"));

        colNoLog.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
        setupSequentialRowNumberCellFactory(colNoLog);
        colRollLog.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        colNameLog.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        colDateLog.setCellValueFactory(cell -> cell.getValue().dateProperty());
        colPeriodLog.setCellValueFactory(cell -> cell.getValue().periodProperty().asObject());
        colSubjLog.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        colMarkedByLog.setCellValueFactory(cell -> cell.getValue().markedByProperty());
        colRemarksLog.setCellValueFactory(cell -> cell.getValue().remarksProperty());

        colNoIndiv.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
        colSubjIndiv.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        if (colCourseCodeIndiv != null) {
            colCourseCodeIndiv.setCellValueFactory(cell -> cell.getValue().courseCodeProperty());
        }
        colAttendedIndiv.setCellValueFactory(cell -> cell.getValue().attendedTotalProperty());
        colPercentageIndiv.setCellValueFactory(cell -> cell.getValue().percentageProperty().asString("%.1f%%"));
        colRemarksIndiv.setCellValueFactory(cell -> cell.getValue().remarksProperty());
        setupStandingBadgeCellFactory(colRemarksIndiv);

        if (chronologicalLogTable != null) {
            colNoChrono.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
            colDateChrono.setCellValueFactory(cell -> {
                LocalDate d = cell.getValue().getDateObj();
                String formatted = (d != null) ? d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : cell.getValue().getDate();
                return new javafx.beans.property.SimpleStringProperty(formatted);
            });
            colCourseChrono.setCellValueFactory(cell -> cell.getValue().subjectProperty());
            colCourseCodeChrono.setCellValueFactory(cell -> cell.getValue().courseCodeProperty());
            colStatusChrono.setCellValueFactory(cell -> cell.getValue().statusProperty());
            colMarkedByChrono.setCellValueFactory(cell -> cell.getValue().markedByProperty());
            colRemarksChrono.setCellValueFactory(cell -> cell.getValue().remarksProperty());
            setupChronoStatusCellFactory(colStatusChrono);
        }
    }

    private void setupFilterComboBoxes() {
        comboDepartment.setItems(FXCollections.observableArrayList(
            "All Departments", "C", "Mech", "EC", "CEIT", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE"
        ));
        comboDepartment.setValue("All Departments");

        comboYear.setItems(FXCollections.observableArrayList(
            "All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"
        ));
        comboYear.setValue("All Years");

        dailyPeriodCombo.setItems(FXCollections.observableArrayList(
            "All Periods", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        ));
        dailyPeriodCombo.setValue("All Periods");

        ObservableList<String> courseOptions = FXCollections.observableArrayList("All Courses");
        try {
            List<String> dbCourses = courseDAO.getAllActiveCourseName();
            if (dbCourses != null) {
                courseOptions.addAll(dbCourses);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (comboIndivLogCourse != null) {
            comboIndivLogCourse.setItems(courseOptions);
            comboIndivLogCourse.setValue("All Courses");
        }
        if (comboIndivLogStatus != null) {
            comboIndivLogStatus.setItems(FXCollections.observableArrayList(
                "All Status", "Present", "Absent", "Excused"
            ));
            comboIndivLogStatus.setValue("All Status");
        }
    }

    private void setupDataLists() {
        matrixData = FXCollections.observableArrayList();
        dailyLogData = FXCollections.observableArrayList();
        individualData = FXCollections.observableArrayList();
        chronoLogData = FXCollections.observableArrayList();

        filteredMatrixData = new FilteredList<>(matrixData, b -> true);
        filteredDailyData = new FilteredList<>(dailyLogData, b -> true);
        filteredIndivData = new FilteredList<>(individualData, b -> true);
        filteredChronoData = new FilteredList<>(chronoLogData, b -> true);

        matrixTable.setItems(filteredMatrixData);
        dailyLogTable.setItems(filteredDailyData);
        individualTable.setItems(filteredIndivData);
        if (chronologicalLogTable != null) {
            chronologicalLogTable.setItems(chronoLogData);
        }
    }

    private void setupSubjectSearchComboBox() {
        ObservableList<String> subjectOptions = FXCollections.observableArrayList("All Subjects");
        try {
            List<String> dbCourses = courseDAO.getAllActiveCourseName();
            if (dbCourses != null) {
                subjectOptions.addAll(dbCourses);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        FilteredList<String> filteredSubjects = new FilteredList<>(subjectOptions, s -> true);
        comboSubject.setItems(filteredSubjects);
        comboSubject.setValue("All Subjects");

        final boolean[] isLocked = {true};

        if (comboSubject.getEditor() != null) {
            comboSubject.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (comboSubject.getValue() != null && comboSubject.getValue().equals(newVal)) return;

                if (newVal == null || newVal.trim().isEmpty()) {
                    isLocked[0] = false;
                    Platform.runLater(() -> filteredSubjects.setPredicate(s -> true));
                } else if (!isLocked[0]) {
                    Platform.runLater(() -> {
                        String filter = newVal.toLowerCase().trim();
                        filteredSubjects.setPredicate(item -> item != null && item.toLowerCase().contains(filter));
                        if (!comboSubject.isShowing() && !filteredSubjects.isEmpty()) {
                            comboSubject.show();
                        }
                    });
                }
            });

            comboSubject.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    isLocked[0] = true;
                    if (comboSubject.getEditor() != null && !comboSubject.getEditor().getText().equals(newVal)) {
                        comboSubject.getEditor().setText(newVal);
                    }
                }
            });

            comboSubject.getEditor().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                javafx.scene.input.KeyCode code = event.getCode();
                if (code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.DELETE) {
                    if (isLocked[0]) {
                        isLocked[0] = false;
                        Platform.runLater(() -> {
                            comboSubject.setValue(null);
                            comboSubject.getEditor().clear();
                            filteredSubjects.setPredicate(s -> true);
                        });
                        event.consume();
                    }
                    return;
                }
                if (isLocked[0]) {
                    event.consume();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Action Restricted");
                        alert.setHeaderText(null);
                        alert.setContentText("Please press backspace once to clear first.");
                        alert.getDialogPane().setMinWidth(380);
                        alert.showAndWait();
                    });
                }
            });
        }
    }

    private void setupSequentialRowNumberCellFactory(TableColumn<AttendanceRecord, Integer> column) {
        if (column == null) return;
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
    }


    // =========================================================================
    // 4. TABLE ROW & CELL STYLING FACTORIES
    // =========================================================================

    private void setupTableRowAndCellFactories() {
        matrixTable.setRowFactory(tv -> {
            TableRow<AttendanceRecord> row = createLowAttendanceRowFactory();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadStudentIntoIndividualDashboard(row.getItem());
                }
            });
            return row;
        });

        // Daily Log: Status is toggled via the Toggle Status button; Remarks is editable ONLY when status is "Excused"
        dailyLogTable.setEditable(true);

        colStatusLog.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colStatusLog.setEditable(false);
        colRemarksLog.setCellFactory(tc -> new TextFieldTableCell<AttendanceRecord, String>(new DefaultStringConverter()) {
            private boolean listenerAttached = false;

            @Override
            public void startEdit() {
                AttendanceRecord record = getTableRow() != null ? getTableRow().getItem() : null;
                if (record == null || record.getStatus() == null || !record.getStatus().equalsIgnoreCase("Excused")) {
                    return; // Allow editing Remarks ONLY when status is Excused
                }
                super.startEdit();

                if (!listenerAttached && getGraphic() instanceof TextField tf) {
                    listenerAttached = true;
                    tf.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                        if (!isNowFocused && isEditing()) {
                            commitEdit(tf.getText());
                        }
                    });
                }
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else if (!isEditing()) {
                    AttendanceRecord record = getTableRow() != null ? getTableRow().getItem() : null;
                    String status = (record != null && record.getStatus() != null) ? record.getStatus().trim() : "";
                    boolean isExcused = "Excused".equalsIgnoreCase(status);

                    if (item == null || item.isEmpty() || item.equals("-")) {
                        if ("Present".equalsIgnoreCase(status)) {
                            setText("On Time");
                        } else if ("Absent".equalsIgnoreCase(status)) {
                            setText("Absent Today");
                        } else if (isExcused) {
                            setText("(Double-click to add remark)");
                        } else {
                            setText("-");
                        }
                    } else {
                        setText(item);
                    }
                    setTooltip(isExcused ? new Tooltip("Double-click to edit remark for Excused status") : null);
                }
            }
        });

        colRemarksLog.setCellValueFactory(cell -> cell.getValue().remarksProperty());
        colRemarksLog.setEditable(true);
        colRemarksLog.setCellFactory(tc -> new TextFieldTableCell<AttendanceRecord, String>(new DefaultStringConverter()) {
            @Override
            public void startEdit() {
                AttendanceRecord record = getTableRow() != null ? getTableRow().getItem() : null;
                if (record == null || record.getStatus() == null || !record.getStatus().equalsIgnoreCase("Excused")) {
                    return; // Allow editing Remarks ONLY when status is Excused
                }
                super.startEdit();
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setTooltip(null);
                } else if (!isEditing()) {
                    AttendanceRecord record = getTableRow() != null ? getTableRow().getItem() : null;
                    boolean isExcused = record != null && "Excused".equalsIgnoreCase(record.getStatus());
                    if (item == null || item.isEmpty() || item.equals("-")) {
                        setText(isExcused ? "(Double-click to add remark)" : "-");
                    } else {
                        setText(item);
                    }
                    setTooltip(isExcused ? new Tooltip("Double-click to edit remark for Excused status") : null);
                }
            }
        });

        colRemarksLog.setOnEditCommit(event -> {
            AttendanceRecord record = event.getRowValue();
            String newRemark = event.getNewValue() != null ? event.getNewValue().trim() : "";
            record.remarksProperty().set(newRemark.isEmpty() ? "-" : newRemark);

            String key = record.getSessionId() + "_" + record.getEnrollmentId();
            modifiedDailyRecords.put(key, record);
            dailyLogTable.refresh();
        });

        dailyLogTable.setRowFactory(tv -> {
            TableRow<AttendanceRecord> row = new TableRow<AttendanceRecord>() {
                private final ChangeListener<String> statusListener = (obs, oldVal, newVal) -> applyDailyRowStyle(this, newVal);

                @Override
                protected void updateItem(AttendanceRecord item, boolean empty) {
                    if (getItem() != null) getItem().statusProperty().removeListener(statusListener);
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle("");
                        getStyleClass().removeAll("row-present", "row-absent", "row-excused");
                    } else {
                        item.statusProperty().addListener(statusListener);
                        applyDailyRowStyle(this, item.getStatus() != null ? item.getStatus().trim() : "");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    boolean clickedRemarksCell = event.getTarget() instanceof Node node && isInsideRemarksColumn(node);
                    if (clickedRemarksCell && "Excused".equalsIgnoreCase(row.getItem().getStatus())) {
                        return; // Let double-click on Remarks edit the cell instead of navigating away
                    }
                    loadStudentIntoIndividualDashboard(row.getItem());
                }
            });
            return row;
        });
    }

    private boolean isInsideRemarksColumn(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof TableCell<?, ?> cell && cell.getTableColumn() == colRemarksLog) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void setupStandingBadgeCellFactory(TableColumn<AttendanceRecord, String> column) {
        column.setCellFactory(col -> new TableCell<AttendanceRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    if (item.equalsIgnoreCase("Excellence") || item.equalsIgnoreCase("Excellent") || item.equalsIgnoreCase("Good") || item.equalsIgnoreCase("Good Standing")) {
                        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setAlignment(Pos.CENTER);
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void setupChronoStatusCellFactory(TableColumn<AttendanceRecord, String> column) {
        column.setCellFactory(col -> new TableCell<AttendanceRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label statusLbl = new Label();
                    if (item.equalsIgnoreCase("Present")) {
                        statusLbl.setText("✔  Present");
                        statusLbl.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("Absent")) {
                        statusLbl.setText("✖  Absent");
                        statusLbl.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("Excused") || item.equalsIgnoreCase("Excuse")) {
                        statusLbl.setText("●  Excused");
                        statusLbl.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
                    } else {
                        statusLbl.setText("●  " + item);
                        statusLbl.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
                    }
                    setAlignment(Pos.CENTER_LEFT);
                    setGraphic(statusLbl);
                    setText(null);
                }
            }
        });
    }

    private void applyDailyRowStyle(TableRow<AttendanceRecord> row, String status) {
        row.setStyle("");
        row.getStyleClass().removeAll("row-present", "row-absent", "row-excused");

        if (status == null) return;
        if (status.equalsIgnoreCase("Present")) {
            row.getStyleClass().add("row-present");
        } else if (status.equalsIgnoreCase("Absent")) {
            row.getStyleClass().add("row-absent");
        } else if (status.equalsIgnoreCase("Excused") || status.equalsIgnoreCase("Excuse")) {
            row.getStyleClass().add("row-excused");
        }
    }

    private TableRow<AttendanceRecord> createLowAttendanceRowFactory() {
        return new TableRow<AttendanceRecord>() {
            @Override
            protected void updateItem(AttendanceRecord item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("");
                getStyleClass().remove("row-low-attendance");

                if (item != null && !empty && item.isLowAttendance()) {
                    getStyleClass().add("row-low-attendance");
                }
            }
        };
    }


    // =========================================================================
    // 5. VIEW SWITCHING & NAVIGATION
    // =========================================================================

    @FXML
    private void switchToClassView() {
        isClassViewActive = true;
        isStudentLocked = false;
        selectedStudentId = -1;
        selectedStudentRollNo = null;
        selectedStudentName = null;

        moveSearchFieldToRow1(false);

        subModeBar.setVisible(true);
        subModeBar.setManaged(true);

        startDatePicker.setVisible(true);
        startDatePicker.setManaged(true);
        endDatePicker.setVisible(true);
        endDatePicker.setManaged(true);

        hintBoxIndiv.setVisible(false);
        hintBoxIndiv.setManaged(false);

        studentHeaderLabel.setVisible(false);
        studentHeaderLabel.setManaged(false);

        if (isMatrixMode) {
            matrixFilterBox.setVisible(true);
            matrixFilterBox.setManaged(true);
            dailyFilterBox.setVisible(false);
            dailyFilterBox.setManaged(false);
            classTablesContainer.setVisible(true);
            classTablesContainer.setManaged(true);
            matrixTable.setVisible(true);
            matrixTable.setManaged(true);
            dailyLogTable.setVisible(false);
            dailyLogTable.setManaged(false);

            comboSubject.setVisible(true);
            comboSubject.setManaged(true);

            hintBoxMatrix.setVisible(true);
            hintBoxMatrix.setManaged(true);
            hintBoxDaily.setVisible(false);
            hintBoxDaily.setManaged(false);

            if (saveChangesBox != null) {
                saveChangesBox.setVisible(false);
                saveChangesBox.setManaged(false);
            }

            updateActiveButtonStyles(btnMatrixSummary, btnDailyLog);
        } else {
            matrixFilterBox.setVisible(false);
            matrixFilterBox.setManaged(false);
            dailyFilterBox.setVisible(true);
            dailyFilterBox.setManaged(true);
            classTablesContainer.setVisible(true);
            classTablesContainer.setManaged(true);
            matrixTable.setVisible(false);
            matrixTable.setManaged(false);
            dailyLogTable.setVisible(true);
            dailyLogTable.setManaged(true);

            hintBoxMatrix.setVisible(false);
            hintBoxMatrix.setManaged(false);
            hintBoxDaily.setVisible(true);
            hintBoxDaily.setManaged(true);

            if (saveChangesBox != null) {
                saveChangesBox.setVisible(true);
                saveChangesBox.setManaged(true);
            }

            updateActiveButtonStyles(btnDailyLog, btnMatrixSummary);
        }

        individualDashboardContainer.setVisible(false);
        individualDashboardContainer.setManaged(false);
        comboDepartment.setVisible(true);
        comboDepartment.setManaged(true);

        if (btnBackToList != null) {
            btnBackToList.setVisible(false);
            btnBackToList.setManaged(false);
        }

        updateActiveButtonStyles(btnClassView, btnIndividualView);
        applyFilters();
    }

    @FXML
    private void switchToIndividualView() {
        isClassViewActive = false;

        moveSearchFieldToRow1(true);

        subModeBar.setVisible(false);
        subModeBar.setManaged(false);

        matrixFilterBox.setVisible(!isStudentLocked);
        matrixFilterBox.setManaged(!isStudentLocked);
        dailyFilterBox.setVisible(false);
        dailyFilterBox.setManaged(false);

        startDatePicker.setVisible(false);
        startDatePicker.setManaged(false);
        endDatePicker.setVisible(false);
        endDatePicker.setManaged(false);

        hintBoxMatrix.setVisible(false);
        hintBoxMatrix.setManaged(false);
        hintBoxDaily.setVisible(false);
        hintBoxDaily.setManaged(false);

        classTablesContainer.setVisible(false);
        classTablesContainer.setManaged(false);
        matrixTable.setVisible(false);
        matrixTable.setManaged(false);
        dailyLogTable.setVisible(false);
        dailyLogTable.setManaged(false);

        comboSubject.setVisible(false);
        comboSubject.setManaged(false);

        comboDepartment.setVisible(!isStudentLocked);
        comboDepartment.setManaged(!isStudentLocked);

        if (btnBackToList != null) {
            btnBackToList.setVisible(isStudentLocked);
            btnBackToList.setManaged(isStudentLocked);
        }

        if (saveChangesBox != null) {
            saveChangesBox.setVisible(false);
            saveChangesBox.setManaged(false);
        }

        if (isStudentLocked && selectedStudentRollNo != null) {
            studentHeaderLabel.setText("👤  Student Details: " + selectedStudentRollNo + " – " + selectedStudentName);
            studentHeaderLabel.setVisible(true);
            studentHeaderLabel.setManaged(true);
            hintBoxIndiv.setVisible(false);
            hintBoxIndiv.setManaged(false);
            individualDashboardContainer.setVisible(true);
            individualDashboardContainer.setManaged(true);
        } else {
            studentHeaderLabel.setVisible(false);
            studentHeaderLabel.setManaged(false);
            hintBoxIndiv.setVisible(true);
            hintBoxIndiv.setManaged(true);
            individualDashboardContainer.setVisible(false);
            individualDashboardContainer.setManaged(false);
            individualData.clear();
            chronoLogData.clear();
        }

        updateActiveButtonStyles(btnIndividualView, btnClassView);
    }

    @FXML
    private void switchToMatrixSummary() {
        isClassViewActive = true;
        isMatrixMode = true;

        moveSearchFieldToRow1(false);

        matrixFilterBox.setVisible(true);
        matrixFilterBox.setManaged(true);
        dailyFilterBox.setVisible(false);
        dailyFilterBox.setManaged(false);

        classTablesContainer.setVisible(true);
        classTablesContainer.setManaged(true);
        matrixTable.setVisible(true);
        matrixTable.setManaged(true);
        dailyLogTable.setVisible(false);
        dailyLogTable.setManaged(false);

        comboSubject.setVisible(true);
        comboSubject.setManaged(true);

        startDatePicker.setVisible(true);
        startDatePicker.setManaged(true);
        endDatePicker.setVisible(true);
        endDatePicker.setManaged(true);

        hintBoxMatrix.setVisible(true);
        hintBoxMatrix.setManaged(true);
        hintBoxDaily.setVisible(false);
        hintBoxDaily.setManaged(false);
        hintBoxIndiv.setVisible(false);
        hintBoxIndiv.setManaged(false);

        if (saveChangesBox != null) {
            saveChangesBox.setVisible(false);
            saveChangesBox.setManaged(false);
        }

        updateActiveButtonStyles(btnMatrixSummary, btnDailyLog);
        applyFilters();
    }

    @FXML
    private void switchToDailyLog() {
        isClassViewActive = true;
        isMatrixMode = false;
        matrixFilterBox.setVisible(false);
        matrixFilterBox.setManaged(false);
        dailyFilterBox.setVisible(true);
        dailyFilterBox.setManaged(true);

        classTablesContainer.setVisible(true);
        classTablesContainer.setManaged(true);
        matrixTable.setVisible(false);
        matrixTable.setManaged(false);
        dailyLogTable.setVisible(true);
        dailyLogTable.setManaged(true);

        hintBoxMatrix.setVisible(false);
        hintBoxMatrix.setManaged(false);
        hintBoxDaily.setVisible(true);
        hintBoxDaily.setManaged(true);
        hintBoxIndiv.setVisible(false);
        hintBoxIndiv.setManaged(false);

        if (saveChangesBox != null) {
            saveChangesBox.setVisible(true);
            saveChangesBox.setManaged(true);
        }

        updateActiveButtonStyles(btnDailyLog, btnMatrixSummary);
        applyFilters();
    }

    @FXML
    private void handleBackToList() {
        isStudentLocked = false;
        selectedStudentId = -1;
        selectedStudentRollNo = null;
        selectedStudentName = null;
        if (btnBackToList != null) {
            btnBackToList.setVisible(false);
            btnBackToList.setManaged(false);
        }

        studentHeaderLabel.setVisible(false);
        studentHeaderLabel.setManaged(false);

        comboDepartment.setVisible(true);
        comboDepartment.setManaged(true);
        comboDepartment.setValue("All Departments");
        comboYear.setValue("All Years");
        comboSubject.setValue("All Subjects");
        searchField.clear();

        isMatrixMode = true;
        switchToClassView();
    }

    private void updateActiveButtonStyles(Button activeBtn, Button inactiveBtn) {
        activeBtn.getStyleClass().remove("reset-button");
        if (!activeBtn.getStyleClass().contains("search-button")) {
            activeBtn.getStyleClass().add("search-button");
        }

        inactiveBtn.getStyleClass().remove("search-button");
        if (!inactiveBtn.getStyleClass().contains("reset-button")) {
            inactiveBtn.getStyleClass().add("reset-button");
        }
    }


    // =========================================================================
    // 6. INDIVIDUAL STUDENT DASHBOARD DATA LOADER
    // =========================================================================

    private void loadStudentIntoIndividualDashboard(AttendanceRecord tableRecord) {
        selectedStudentId = tableRecord.getNo();
        selectedStudentRollNo = tableRecord.getRollNo();
        selectedStudentName = tableRecord.getStudentName();
        isStudentLocked = true;

        refreshSelectedStudentDashboard();

        if (comboIndivLogCourse != null) comboIndivLogCourse.setValue("All Courses");
        if (comboIndivLogStatus != null) comboIndivLogStatus.setValue("All Status");
        if (indivLogStartDate != null) indivLogStartDate.setValue(null);
        if (indivLogEndDate != null) indivLogEndDate.setValue(LocalDate.now());
        applyIndivLogFilters();

        switchToIndividualView();
    }

    public void refreshSelectedStudentDashboard() {
        if (!isStudentLocked || selectedStudentId <= 0) {
            return;
        }

        StudentProfile profile = attendanceDAO.getStudentProfile(selectedStudentId);
        StudentDAO studentDAO = new StudentDAO();
        Student dbStudent = studentDAO.getStudentbyId(selectedStudentId);

        if (dbStudent != null) {
            selectedStudentName = dbStudent.getName();
            selectedStudentRollNo = dbStudent.getRollNo();
        } else if (profile != null) {
            selectedStudentName = profile.getFullName();
            selectedStudentRollNo = profile.getRollNo();
        }

        String majorStr = (dbStudent != null && dbStudent.getMajor() != null)
                ? dbStudent.getMajor()
                : (profile != null ? profile.getDeptCode() : "");

        String studentIdStr = dbStudent != null
                ? "21-" + majorStr + "-0" + dbStudent.getNo()
                : "21-" + majorStr + "-0" + selectedStudentId;
        String deptStr = "Department of " + majorStr;
        String yearSecStr = (dbStudent != null && dbStudent.getRollNo() != null)
                ? dbStudent.getRollNo()
                : (profile != null ? profile.getRollNo() : selectedStudentRollNo);

        String emailStr = (dbStudent != null && dbStudent.getEmail() != null && !dbStudent.getEmail().isEmpty())
                ? dbStudent.getEmail()
                : (profile != null && profile.getEmail() != null && !profile.getEmail().isEmpty()
                    ? profile.getEmail()
                    : selectedStudentName.toLowerCase().replace(" ", ".") + "@student.ytu.edu.mm");

        String phoneStr = (dbStudent != null && dbStudent.getPhoneNo() != null && !dbStudent.getPhoneNo().isEmpty())
                ? dbStudent.getPhoneNo()
                : (profile != null && profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "09-111111111");

        String addressStr = (dbStudent != null && dbStudent.getCurrentAddress() != null && !dbStudent.getCurrentAddress().isEmpty())
                ? dbStudent.getCurrentAddress()
                : (profile != null && profile.getAddress() != null && !profile.getAddress().isEmpty()
                    ? profile.getAddress()
                    : "Hlaing Township, Yangon");

        String photoPath = (dbStudent != null && dbStudent.getPhotoPath() != null && !dbStudent.getPhotoPath().trim().isEmpty())
                ? dbStudent.getPhotoPath()
                : (profile != null ? profile.getProfileImageUrl() : null);

        if (indivProfileImage != null) {
            indivProfileImage.setImage(null);
        }

        if (photoPath != null && !photoPath.trim().isEmpty()
                && !photoPath.trim().equalsIgnoreCase("null")
                && !photoPath.toLowerCase().contains("freddy.jpg")
                && indivProfileImage != null) {
            try {
                Image img = null;
                String cleanPath = photoPath.trim();

                if (cleanPath.startsWith("file:") || cleanPath.startsWith("http:") || cleanPath.startsWith("https:")) {
                    img = new Image(cleanPath, 86, 111, false, true, true);
                } else {
                    java.io.File localFile = new java.io.File(cleanPath);
                    if (localFile.exists()) {
                        img = new Image(localFile.toURI().toString(), 86, 111, false, true, true);
                    } else {
                        var resStream = getClass().getResourceAsStream(cleanPath.startsWith("/") ? cleanPath : "/" + cleanPath);
                        if (resStream != null) {
                            img = new Image(resStream, 86, 111, false, true);
                        }
                    }
                }

                if (img != null && !img.isError()) {
                    indivProfileImage.setImage(img);
                    indivProfileImage.setFitWidth(86.0);
                    indivProfileImage.setFitHeight(111.0);
                    indivProfileImage.setPreserveRatio(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (studentHeaderLabel != null)
            studentHeaderLabel.setText("👤  Student Details: " + selectedStudentRollNo + " – " + selectedStudentName);
        if (lblIndivName != null) lblIndivName.setText(selectedStudentName);
        if (lblIndivStudentId != null) lblIndivStudentId.setText(studentIdStr);
        if (lblIndivDept != null) lblIndivDept.setText(deptStr);
        if (lblIndivYearSec != null) lblIndivYearSec.setText(yearSecStr);
        if (lblIndivEmail != null) lblIndivEmail.setText(emailStr);
        if (lblIndivPhone != null) lblIndivPhone.setText(phoneStr);
        if (lblIndivAddress != null) lblIndivAddress.setText(addressStr);

        List<AttendanceRecord> breakdown = attendanceDAO.getStudentCourseBreakdown(selectedStudentId);
        for (int i = 0; i < breakdown.size(); i++) {
            breakdown.get(i).noProperty().set(i + 1);
        }
        individualData.setAll(breakdown);

        int totalAttended = profile != null ? profile.getTotalAttended() : 0;
        int totalClasses = profile != null ? profile.getTotalSessions() : 0;
        double overallRate = profile != null ? profile.getAttendancePercentage() : 0.0;
        String standing = profile != null && profile.getAcademicStanding() != null
                ? profile.getAcademicStanding()
                : (overallRate >= 90 ? "Excellent" : (overallRate >= 75 ? "Good" : "Warning"));

        lblTotalAttended.setText(String.valueOf(totalAttended));
        lblTotalClasses.setText(String.valueOf(totalClasses));
        lblDeptTotalAttended.setText(totalAttended + " / " + totalClasses);
        lblDeptTotalRate.setText(String.format("%.1f%%", overallRate));
        lblAcademicStanding.setText(standing);
        lblDeptTotalStanding.setText(standing);

        String successStyle = "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;";
        String warningStyle = "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;";

        if (standing.equalsIgnoreCase("Excellent") || standing.equalsIgnoreCase("Good")) {
            lblAcademicStanding.setStyle(successStyle);
            lblDeptTotalStanding.setStyle(successStyle);
        } else {
            lblAcademicStanding.setStyle(warningStyle);
            lblDeptTotalStanding.setStyle(warningStyle);
        }
    }


    // =========================================================================
    // 7. FILTERING & SEARCH LOGIC
    // =========================================================================

    @FXML
    private void applyFilters() {
        if (isClassViewActive && !isMatrixMode) {
            if (dailyDatePicker != null && dailyDatePicker.getValue() == null) {
                dailyDatePicker.setValue(LocalDate.now());
            }

            LocalDate selectedDate = dailyDatePicker != null ? dailyDatePicker.getValue() : null;
            String selectedPeriod = dailyPeriodCombo != null ? dailyPeriodCombo.getValue() : "All Periods";
            String searchText = dailySearchField != null && dailySearchField.getText() != null
                    ? dailySearchField.getText().trim() : "";

            boolean pChecked = chkPresent != null && chkPresent.isSelected();
            boolean aChecked = chkAbsent != null && chkAbsent.isSelected();
            boolean eChecked = chkExcuse != null && chkExcuse.isSelected();

            List<AttendanceRecord> logs = attendanceDAO.dailyLog(
                    selectedDate, selectedPeriod, searchText, pChecked, aChecked, eChecked);

            dailyLogData.setAll(logs);
            filteredDailyData.setPredicate(b -> true);
            dailyLogTable.setItems(filteredDailyData);
            dailyLogTable.refresh();

        } else if (isClassViewActive && isMatrixMode) {
            if (endDatePicker != null && endDatePicker.getValue() == null) {
                endDatePicker.setValue(LocalDate.now());
            }

            String searchText = searchField.getText() == null ? "" : searchField.getText().trim();
            String selectedDept = comboDepartment.getValue();
            String selectedYear = comboYear.getValue();
            String selectedSubject = comboSubject.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            boolean lowOnly = chkLowAttendance.isSelected();

            String deptParam = (selectedDept == null || selectedDept.equals("All Departments")) ? null : selectedDept;
            String yearParam = (selectedYear == null || selectedYear.equals("All Years")) ? null : selectedYear;

            List<AttendanceRecord> results;
            if (selectedSubject != null && !selectedSubject.trim().isEmpty() && !selectedSubject.equalsIgnoreCase("All Subjects")) {
                results = attendanceDAO.SubjectFilterMatrixSummary(
                        searchText, deptParam, yearParam, selectedSubject.trim(), startDate, endDate, lowOnly, selectedSubject.trim());
            } else {
                results = attendanceDAO.AtteandanceMatrixSummary(
                        searchText, deptParam, yearParam, null, startDate, endDate, lowOnly);
            }

            matrixData.setAll(results);
            filteredMatrixData.setPredicate(b -> true);

        } else {
            isClassViewActive = true;
            isMatrixMode = true;
            switchToClassView();
        }
    }

    @FXML
    private void applyIndivLogFilters() {
        if (indivLogEndDate != null && indivLogEndDate.getValue() == null) {
            indivLogEndDate.setValue(LocalDate.now());
        }
        currentPage = 1;
        currentPaginationWindow = 1;
        updateChronoTablePagination();
    }

    @FXML
    private void resetIndivLogFilters() {
        if (comboIndivLogCourse != null) comboIndivLogCourse.setValue("All Courses");
        if (comboIndivLogStatus != null) comboIndivLogStatus.setValue("All Status");
        if (indivLogStartDate != null) indivLogStartDate.setValue(null);
        if (indivLogEndDate != null) indivLogEndDate.setValue(LocalDate.now());
        applyIndivLogFilters();
    }

    @FXML
    private void handleReset() {
        if (isClassViewActive && isMatrixMode) {
            if (searchField != null) searchField.clear();
            if (comboDepartment != null) comboDepartment.setValue("All Departments");
            if (comboYear != null) comboYear.setValue("All Years");

            if (comboSubject != null) {
                comboSubject.setValue("All Subjects");
                if (comboSubject.getEditor() != null) {
                    comboSubject.getEditor().clear();
                }
            }

            if (startDatePicker != null) startDatePicker.setValue(null);
            if (endDatePicker != null) endDatePicker.setValue(LocalDate.now());
            if (chkLowAttendance != null) chkLowAttendance.setSelected(false);

            applyFilters();

        } else if (isClassViewActive && !isMatrixMode) {
            if (dailyDatePicker != null) dailyDatePicker.setValue(LocalDate.now());
            if (dailyPeriodCombo != null) dailyPeriodCombo.setValue("All Periods");
            if (dailySearchField != null) dailySearchField.clear();
            if (chkPresent != null) chkPresent.setSelected(false);
            if (chkAbsent != null) chkAbsent.setSelected(false);
            if (chkExcuse != null) chkExcuse.setSelected(false);

            applyFilters();

        } else {
            if (searchField != null) searchField.clear();
            if (comboDepartment != null) comboDepartment.setValue("All Departments");
            if (comboYear != null) comboYear.setValue("All Years");
            if (chkLowAttendance != null) chkLowAttendance.setSelected(false);
            resetIndivLogFilters();
        }
    }


    // =========================================================================
    // 8. CHRONOLOGICAL LOG PAGINATION
    // =========================================================================

    @FXML
    private void goToPage1() { changePage(btnPage1); }

    @FXML
    private void goToPage2() { changePage(btnPage2); }

    @FXML
    private void goToPage3() { changePage(btnPage3); }

    private void changePage(Button btn) {
        if (btn != null && btn.isVisible()) {
            currentPage = Integer.parseInt(btn.getText());
            updateChronoTablePagination();
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            if (currentPage < ((currentPaginationWindow - 1) * 3 + 1)) {
                currentPaginationWindow--;
            }
            updateChronoTablePagination();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalRecords = attendanceDAO.getLastFetchedTotalCount();
        int totalPages = (int) Math.ceil((double) totalRecords / rowsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            if (currentPage > (currentPaginationWindow * 3)) {
                currentPaginationWindow++;
            }
            updateChronoTablePagination();
        }
    }

    private void updateChronoTablePagination() {
        if (chronologicalLogTable == null) return;

        if (!isStudentLocked || selectedStudentId <= 0) {
            Platform.runLater(() -> {
                chronoLogData.clear();
                chronologicalLogTable.setItems(chronoLogData);
                chronologicalLogTable.refresh();
                if (lblPaginationInfo != null) {
                    lblPaginationInfo.setText("Showing 0 – 0 of 0 records");
                }
                updatePaginationButtonUI(1);
            });
            return;
        }

        String selectedCourse = comboIndivLogCourse != null ? comboIndivLogCourse.getValue() : "All Courses";
        String selectedStatus = comboIndivLogStatus != null ? comboIndivLogStatus.getValue() : "All Status";
        LocalDate fromDate = indivLogStartDate != null ? indivLogStartDate.getValue() : null;
        LocalDate toDate = indivLogEndDate != null ? indivLogEndDate.getValue() : null;

        int offset = (currentPage - 1) * rowsPerPage;
        List<AttendanceRecord> pageSlice = attendanceDAO.detailedChronoLog(
                selectedStudentId, selectedCourse, selectedStatus, fromDate, toDate, offset);

        int totalRecords = attendanceDAO.getLastFetchedTotalCount();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / rowsPerPage));

        if (currentPage > totalPages) {
            currentPage = totalPages;
            offset = (currentPage - 1) * rowsPerPage;
            pageSlice = attendanceDAO.detailedChronoLog(
                    selectedStudentId, selectedCourse, selectedStatus, fromDate, toDate, offset);
        }

        for (int i = 0; i < pageSlice.size(); i++) {
            pageSlice.get(i).noProperty().set(offset + i + 1);
        }

        final List<AttendanceRecord> finalSlice = pageSlice;
        final int finalOffset = offset;

        Platform.runLater(() -> {
            chronoLogData.setAll(finalSlice);
            chronologicalLogTable.setItems(chronoLogData);
            chronologicalLogTable.refresh();
            chronologicalLogTable.requestLayout();

            if (lblPaginationInfo != null) {
                if (totalRecords == 0) {
                    lblPaginationInfo.setText("Showing 0 – 0 of 0 records");
                } else {
                    int toIndex = Math.min(finalOffset + finalSlice.size(), totalRecords);
                    lblPaginationInfo.setText(String.format("Showing %d – %d of %d records", (finalOffset + 1), toIndex, totalRecords));
                }
            }

            updatePaginationButtonUI(totalPages);
        });
    }

    private void updatePaginationButtonUI(int totalPages) {
        int startPage = ((currentPaginationWindow - 1) * 3) + 1;
        Button[] pageBtns = {btnPage1, btnPage2, btnPage3};

        for (int i = 0; i < 3; i++) {
            int pageForBtn = startPage + i;

            if (pageBtns[i] != null) {
                if (pageForBtn <= totalPages) {
                    pageBtns[i].setText(String.valueOf(pageForBtn));
                    pageBtns[i].setVisible(true);
                    pageBtns[i].setManaged(true);

                    pageBtns[i].getStyleClass().removeAll("search-button", "reset-button");
                    if (pageForBtn == currentPage) {
                        pageBtns[i].getStyleClass().add("search-button");
                    } else {
                        pageBtns[i].getStyleClass().add("reset-button");
                    }
                } else {
                    pageBtns[i].setVisible(false);
                    pageBtns[i].setManaged(false);
                }
            }
        }

        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage != null) btnNextPage.setDisable(currentPage == totalPages || totalPages == 0);
    }


    // =========================================================================
    // 9. SAVE & TOGGLE ACTIONS
    // =========================================================================

    @FXML
    private void handleToggleStatus() {
        AttendanceRecord selected = dailyLogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Row Selected");
            alert.setHeaderText(null);
            alert.setContentText("Please select a student row in the Daily Log table first.");
            alert.getDialogPane().setMinWidth(400);
            alert.showAndWait();
            return;
        }

        String currentStatus = selected.getStatus() != null ? selected.getStatus().trim() : "Present";
        String nextStatus;
        if (currentStatus.equalsIgnoreCase("Present")) {
            nextStatus = "Absent";
        } else if (currentStatus.equalsIgnoreCase("Absent")) {
            nextStatus = "Excused";
        } else {
            nextStatus = "Present";
        }

        selected.statusProperty().set(nextStatus);

        if ("Present".equalsIgnoreCase(nextStatus)) {
            selected.remarksProperty().set("On Time");
        } else if ("Absent".equalsIgnoreCase(nextStatus)) {
            selected.remarksProperty().set("Absent Today");
        } else {
            // Clear automatic Present/Absent remarks when cycling to Excused so a custom excuse reason can be entered
            String existingRemark = selected.getRemarks();
            if (existingRemark == null
                    || existingRemark.equalsIgnoreCase("On Time")
                    || existingRemark.equalsIgnoreCase("Absent Today")) {
                selected.remarksProperty().set("-");
            }
        }

        String key = selected.getSessionId() + "_" + selected.getEnrollmentId();
        modifiedDailyRecords.put(key, selected);

        dailyLogTable.refresh();
    }

    @FXML
    private void handleMarkAllPresent() {
        if (filteredDailyData == null || filteredDailyData.isEmpty()) {
            return;
        }

        for (AttendanceRecord record : filteredDailyData) {
            if (!"Present".equalsIgnoreCase(record.getStatus())) {
                record.statusProperty().set("Present");
                record.remarksProperty().set("On Time");
                String key = record.getSessionId() + "_" + record.getEnrollmentId();
                modifiedDailyRecords.put(key, record);
            }
        }

        dailyLogTable.getSelectionModel().clearSelection();
        dailyLogTable.refresh();
    }

    @FXML
    private void handleSaveChanges() {
        for (AttendanceRecord record : modifiedDailyRecords.values()) {
            attendanceDAO.updateAttendanceStatus(record, record.getStatus(), record.getRemarks());
        }
        modifiedDailyRecords.clear();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Database Updated");
        alert.setContentText("Attendance status modifications have been successfully saved.");
        alert.getDialogPane().setMinWidth(380);
        alert.showAndWait();
    }

    @FXML
    private void handleResetDailyStatus() {
        if (modifiedDailyRecords != null) {
            modifiedDailyRecords.clear();
        }
        dailyLogTable.getSelectionModel().clearSelection();
        applyFilters();
    }
    public static void refreshLive() {
        if (instance != null) {
            Platform.runLater(() -> {
                instance.applyFilters();
                if (instance.isStudentLocked && instance.selectedStudentId > 0) {
                    instance.refreshSelectedStudentDashboard();
                }
            });
        }
    }
    
}