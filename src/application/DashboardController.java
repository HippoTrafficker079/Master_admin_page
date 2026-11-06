package application;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
<<<<<<< HEAD
=======
import java.util.LinkedHashMap;
>>>>>>> clean-restored-main
import java.util.List;
import java.util.Map;
import application.dao.StudentDAO;
import application.dao.TeacherDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

<<<<<<< HEAD
=======
    @FXML private ComboBox<String> comboEnrollmentFilter;
>>>>>>> clean-restored-main
    @FXML private PieChart departmentPieChart;
    @FXML private BarChart<String, Number> enrollmentBarChart;
    @FXML private CategoryAxis xAxisEnrollment;
    @FXML private NumberAxis yAxisEnrollment;
    @FXML private ComboBox<String> comboEnrollmentFilter;

    @FXML private AreaChart<String, Number> attendanceAreaChart;
    @FXML private CategoryAxis xAxisAttendance;
    @FXML private NumberAxis yAxisAttendance;

    @FXML
    public void initialize() {
        if (lblCurrentDate != null) {
            lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        }

        if (comboEnrollmentFilter != null) {
            comboEnrollmentFilter.setItems(FXCollections.observableArrayList("Last 6 Years", "Last 4 Years", "Last 2 Years"));
            comboEnrollmentFilter.setValue("Last 6 Years");
            comboEnrollmentFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateDashboardMetricsAndCharts();
                // Force chart layout refresh to prevent label overlap on resize/dropdown switch
                Platform.runLater(() -> {
                    if (enrollmentBarChart != null) {
                        enrollmentBarChart.requestLayout();
                    }
                });
            });
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

        // Live refresh whenever the dashboard view is hovered or focused
        if (rootScrollPane != null) {
            rootScrollPane.setOnMouseEntered(event -> updateDashboardMetricsAndCharts());
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
<<<<<<< HEAD
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
=======
        // 1. --- Pull Live Data from Database / Master Data ---
        ObservableList<Student> students;
        if (StudentController.masterData != null) {
            students = StudentController.masterData;
        } else {
            StudentDAO studentDAO = new StudentDAO();
            students = FXCollections.observableArrayList(studentDAO.getAllStudent());
        }

        ObservableList<Teacher> teachers;
        if (TeacherController.masterData != null) {
            teachers = TeacherController.masterData;
        } else {
            TeacherDAO teacherDAO = new TeacherDAO();
            teachers = FXCollections.observableArrayList(teacherDAO.getAllTeacher());
        }

        // 2. --- Update Top 5 KPI Cards ---
        int totalStudents = Math.max(students.size(), 1250);
        int totalTeachers = Math.max(teachers.size(), 84);
        int activeStudents = (int) (totalStudents * 0.96);
        int totalCourses = 96;
        double todaysAttendance = 94.5;

        if (lblTotalStudents != null) lblTotalStudents.setText(String.format("%,d", totalStudents));
        if (lblActiveStudents != null) lblActiveStudents.setText(String.format("%,d", activeStudents));
        if (lblTotalTeachers != null) lblTotalTeachers.setText(String.valueOf(totalTeachers));
        if (lblTotalCourses != null) lblTotalCourses.setText(String.valueOf(totalCourses));
        if (lblTodaysAttendance != null) lblTodaysAttendance.setText(String.format("%.1f%%", todaysAttendance));

        // 3. --- Update Student Enrollment Trend Bar Chart ---
        updateEnrollmentTrend(students);

        // 4. --- Update Mark Registrations Table ---
>>>>>>> clean-restored-main
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
                    String score = String.valueOf(78 + (i * 3) % 22);
                    
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

<<<<<<< HEAD
        // 4. --- Update Students by Department Pie Chart ---
        updateDepartmentPieChart(studentsObservable, totalStudents);
=======
        // 5. --- Update Students by Department Pie Chart ---
        updateDepartmentPieChart(students, totalStudents);
>>>>>>> clean-restored-main

        // 6. --- Update Attendance Overview Area Chart ---
        updateAttendanceChart();
    }

    private String getStudentDepartmentSafe(Student s) {
        String dept = getStudentProperty(s, "getDepartment", "getDept", "CEIT");
        return dept != null ? dept.trim() : "CEIT";
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
<<<<<<< HEAD
=======

            // 🧪 CUSTOM TEST VALUES PER YEAR
            // You can change these numbers freely to test bar lengths!
>>>>>>> clean-restored-main
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
<<<<<<< HEAD
                    f1 = 1150; f2 = 900; f3 = 1000; f4 = 100; f5 = 50;
=======
                    // 🎯 Try changing these specific numbers to test 2026 bars!
                    f1 = 1150; // Super long First Year bar
                    f2 = 900;  
                    f3 = 1000;  
                    f4 = 100;  
                    f5 = 50;  
>>>>>>> clean-restored-main
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
<<<<<<< HEAD

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

=======

        Platform.runLater(() -> {
            if (xAxisEnrollment != null) {
                xAxisEnrollment.requestAxisLayout();
            }
            enrollmentBarChart.applyCss();
            enrollmentBarChart.requestLayout();
        });
    }
    
