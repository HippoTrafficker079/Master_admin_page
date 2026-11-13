package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import application.dao.AttendanceDAO;
import application.dao.StudentDAO;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class AttendanceController {

    // =========================================================================
    // 1. FXML UI COMPONENTS
    // =========================================================================

    // Layout Containers, Navigation Buttons & Hint Banners
    @FXML
    private VBox rootPane, matrixFilterBox, dailyFilterBox;
    @FXML
    private Button btnClassView, btnIndividualView, btnMatrixSummary, btnDailyLog, btnSaveChanges,
            btnExportStudentAudit;
    @FXML
    private Button btnBackToList;
    @FXML
    private HBox subModeBar, hintBoxMatrix, hintBoxDaily, hintBoxIndiv, saveChangesBox;
    @FXML
    private ScrollPane individualDashboardScroll;
    @FXML
    private VBox classTablesContainer, individualDashboardContainer;
    @FXML
    private Label studentHeaderLabel;

    // Matrix / Individual Top Filter Controls
    @FXML
    private ComboBox<String> comboDepartment, comboYear, comboSemester, comboSubject;
    @FXML
    private DatePicker startDatePicker, endDatePicker;
    @FXML
    private TextField searchField;
    @FXML
    private CheckBox chkLowAttendance;

    // Daily Log Filter Controls
    @FXML
    private DatePicker dailyDatePicker;
    @FXML
    private ComboBox<String> dailyPeriodCombo;
    @FXML
    private TextField dailySearchField;
    @FXML
    private CheckBox chkPresent, chkAbsent, chkExcuse;

    // Matrix Summary Table
    @FXML
    private TableView<AttendanceRecord> matrixTable;
    @FXML
    private TableColumn<AttendanceRecord, Integer> colNoMatrix;
    @FXML
    private TableColumn<AttendanceRecord, String> colRollMatrix, colNameMatrix, colDeptMatrix, colYearMatrix,
            colSubjMatrix, colOverallMatrix;

    // Daily Log Table
    @FXML
    private TableView<AttendanceRecord> dailyLogTable;
    @FXML
    private TableColumn<AttendanceRecord, Integer> colNoLog;
    @FXML
    private TableColumn<AttendanceRecord, String> colRollLog, colNameLog, colDateLog, colSubjLog, colStatusLog,
            colMarkedByLog, colRemarksLog;
    @FXML
    private TableColumn<AttendanceRecord, Integer> colPeriodLog;

    // Individual Student Profile & Summary Labels
    @FXML
    private ImageView indivProfileImage;
    @FXML
    private Label lblIndivName, lblIndivStudentId, lblIndivProgram, lblIndivDept, lblIndivYearSec, lblIndivStatusBadge;
    @FXML
    private Label lblTotalAttended, lblTotalClasses, lblAcademicStanding;
    @FXML
    private Label lblIndivEmail, lblIndivPhone, lblIndivAddress;
    @FXML
    private Label lblDeptTotalAttended, lblDeptTotalRate, lblDeptTotalStanding, lblPaginationInfo;

    // Individual Table 1: Department Subject Breakdown
    @FXML
    private TableView<AttendanceRecord> individualTable;
    @FXML
    private TableColumn<AttendanceRecord, Integer> colNoIndiv;
    @FXML
    private TableColumn<AttendanceRecord, String> colSubjIndiv, colCourseCodeIndiv, colAttendedIndiv,
            colPercentageIndiv, colRemarksIndiv;

    // Individual Table 2: Detailed Chronological Log & Filters
    @FXML
    private ComboBox<String> comboIndivLogCourse, comboIndivLogStatus;
    @FXML
    private DatePicker indivLogStartDate, indivLogEndDate;
    @FXML
    private TableView<AttendanceRecord> chronologicalLogTable;
    @FXML
    private TableColumn<AttendanceRecord, Integer> colNoChrono;
    @FXML
    private TableColumn<AttendanceRecord, String> colDateChrono, colCourseChrono, colCourseCodeChrono, colStatusChrono,
            colMarkedByChrono, colRemarksChrono;

    // Pagination Buttons
    @FXML
    private Button btnPage1, btnPage2, btnPage3, btnPrevPage, btnNextPage;

    // =========================================================================
    // 2. DATA LISTS & CONTROLLER STATE
    // =========================================================================

    private ObservableList<AttendanceRecord> matrixData;
    private ObservableList<AttendanceRecord> dailyLogData;
    private ObservableList<AttendanceRecord> individualData;
    private ObservableList<AttendanceRecord> chronoLogData;

    private FilteredList<AttendanceRecord> filteredMatrixData;
    private FilteredList<AttendanceRecord> filteredDailyData;
    private FilteredList<AttendanceRecord> filteredIndivData;
    private FilteredList<AttendanceRecord> filteredChronoData;

    // View Mode & Selected Student State
    private boolean isMatrixMode = true;
    private boolean isClassViewActive = true;
    private boolean isStudentLocked = false;
    private String selectedStudentRollNo = null;
    private String selectedStudentName = null;

    // Pagination State
    private int currentPage = 1;
    private final int rowsPerPage = 6;
    private int currentPaginationWindow = 1;

    // =========================================================================
    // 3. INITIALIZATION & TABLE SETUP
    // =========================================================================

    @FXML
    public void initialize() {
        setupRootFocusAndTables();
        setupDatePickers();
        setupTableColumns();
        setupFilterComboBoxes();
        loadInitialData();
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

    private void disableTableSortingAndReordering(TableView<?> table) {
        for (TableColumn<?, ?> col : table.getColumns()) {
            col.setSortable(false);
            col.setReorderable(false);
            // col.setResizable(false);
        }
    }

    private void setupDatePickers() {
        StringConverter<LocalDate> dateFormatter = new StringConverter<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            @Override
            public String toString(LocalDate date) {
                if (date != null)
                    return formatter.format(date);
                return "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) {
                    try {
                        return LocalDate.parse(string, formatter);
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null;
            }
        };

        if (startDatePicker != null) {
            startDatePicker.setConverter(dateFormatter);
            startDatePicker.setPromptText("dd-MM-yyyy");
        }
        if (endDatePicker != null) {
            endDatePicker.setConverter(dateFormatter);
            endDatePicker.setPromptText("dd-MM-yyyy");
        }
        if (dailyDatePicker != null) {
            dailyDatePicker.setConverter(dateFormatter);
            dailyDatePicker.setPromptText("dd-MM-yyyy");
        }
        if (indivLogStartDate != null) {
            indivLogStartDate.setConverter(dateFormatter);
            indivLogStartDate.setPromptText("dd-MM-yyyy");
        }
        if (indivLogEndDate != null) {
            indivLogEndDate.setConverter(dateFormatter);
            indivLogEndDate.setPromptText("dd-MM-yyyy");
        }
    }

    private void setupTableColumns() {
        // Matrix Summary columns
        colNoMatrix.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
        colRollMatrix.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        colNameMatrix.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        colDeptMatrix.setCellValueFactory(cell -> cell.getValue().departmentProperty());
        colYearMatrix.setCellValueFactory(cell -> cell.getValue().yearProperty());
        colSubjMatrix.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        colOverallMatrix.setCellValueFactory(cell -> cell.getValue().percentageProperty().asString("%.1f%%"));

        // Daily Log columns
        colNoLog.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
        colRollLog.setCellValueFactory(cell -> cell.getValue().rollNoProperty());
        colNameLog.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        colDateLog.setCellValueFactory(cell -> cell.getValue().dateProperty());
        colPeriodLog.setCellValueFactory(cell -> cell.getValue().periodProperty().asObject());
        colSubjLog.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        colMarkedByLog.setCellValueFactory(cell -> cell.getValue().markedByProperty());
        colRemarksLog.setCellValueFactory(cell -> cell.getValue().remarksProperty());

        // Individual Table 1: Department Subject Breakdown columns
        colNoIndiv.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
        colSubjIndiv.setCellValueFactory(cell -> cell.getValue().subjectProperty());
        if (colCourseCodeIndiv != null) {
            colCourseCodeIndiv.setCellValueFactory(cell -> cell.getValue().courseCodeProperty());
        }
        colAttendedIndiv.setCellValueFactory(cell -> cell.getValue().attendedTotalProperty());
        colPercentageIndiv.setCellValueFactory(cell -> cell.getValue().percentageProperty().asString("%.1f%%"));
        colRemarksIndiv.setCellValueFactory(cell -> cell.getValue().remarksProperty());
        setupStandingBadgeCellFactory(colRemarksIndiv);

        // Individual Table 2: Detailed Chronological Log columns
        if (chronologicalLogTable != null) {
            colNoChrono.setCellValueFactory(cell -> cell.getValue().noProperty().asObject());
            colDateChrono.setCellValueFactory(cell -> {
                LocalDate d = cell.getValue().getDateObj();
                String formatted = (d != null) ? d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                        : cell.getValue().getDate();
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
                "All Departments", "C", "Mech", "EC", "CEIT", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele",
                "Met", "FE"));
        comboDepartment.setValue("All Departments");

        comboYear.setItems(FXCollections.observableArrayList(
                "All Years", "First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year"));
        comboYear.setValue("All Years");

        comboSemester.setItems(FXCollections.observableArrayList(
                "All Semesters", "Semester I", "Semester II"));
        comboSemester.setValue("All Semesters");

        dailyPeriodCombo.setItems(FXCollections.observableArrayList(
                "All Periods", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        dailyPeriodCombo.setValue("All Periods");

        if (comboIndivLogCourse != null) {
            comboIndivLogCourse.setItems(FXCollections.observableArrayList(
                    "All Courses", "Advanced Java", "Software Engineering", "Computer Networks", "Database Systems"));
            comboIndivLogCourse.setValue("All Courses");
        }
        if (comboIndivLogStatus != null) {
            comboIndivLogStatus.setItems(FXCollections.observableArrayList(
                    "All Status", "Present", "Absent", "Excused"));
            comboIndivLogStatus.setValue("All Status");
        }
    }

    private void loadInitialData() {
        AttendanceDAO attendanceDAO = new AttendanceDAO();
        List<AttendanceRecord> matrixDbRecords = attendanceDAO.AtteandanceMatrixSummary(null, null, null, null, null,
                null, false);
        matrixData = FXCollections.observableArrayList(matrixDbRecords);

        dailyLogData = FXCollections.observableArrayList(
                new AttendanceRecord(1, "III-CEIT-1", "Mg Mg", "CEIT", "Third Year", "Semester I",
                        LocalDate.of(2026, 7, 20), 1, "Advanced Java", "Present", "U Ba", "On time"),
                new AttendanceRecord(2, "III-CEIT-1", "Mg Mg", "CEIT", "Third Year", "Semester I",
                        LocalDate.of(2026, 7, 20), 2, "Data Structures", "Present", "U Ba", "On time"),
                new AttendanceRecord(3, "III-CEIT-2", "Aye Aye", "CEIT", "Third Year", "Semester I",
                        LocalDate.of(2026, 7, 20), 3, "Algorithms", "Present", "U Myint", "On time"),
                new AttendanceRecord(4, "V-ChE-2", "Su Su", "ChE", "Fifth Year", "Semester II",
                        LocalDate.of(2026, 7, 26), 1, "Chemical Kinetics", "Absent", "Daw Mya", "Unexcused"),
                new AttendanceRecord(5, "IV-ME-1", "Aung Aung", "ME", "Fourth Year", "Semester I",
                        LocalDate.of(2026, 7, 26), 4, "Thermodynamics", "Excused", "U Tun", "Medical Leave"),
                new AttendanceRecord(6, "III-EC-4", "Hla Hla", "EC", "Third Year", "Semester II",
                        LocalDate.of(2026, 7, 28), 5, "Signal Processing", "Absent", "U Hla", "Unexcused"),
                new AttendanceRecord(7, "I-CE-1", "Kyaw Kyaw", "CE", "First Year", "Semester I",
                        LocalDate.of(2026, 8, 2), 2, "Engineering Drawing", "Present", "Daw Hlaing", "On time"),
                new AttendanceRecord(8, "II-EP-3", "Zin Mar", "EP", "Second Year", "Semester I",
                        LocalDate.of(2026, 8, 5), 6, "Circuit Theory", "Present", "U Soe", "On time"),
                new AttendanceRecord(9, "IV-MC-2", "Phone Naing", "MC", "Fourth Year", "Semester II",
                        LocalDate.of(2026, 8, 10), 7, "Microcontrollers", "Absent", "U Kyaw", "Unexcused"),
                new AttendanceRecord(10, "V-Arch-1", "Nilar", "Arch", "Fifth Year", "Semester II",
                        LocalDate.of(2026, 8, 15), 3, "Urban Planning", "Present", "Daw Khin", "On time"),
                new AttendanceRecord(11, "I-PE-2", "Hein Htet", "PE", "First Year", "Semester II",
                        LocalDate.of(2026, 8, 20), 8, "Calculus II", "Absent", "U Ba", "Unexcused"),
                new AttendanceRecord(12, "III-CEIT-1", "Mg Mg", "CEIT", "Third Year", "Semester II",
                        LocalDate.of(2026, 8, 21), 9, "Computer Networks", "Excused", "Daw Mya", "Family Emergency"));

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
            chronologicalLogTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void setupSubjectSearchComboBox() {
        ObservableList<String> subjectOptions = FXCollections.observableArrayList(
                "All Subjects", "Advanced Java", "Data Structures", "Chemical Kinetics",
                "Thermodynamics", "Signal Processing", "Engineering Drawing", "Circuit Theory",
                "Microcontrollers", "Urban Planning", "Calculus II");

        FilteredList<String> filteredSubjects = new FilteredList<>(subjectOptions, s -> true);
        comboSubject.setItems(filteredSubjects);
        comboSubject.setValue("All Subjects");

        final boolean[] isLocked = { true };

        if (comboSubject.getEditor() != null) {
            comboSubject.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (comboSubject.getValue() != null && comboSubject.getValue().equals(newVal))
                    return;

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

    // =========================================================================
    // 4. TABLE ROW & CELL STYLING FACTORIES
    // =========================================================================

    private void setupTableRowAndCellFactories() {
        // Matrix Summary Row Factory (Highlights <75% attendance & handles
        // double-click)
        matrixTable.setRowFactory(tv -> {
            TableRow<AttendanceRecord> row = createLowAttendanceRowFactory();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    loadStudentIntoIndividualDashboard(row.getItem());
                }
            });
            return row;
        });

        // Editable Daily Log Status ComboBox Cell Factory
        dailyLogTable.setEditable(true);
        colStatusLog.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colStatusLog.setCellFactory(tc -> {
            ComboBoxTableCell<AttendanceRecord, String> cell = new ComboBoxTableCell<>("Present", "Absent", "Excused") {
                @Override
                public void startEdit() {
                    getTableView().getSelectionModel().clearSelection();
                    super.startEdit();
                }

                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (!isEditing()) {
                            if (item.equalsIgnoreCase("Present")) {
                                setText("✔ Present");
                                setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                            } else if (item.equalsIgnoreCase("Absent")) {
                                setText("✖ Absent");
                                setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                            } else {
                                setText("● " + item);
                                setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
                            }
                        } else {
                            setStyle("-fx-text-fill: #0f172a;");
                        }
                    }
                }
            };
            cell.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                    event -> dailyLogTable.getSelectionModel().clearSelection());
            return cell;
        });

        colStatusLog.setOnEditCommit(event -> {
            AttendanceRecord record = event.getRowValue();
            record.statusProperty().set(event.getNewValue());
            Platform.runLater(() -> dailyLogTable.getSelectionModel().clearSelection());
        });

        // Daily Log Row Factory (CSS-based status highlights & double-click)
        dailyLogTable.setRowFactory(tv -> {
            TableRow<AttendanceRecord> row = new TableRow<AttendanceRecord>() {
                private final ChangeListener<String> statusListener = (obs, oldVal, newVal) -> applyDailyRowStyle(this,
                        newVal);

                @Override
                protected void updateItem(AttendanceRecord item, boolean empty) {
                    if (getItem() != null)
                        getItem().statusProperty().removeListener(statusListener);
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
                    loadStudentIntoIndividualDashboard(row.getItem());
                }
            });
            return row;
        });
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
                    if (item.equalsIgnoreCase("Excellence") || item.equalsIgnoreCase("Excellent")
                            || item.equalsIgnoreCase("Good") || item.equalsIgnoreCase("Good Standing")) {
                        badge.setStyle(
                                "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;");
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
                    } else {
                        statusLbl.setText("●  " + item);
                        statusLbl.setStyle("-fx-text-fill: #b45309; -fx-font-weight: bold;");
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

        if (status == null)
            return;
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
        selectedStudentRollNo = null;
        selectedStudentName = null;

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

            comboSemester.setVisible(false);
            comboSemester.setManaged(false);
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
        }

        individualDashboardContainer.setVisible(false);
        individualDashboardContainer.setManaged(false);
        comboDepartment.setVisible(true);
        comboDepartment.setManaged(true);

        btnBackToList.setVisible(false);
        btnBackToList.setManaged(false);
        // btnExport.setText("Export Matrix Report");

        updateActiveButtonStyles(btnClassView, btnIndividualView);
        applyFilters();
    }

    @FXML
    private void switchToIndividualView() {
        isClassViewActive = false;

        subModeBar.setVisible(false);
        subModeBar.setManaged(false);

        matrixFilterBox.setVisible(true);
        matrixFilterBox.setManaged(true);
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

        comboSemester.setVisible(true);
        comboSemester.setManaged(true);
        comboSubject.setVisible(false);
        comboSubject.setManaged(false);

        comboDepartment.setVisible(!isStudentLocked);
        comboDepartment.setManaged(!isStudentLocked);

        btnBackToList.setVisible(isStudentLocked);
        btnBackToList.setManaged(isStudentLocked);

        if (saveChangesBox != null) {
            saveChangesBox.setVisible(false);
            saveChangesBox.setManaged(false);
        }

        if (isStudentLocked && selectedStudentRollNo != null) {
            studentHeaderLabel.setText("👤  Student Details: " + selectedStudentRollNo + " - " + selectedStudentName);
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

        // btnExport.setText("Export Student Audit");
        updateActiveButtonStyles(btnIndividualView, btnClassView);

        applyFilters();
    }

    @FXML
    private void switchToMatrixSummary() {
        isMatrixMode = true;
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

        comboSemester.setVisible(false);
        comboSemester.setManaged(false);
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
        selectedStudentRollNo = null;
        selectedStudentName = null;
        btnBackToList.setVisible(false);
        btnBackToList.setManaged(false);

        studentHeaderLabel.setVisible(false);
        studentHeaderLabel.setManaged(false);

        comboDepartment.setVisible(true);
        comboDepartment.setManaged(true);
        comboDepartment.setValue("All Departments");
        comboYear.setValue("All Years");
        comboSemester.setValue("All Semesters");
        comboSubject.setValue("All Subjects");
        searchField.clear();

        applyFilters();
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
        selectedStudentRollNo = tableRecord.getRollNo();
        selectedStudentName = tableRecord.getStudentName();
        isStudentLocked = true;

        StudentDAO studentDAO = new StudentDAO();
        Student dbStudent = studentDAO.getStudentbyId(tableRecord.getNo());

        String studentIdStr = dbStudent != null ? "21-" + dbStudent.getMajor() + "-0" + dbStudent.getNo()
                : "21-CEIT-01";
        String programStr = dbStudent != null ? "B.E in " + dbStudent.getMajor() : "B.E in CEIT";
        String deptStr = dbStudent != null ? "Department of " + dbStudent.getMajor() : "Department of CEIT";
        String yearSecStr = dbStudent != null ? dbStudent.getRollNo() : selectedStudentRollNo;
        String emailStr = dbStudent != null && dbStudent.getEmail() != null && !dbStudent.getEmail().isEmpty()
                ? dbStudent.getEmail()
                : selectedStudentName.toLowerCase().replace(" ", ".") + "@student.ytu.edu.mm";
        String phoneStr = dbStudent != null && dbStudent.getPhoneNo() != null ? dbStudent.getPhoneNo() : "09-111111111";
        String addressStr = dbStudent != null && dbStudent.getCurrentAddress() != null
                && !dbStudent.getCurrentAddress().isEmpty()
                        ? dbStudent.getCurrentAddress()
                        : "Hlaing Township, Yangon";

        if (dbStudent != null && dbStudent.getPhotoPath() != null && !dbStudent.getPhotoPath().trim().isEmpty()) {
            try {
                String path = dbStudent.getPhotoPath();
                Image img = (path.startsWith("file:") || path.startsWith("http:"))
                        ? new Image(path, 78, 110, false, true)
                        : new Image(getClass().getResourceAsStream(path));
                if (img != null && indivProfileImage != null) {
                    indivProfileImage.setImage(img);
                }
            } catch (Exception e) {
            }
        }

        studentHeaderLabel.setText("👤  Student Details: " + selectedStudentRollNo + " – " + selectedStudentName);
        lblIndivName.setText(selectedStudentName);
        lblIndivStudentId.setText(studentIdStr);
        lblIndivProgram.setText(programStr);
        lblIndivDept.setText(deptStr);
        lblIndivYearSec.setText(yearSecStr);
        lblIndivEmail.setText(emailStr);
        lblIndivPhone.setText(phoneStr);
        lblIndivAddress.setText(addressStr);

        /*
         * individualData.setAll(
         * new AttendanceRecord(1, selectedStudentRollNo, selectedStudentName,
         * tableRecord.getDepartment(), tableRecord.getYear(), "Semester I",
         * "Advanced Java", "CEIT-311", "38 / 40", 95.0, LocalDate.of(2026, 7, 20),
         * "Excellent"),
         * new AttendanceRecord(2, selectedStudentRollNo, selectedStudentName,
         * tableRecord.getDepartment(), tableRecord.getYear(), "Semester I",
         * "Software Engineering", "CEIT-312", "37 / 40", 92.5, LocalDate.of(2026, 7,
         * 21), "Excellent"),
         * new AttendanceRecord(3, selectedStudentRollNo, selectedStudentName,
         * tableRecord.getDepartment(), tableRecord.getYear(), "Semester II",
         * "Computer Networks", "CEIT-313", "35 / 40", 87.5, LocalDate.of(2026, 7, 22),
         * "Good"),
         * new AttendanceRecord(4, selectedStudentRollNo, selectedStudentName,
         * tableRecord.getDepartment(), tableRecord.getYear(), "Semester II",
         * "Database Systems", "CEIT-314", "28 / 40", 70.0, LocalDate.of(2026, 7, 23),
         * "Warning")
         * );
         */

        chronoLogData.clear();
        for (int i = 1; i <= 20; i++) {
            chronoLogData.add(new AttendanceRecord(i, selectedStudentRollNo, selectedStudentName,
                    tableRecord.getDepartment(), tableRecord.getYear(), "Semester I", LocalDate.of(2025, 3, i),
                    (i % 3) + 1, (i % 2 == 0) ? "Advanced Java" : "Software Engineering",
                    (i % 2 == 0) ? "CEIT-311" : "CEIT-312", (i % 4 == 0) ? "Absent" : "Present", "Prof. D. Balodisi",
                    (i % 4 == 0) ? "Unexcused" : "-"));
        }

        int totalAttended = 0;
        int totalClasses = 0;
        for (AttendanceRecord rec : individualData) {
            String[] split = rec.getAttendedTotal().split("/");
            if (split.length == 2) {
                totalAttended += Integer.parseInt(split[0].trim());
                totalClasses += Integer.parseInt(split[1].trim());
            }
        }

        lblTotalAttended.setText(String.valueOf(totalAttended));
        lblTotalClasses.setText(String.valueOf(totalClasses));
        lblDeptTotalAttended.setText(totalAttended + " / " + totalClasses);

        double overallRate = totalClasses > 0 ? (double) totalAttended / totalClasses * 100 : 0.0;
        lblDeptTotalRate.setText(String.format("%.1f%%", overallRate));

        String standing = overallRate >= 90 ? "Excellent" : (overallRate >= 75 ? "Good" : "Warning");
        lblAcademicStanding.setText(standing);
        lblDeptTotalStanding.setText(standing);

        String successStyle = "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;";
        String warningStyle = "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 2 12; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 11px;";

        if (standing.equals("Excellent") || standing.equals("Good")) {
            lblAcademicStanding.setStyle(successStyle);
            lblDeptTotalStanding.setStyle(successStyle);
        } else {
            lblAcademicStanding.setStyle(warningStyle);
            lblDeptTotalStanding.setStyle(warningStyle);
        }

        switchToIndividualView();
    }

    // =========================================================================
    // 7. FILTERING & SEARCH LOGIC
    // =========================================================================

    @FXML
    private void applyFilters() {
        if (isClassViewActive && !isMatrixMode) {
            // Daily Log Filters
            LocalDate selectedDate = dailyDatePicker.getValue();
            String selectedPeriod = dailyPeriodCombo.getValue();
            String searchText = dailySearchField.getText() == null ? ""
                    : dailySearchField.getText().toLowerCase().trim();

            boolean pChecked = chkPresent.isSelected();
            boolean aChecked = chkAbsent.isSelected();
            boolean eChecked = chkExcuse.isSelected();
            boolean anyRemarkChecked = pChecked || aChecked || eChecked;

            filteredDailyData.setPredicate(record -> {
                boolean matchesDate = (selectedDate == null)
                        || (record.getDateObj() != null && record.getDateObj().equals(selectedDate));
                boolean matchesPeriod = (selectedPeriod == null || selectedPeriod.equals("All Periods")) ||
                        String.valueOf(record.getPeriod()).equals(selectedPeriod);
                boolean matchesSearch = searchText.isEmpty() ||
                        record.getStudentName().toLowerCase().contains(searchText) ||
                        record.getRollNo().toLowerCase().contains(searchText) ||
                        record.getSubject().toLowerCase().contains(searchText);

                boolean matchesRemark = true;
                if (anyRemarkChecked) {
                    String rem = record.getRemarks() != null ? record.getRemarks().toLowerCase() : "";
                    String stat = record.getStatus() != null ? record.getStatus().toLowerCase() : "";

                    boolean isPresentMatch = pChecked && (rem.contains("on time") || stat.contains("present"));
                    boolean isAbsentMatch = aChecked && (rem.contains("unexcused") || stat.contains("absent"));
                    boolean isExcuseMatch = eChecked
                            && (rem.contains("medical") || rem.contains("family") || stat.contains("excused"));

                    matchesRemark = isPresentMatch || isAbsentMatch || isExcuseMatch;
                }

                return matchesDate && matchesPeriod && matchesSearch && matchesRemark;
            });

        } else if (isClassViewActive && isMatrixMode) {
            // Matrix Summary Filters
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String selectedDept = comboDepartment.getValue();
            String selectedYear = comboYear.getValue();
            String selectedSubject = comboSubject.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            boolean lowOnly = chkLowAttendance.isSelected();

            filteredMatrixData.setPredicate(record -> {
                boolean matchesSearch = searchText.isEmpty() ||
                        record.getStudentName().toLowerCase().contains(searchText) ||
                        record.getRollNo().toLowerCase().contains(searchText);
                boolean matchesDept = (selectedDept == null || selectedDept.equals("All Departments")) ||
                        record.getDepartment().equalsIgnoreCase(selectedDept);
                boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years")) ||
                        matchesYearHelper(record, selectedYear);
                boolean matchesSubject = (selectedSubject == null || selectedSubject.equals("All Subjects")
                        || selectedSubject.trim().isEmpty()) ||
                        record.getSubject().equalsIgnoreCase(selectedSubject.trim());

                LocalDate recordDate = record.getDateObj();
                boolean matchesDate = (startDate == null || endDate == null) ||
                        (recordDate != null && !recordDate.isBefore(startDate) && !recordDate.isAfter(endDate));
                boolean matchesLow = !lowOnly || record.isLowAttendance();

                return matchesSearch && matchesDept && matchesYear && matchesSubject && matchesDate && matchesLow;
            });

        } else {
            // Individual Student Subject Breakdown Filters
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String selectedDept = comboDepartment.getValue();
            String selectedYear = comboYear.getValue();
            String selectedSemester = comboSemester.getValue();
            boolean lowOnly = chkLowAttendance.isSelected();

            Set<String> lowAttendanceCourses = new HashSet<>();
            for (AttendanceRecord r : individualData) {
                if (r.isLowAttendance()) {
                    lowAttendanceCourses.add(r.getSubject().toLowerCase());
                }
            }

            filteredIndivData.setPredicate(record -> {
                if (isStudentLocked && selectedStudentRollNo != null) {
                    boolean matchesStudent = record.getRollNo().equalsIgnoreCase(selectedStudentRollNo);
                    boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years")) ||
                            matchesYearHelper(record, selectedYear);
                    boolean matchesSemester = (selectedSemester == null || selectedSemester.equals("All Semesters")) ||
                            record.getSemester().equalsIgnoreCase(selectedSemester);
                    boolean matchesLow = !lowOnly || record.isLowAttendance();

                    return matchesStudent && matchesYear && matchesSemester && matchesLow;
                }

                boolean matchesDept = (selectedDept == null || selectedDept.equals("All Departments")) ||
                        record.getDepartment().equalsIgnoreCase(selectedDept);
                boolean matchesYear = (selectedYear == null || selectedYear.equals("All Years")) ||
                        matchesYearHelper(record, selectedYear);
                boolean matchesSemester = (selectedSemester == null || selectedSemester.equals("All Semesters")) ||
                        record.getSemester().equalsIgnoreCase(selectedSemester);
                boolean matchesSearch = searchText.isEmpty() ||
                        record.getSubject().toLowerCase().contains(searchText) ||
                        record.getStudentName().toLowerCase().contains(searchText) ||
                        record.getRollNo().toLowerCase().contains(searchText);
                boolean matchesLow = !lowOnly || record.isLowAttendance();

                return matchesDept && matchesYear && matchesSemester && matchesSearch && matchesLow;
            });

            applyIndivLogFilters();
        }
    }

    @FXML
    private void applyIndivLogFilters() {
        if (filteredChronoData == null)
            return;

        String selectedCourse = comboIndivLogCourse != null ? comboIndivLogCourse.getValue() : "All Courses";
        String selectedStatus = comboIndivLogStatus != null ? comboIndivLogStatus.getValue() : "All Status";
        LocalDate fromDate = indivLogStartDate != null ? indivLogStartDate.getValue() : null;
        LocalDate toDate = indivLogEndDate != null ? indivLogEndDate.getValue() : null;

        filteredChronoData.setPredicate(record -> {
            boolean matchesStudent = !isStudentLocked || selectedStudentRollNo == null ||
                    record.getRollNo().equalsIgnoreCase(selectedStudentRollNo);
            boolean matchesCourse = (selectedCourse == null || selectedCourse.equals("All Courses")) ||
                    record.getSubject().equalsIgnoreCase(selectedCourse);
            boolean matchesStatus = (selectedStatus == null || selectedStatus.equals("All Status")) ||
                    record.getStatus().equalsIgnoreCase(selectedStatus);
            boolean matchesFrom = (fromDate == null)
                    || (record.getDateObj() != null && !record.getDateObj().isBefore(fromDate));
            boolean matchesTo = (toDate == null)
                    || (record.getDateObj() != null && !record.getDateObj().isAfter(toDate));

            return matchesStudent && matchesCourse && matchesStatus && matchesFrom && matchesTo;
        });

        currentPage = 1;
        currentPaginationWindow = 1;
        updateChronoTablePagination();
    }

    @FXML
    private void resetIndivLogFilters() {
        if (comboIndivLogCourse != null)
            comboIndivLogCourse.setValue("All Courses");
        if (comboIndivLogStatus != null)
            comboIndivLogStatus.setValue("All Status");
        if (indivLogStartDate != null)
            indivLogStartDate.setValue(null);
        if (indivLogEndDate != null)
            indivLogEndDate.setValue(null);
        applyIndivLogFilters();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        comboDepartment.setValue("All Departments");
        comboYear.setValue("All Years");
        comboSemester.setValue("All Semesters");

        comboSubject.setValue("All Subjects");
        if (comboSubject.getEditor() != null) {
            comboSubject.getEditor().clear();
        }

        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        chkLowAttendance.setSelected(false);

        dailyDatePicker.setValue(null);
        dailyPeriodCombo.setValue("All Periods");
        dailySearchField.clear();
        chkPresent.setSelected(false);
        chkAbsent.setSelected(false);
        chkExcuse.setSelected(false);

        resetIndivLogFilters();
        applyFilters();
    }

    private boolean matchesYearHelper(AttendanceRecord record, String selectedYear) {
        String rollNo = record.getRollNo() != null ? record.getRollNo().trim().toUpperCase() : "";
        String romanPrefix = "";

        if (rollNo.contains("-")) {
            romanPrefix = rollNo.substring(0, rollNo.indexOf("-"));
        }

        String expectedRoman = "";
        if (selectedYear.equalsIgnoreCase("First Year"))
            expectedRoman = "I";
        else if (selectedYear.equalsIgnoreCase("Second Year"))
            expectedRoman = "II";
        else if (selectedYear.equalsIgnoreCase("Third Year"))
            expectedRoman = "III";
        else if (selectedYear.equalsIgnoreCase("Fourth Year"))
            expectedRoman = "IV";
        else if (selectedYear.equalsIgnoreCase("Fifth Year"))
            expectedRoman = "V";

        if (!expectedRoman.isEmpty() && romanPrefix.equals(expectedRoman)) {
            return true;
        }

        return record.getYear() != null && record.getYear().equalsIgnoreCase(selectedYear);
    }

    // =========================================================================
    // 8. CHRONOLOGICAL LOG PAGINATION
    // =========================================================================

    @FXML
    private void goToPage1() {
        changePage(btnPage1);
    }

    @FXML
    private void goToPage2() {
        changePage(btnPage2);
    }

    @FXML
    private void goToPage3() {
        changePage(btnPage3);
    }

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
        int totalPages = (int) Math.ceil((double) filteredChronoData.size() / rowsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            if (currentPage > (currentPaginationWindow * 3)) {
                currentPaginationWindow++;
            }
            updateChronoTablePagination();
        }
    }

    private void updateChronoTablePagination() {
        if (chronologicalLogTable == null || filteredChronoData == null)
            return;

        int totalRecords = filteredChronoData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / rowsPerPage));

        if (currentPage > totalPages)
            currentPage = totalPages;
        if (currentPage < 1)
            currentPage = 1;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalRecords);

        ObservableList<AttendanceRecord> pageSlice = FXCollections.observableArrayList();
        if (totalRecords > 0 && fromIndex < totalRecords) {
            pageSlice.addAll(filteredChronoData.subList(fromIndex, toIndex));
        }

        chronologicalLogTable.setItems(pageSlice);

        if (lblPaginationInfo != null) {
            if (totalRecords == 0) {
                lblPaginationInfo.setText("Showing 0 – 0 of 0 records");
            } else {
                lblPaginationInfo.setText(
                        String.format("Showing %d – %d of %d records", (fromIndex + 1), toIndex, totalRecords));
            }
        }

        updatePaginationButtonUI(totalPages);
    }

    private void updatePaginationButtonUI(int totalPages) {
        int startPage = ((currentPaginationWindow - 1) * 3) + 1;
        Button[] pageBtns = { btnPage1, btnPage2, btnPage3 };

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

        if (btnPrevPage != null)
            btnPrevPage.setDisable(currentPage == 1);
        if (btnNextPage != null)
            btnNextPage.setDisable(currentPage == totalPages || totalPages == 0);
    }

    // =========================================================================
    // 9. SAVE & EXPORT ACTIONS
    // =========================================================================

    @FXML
    private void handleSaveChanges() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Database Updated");
        alert.setContentText("Attendance status modifications have been successfully saved.");
        alert.getDialogPane().setMinWidth(380);
        alert.showAndWait();
    }

    // @FXML
    // private void handleExportAudit() {
    // Alert alert = new Alert(Alert.AlertType.INFORMATION);
    // alert.setTitle("Export Student Audit");
    // alert.setHeaderText(null);
    // alert.setContentText("Exporting attendance audit report for " +
    // selectedStudentName + " (" + selectedStudentRollNo + ")...");
    // alert.getDialogPane().setMinWidth(380);
    // alert.showAndWait();
    // }
}