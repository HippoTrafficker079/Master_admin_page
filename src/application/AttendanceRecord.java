package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.*;

public class AttendanceRecord {
    private final IntegerProperty userId;
    private final IntegerProperty no;
    private final StringProperty rollNo;
    private final StringProperty studentName;
    private final StringProperty department;
    private final StringProperty year;
    private final StringProperty semester;
    private final StringProperty subject;
    private final StringProperty courseCode;
    private final IntegerProperty totalSessions;
    private final IntegerProperty totalAbsent;
    private final IntegerProperty sessionId = new SimpleIntegerProperty();
    private final IntegerProperty enrollmentId = new SimpleIntegerProperty();

    private final StringProperty attendedTotal;
    private final DoubleProperty percentage;
    private final StringProperty date;
    private final IntegerProperty period;
    private final StringProperty status;
    private final StringProperty markedBy;
    private final StringProperty remarks;
    private final LocalDate dateObj;

    // 1. Existing Constructor for Matrix Summary / Individual Subject Summary
    // Constructor for Matrix Summary / Attendance Summary DAO
    public AttendanceRecord(int userId, int studentId, String rollNo, String studentName,
            String department, String year, int totalSessions,
            int totalAbsent, double percentage) {
        this.userId = new SimpleIntegerProperty(userId);
        this.no = new SimpleIntegerProperty(studentId);

        this.rollNo = new SimpleStringProperty(rollNo);
        this.studentName = new SimpleStringProperty(studentName);
        this.department = new SimpleStringProperty(department);
        this.year = new SimpleStringProperty(year);
        this.totalSessions = new SimpleIntegerProperty(totalSessions);
        this.totalAbsent = new SimpleIntegerProperty(totalAbsent);
        this.percentage = new SimpleDoubleProperty(percentage);

        // Initialize other unused final fields so Java doesn't complain
        this.courseCode = new SimpleStringProperty("");
        this.semester = new SimpleStringProperty("");
        this.subject = new SimpleStringProperty("");
        this.attendedTotal = new SimpleStringProperty(totalSessions + " / " + (totalSessions - totalAbsent));
        this.status = new SimpleStringProperty("");
        this.markedBy = new SimpleStringProperty("");
        this.remarks = new SimpleStringProperty("");
        this.period = new SimpleIntegerProperty(0);
        this.dateObj = null;
        this.date = null;
    }

    // 2. New Constructor for Department Subject Breakdown (with Course Code &
    // Standing/Remarks)
    // 3. Clean Constructor for Department Subject Breakdown (No redundant student
    // info)
    public AttendanceRecord(int no, String subject, String courseCode, int totalSessions,
            int totalAbsent, double percentage, String remarks) {
        this.no = new SimpleIntegerProperty(no);
        this.rollNo = new SimpleStringProperty("");
        this.studentName = new SimpleStringProperty("");
        this.department = new SimpleStringProperty("");
        this.year = new SimpleStringProperty("");
        this.semester = new SimpleStringProperty("");
        this.subject = new SimpleStringProperty(subject);
        this.courseCode = new SimpleStringProperty(courseCode);

        // Attended = Total Sessions - Total Absent
        int attended = totalSessions - totalAbsent;
        this.attendedTotal = new SimpleStringProperty(attended + " / " + totalSessions);

        this.percentage = new SimpleDoubleProperty(percentage);
        this.dateObj = null;
        this.date = new SimpleStringProperty("");
        this.period = new SimpleIntegerProperty(0);
        this.status = new SimpleStringProperty("");
        this.markedBy = new SimpleStringProperty("");
        this.userId = new SimpleIntegerProperty(0);
        this.totalSessions = new SimpleIntegerProperty(totalSessions);
        this.totalAbsent = new SimpleIntegerProperty(totalAbsent);

        if (remarks == null || remarks.isEmpty()) {
            this.remarks = new SimpleStringProperty(
                    percentage >= 90 ? "Excellent" : (percentage >= 75 ? "Good" : "Warning"));
        } else {
            this.remarks = new SimpleStringProperty(remarks);
        }
    }

