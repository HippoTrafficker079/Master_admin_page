package application.dto;

public class StudentProfile {
    private int userId;
    private int studentId;
    private String rollNo;
    private String fullName;
    private String deptCode;
    private String currentYear;
    private String profileImageUrl;
    private String nrc;
    private boolean userStatus; // Active / Inactive
    private int totalSessions;
    private int totalAbsent;
    private int totalAttended;
    private double attendancePercentage;
    private String academicStanding; // "Good", "Warning", etc.
    private String phoneNumber;
    private String email;
    private String address;

    public StudentProfile(int userId, int studentId, String rollNo, String fullName,
            String deptCode, String currentYear, String profileImageUrl,
            String nrc, boolean userStatus, int totalSessions, int totalAbsent,
            int totalAttended, double attendancePercentage, String academicStanding,
            String phoneNumber, String email, String address) {
        this.userId = userId;
        this.studentId = studentId;
        this.rollNo = rollNo;
        this.fullName = fullName;
        this.deptCode = deptCode;
        this.currentYear = currentYear;
        this.profileImageUrl = profileImageUrl;
        this.nrc = nrc;
        this.userStatus = userStatus;
        this.totalSessions = totalSessions;
        this.totalAbsent = totalAbsent;
        this.totalAttended = totalAttended;
        this.attendancePercentage = attendancePercentage;
        this.academicStanding = academicStanding;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
    }

    // Getters
    public int getUserId() {
        return userId;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public String getCurrentYear() {
        return currentYear;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getNrc() {
        return nrc;
    }

    public boolean isUserStatus() {
        return userStatus;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public int getTotalAbsent() {
        return totalAbsent;
    }

    public int getTotalAttended() {
        return totalAttended;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public String getAcademicStanding() {
        return academicStanding;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    // Setters
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public void setCurrentYear(String currentYear) {
        this.currentYear = currentYear;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setNrc(String nrc) {
        this.nrc = nrc;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public void setTotalAbsent(int totalAbsent) {
        this.totalAbsent = totalAbsent;
    }

    public void setTotalAttended(int totalAttended) {
        this.totalAttended = totalAttended;
    }

    public void setContact(String contact) {
        this.email = email;
    }
}