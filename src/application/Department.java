package application;

import java.time.LocalDate;

public class Department {
    private int deptId;
    private Integer deptHeadId;
    private String deptCode;
    private String deptName;
    private LocalDate establishedDate;
    private String deptLocation;

    // Default Constructor
    public Department() {
    }

    // Parameterized Constructor
    public Department(int deptId, Integer deptHeadId, String deptCode, String deptName, LocalDate establishedDate,
            String deptLocation) {
        this.deptId = deptId;
        this.deptHeadId = deptHeadId;
        this.deptCode = deptCode;
        this.deptName = deptName;
        this.establishedDate = establishedDate;
        this.deptLocation = deptLocation;
    }

    // Getters and Setters
    public int getDeptId() {
        return deptId;
    }

    public void setDeptId(int deptId) {
        this.deptId = deptId;
    }

    public Integer getDeptHeadId() {
        return deptHeadId;
    }

    public void setDeptHeadId(Integer deptHeadId) {
        this.deptHeadId = deptHeadId;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public LocalDate getEstablishedDate() {
        return establishedDate;
    }

    public void setEstablishedDate(LocalDate establishedDate) {
        this.establishedDate = establishedDate;
    }

    public String getDeptLocation() {
        return deptLocation;
    }

    public void setDeptLocation(String deptLocation) {
        this.deptLocation = deptLocation;
    }
}
