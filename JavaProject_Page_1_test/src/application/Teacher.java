package application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Teacher {
    // Primary database ID (formerly 'no')
    private final IntegerProperty teacherId;
    
    // Formatted Code (formerly 'teacherId', e.g. TCH-CEIT-101)
    private final StringProperty teacherCode;

    private final StringProperty name;
    private final StringProperty department;
    private final StringProperty position;
    private final StringProperty phoneNo;

    // Extended fields for the Teacher Information Form
    private final StringProperty dob;
    private final StringProperty gender;
    private final StringProperty nationality;
    private final StringProperty nrc;
    private final StringProperty email;
    private final StringProperty currentAddress;
    private final StringProperty permanentAddress;
    private final StringProperty degree;
    private final StringProperty university;
    private final StringProperty yearCompleted;
    private final StringProperty specialization;
    private final StringProperty emergencyContact;
    private final StringProperty photoPath;

    // Original backups for reset functionality
    private final int origTeacherId;
    private final String origTeacherCode, origName, origDepartment, origPosition, origPhoneNo;
    private final String origDob, origGender, origNationality, origNrc, origEmail;
    private final String origCurrentAddress, origPermanentAddress;
    private final String origDegree, origUniversity, origYearCompleted, origSpecialization, origEmergencyContact;

    // Summary Constructor (used by DAO getAllTeacher / getFilteredTeachers)
    public Teacher(int teacherId, String teacherCode, String name, String department, String position, String phoneNo) {
        this(teacherId, teacherCode, name, department, position, phoneNo,
             "15-05-1985", "Male", "Myanmar", "-", name.toLowerCase().replace(" ", "").replace(".", "") + "@ytu.edu.mm",
             "-", "-", "Ph.D.", "Yangon Technological University", "2012", "Engineering", phoneNo, null);
    }

    // Backwards-compatible 5-argument constructor
    public Teacher(int teacherId, String name, String department, String position, String phoneNo) {
        this(teacherId, "TCH-" + (department != null ? department : "CEIT") + "-" + teacherId,
             name, department, position, phoneNo);
    }

    // Full Details Constructor
    public Teacher(int teacherId, String teacherCode, String name, String department, String position, String phoneNo,
                   String dob, String gender, String nationality, String nrc, String email, 
                   String currentAddress, String permanentAddress, String degree, String university, 
                   String yearCompleted, String specialization, String emergencyContact, String photoPath) {
        this.teacherId = new SimpleIntegerProperty(teacherId);
        this.teacherCode = new SimpleStringProperty(teacherCode != null && !teacherCode.isEmpty() ? teacherCode : "TCH-" + department + "-" + teacherId);
        this.name = new SimpleStringProperty(name);
        this.department = new SimpleStringProperty(department);
        this.position = new SimpleStringProperty(position);
        this.phoneNo = new SimpleStringProperty(phoneNo);

        this.dob = new SimpleStringProperty(dob != null && !dob.isEmpty() ? dob : "-");
        this.gender = new SimpleStringProperty(gender != null && !gender.isEmpty() ? gender : "-");
        this.nationality = new SimpleStringProperty(nationality != null && !nationality.isEmpty() ? nationality : "Myanmar");
        this.nrc = new SimpleStringProperty(nrc != null && !nrc.isEmpty() ? nrc : "-");
        this.email = new SimpleStringProperty(email != null && !email.isEmpty() ? email : "-");
        this.currentAddress = new SimpleStringProperty(currentAddress != null && !currentAddress.isEmpty() ? currentAddress : "-");
        this.permanentAddress = new SimpleStringProperty(permanentAddress != null && !permanentAddress.isEmpty() ? permanentAddress : "-");
        this.degree = new SimpleStringProperty(degree != null && !degree.isEmpty() ? degree : "-");
        this.university = new SimpleStringProperty(university != null && !university.isEmpty() ? university : "-");
        this.yearCompleted = new SimpleStringProperty(yearCompleted != null && !yearCompleted.isEmpty() ? yearCompleted : "-");
        this.specialization = new SimpleStringProperty(specialization != null && !specialization.isEmpty() ? specialization : "-");
        this.emergencyContact = new SimpleStringProperty(emergencyContact != null && !emergencyContact.isEmpty() ? emergencyContact : "-");
        this.photoPath = new SimpleStringProperty(photoPath != null ? photoPath : "");

        this.origTeacherId = teacherId;
        this.origTeacherCode = this.teacherCode.get();
        this.origName = name;
        this.origDepartment = department;
        this.origPosition = position;
        this.origPhoneNo = phoneNo;
        this.origDob = this.dob.get();
        this.origGender = this.gender.get();
        this.origNationality = this.nationality.get();
        this.origNrc = this.nrc.get();
        this.origEmail = this.email.get();
        this.origCurrentAddress = this.currentAddress.get();
        this.origPermanentAddress = this.permanentAddress.get();
        this.origDegree = this.degree.get();
        this.origUniversity = this.university.get();
        this.origYearCompleted = this.yearCompleted.get();
        this.origSpecialization = this.specialization.get();
        this.origEmergencyContact = this.emergencyContact.get();
    }

    public void resetToOriginal() {
        teacherId.set(origTeacherId);
        teacherCode.set(origTeacherCode);
        name.set(origName);
        department.set(origDepartment);
        position.set(origPosition);
        phoneNo.set(origPhoneNo);
        dob.set(origDob);
        gender.set(origGender);
        nationality.set(origNationality);
        nrc.set(origNrc);
        email.set(origEmail);
        currentAddress.set(origCurrentAddress);
        permanentAddress.set(origPermanentAddress);
        degree.set(origDegree);
        university.set(origUniversity);
        yearCompleted.set(origYearCompleted);
        specialization.set(origSpecialization);
        emergencyContact.set(origEmergencyContact);
    }

    // teacherId (Database ID)
    public int getTeacherId() { return teacherId.get(); }
    public IntegerProperty teacherIdProperty() { return teacherId; }
    public void setTeacherId(int id) { this.teacherId.set(id); }

    // Backward compatibility helper for getNo()
    public int getNo() { return teacherId.get(); }
    public IntegerProperty noProperty() { return teacherId; }
    public void setNo(int no) { this.teacherId.set(no); }

    // teacherCode (e.g. TCH-CEIT-101)
    public String getTeacherCode() { return teacherCode.get(); }
    public StringProperty teacherCodeProperty() { return teacherCode; }
    public void setTeacherCode(String code) { this.teacherCode.set(code); }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    public String getDepartment() { return department.get(); }
    public StringProperty departmentProperty() { return department; }
    public void setDepartment(String department) { this.department.set(department); }

    public String getPosition() { return position.get(); }
    public StringProperty positionProperty() { return position; }
    public void setPosition(String position) { this.position.set(position); }

    public String getPhoneNo() { return phoneNo.get(); }
    public StringProperty phoneNoProperty() { return phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo.set(phoneNo); }

    public String getDob() { return dob.get(); }
    public StringProperty dobProperty() { return dob; }
    public void setDob(String dob) { this.dob.set(dob); }

    public String getGender() { return gender.get(); }
    public StringProperty genderProperty() { return gender; }
    public void setGender(String gender) { this.gender.set(gender); }

    public String getNationality() { return nationality.get(); }
    public StringProperty nationalityProperty() { return nationality; }
    public void setNationality(String nationality) { this.nationality.set(nationality); }

    public String getNrc() { return nrc.get(); }
    public StringProperty nrcProperty() { return nrc; }
    public void setNrc(String nrc) { this.nrc.set(nrc); }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }
    public void setEmail(String email) { this.email.set(email); }

    public String getCurrentAddress() { return currentAddress.get(); }
    public StringProperty currentAddressProperty() { return currentAddress; }
    public void setCurrentAddress(String currentAddress) { this.currentAddress.set(currentAddress); }

    public String getPermanentAddress() { return permanentAddress.get(); }
    public StringProperty permanentAddressProperty() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress.set(permanentAddress); }

    public String getDegree() { return degree.get(); }
    public StringProperty degreeProperty() { return degree; }
    public void setDegree(String degree) { this.degree.set(degree); }

    public String getUniversity() { return university.get(); }
    public StringProperty universityProperty() { return university; }
    public void setUniversity(String university) { this.university.set(university); }

    public String getYearCompleted() { return yearCompleted.get(); }
    public StringProperty yearCompletedProperty() { return yearCompleted; }
    public void setYearCompleted(String yearCompleted) { this.yearCompleted.set(yearCompleted); }

    public String getSpecialization() { return specialization.get(); }
    public StringProperty specializationProperty() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization.set(specialization); }

    public String getEmergencyContact() { return emergencyContact.get(); }
    public StringProperty emergencyContactProperty() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact.set(emergencyContact); }

    public String getPhotoPath() { return photoPath.get(); }
    public StringProperty photoPathProperty() { return photoPath; }
    public void setPhotoPath(String path) { this.photoPath.set(path); }
}