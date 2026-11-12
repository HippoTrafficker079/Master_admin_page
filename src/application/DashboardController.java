package application;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import application.dao.StudentDAO;
import application.dao.TeacherDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class DashboardController {

    // --- KPI Top Metric Labels ---
    @FXML private Label lblTotalStudents;
    @FXML private Label lblActiveStudents;
    @FXML private Label lblTotalTeachers;
    @FXML private Label lblTotalCourses;
    @FXML private Label lblTodaysAttendance;
    @FXML private Label lblCurrentDate;
    @FXML private ScrollPane rootScrollPane;

    // --- Tables & Charts ---
    @FXML private TableView<MarkRecord> markTable;
    @FXML private TableColumn<MarkRecord, String> colStudentId;
    @FXML private TableColumn<MarkRecord, String> colStudentName;
    @FXML private TableColumn<MarkRecord, String> colSubject;
    @FXML private TableColumn<MarkRecord, String> colMarks;
    @FXML private TableColumn<MarkRecord, String> colStatus;

    @FXML private PieChart departmentPieChart;
    @FXML private VBox departmentLegendBox;
    @FXML private BarChart<String, Number> enrollmentBarChart;
    @FXML private CategoryAxis xAxisEnrollment;
    @FXML private NumberAxis yAxisEnrollment;
    @FXML private ComboBox<String> comboEnrollmentFilter;

    @FXML private AreaChart<String, Number> attendanceAreaChart;
    @FXML private CategoryAxis xAxisAttendance;
    @FXML private NumberAxis yAxisAttendance;
    

    private static DashboardController instance;
    @FXML
    public void initialize() {
        instance = this;

        if (lblCurrentDate != null) {
            lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        }

        if (markTable != null) {
            markTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            colStudentId.setCellValueFactory(cell -> cell.getValue().studentIdProperty());
            colStudentName.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
            colSubject.setCellValueFactory(cell -> cell.getValue().subjectProperty());
            colMarks.setCellValueFactory(cell -> cell.getValue().marksProperty());
            colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        }

        if (comboEnrollmentFilter != null) {
            comboEnrollmentFilter.setItems(FXCollections.observableArrayList("Last 6 Years", "Last 4 Years", "Last 2 Years"));
            comboEnrollmentFilter.setValue("Last 6 Years");
            comboEnrollmentFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateDashboardMetricsAndCharts();
                }
            });
        }

        // Automatically refresh whenever Dashboard is switched into view
        if (rootScrollPane != null) {
            rootScrollPane.parentProperty().addListener((obs, oldParent, newParent) -> {
                if (newParent != null) {
                    updateDashboardMetricsAndCharts();
                }
            });
        }

        // Initial update
        updateDashboardMetricsAndCharts();


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

    private void updateDashboardMetricsAndCharts() {
        // 1. --- Fetch Fresh Live Counts Directly From Database DAOs ---
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

        // Update KPI UI Labels instantly
        if (lblTotalStudents != null) lblTotalStudents.setText(String.format("%,d", totalStudents));
        if (lblActiveStudents != null) lblActiveStudents.setText(String.format("%,d", activeStudents));
        if (lblTotalTeachers != null) lblTotalTeachers.setText(String.format("%,d", totalTeachers));
        if (lblTotalCourses != null) lblTotalCourses.setText(String.valueOf(totalCourses));
        if (lblTodaysAttendance != null) lblTodaysAttendance.setText(String.format("%.1f%%", todaysAttendance));

        ObservableList<Student> studentsObservable = dbStudents != null ? FXCollections.observableArrayList(dbStudents) 
                : (StudentController.masterData != null ? StudentController.masterData : FXCollections.observableArrayList());

        // 2. --- Update Student Enrollment Trend Bar Chart ---
        updateEnrollmentTrend(studentsObservable);

        // 3. --- Update Mark Registrations Table ---
        if (markTable != null) {
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

        // 4. --- Update Students by Department Pie Chart ---
        updateDepartmentPieChart(studentsObservable, totalStudents);

        // 5. --- Update Attendance Overview Area Chart ---
        updateAttendanceChart();
    }

    private String getStudentDepartmentSafe(Student s) {
        if (s == null) return "Pending";
        // Student model uses getMajor()
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

    private void updateDepartmentPieChart(ObservableList<Student> students, int totalStudents) {
        if (departmentPieChart == null) return;
        departmentPieChart.getData().clear();
        if (departmentLegendBox != null) {
            departmentLegendBox.getChildren().clear();
        }

        // Ordered list of majors matching ManageStudent (code -> {Full Name, Hex Color})
        Map<String, String[]> majorMeta = new LinkedHashMap<>();
        majorMeta.put("Arch",    new String[]{"Architecture",                                  "#0a192f"});
        majorMeta.put("ChE",     new String[]{"Chemical Engineering",                          "#0f2942"});
        majorMeta.put("CE",      new String[]{"Civil Engineering",                             "#13385c"});
        majorMeta.put("CEIT",    new String[]{"Computer Engineering and Information Technology","#1d4ed8"});
        majorMeta.put("EP",      new String[]{"Electrical Power Engineering",                  "#2563eb"});
        majorMeta.put("EC",      new String[]{"Electronic Engineering",                        "#3b82f6"});
        majorMeta.put("FE",      new String[]{"Food Engineering",                              "#60a5fa"});
        majorMeta.put("ME",      new String[]{"Mechanical Engineering",                        "#1e40af"});
        majorMeta.put("MC",      new String[]{"Mechatronic Engineering",                       "#0284c7"});
        majorMeta.put("Met",     new String[]{"Metallurgy Engineering and Material Science",   "#0f766e"});
        majorMeta.put("MnE",     new String[]{"Mining Engineering",                            "#0d9488"});
        majorMeta.put("PE",      new String[]{"Petroleum Engineering",                         "#10b981"});
        majorMeta.put("Tex",     new String[]{"Textile Engineering",                           "#94a3b8"});
        majorMeta.put("Pending", new String[]{"Pending Assignment",                            "#64748b"});

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

    private void updateAttendanceChart() {
        if (attendanceAreaChart == null) return;
        attendanceAreaChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance");
        series.getData().add(new XYChart.Data<>("Mon", 91));
        series.getData().add(new XYChart.Data<>("Tue", 88));
        series.getData().add(new XYChart.Data<>("Wed", 92));
        series.getData().add(new XYChart.Data<>("Thu", 94));
        series.getData().add(new XYChart.Data<>("Fri", 90));
        series.getData().add(new XYChart.Data<>("Sat", 92.3));

        attendanceAreaChart.getData().add(series);
        attendanceAreaChart.setLegendVisible(false);
    }
    
    public static void refreshDashboard() {
        if (instance != null) {
            Platform.runLater(() -> instance.updateDashboardMetricsAndCharts());
        }
    }
}