>>>>>>> clean-restored-main
    private void updateDepartmentPieChart(ObservableList<Student> students, int totalStudents) {
        if (departmentPieChart == null) return;
        departmentPieChart.getData().clear();

        Map<String, Integer> deptCounts = new LinkedHashMap<>();
        String[] standardDepts = {"ARCH", "ChE", "CE", "CEIT", "EP", "EcE", "FE", "ME", "McE", "MmsE", "MnE", "PE", "TE", "TxE"};
        
        for (String d : standardDepts) {
            deptCounts.put(d, 0);
        }

        for (Student s : students) {
            String rawDept = getStudentDepartmentSafe(s);
            String normalizedDept = normalizeDeptCode(rawDept);
            if (deptCounts.containsKey(normalizedDept)) {
                deptCounts.put(normalizedDept, deptCounts.get(normalizedDept) + 1);
            } else {
                deptCounts.put("CEIT", deptCounts.get("CEIT") + 1);
            }
        }

<<<<<<< HEAD
        long activeDeptsCount = deptCounts.values().stream().filter(v -> v > 0).count();
        int total = totalStudents > 0 ? totalStudents : students.size();

        if (total <= 10 || activeDeptsCount <= 1) {
            deptCounts.put("ARCH", 200);
            deptCounts.put("ChE", 90);
            deptCounts.put("CE", 125);
            deptCounts.put("CEIT", 180);
            deptCounts.put("EP", 130);
            deptCounts.put("EcE", 110);
            deptCounts.put("FE", 45);
            deptCounts.put("ME", 100);
            deptCounts.put("McE", 75);
            deptCounts.put("MmsE", 50);
            deptCounts.put("MnE", 40);
            deptCounts.put("PE", 85);
            deptCounts.put("TE", 80);
            deptCounts.put("TxE", 40);
            total = 1250;
=======
        if (students.isEmpty() || totalStudents <= 1250) {
            deptCounts.put("ARCH", 115);
            deptCounts.put("ChE", 95);
            deptCounts.put("CE", 135);
            deptCounts.put("CEIT", 210);
            deptCounts.put("EP", 145);
            deptCounts.put("EcE", 120);
            deptCounts.put("FE", 55);
            deptCounts.put("ME", 110);
            deptCounts.put("McE", 85);
            deptCounts.put("MmsE", 60);
            deptCounts.put("MnE", 45);
            deptCounts.put("PE", 95);
            deptCounts.put("TE", 85);
            deptCounts.put("TxE", 45);
            totalStudents = 1400;
>>>>>>> clean-restored-main
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : deptCounts.entrySet()) {
            if (entry.getValue() > 0) {
                double pct = (entry.getValue() * 100.0) / totalStudents;
                pieData.add(new PieChart.Data(String.format("%s (%.1f%%)", entry.getKey(), pct), entry.getValue()));
            }
        }

        departmentPieChart.setData(pieData);
        departmentPieChart.setLegendVisible(false);
        departmentPieChart.setLabelsVisible(true);

        departmentPieChart.applyCss();
        String[] sliceColors = {
            "#0a192f", "#0f2942", "#13385c", "#1d4ed8", "#2563eb", 
            "#3b82f6", "#60a5fa", "#1e40af", "#0284c7", "#0f766e", 
            "#0d9488", "#10b981", "#64748b", "#94a3b8"
        };
        int idx = 0;
        for (PieChart.Data data : departmentPieChart.getData()) {
            Node node = data.getNode();
            if (node != null && idx < sliceColors.length) {
                node.setStyle("-fx-pie-color: " + sliceColors[idx] + ";");
            }
            idx++;
        }
    }

    private String normalizeDeptCode(String code) {
        if (code == null) return "CEIT";
        String upper = code.trim().toUpperCase();
        if (upper.contains("ARCH")) return "ARCH";
        if (upper.contains("CHE") || upper.contains("CHEMICAL")) return "ChE";
        if (upper.contains("CE") && !upper.contains("CEIT")) return "CE";
        if (upper.contains("CEIT") || upper.contains("COMPUTER")) return "CEIT";
        if (upper.contains("EP") || upper.contains("POWER")) return "EP";
        if (upper.contains("ECE") || upper.contains("ELECTRONIC")) return "EcE";
        if (upper.contains("FE") || upper.contains("FOOD")) return "FE";
        if (upper.contains("ME") && !upper.contains("MCE") && !upper.contains("MMS")) return "ME";
        if (upper.contains("MCE") || upper.contains("MECHATRONIC")) return "McE";
        if (upper.contains("MMS") || upper.contains("METALLURGY")) return "MmsE";
        if (upper.contains("MN") || upper.contains("MINING")) return "MnE";
        if (upper.contains("PE") || upper.contains("PETROLEUM")) return "PE";
        if (upper.contains("TE") && !upper.contains("TEXTILE")) return "TE";
        if (upper.contains("TX") || upper.contains("TEXTILE")) return "TxE";
        return upper;
    }

    private void updateAttendanceChart() {
        if (attendanceAreaChart == null) return;
        attendanceAreaChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance");
        series.getData().add(new XYChart.Data<>("Mon", 93.5));
        series.getData().add(new XYChart.Data<>("Tue", 92.1));
        series.getData().add(new XYChart.Data<>("Wed", 94.8));
        series.getData().add(new XYChart.Data<>("Thu", 95.2));
        series.getData().add(new XYChart.Data<>("Fri", 93.9));
        series.getData().add(new XYChart.Data<>("Sat", 94.5));

        attendanceAreaChart.getData().add(series);
        attendanceAreaChart.setLegendVisible(false);
    }
}