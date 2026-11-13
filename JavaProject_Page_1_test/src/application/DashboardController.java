package application;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import application.dao.StudentDAO;
import application.dao.TeacherDAO;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class DashboardController {

    // =========================================================================
    // 1. FXML UI COMPONENTS
    // =========================================================================

    // Root & KPI Summary Labels
    @FXML private ScrollPane rootScrollPane;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblActiveStudents;
    @FXML private Label lblTotalTeachers;
    @FXML private Label lblTotalCourses;
    @FXML private Label lblTodaysAttendance;
    @FXML private Label lblCurrentDate;

    // Recent Mark Registrations Table
    @FXML private TableView<MarkRecord> markTable;
    @FXML private TableColumn<MarkRecord, String> colStudentId;
    @FXML private TableColumn<MarkRecord, String> colStudentName;
    @FXML private TableColumn<MarkRecord, String> colSubject;
    @FXML private TableColumn<MarkRecord, String> colMarks;
    @FXML private TableColumn<MarkRecord, String> colStatus;

    // Students by Department Pie Chart
    @FXML private PieChart departmentPieChart;
    @FXML private VBox departmentLegendBox;

    // Student Enrollment Trend Bar Chart
    @FXML private BarChart<String, Number> enrollmentBarChart;
    @FXML private CategoryAxis xAxisEnrollment;
    @FXML private NumberAxis yAxisEnrollment;
    @FXML private ComboBox<String> comboEnrollmentFilter;

    // Attendance Overview Area Chart & Interactive Controls
    @FXML private AreaChart<String, Number> attendanceAreaChart;
    @FXML private CategoryAxis xAxisAttendance;
    @FXML private NumberAxis yAxisAttendance;
    @FXML private Label lblWeeklyAttendanceAvg;
    @FXML private Label lblAttendanceDateRange;
    @FXML private Button btnAttDaily;
    @FXML private Button btnAttWeekly;
    @FXML private Button btnAttMonthly;
    @FXML private ComboBox<String> comboAttendanceMajor;
    @FXML private Pane attendanceOverlayPane;


    // =========================================================================
    // 2. CONTROLLER STATE & SINGLETON INSTANCE
    // =========================================================================

    private static DashboardController instance;

    private int cachedTotalStudents = 510;
    private Timeline activeAttendanceAnim;
    private String lastRenderedPeriod = "";
    private String currentAttendancePeriod = "Weekly";


    // =========================================================================
    // 3. INITIALIZATION & REAL-TIME LISTENERS
    // =========================================================================

    @FXML
    public void initialize() {
        instance = this;

        if (lblCurrentDate != null) {
            lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        }

        setupMarkTable();
        setupChartFilters();
        setupDataRefreshListeners();

        // Initial load
        updateDashboardMetricsAndCharts();
    }

    private void setupMarkTable() {
        if (markTable != null) {
            markTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            colStudentId.setCellValueFactory(cell -> cell.getValue().studentIdProperty());
            colStudentName.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
            colSubject.setCellValueFactory(cell -> cell.getValue().subjectProperty());
            colMarks.setCellValueFactory(cell -> cell.getValue().marksProperty());
            colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        }
    }

    private void setupChartFilters() {
        if (comboEnrollmentFilter != null) {
            comboEnrollmentFilter.setItems(FXCollections.observableArrayList("Last 6 Years", "Last 4 Years", "Last 2 Years"));
            comboEnrollmentFilter.setValue("Last 6 Years");
            comboEnrollmentFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    ObservableList<Student> currentStudents = StudentController.masterData != null
                            ? StudentController.masterData
                            : FXCollections.observableArrayList();
                    updateEnrollmentTrend(currentStudents);
                }
            });
        }

        if (comboAttendanceMajor != null) {
            comboAttendanceMajor.setItems(FXCollections.observableArrayList(
                "All Majors", "C", "Mech", "EC", "CEIT", "EP", "McE", "ChE", "Tex", "Mn", "PE", "Arch", "Tele", "Met", "FE"
            ));
            comboAttendanceMajor.setValue("All Majors");
            comboAttendanceMajor.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateAttendanceChart();
                }
            });
        }
    }

    private void setupDataRefreshListeners() {
        // Automatically refresh whenever Dashboard is switched into view
        if (rootScrollPane != null) {
            rootScrollPane.parentProperty().addListener((obs, oldParent, newParent) -> {
                if (newParent != null) {
                    updateDashboardMetricsAndCharts();
                }
            });
        }

        // Listen for real-time changes in Student master data if loaded
        if (StudentController.masterData != null) {
            StudentController.masterData.addListener((javafx.collections.ListChangeListener.Change<? extends Student> c) -> {
                updateDashboardMetricsAndCharts();
            });
        }

        // Listen for real-time changes in Teacher master data if loaded
        if (TeacherController.masterData != null) {
            TeacherController.masterData.addListener((javafx.collections.ListChangeListener.Change<? extends Teacher> c) -> {
                updateDashboardMetricsAndCharts();
            });
        }
    }

    public static void refreshDashboard() {
        if (instance != null) {
            Platform.runLater(() -> instance.updateDashboardMetricsAndCharts());
        }
    }


    // =========================================================================
    // 4. KPI METRICS & MARK REGISTRATIONS TABLE REFRESH
    // =========================================================================

    private void updateDashboardMetricsAndCharts() {
        // 1. Fetch fresh live counts directly from Database DAOs
        int totalStudents = 0;
        int totalTeachers = 0;
        int totalCourses = 96;
        double todaysAttendance = 94.5;

        List<Student> dbStudents = null;

        try {
            StudentDAO studentDAO = new StudentDAO();
            dbStudents = studentDAO.getAllStudent();
            if (dbStudents != null) {
                totalStudents = dbStudents.size();
            }
        } catch (Exception e) {
            if (StudentController.masterData != null) {
                dbStudents = StudentController.masterData;
                totalStudents = dbStudents.size();
            }
        }

        try {
            TeacherDAO teacherDAO = new TeacherDAO();
            List<Teacher> dbTeachers = teacherDAO.getAllTeacher();
            if (dbTeachers != null) {
                totalTeachers = dbTeachers.size();
            }
        } catch (Exception e) {
            if (TeacherController.masterData != null) {
                totalTeachers = TeacherController.masterData.size();
            }
        }

        int activeStudents = (int) (totalStudents * 0.96);

        // Update KPI UI labels
        if (lblTotalStudents != null) lblTotalStudents.setText(String.format("%,d", totalStudents));
        if (lblActiveStudents != null) lblActiveStudents.setText(String.format("%,d", activeStudents));
        if (lblTotalTeachers != null) lblTotalTeachers.setText(String.format("%,d", totalTeachers));
        if (lblTotalCourses != null) lblTotalCourses.setText(String.valueOf(totalCourses));
        if (lblTodaysAttendance != null) lblTodaysAttendance.setText(String.format("%.1f%%", todaysAttendance));

        ObservableList<Student> studentsObservable = dbStudents != null ? FXCollections.observableArrayList(dbStudents)
                : (StudentController.masterData != null ? StudentController.masterData : FXCollections.observableArrayList());

        // 2. Update Student Enrollment Trend Bar Chart
        updateEnrollmentTrend(studentsObservable);

        // 3. Update Mark Registrations Table
        updateMarkRegistrationsTable(studentsObservable);

        // 4. Update Students by Department Pie Chart
        updateDepartmentPieChart(studentsObservable, totalStudents);

        // 5. Update Attendance Overview Area Chart
        updateAttendanceChart();
    }

    private void updateMarkRegistrationsTable(ObservableList<Student> studentsObservable) {
        if (markTable == null) return;

        ObservableList<MarkRecord> markRecords = FXCollections.observableArrayList();
        if (!studentsObservable.isEmpty()) {
            int limit = Math.min(studentsObservable.size(), 10);
            String[] subjects = {"Data Structures", "Database Systems", "Embedded Systems", "Digital Electronics", "Computer Networks", "Algorithms"};
            for (int i = studentsObservable.size() - 1; i >= Math.max(0, studentsObservable.size() - limit); i--) {
                Student s = studentsObservable.get(i);
                String id = getStudentProperty(s, "getStudentId", "getNo", "YTU2026018");
                String name = getStudentProperty(s, "getName", "getStudentName", "Student");
                String subj = subjects[i % subjects.length];
                String score = String.valueOf(75 + (i * 3) % 25);

                markRecords.add(new MarkRecord(
                    id.startsWith("YTU") ? id : "YTU20260" + id,
                    name,
                    subj,
                    score,
                    "● Passed"
                ));
            }
        } else {
            markRecords.addAll(
                new MarkRecord("YTU2026018", "Aye Chan", "Embedded Systems", "88", "● Passed"),
                new MarkRecord("YTU2026017", "Htet Htet", "Database Systems", "92", "● Passed")
            );
        }
        markTable.setItems(markRecords);
    }


    // =========================================================================
    // 5. STUDENT ENROLLMENT TREND BAR CHART
    // =========================================================================

    private void updateEnrollmentTrend(ObservableList<Student> students) {
        if (enrollmentBarChart == null) return;

        String filter = comboEnrollmentFilter != null && comboEnrollmentFilter.getValue() != null
                ? comboEnrollmentFilter.getValue()
                : "Last 6 Years";

        int currentYear = LocalDate.now().getYear(); // 2026
        int numYears = 6;
        if (filter.equals("Last 2 Years")) {
            numYears = 2;
        } else if (filter.equals("Last 4 Years")) {
            numYears = 4;
        } else {
            numYears = 6;
        }

        String[] years = new String[numYears];
        int startYear = currentYear - numYears + 1;
        for (int i = 0; i < numYears; i++) {
            years[i] = String.valueOf(startYear + i);
        }

        ObservableList<String> categoryList = FXCollections.observableArrayList(years);
        if (xAxisEnrollment != null) {
            xAxisEnrollment.getCategories().clear();
            xAxisEnrollment.setCategories(categoryList);
        }

        enrollmentBarChart.getData().clear();
        enrollmentBarChart.setLegendVisible(false);

        XYChart.Series<String, Number> s1 = new XYChart.Series<>(); s1.setName("First Year");
        XYChart.Series<String, Number> s2 = new XYChart.Series<>(); s2.setName("Second Year");
        XYChart.Series<String, Number> s3 = new XYChart.Series<>(); s3.setName("Third Year");
        XYChart.Series<String, Number> s4 = new XYChart.Series<>(); s4.setName("Fourth Year");
        XYChart.Series<String, Number> s5 = new XYChart.Series<>(); s5.setName("Fifth Year");

        for (int i = 0; i < years.length; i++) {
            String yr = years[i];
            int f1 = 0, f2 = 0, f3 = 0, f4 = 0, f5 = 0;

            switch (yr) {
                case "2021":
                    f1 = 400; f2 = 350; f3 = 300; f4 = 250; f5 = 200;
                    break;
                case "2022":
                    f1 = 500; f2 = 420; f3 = 350; f4 = 280; f5 = 220;
                    break;
                case "2023":
                    f1 = 650; f2 = 550; f3 = 450; f4 = 350; f5 = 280;
                    break;
                case "2024":
                    f1 = 800; f2 = 700; f3 = 600; f4 = 450; f5 = 350;
                    break;
                case "2025":
                    f1 = 50; f2 = 150; f3 = 250; f4 = 60; f5 = 450;
                    break;
                case "2026":
                    f1 = 1150; f2 = 900; f3 = 1000; f4 = 100; f5 = 50;
                    break;
                default:
                    f1 = 300; f2 = 250; f3 = 200; f4 = 150; f5 = 100;
                    break;
            }

            s1.getData().add(new XYChart.Data<>(yr, f1));
            s2.getData().add(new XYChart.Data<>(yr, f2));
            s3.getData().add(new XYChart.Data<>(yr, f3));
            s4.getData().add(new XYChart.Data<>(yr, f4));
            s5.getData().add(new XYChart.Data<>(yr, f5));
        }

        enrollmentBarChart.getData().addAll(s1, s2, s3, s4, s5);

        // Lock consistent colors to each series name regardless of filter/index shifts
        enrollmentBarChart.applyCss();
        for (XYChart.Series<String, Number> series : enrollmentBarChart.getData()) {
            String colorStyle = "";
            switch (series.getName()) {
                case "First Year":
                    colorStyle = "-fx-bar-fill: #1d4ed8;";
                    break;
                case "Second Year":
                    colorStyle = "-fx-bar-fill: #2563eb;";
                    break;
                case "Third Year":
                    colorStyle = "-fx-bar-fill: #3b82f6;";
                    break;
                case "Fourth Year":
                    colorStyle = "-fx-bar-fill: #60a5fa;";
                    break;
                case "Fifth Year":
                    colorStyle = "-fx-bar-fill: #93c5fd;";
                    break;
            }

            for (XYChart.Data<String, Number> data : series.getData()) {
                Node bar = data.getNode();
                if (bar != null && !colorStyle.isEmpty()) {
                    bar.setStyle(colorStyle);
                }
            }
        }

        Platform.runLater(() -> {
            if (xAxisEnrollment != null) {
                xAxisEnrollment.requestAxisLayout();
            }
            enrollmentBarChart.applyCss();
            enrollmentBarChart.requestLayout();
        });
    }


    // =========================================================================
    // 6. STUDENTS BY DEPARTMENT PIE CHART
    // =========================================================================

    private void updateDepartmentPieChart(ObservableList<Student> students, int totalStudents) {
        if (departmentPieChart == null) return;
        departmentPieChart.getData().clear();
        if (departmentLegendBox != null) {
            departmentLegendBox.getChildren().clear();
        }
        if (departmentLegendBox != null) {
            departmentLegendBox.getChildren().clear();
            departmentLegendBox.setAlignment(Pos.CENTER_LEFT);
        }
        
        // Ordered list of majors matching ManageStudent (code -> {Full Name, Hex Color})
        Map<String, String[]> majorMeta = new LinkedHashMap<>();
        majorMeta.put("Arch",    new String[]{"Architecture",                                  "#0a192f"});
        majorMeta.put("ChE",     new String[]{"Chemical Engineering",                          "#0f2942"});
        majorMeta.put("C",       new String[]{"Civil Engineering",                             "#13385c"});
        majorMeta.put("CEIT",    new String[]{"Computer Engineering and Information Technology","#1d4ed8"});
        majorMeta.put("EP",      new String[]{"Electrical Power Engineering",                  "#2563eb"});
        majorMeta.put("EC",      new String[]{"Electronic Engineering",                        "#3b82f6"});
        majorMeta.put("FE",      new String[]{"Food Engineering",                              "#60a5fa"});
        majorMeta.put("Mech",    new String[]{"Mechanical Engineering",                        "#1e40af"});
        majorMeta.put("McE",     new String[]{"Mechatronic Engineering",                       "#0284c7"});
        majorMeta.put("Met",     new String[]{"Metallurgical Engineering",                     "#0f766e"});
        majorMeta.put("Mn",      new String[]{"Mining Engineering",                            "#0d9488"});
        majorMeta.put("PE",      new String[]{"Petroleum Engineering",                         "#10b981"});
        majorMeta.put("Tele",    new String[]{"Telecommunication Engineering",                 "#64748b"});
        majorMeta.put("Tex",     new String[]{"Textile Engineering",                           "#94a3b8"});
        majorMeta.put("Pending", new String[]{"Pending Assignment",                            "#cbd5e1"});

        // Count students per major from live ManageStudent data
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String code : majorMeta.keySet()) {
            counts.put(code, 0);
        }

        int total = 0;
        if (students != null) {
            for (Student s : students) {
                String rawMajor = getStudentDepartmentSafe(s);
                String matchedKey = "Pending";
                for (String key : majorMeta.keySet()) {
                    if (key.equalsIgnoreCase(rawMajor)) {
                        matchedKey = key;
                        break;
                    }
                }
                counts.put(matchedKey, counts.get(matchedKey) + 1);
                total++;
            }
        }

        // Handle empty database gracefully
        if (total == 0) {
            if (departmentLegendBox != null) {
                Label emptyLbl = new Label("No student records available.");
                emptyLbl.setTextFill(Color.web("#64748b"));
                departmentLegendBox.getChildren().add(emptyLbl);
            }
            return;
        }

        // Build PieChart slices & matching right-side legend rows for all majors with >= 1 student
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        List<String> activeColors = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int count = entry.getValue();
            if (count > 0) {
                String code = entry.getKey();
                String fullName = majorMeta.get(code)[0];
                String hexColor = majorMeta.get(code)[1];
                double pct = (count * 100.0) / total;

                // Slice label on PieChart (e.g. "CEIT (35.0%)")
                pieData.add(new PieChart.Data(String.format("%s (%.1f%%)", code, pct), count));
                activeColors.add(hexColor);

                // Matching legend row on the right
                if (departmentLegendBox != null) {
                    HBox row = new HBox(10.0);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Region swatch = new Region();
                    swatch.setPrefSize(12.0, 12.0);
                    swatch.setMinSize(12.0, 12.0);
                    swatch.setStyle("-fx-background-color: " + hexColor + "; -fx-background-radius: 3;");

                    Label nameLbl = new Label(fullName);
                    nameLbl.setTextFill(Color.web("#334155"));
                    nameLbl.setFont(Font.font("System", FontWeight.BOLD, 11.5));

                    Pane spacer = new Pane();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label statLbl = new Label(String.format("%d (%.1f%%)", count, pct));
                    statLbl.setTextFill(Color.web("#64748b"));
                    statLbl.setFont(Font.font("System", 11.5));

                    row.getChildren().addAll(swatch, nameLbl, spacer, statLbl);
                    departmentLegendBox.getChildren().add(row);
                }
            }
        }

        departmentPieChart.setData(pieData);
        departmentPieChart.setLegendVisible(false);
        departmentPieChart.setLabelsVisible(true);

        // Apply matching slice colors immediately and after layout pass
        Runnable applySliceColors = () -> {
            departmentPieChart.applyCss();
            for (int i = 0; i < departmentPieChart.getData().size() && i < activeColors.size(); i++) {
                Node node = departmentPieChart.getData().get(i).getNode();
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + activeColors.get(i) + ";");
                }
            }
        };
        applySliceColors.run();
        Platform.runLater(applySliceColors);
    }


    // =========================================================================
    // 7. ATTENDANCE OVERVIEW AREA CHART & INTERACTIVE OVERLAY
    // =========================================================================

    @FXML
    private void handleAttDaily() {
        currentAttendancePeriod = "Daily";
        updateAttendanceToggleStyles();
        updateAttendanceChart();
    }

    @FXML
    private void handleAttWeekly() {
        currentAttendancePeriod = "Weekly";
        updateAttendanceToggleStyles();
        updateAttendanceChart();
    }

    @FXML
    private void handleAttMonthly() {
        currentAttendancePeriod = "Monthly";
        updateAttendanceToggleStyles();
        updateAttendanceChart();
    }

    private void updateAttendanceToggleStyles() {
        String activeStyle = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-radius: 4; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-cursor: hand;";

        if (btnAttDaily != null) btnAttDaily.setStyle("Daily".equals(currentAttendancePeriod) ? activeStyle : inactiveStyle);
        if (btnAttWeekly != null) btnAttWeekly.setStyle("Weekly".equals(currentAttendancePeriod) ? activeStyle : inactiveStyle);
        if (btnAttMonthly != null) btnAttMonthly.setStyle("Monthly".equals(currentAttendancePeriod) ? activeStyle : inactiveStyle);
    }

    private void updateAttendanceChart() {
        if (attendanceAreaChart == null) return;

        String selectedMajor = (comboAttendanceMajor != null && comboAttendanceMajor.getValue() != null)
                ? comboAttendanceMajor.getValue() : "All Majors";

        double majorOffset = "All Majors".equals(selectedMajor) ? 0.0 : ((Math.abs(selectedMajor.hashCode()) % 5) - 2) * 0.6;

        String[] categories;
        String[] popupTitles;
        double[] rawValues;
        String xAxisTitle;
        String dateRangeText;

        if ("Daily".equals(currentAttendancePeriod)) {
            categories = new String[]{"P-1", "P-2", "P-3", "P-4", "P-5", "P-6", "P-7", "P-8", "P-9"};
            rawValues = new double[]{93.0, 91.5, 94.0, 92.0, 89.5, 90.8, 92.4, 91.0, 88.5};
            xAxisTitle = "Class Periods";
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"));
            dateRangeText = "📅  " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"));
            popupTitles = new String[categories.length];
            for (int i = 0; i < categories.length; i++) {
                popupTitles[i] = "Period " + (i + 1) + " • " + todayStr;
            }
        } else if ("Monthly".equals(currentAttendancePeriod)) {
            categories = new String[]{"Week 1", "Week 2", "Week 3", "Week 4"};
            rawValues = new double[]{90.2, 91.8, 92.5, 91.4};
            xAxisTitle = "Weeks of Month";
            String monthStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            dateRangeText = "📅  " + monthStr;
            popupTitles = new String[]{
                "Week 1 • " + monthStr,
                "Week 2 • " + monthStr,
                "Week 3 • " + monthStr,
                "Week 4 • " + monthStr
            };
        } else {
            categories = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            rawValues = new double[]{91.0, 88.0, 92.0, 94.0, 90.0, 92.3};
            xAxisTitle = "Days of the Week";
            LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate saturday = monday.plusDays(5);
            dateRangeText = "📅  " + monday.format(DateTimeFormatter.ofPattern("EEE d")) + " – "
                    + saturday.format(DateTimeFormatter.ofPattern("EEE d MMM"));
            popupTitles = new String[categories.length];
            for (int i = 0; i < categories.length; i++) {
                popupTitles[i] = monday.plusDays(i).format(DateTimeFormatter.ofPattern("EEEE, MMM d"));
            }
        }

        // Compute final target percentages
        double[] targetValues = new double[categories.length];
        double sum = 0.0;
        double minFound = 100.0;
        for (int i = 0; i < categories.length; i++) {
            double val = Math.min(99.5, Math.max(80.5, Math.round((rawValues[i] + majorOffset) * 10.0) / 10.0));
            targetValues[i] = val;
            sum += val;
            minFound = Math.min(minFound, val);
        }

        if (lblAttendanceDateRange != null) {
            lblAttendanceDateRange.setText(dateRangeText);
        }

        if (lblWeeklyAttendanceAvg != null && targetValues.length > 0) {
            double avg = sum / targetValues.length;
            lblWeeklyAttendanceAvg.setText(String.format("%s Avg: %.1f%%", currentAttendancePeriod, avg));
        }

        double baseLowerBound = (minFound >= 80.0) ? 80.0 : 0.0;
        if (yAxisAttendance != null) {
            yAxisAttendance.setAutoRanging(false);
            yAxisAttendance.setUpperBound(100.0);
            yAxisAttendance.setLowerBound(baseLowerBound);
            yAxisAttendance.setTickUnit(minFound >= 80.0 ? 5.0 : 20.0);
        }

        if (activeAttendanceAnim != null) {
            activeAttendanceAnim.stop();
        }

        boolean sameStructure = currentAttendancePeriod.equals(lastRenderedPeriod)
                && !attendanceAreaChart.getData().isEmpty()
                && attendanceAreaChart.getData().get(0).getData().size() == categories.length;

        XYChart.Series<String, Number> series;
        if (sameStructure) {
            // Major filter changed: morph smoothly from current Y values to target Y values
            series = attendanceAreaChart.getData().get(0);
        } else {
            // Timeframe changed (Daily / Weekly / Monthly): rebuild categories & rise from baseline
            lastRenderedPeriod = currentAttendancePeriod;
            if (xAxisAttendance != null) {
                xAxisAttendance.setLabel(xAxisTitle);
                xAxisAttendance.getCategories().clear();
                xAxisAttendance.setCategories(FXCollections.observableArrayList(categories));
            }

            attendanceAreaChart.getData().clear();
            series = new XYChart.Series<>();
            series.setName("Attendance");
            for (String cat : categories) {
                series.getData().add(new XYChart.Data<>(cat, baseLowerBound));
            }
            attendanceAreaChart.getData().add(series);
            attendanceAreaChart.setLegendVisible(false);

            // Subtle fade-in for the chart area when switching timeframe tabs
            FadeTransition fade = new FadeTransition(Duration.millis(260), attendanceAreaChart);
            fade.setFromValue(0.35);
            fade.setToValue(1.0);
            fade.play();
        }

        final XYChart.Series<String, Number> animSeries = series;
        final String[] finalTitles = popupTitles;
        final double[] finalTargets = targetValues;

        Platform.runLater(() -> {
            if (xAxisAttendance != null) xAxisAttendance.requestAxisLayout();
            attendanceAreaChart.applyCss();
            attendanceAreaChart.requestLayout();

            List<Label> floatingLabels = renderAttendanceDotsOverlay(animSeries, finalTitles);

            // Build smooth 380ms interpolation across every data point & floating label
            Timeline timeline = new Timeline();
            Duration duration = Duration.millis(380);

            for (int i = 0; i < animSeries.getData().size(); i++) {
                XYChart.Data<String, Number> dataPoint = animSeries.getData().get(i);
                double targetY = finalTargets[i];

                KeyValue kv = new KeyValue(dataPoint.YValueProperty(), targetY, Interpolator.EASE_BOTH);
                timeline.getKeyFrames().add(new KeyFrame(duration, kv));

                // Keep the percentage text above each dot synced live during the animation
                if (i < floatingLabels.size()) {
                    Label lbl = floatingLabels.get(i);
                    dataPoint.YValueProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            double v = newVal.doubleValue();
                            lbl.setText((Math.abs(v - Math.round(v)) < 0.05)
                                    ? String.format("%.0f%%", v)
                                    : String.format("%.1f%%", v));
                        }
                    });
                }
            }

            // Snap exact final label formatting when animation finishes
            timeline.setOnFinished(e -> {
                for (int i = 0; i < floatingLabels.size() && i < finalTargets.length; i++) {
                    double v = finalTargets[i];
                    floatingLabels.get(i).setText((v % 1 == 0)
                            ? String.format("%.0f%%", v)
                            : String.format("%.1f%%", v));
                }
            });

            activeAttendanceAnim = timeline;
            timeline.play();
        });
    }

    private List<Label> renderAttendanceDotsOverlay(XYChart.Series<String, Number> series, String[] popupTitles) {
        List<Label> createdLabels = new ArrayList<>();
        if (attendanceOverlayPane == null) return createdLabels;
        attendanceOverlayPane.getChildren().clear();

        VBox activePopup = new VBox();
        activePopup.setVisible(false);
        activePopup.setManaged(false);
        activePopup.setMouseTransparent(true);

        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<String, Number> data = series.getData().get(i);
            Node dotNode = data.getNode();
            if (dotNode == null) continue;

            double pctValue = data.getYValue().doubleValue();
            String topPctText = (pctValue % 1 == 0)
                    ? String.format("%.0f%%", pctValue)
                    : String.format("%.1f%%", pctValue);

            Label pctLabel = new Label(topPctText);
            pctLabel.setMouseTransparent(true);
            pctLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 4; -fx-padding: 1 4;");
            attendanceOverlayPane.getChildren().add(pctLabel);
            createdLabels.add(pctLabel);

            Runnable updateLabelPos = () -> {
                Bounds sceneBounds = dotNode.localToScene(dotNode.getBoundsInLocal());
                if (sceneBounds != null && attendanceOverlayPane.getScene() != null) {
                    Bounds overlayBounds = attendanceOverlayPane.sceneToLocal(sceneBounds);
                    double centerX = (overlayBounds.getMinX() + overlayBounds.getMaxX()) / 2.0;
                    double topY = overlayBounds.getMinY();
                    pctLabel.applyCss();
                    double labelW = pctLabel.prefWidth(-1);
                    pctLabel.setLayoutX(centerX - (labelW / 2.0));
                    pctLabel.setLayoutY(Math.max(2.0, topY - 22.0));
                }
            };

            dotNode.boundsInParentProperty().addListener((obs, o, n) -> Platform.runLater(updateLabelPos));
            Platform.runLater(updateLabelPos);

            final String headerTitle = (i < popupTitles.length) ? popupTitles[i] : data.getXValue();
            dotNode.setStyle("-fx-cursor: hand;");

            // Hover tooltip reads live YValue so it always displays the exact current value
            dotNode.setOnMouseEntered(event -> {
                double livePct = data.getYValue().doubleValue();
                int livePresent = (int) Math.round((livePct / 100.0) * cachedTotalStudents);

                dotNode.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.65), 12, 0.5, 0, 0);");

                activePopup.getChildren().clear();
                activePopup.setAlignment(Pos.CENTER);

                Polygon arrow = new Polygon(0.0, 7.0, 7.0, 0.0, 14.0, 7.0);
                arrow.setFill(Color.web("#0f172a"));

                VBox card = new VBox(5.0);
                card.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 8; -fx-padding: 10 14; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.35), 10, 0, 0, 4);");

                Label lblTitle = new Label(headerTitle);
                lblTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");

                HBox row1 = new HBox(7.0, new Circle(3.5, Color.web("#60a5fa")),
                        styledBrightLabel(String.format("%.1f%% Attendance", livePct)));
                row1.setAlignment(Pos.CENTER_LEFT);

                HBox row2 = new HBox(7.0, new Circle(3.5, Color.web("#60a5fa")),
                        styledBrightLabel(livePresent + " Stud. Present"));
                row2.setAlignment(Pos.CENTER_LEFT);

                card.getChildren().addAll(lblTitle, row1, row2);
                activePopup.getChildren().addAll(arrow, card);
                activePopup.applyCss();
                activePopup.autosize();

                Bounds sceneBounds = dotNode.localToScene(dotNode.getBoundsInLocal());
                if (sceneBounds != null) {
                    Bounds overlayBounds = attendanceOverlayPane.sceneToLocal(sceneBounds);
                    double centerX = (overlayBounds.getMinX() + overlayBounds.getMaxX()) / 2.0;
                    double bottomY = overlayBounds.getMaxY();

                    double cardW = activePopup.prefWidth(-1);
                    double maxX = Math.max(4.0, attendanceOverlayPane.getWidth() - cardW - 4.0);
                    double targetX = Math.min(maxX, Math.max(4.0, centerX - (cardW / 2.0)));

                    activePopup.setLayoutX(targetX);
                    activePopup.setLayoutY(bottomY + 3.0);
                    activePopup.setVisible(true);
                }
            });

            dotNode.setOnMouseExited(event -> {
                dotNode.setStyle("-fx-cursor: hand;");
                activePopup.setVisible(false);
            });
        }

        attendanceOverlayPane.getChildren().add(activePopup);
        return createdLabels;
    }

    private Label styledBrightLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px; -fx-font-weight: bold;");
        return lbl;
    }


    // =========================================================================
    // 8. HELPER UTILITIES
    // =========================================================================

    private String getStudentDepartmentSafe(Student s) {
        if (s == null) return "Pending";
        String dept = getStudentProperty(s, "getMajor", "getDepartment", null);
        if (dept == null || dept.trim().isEmpty() || dept.trim().equals("-")) {
            return "Pending";
        }
        return dept.trim();
    }

    private String getStudentProperty(Object obj, String method1, String method2, String fallback) {
        try {
            Method m = obj.getClass().getMethod(method1);
            Object val = m.invoke(obj);
            if (val != null) return val.toString();
        } catch (Exception e1) {
            try {
                Method m = obj.getClass().getMethod(method2);
                Object val = m.invoke(obj);
                if (val != null) return val.toString();
            } catch (Exception e2) {
                // Ignore
            }
        }
        return fallback;
    }
}