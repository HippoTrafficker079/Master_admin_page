package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.*;

public class AttendanceRecord {
    private final IntegerProperty no;
    private final StringProperty rollNo;
    private final StringProperty studentName;
    private final StringProperty department;
    private final StringProperty year;
    private final StringProperty semester;
    private final StringProperty subject;
    private final StringProperty courseCode;
    private final StringProperty attendedTotal;
    private final DoubleProperty percentage;
    private final StringProperty date;
    private final IntegerProperty period;
    private final StringProperty status;
    private final StringProperty markedBy;
    private final StringProperty remarks;
    private final LocalDate dateObj;

    // 1. Existing Constructor for Matrix Summary / Individual Subject Summary
    public AttendanceRecord(int no, String rollNo, String studentName, String department, String year, 
                            String semester, String subject, String attendedTotal, double percentage, LocalDate dateObj) {
        this(no, rollNo, studentName, department, year, semester, subject, "", attendedTotal, percentage, dateObj, "");
    }

    // 2. New Constructor for Department Subject Breakdown (with Course Code & Standing/Remarks)
    public AttendanceRecord(int no, String rollNo, String studentName, String department, String year, 
                            String semester, String subject, String courseCode, String attendedTotal, 
                            double percentage, LocalDate dateObj, String remarks) {
        this.no = new SimpleIntegerProperty(no);
        this.rollNo = new SimpleStringProperty(rollNo);
        this.studentName = new SimpleStringProperty(studentName);
        this.department = new SimpleStringProperty(department);
        this.year = new SimpleStringProperty(year);
        this.semester = new SimpleStringProperty(semester);
        this.subject = new SimpleStringProperty(subject);
        this.courseCode = new SimpleStringProperty(courseCode);
        this.attendedTotal = new SimpleStringProperty(attendedTotal);
        this.percentage = new SimpleDoubleProperty(percentage);
        this.dateObj = dateObj;
        this.date = new SimpleStringProperty(dateObj != null ? dateObj.toString() : "");
        this.period = new SimpleIntegerProperty(0);
        this.status = new SimpleStringProperty("");
        this.markedBy = new SimpleStringProperty("");
        
        if (remarks == null || remarks.isEmpty()) {
            this.remarks = new SimpleStringProperty(percentage >= 90 ? "Excellent" : (percentage >= 75 ? "Good" : "Warning"));
        } else {
            this.remarks = new SimpleStringProperty(remarks);
        }
    }

    // 3. Existing Constructor for Daily Log
    public AttendanceRecord(int no, String rollNo, String studentName, String department, String year, 
                            String semester, LocalDate dateObj, int period, String subject, 
                            String status, String markedBy, String remarks) {
        this(no, rollNo, studentName, department, year, semester, dateObj, period, subject, "", status, markedBy, remarks);
    }

    // 4. New Constructor for Detailed Chronological Log (with Course Code & Formatted Date)
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
    }

    public boolean isLowAttendance() {
        return percentage.get() > 0 && percentage.get() < 75.0;
    }

    public String getFormattedDateWithDay() {
        if (dateObj == null) return getDate();
        return dateObj.format(DateTimeFormatter.ofPattern("MMM dd, yyyy (EEE)"));
    }

    // Property Getters
    public IntegerProperty noProperty() { return no; }
    public StringProperty rollNoProperty() { return rollNo; }
    public StringProperty studentNameProperty() { return studentName; }
    public StringProperty departmentProperty() { return department; }
    public StringProperty yearProperty() { return year; }
    public StringProperty semesterProperty() { return semester; }
    public StringProperty subjectProperty() { return subject; }
    public StringProperty courseCodeProperty() { return courseCode; }
    public StringProperty attendedTotalProperty() { return attendedTotal; }
    public DoubleProperty percentageProperty() { return percentage; }
    public StringProperty dateProperty() { return date; }
    public IntegerProperty periodProperty() { return period; }
    public StringProperty statusProperty() { return status; }
    public StringProperty markedByProperty() { return markedBy; }
    public StringProperty remarksProperty() { return remarks; }

    // Standard Value Getters
    public int getNo() { return no.get(); }
    public String getRollNo() { return rollNo.get(); }
    public String getStudentName() { return studentName.get(); }
    public String getDepartment() { return department.get(); }
    public String getYear() { return year.get(); }
    public String getSemester() { return semester.get(); }
    public String getSubject() { return subject.get(); }
    public String getCourseCode() { return courseCode.get(); }
    public String getAttendedTotal() { return attendedTotal.get(); }
    public double getPercentage() { return percentage.get(); }
    public String getDate() { return date.get(); }
    public LocalDate getDateObj() { return dateObj; }
    public int getPeriod() { return period.get(); }
    public String getStatus() { return status.get(); }
    public String getMarkedBy() { return markedBy.get(); }
    public String getRemarks() { return remarks.get(); }
}