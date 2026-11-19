package application.dto;

import java.time.LocalDate;

public class AttendanceTrendRecord {
    private int period;
    private LocalDate sessionDate;
    private int weekNumber;
    private double attendancePercentage;
    private int presentCount;
    private int totalCount;

    // Constructor for Daily view (Period-based)
    public AttendanceTrendRecord(int period, double attendancePercentage) {
        this(period, attendancePercentage, 0, 0);
    }

    public AttendanceTrendRecord(int period, double attendancePercentage, int presentCount, int totalCount) {
        this.period = period;
        this.attendancePercentage = attendancePercentage;
        this.presentCount = presentCount;
        this.totalCount = totalCount;
    }

    // Constructor for Weekly view (Date-based)
    public AttendanceTrendRecord(LocalDate sessionDate, double attendancePercentage) {
        this(sessionDate, attendancePercentage, 0, 0);
    }

    public AttendanceTrendRecord(LocalDate sessionDate, double attendancePercentage, int presentCount, int totalCount) {
        this.sessionDate = sessionDate;
        this.attendancePercentage = attendancePercentage;
        this.presentCount = presentCount;
        this.totalCount = totalCount;
    }

    // Constructor for Monthly view (Week-based)
    public AttendanceTrendRecord(int weekNumber, double attendancePercentage, boolean isMonthly) {
        this(weekNumber, attendancePercentage, isMonthly, 0, 0);
    }

    public AttendanceTrendRecord(int weekNumber, double attendancePercentage, boolean isMonthly, int presentCount, int totalCount) {
        this.weekNumber = weekNumber;
        this.attendancePercentage = attendancePercentage;
        this.presentCount = presentCount;
        this.totalCount = totalCount;
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
    
    public int getPresentCount() { 
    	return presentCount; 
    }
    
    public int getTotalCount() { 
    	return totalCount; 
    }
}