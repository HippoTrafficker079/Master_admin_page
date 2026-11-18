package application.dto;

import java.time.LocalDate;

public class AttendanceTrendRecord {
    private int period;
    private LocalDate sessionDate;
    private int weekNumber;
    private double attendancePercentage;

    // Constructor for Daily view (Period-based)
    public AttendanceTrendRecord(int period, double attendancePercentage) {
        this.period = period;
        this.attendancePercentage = attendancePercentage;
    }

    // Constructor for Weekly view (Date-based)
    public AttendanceTrendRecord(LocalDate sessionDate, double attendancePercentage) {
        this.sessionDate = sessionDate;
        this.attendancePercentage = attendancePercentage;
    }

    public AttendanceTrendRecord(int weekNumber, double attendancePercentage, boolean isMonthly) {
        this.weekNumber = weekNumber;
        this.attendancePercentage = attendancePercentage;
    }

    public int getPeriod() {
        return period;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public int getWeekNumber() {
        return weekNumber;
    }
}