    // Updated Daily Log Constructor with session_id and enrollment_id
    // 3. Updated Constructor for Daily Log
    public AttendanceRecord(int sessionId, int enrollmentId, int no, String rollNo, String studentName,
            String department, String year, String semester, LocalDate dateObj,
            int period, String subject, String status, String markedBy, String remarks) {
        this(no, rollNo, studentName, department, year, semester, dateObj, period, subject, "", status, markedBy,
                remarks);

        // Set the Daily Log IDs
        this.sessionId.set(sessionId);
        this.enrollmentId.set(enrollmentId);
    }

    // 4. New Constructor for Detailed Chronological Log (with Course Code &
    // Formatted Date)
    public AttendanceRecord(int no, String rollNo, String studentName, String department, String year,
            String semester, LocalDate dateObj, int period, String subject, String courseCode,
            String status, String markedBy, String remarks) {
        this.no = new SimpleIntegerProperty(no);
        this.rollNo = new SimpleStringProperty(rollNo);
        this.studentName = new SimpleStringProperty(studentName);
        this.department = new SimpleStringProperty(department);
        this.year = new SimpleStringProperty(year);
        this.semester = new SimpleStringProperty(semester);
        this.dateObj = dateObj;
        this.date = new SimpleStringProperty(dateObj != null ? dateObj.toString() : "");
        this.period = new SimpleIntegerProperty(period);
        this.subject = new SimpleStringProperty(subject);
        this.courseCode = new SimpleStringProperty(courseCode);
        this.status = new SimpleStringProperty(status);
        this.markedBy = new SimpleStringProperty(markedBy);
        this.remarks = new SimpleStringProperty(remarks);
        this.attendedTotal = new SimpleStringProperty("");
        this.percentage = new SimpleDoubleProperty(0.0);
        this.userId = new SimpleIntegerProperty(0);
        this.totalSessions = new SimpleIntegerProperty(0);
        this.totalAbsent = new SimpleIntegerProperty(0);
    }

    public boolean isLowAttendance() {
        return percentage.get() > 0 && percentage.get() < 75.0;
    }

    public String getFormattedDateWithDay() {
        if (dateObj == null)
            return getDate();
        return dateObj.format(DateTimeFormatter.ofPattern("MMM dd, yyyy (EEE)"));
    }

    // Property Getters
    public IntegerProperty noProperty() {
        return no;
    }

    public StringProperty rollNoProperty() {
        return rollNo;
    }

    public StringProperty studentNameProperty() {
        return studentName;
    }

    public StringProperty departmentProperty() {
        return department;
    }

    public StringProperty yearProperty() {
        return year;
    }

    public StringProperty semesterProperty() {
        return semester;
    }

    public StringProperty subjectProperty() {
        return subject;
    }

    public StringProperty courseCodeProperty() {
        return courseCode;
    }

    public StringProperty attendedTotalProperty() {
        return attendedTotal;
    }

    public DoubleProperty percentageProperty() {
        return percentage;
    }

    public StringProperty dateProperty() {
        return date;
    }

    public IntegerProperty periodProperty() {
        return period;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty markedByProperty() {
        return markedBy;
    }

    public StringProperty remarksProperty() {
        return remarks;
    }

    // Standard Value Getters
    public int getNo() {
        return no.get();
    }

    public String getRollNo() {
        return rollNo.get();
    }

    public String getStudentName() {
        return studentName.get();
    }

    public String getDepartment() {
        return department.get();
    }

    public String getYear() {
        return year.get();
    }

    public String getSemester() {
        return semester.get();
    }

    public String getSubject() {
        return subject.get();
    }

    public String getCourseCode() {
        return courseCode.get();
    }

    public String getAttendedTotal() {
        return attendedTotal.get();
    }

    public double getPercentage() {
        return percentage.get();
    }

    public String getDate() {
        return date.get();
    }

    public LocalDate getDateObj() {
        return dateObj;
    }

    public int getPeriod() {
        return period.get();
    }

    public String getStatus() {
        return status.get();
    }

    public String getMarkedBy() {
        return markedBy.get();
    }

    public String getRemarks() {
        return remarks.get();
    }

    public int getSessionId() {
        return sessionId.get();
    }

    public int getEnrollmentId() {
        return enrollmentId.get();
    }
}
