package application.dto;

public class EnrollmentTrendRecord {
    private final int academicSchoolYear;
    private final int historicalYearLevel;
    private final int studentCount;

    public EnrollmentTrendRecord(int academicSchoolYear, int historicalYearLevel, int studentCount) {
        this.academicSchoolYear = academicSchoolYear;
        this.historicalYearLevel = historicalYearLevel;
        this.studentCount = studentCount;
    }

    public int getAcademicSchoolYear() {
        return academicSchoolYear;
    }

    public int getHistoricalYearLevel() {
        return historicalYearLevel;
    }

    public int getStudentCount() {
        return studentCount;
    }
}
