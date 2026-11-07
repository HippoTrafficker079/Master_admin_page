package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import application.Student;

public class StudentDAO {
    public List<Student> getAllStudent() {
        List<Student> students = new ArrayList<>();
        String sql = """
                    WITH
                    RankedStudents AS (
                        SELECT s.student_id, s.roll_no, s.full_name, d.dept_code, s.current_year, COALESCE(co.term, 'Pending') AS term,
                            ci.primary_phone_number, ua.status, ROW_NUMBER() OVER (
                                PARTITION BY
                                    s.student_id
                                ORDER BY ce.enrollment_id DESC
                            ) AS rn
                        FROM
                            student s
                            JOIN user_account ua ON s.user_id = ua.user_id
                            JOIN department d ON s.dept_id = d.dept_id
                            JOIN student_contact_information ci ON s.student_id = ci.student_id
                            LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id
                            LEFT JOIN course_offering co ON ce.offering_id = co.offering_id)

                SELECT
                    student_id,
                    roll_no,
                    full_name,
                    dept_code,
                    current_year,
                    term,
                    primary_phone_number
                FROM RankedStudents
                WHERE
                    rn = 1
                    AND `status` = TRUE;""";
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("student_id");
                String rollNum = rs.getString("roll_no");
                String name = rs.getString("full_name");
                String dept_code = rs.getString("dept_code");
                String current_year = rs.getString("current_year");
                String term = rs.getString("term");
                String primary_phone_number = rs.getString("primary_phone_number");

                Student student = new Student(id, rollNum, name, dept_code, current_year, term, primary_phone_number);

                students.add(student);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public List<Student> getFilteredStudents(String searchText, String department, String currentYear,
            String semester) {
        StringBuilder sql = new StringBuilder(
                """
                            WITH
                            RankedStudents AS (
                                SELECT s.student_id, s.roll_no, s.full_name, d.dept_code, s.current_year, COALESCE(co.term, 'Pending') AS term, ci.primary_phone_number, ua.status, ROW_NUMBER() OVER (
                                        PARTITION BY
                                            s.student_id
                                        ORDER BY ce.enrollment_id DESC
                                    ) AS rn
                                FROM
                                    student s
                                    JOIN user_account ua ON s.user_id = ua.user_id
                                    JOIN department d ON s.dept_id = d.dept_id
                                    JOIN student_contact_information ci ON s.student_id = ci.student_id
                                    LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id
                                    LEFT JOIN course_offering co ON ce.offering_id = co.offering_id
                            )
                        SELECT
                            student_id,
                            roll_no,
                            full_name,
                            dept_code,
                            current_year,
                            term,
                            primary_phone_number
                        FROM RankedStudents
                        WHERE
                            rn = 1
                            AND `status` = TRUE
                             """);
        List<Object> parameters = new ArrayList<>();
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append("""
                       AND (
                       full_name LIKE ?
                       OR roll_no LIKE ?
                       OR primary_phone_number LIKE ?
                      )
                    """);
            parameters.add("%" + searchText.toLowerCase() + "%");
            parameters.add("%" + searchText.toLowerCase() + "%");
            parameters.add("%" + searchText.toLowerCase() + "%");
        }
        if (department != null && !department.equalsIgnoreCase("All Majors")) {
            sql.append("AND (dept_code = ?)");
            parameters.add(department);
        }
        if (currentYear != null && !currentYear.equalsIgnoreCase("All Years")) {
            sql.append("AND (current_year = ?)");
            parameters.add(currentYear);
        }
        if (semester != null && !semester.equalsIgnoreCase("All Semesters")) {
            sql.append("AND (term = ?)");
            parameters.add(semester);
        }

        List<Student> students = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("student_id");
                    String rollNum = rs.getString("roll_no");
                    String name = rs.getString("full_name");
                    String dept_code = rs.getString("dept_code");
                    String current_year = rs.getString("current_year");
                    String term = rs.getString("term");
                    String primary_phone_number = rs.getString("primary_phone_number");

                    Student student = new Student(id, rollNum, name, dept_code, current_year, term,
                            primary_phone_number);

                    students.add(student);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public Student getStudentbyId(int studentId) {
        String sql = """
                      WITH
                    RankedStudent AS (
                        SELECT
                            s.student_id,
                            s.roll_no,
                            s.full_name,
                            d.dept_code AS faculty,
                            s.current_year,
                            COALESCE(co.term, 'Pending') AS term,
                            s.date_of_birth,
                            s.gender,
                            s.nationality,
                            s.nrc,
                            s.profile_image_url,
                            ci.primary_phone_number,
                            ci.email_address,
                            ci.current_address,
                            ci.permanent_address,
                            sg.father_full_name AS father_name,
                            sg.father_occupation AS father_occupation,
                            sg.father_phone_number AS father_phone,
                            sg.father_email_address AS father_email,
                            sg.mother_full_name AS mother_name,
                            sg.mother_occupation AS mother_occupation,
                            sg.mother_phone_number AS mother_phone,
                            sg.mother_email_address AS mother_email,
                            seb.previous_school,
                            seb.year_completed,
                            seb.additional_info,
                            ROW_NUMBER() OVER (
                                ORDER BY ce.enrollment_id DESC
                            ) AS rn
                        FROM
                            student s
                            LEFT JOIN department d ON s.dept_id = d.dept_id
                            LEFT JOIN student_contact_information ci ON s.student_id = ci.student_id
                            LEFT JOIN student_guardian sg ON s.student_id = sg.student_id
                            LEFT JOIN student_education_background seb ON s.student_id = seb.student_id
                            LEFT JOIN course_enrollment ce ON s.student_id = ce.student_id
                            LEFT JOIN course_offering co ON ce.offering_id = co.offering_id
                        WHERE
                            s.student_id = ?
                    )
                SELECT
                    student_id,
                    roll_no,
                    full_name,
                    faculty,
                    current_year,
                    term,
                    date_of_birth,
                    gender,
                    nationality,
                    nrc,
                    profile_image_url,
                    primary_phone_number,
                    email_address,
                    current_address,
                    permanent_address,
                    father_name,
                    father_occupation,
                    father_phone,
                    father_email,
                    mother_name,
                    mother_occupation,
                    mother_phone,
                    mother_email,
                    previous_school,
                    year_completed,
                    additional_info
                FROM RankedStudent
                WHERE
                    rn = 1;
                      """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("date_of_birth");
                    String dobString = (sqlDate != null) ? sqlDate.toString() : "";

                    return new Student(
                            rs.getInt("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("full_name"),
                            rs.getString("faculty"),
                            rs.getString("current_year"),
                            rs.getString("term"),
                            rs.getString("primary_phone_number"), // 7: phoneNo
                            dobString, // 8: dob
                            rs.getString("gender"), // 9: gender ("Female")
                            rs.getString("nationality"), // 10: nationality ("Myanmar")
                            rs.getString("nrc"), // 11: nrc ("12/TaTaNa(N)...")
                            rs.getString("email_address"), // 12: email
                            rs.getString("current_address"), // 13: currentAddress
                            rs.getString("permanent_address"), // 14: permanentAddress
                            rs.getString("father_name"), // 15: fatherName
                            rs.getString("father_occupation"), // 16: fatherOccupation
                            rs.getString("father_phone"), // 17: fatherPhone
                            rs.getString("father_email"), // 18: fatherEmail
                            rs.getString("mother_name"), // 19: motherName
                            rs.getString("mother_occupation"), // 20: motherOccupation
                            rs.getString("mother_phone"), // 21: motherPhone
                            rs.getString("mother_email"), // 22: motherEmail
                            rs.getString("previous_school"), // 23: prevSchool
                            rs.getString("year_completed"), // 24: yearCompleted
                            rs.getString("additional_info"), // 25: additionalInfo
                            rs.getString("profile_image_url") // 26: photoPath
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insertStudent(Student student) {
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            int studentId = insertSingleStudent(conn, student, "Default123");

            conn.commit();
            return studentId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    public int insertBatchStudents(List<Student> students) {
        Connection conn = null;
        int successCount = 0;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);
            for (Student student : students) {
                int studentId = insertSingleStudent(conn, student, "Default123");
                if (studentId > -1) {
                    successCount++;
                }
            }
            conn.commit();
            return successCount;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    private int insertSingleStudent(Connection conn, Student student, String password) throws SQLException {
        String userCreateSql = """
                INSERT INTO user_account (
                    username,
                    email,
                    password_hash,
                    user_role
                ) VALUES (?, ?, ?, 'STUDENT');
                """;

        String studentCreateSql = """
                INSERT INTO student (
                    user_id,
                    roll_no,
                    full_name,
                    dept_id,
                    current_year,
                    date_of_birth,
                    gender,
                    nationality,
                    nrc,
                    profile_image_url
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        String studentContactSql = """
                INSERT INTO student_contact_information (
                    student_id,
                    primary_phone_number,
                    secondary_phone_number,
                    email_address,
                    current_address,
                    permanent_address
                ) VALUES (?, ?, ?, ?, ?, ?);
                """;

        String studentGuardianSql = """
                INSERT INTO student_guardian (
                    student_id,
                    father_full_name,
                    father_occupation,
                    father_phone_number,
                    father_email_address,
                    mother_full_name,
                    mother_occupation,
                    mother_phone_number,
                    mother_email_address
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        String studentEducationBackgroundSql = """
                INSERT INTO
                student_education_background (
                    student_id,
                    previous_school,
                    year_completed,
                    additional_info
                )
                VALUES (?, ?, ?, ?);
                """;

        String username = student.getRollNo();
        String email = student.getEmail() != null && !student.getEmail().trim().isEmpty()
                ? student.getEmail().trim()
                : student.getRollNo().toLowerCase().replace("-", "") + "@ytu.edu.mm";

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        int userId = -1;

        try (PreparedStatement pstmtUser = conn.prepareStatement(userCreateSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmtUser.setString(1, username);
            pstmtUser.setString(2, email);
            pstmtUser.setString(3, hashedPassword);
            pstmtUser.executeUpdate();

            try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating user failed! No userID found.");
                }
            }
        }

        int studentId = -1;
        try (PreparedStatement pstmtStudent = conn.prepareStatement(studentCreateSql,
                Statement.RETURN_GENERATED_KEYS)) {

            DepartmentDAO departmentDAO = new DepartmentDAO();
            int deptId = departmentDAO.getDeptIdByCodeOrName(conn, student.getMajor());

            String rawDob = student.getDob();
            String formattedDob = rawDob;
            if (rawDob != null && rawDob.matches("\\d{2}-\\d{2}-\\d{4}")) {
                String[] parts = rawDob.split("-");
                formattedDob = parts[2] + "-" + parts[1] + "-" + parts[0];
            }

            pstmtStudent.setInt(1, userId);
            pstmtStudent.setString(2, student.getRollNo());
            pstmtStudent.setString(3, student.getName());
            pstmtStudent.setInt(4, deptId);
            pstmtStudent.setString(5, student.getYear());
            pstmtStudent.setString(6, formattedDob);
            pstmtStudent.setString(7, student.getGender());
            pstmtStudent.setString(8, student.getNationality());
            pstmtStudent.setString(9, student.getNrc());
            pstmtStudent.setString(10, student.getPhotoPath());

            pstmtStudent.executeUpdate();

            try (ResultSet generatedKeys = pstmtStudent.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    studentId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating student failed, no student ID obtained.");
                }
            }
        }

        try (PreparedStatement pstmtContact = conn.prepareStatement(studentContactSql)) {
            pstmtContact.setInt(1, studentId);
            pstmtContact.setString(2, student.getPhoneNo());
            pstmtContact.setString(3, null);
            pstmtContact.setString(4, student.getEmail());
            pstmtContact.setString(5, student.getCurrentAddress());
            pstmtContact.setString(6, student.getPermanentAddress());

            pstmtContact.executeUpdate();
        }

        try (PreparedStatement pstmtGuardian = conn.prepareStatement(studentGuardianSql)) {
            pstmtGuardian.setInt(1, studentId);
            pstmtGuardian.setString(2, student.getFatherName());
            pstmtGuardian.setString(3, student.getFatherOccupation());
            pstmtGuardian.setString(4, student.getFatherPhone());
            pstmtGuardian.setString(5, student.getFatherEmail());
            pstmtGuardian.setString(6, student.getMotherName());
            pstmtGuardian.setString(7, student.getMotherOccupation());
            pstmtGuardian.setString(8, student.getMotherPhone());
            pstmtGuardian.setString(9, student.getMotherEmail());

            pstmtGuardian.executeUpdate();
        }

        try (PreparedStatement pstmtBackground = conn.prepareStatement(studentEducationBackgroundSql)) {
            pstmtBackground.setInt(1, studentId);
            pstmtBackground.setString(2, student.getPrevSchool());
            pstmtBackground.setString(3, student.getYearCompleted());
            pstmtBackground.setString(4, student.getAdditionalInfo());

            pstmtBackground.executeUpdate();
        }

        return studentId;
    }

    public boolean modifyStudent(Student student) {
        Connection conn = null;
        String studentSql = """
                UPDATE student
                SET
                    roll_no = ?,
                    full_name = ?,
                    dept_id = ?,
                    current_year = ?,
                    date_of_birth = ?,
                    gender = ?,
                    nationality = ?,
                    nrc = ?,
                    profile_image_url = ?
                WHERE
                    student_id = ?
                    """;
        String studentContactSql = """
                UPDATE student_contact_information
                SET
                    primary_phone_number = ?,
                    email_address = ?,
                    current_address = ?,
                    permanent_address = ?
                WHERE
                    student_id = ?
                        """;
        String studentGuardianSql = """
                UPDATE student_guardian
                SET
                    father_full_name = ?,
                    father_occupation = ?,
                    father_phone_number = ?,
                    father_email_address = ?,
                    mother_full_name = ?,
                    mother_occupation = ?,
                    mother_phone_number = ?,
                    mother_email_address = ?
                WHERE
                    student_id = ?
                        """;
        String studentEducationSql = """
                UPDATE student_education_background
                SET
                    previous_school = ?,
                    year_completed = ?,
                    additional_info = ?
                WHERE
                    student_id = ?
                        """;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtStudent = conn.prepareStatement(studentSql)) {
                DepartmentDAO departmentDAO = new DepartmentDAO();
                int deptId = departmentDAO.getDeptIdByCodeOrName(conn, student.getMajor());

                String rawDob = student.getDob();
                String formattedDob = rawDob;
                if (rawDob != null && rawDob.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    String[] parts = rawDob.split("-");
                    formattedDob = parts[2] + "-" + parts[1] + "-" + parts[0];
                }
                pstmtStudent.setString(1, student.getRollNo());
                pstmtStudent.setString(2, student.getName());
                pstmtStudent.setInt(3, deptId);
                pstmtStudent.setString(4, student.getYear());
                pstmtStudent.setString(5, formattedDob);
                pstmtStudent.setString(6, student.getGender());
                pstmtStudent.setString(7, student.getNationality());
                pstmtStudent.setString(8, student.getNrc());
                pstmtStudent.setString(9, student.getPhotoPath());
                pstmtStudent.setInt(10, student.getNo());
                pstmtStudent.executeUpdate();
            }
            try (PreparedStatement pstmtContact = conn.prepareStatement(studentContactSql)) {
                pstmtContact.setString(1, student.getPhoneNo());
                pstmtContact.setString(2, student.getEmail());
                pstmtContact.setString(3, student.getCurrentAddress());
                pstmtContact.setString(4, student.getPermanentAddress());
                pstmtContact.setInt(5, student.getNo());
                pstmtContact.executeUpdate();
            }
            try (PreparedStatement pstmtGuardian = conn.prepareStatement(studentGuardianSql)) {
                pstmtGuardian.setString(1, student.getFatherName());
                pstmtGuardian.setString(2, student.getFatherOccupation());
                pstmtGuardian.setString(3, student.getFatherPhone());
                pstmtGuardian.setString(4, student.getFatherEmail());
                pstmtGuardian.setString(5, student.getMotherName());
                pstmtGuardian.setString(6, student.getMotherOccupation());
                pstmtGuardian.setString(7, student.getMotherPhone());
                pstmtGuardian.setString(8, student.getMotherEmail());
                pstmtGuardian.setInt(9, student.getNo());
                pstmtGuardian.executeUpdate();
            }
            try (PreparedStatement pstmtEducation = conn.prepareStatement(studentEducationSql)) {
                pstmtEducation.setString(1, student.getPrevSchool());
                pstmtEducation.setString(2, student.getYearCompleted());
                pstmtEducation.setString(3, student.getAdditionalInfo());
                pstmtEducation.setInt(4, student.getNo());
                pstmtEducation.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }

    }

    public boolean removeStudent(int studentId) {
        String sql = """
                UPDATE user_account
                SET
                    `status` = FALSE
                WHERE
                    user_id = (
                        SELECT user_id
                        FROM student
                        WHERE
                            student_id = ?
                    );
                        """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);

            int deletedRows = pstmt.executeUpdate();
            return deletedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Batch Soft-Delete Students ---
    public boolean removeStudents(List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return false;

        String sql = """
                UPDATE user_account
                SET `status` = FALSE
                WHERE user_id IN (
                    SELECT user_id
                    FROM student
                    WHERE student_id = ?
                );
                """;

        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Integer id : studentIds) {
                    pstmt.setInt(1, id);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }
}