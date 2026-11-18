package application.dao;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import application.Teacher;

public class TeacherDAO {

	public List<Teacher> getAllTeacher() {
        List<Teacher> teachers = new ArrayList<>();
        String sql = """
                SELECT t.teacher_id,
                       t.full_name,
                       COALESCE(d.dept_code, 'N/A') AS dept_code,
                       t.position,
                       COALESCE(tci.primary_phone_number, '') AS primary_phone_number
                FROM teacher t
                LEFT JOIN user_account ua ON t.user_id = ua.user_id
                LEFT JOIN department d ON t.dept_id = d.dept_id
                LEFT JOIN teacher_contact_information tci ON t.teacher_id = tci.teacher_id
                WHERE ua.status = TRUE OR ua.status IS NULL
                ORDER BY t.teacher_id ASC;
                """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int teacherId = rs.getInt("teacher_id");
                String name = rs.getString("full_name");
                String dept = rs.getString("dept_code");
                String position = rs.getString("position");
                String phone = rs.getString("primary_phone_number");
                String teacherCode = "TCH-" + dept + "-" + teacherId;

                teachers.add(new Teacher(teacherId, teacherCode, name, dept, position, phone));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teachers;
    }

    public List<Teacher> getDeletedTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        String sql = """
                SELECT t.teacher_id,
                       t.full_name,
                       COALESCE(d.dept_code, 'N/A') AS dept_code,
                       t.position,
                       COALESCE(tci.primary_phone_number, '') AS primary_phone_number
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                LEFT JOIN department d ON t.dept_id = d.dept_id
                LEFT JOIN teacher_contact_information tci ON t.teacher_id = tci.teacher_id
                WHERE ua.status = FALSE
                ORDER BY t.teacher_id ASC;
                """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("teacher_id");
                String fullName = rs.getString("full_name");
                String deptCode = rs.getString("dept_code");
                String position = rs.getString("position");
                String phoneNo = rs.getString("primary_phone_number");
                teachers.add(new Teacher(id, fullName, deptCode, position, phoneNo));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teachers;
    }

    public List<Teacher> getFilteredTeachers(String searchText, String department, String position) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.teacher_id, t.full_name, d.dept_code, t.position, tci.primary_phone_number
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                JOIN department d ON t.dept_id = d.dept_id
                JOIN teacher_contact_information tci ON t.teacher_id = tci.teacher_id
                WHERE ua.status = TRUE
                """);
        List<Object> parameters = new ArrayList<>();
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (LOWER(t.full_name) LIKE ? OR tci.primary_phone_number LIKE ?)");
            parameters.add("%" + searchText.toLowerCase().trim() + "%");
            parameters.add("%" + searchText.toLowerCase().trim() + "%");
        }
        if (department != null && !department.equalsIgnoreCase("All Departments")) {
            sql.append(" AND d.dept_code = ?");
            parameters.add(department);
        }
        if (position != null && !position.equalsIgnoreCase("All Positions")) {
            sql.append(" AND t.position = ?");
            parameters.add(position);
        }

        List<Teacher> teachers = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int teacherId = rs.getInt("teacher_id");
                    String name = rs.getString("full_name");
                    String dept = rs.getString("dept_code");
                    String teacherPosition = rs.getString("position");
                    String phone = rs.getString("primary_phone_number");
                    String teacherCode = "TCH-" + dept + "-" + teacherId;

                    Teacher teacher = new Teacher(teacherId, teacherCode, name, dept, teacherPosition, phone);
                    teachers.add(teacher);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teachers;
    }

    public Teacher getFullTeacher(int teacherId) {
        String sql = """
                SELECT
                    t.teacher_id,
                    t.full_name,
                    d.dept_code,
                    t.position,
                    t.date_of_birth,
                    t.gender,
                    t.nationality,
                    t.nrc,
                    t.profile_image_url,
                    tci.primary_phone_number,
                    tci.email_address,
                    tci.current_address,
                    tci.permanent_address,
                    tab.highest_degree,
                    tab.university,
                    tab.year_completed,
                    tab.specialization,
                    tab.emergency_contact
                FROM teacher t
                LEFT JOIN department d ON t.dept_id = d.dept_id
                LEFT JOIN teacher_contact_information tci ON t.teacher_id = tci.teacher_id
                LEFT JOIN teacher_academic_background tab ON t.teacher_id = tab.teacher_id
                WHERE t.teacher_id = ?;
                """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teacherId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("date_of_birth");
                    String dobString = (sqlDate != null) ? sqlDate.toString() : "";
                    String deptCode = rs.getString("dept_code") != null ? rs.getString("dept_code") : "CEIT";
                    String teacherCode = "TCH-" + deptCode + "-" + rs.getInt("teacher_id");

                    return new Teacher(
                            rs.getInt("teacher_id"), // 1. teacherId (int)
                            teacherCode, // 2. teacherCode (String)
                            rs.getString("full_name"), // 3. name
                            deptCode, // 4. department
                            rs.getString("position"), // 5. position
                            rs.getString("primary_phone_number"), // 6. phoneNo
                            dobString, // 7. dob
                            rs.getString("gender"), // 8. gender
                            rs.getString("nationality"), // 9. nationality
                            rs.getString("nrc"), // 10. nrc
                            rs.getString("email_address"), // 11. email
                            rs.getString("current_address"), // 12. currentAddress
                            rs.getString("permanent_address"), // 13. permanentAddress
                            rs.getString("highest_degree"), // 14. degree
                            rs.getString("university"), // 15. university
                            String.valueOf(rs.getInt("year_completed")), // 16. yearCompleted
                            rs.getString("specialization"), // 17. specialization
                            rs.getString("emergency_contact"), // 18. emergencyContact
                            rs.getString("profile_image_url") // 19. photoPath
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insertTecher(Teacher teacher) {
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);
            int teacherId = insertSingleTeacher(conn, teacher, "Default123");
            conn.commit();
            return teacherId;
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

    public int insertBatchTeachers(List<Teacher> teachers) {
        Connection conn = null;
        int successCount = 0;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);
            for (Teacher teacher : teachers) {
                int teacherId = insertSingleTeacher(conn, teacher, "Default123");
                if (teacherId > -1) {
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

    private int insertSingleTeacher(Connection conn, Teacher teacher, String password) throws SQLException {
        String userCreateSql = """
                        INSERT INTO
                    user_account (
                        username,
                        email,
                        password_hash,
                        user_role
                    )
                VALUES (?, ?, ?, 'TEACHER')
                                    """;
        String teacherCreateSql = """
                        INSERT INTO
                    teacher (
                        user_id,
                        full_name,
                        dept_id,
                        position,
                        date_of_birth,
                        gender,
                        nationality,
                        nrc,
                        profile_image_url
                    )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                        """;
        String teacherContactSql = """
                        INSERT INTO
                    teacher_contact_information (
                        teacher_id,
                        primary_phone_number,
                        email_address,
                        current_address,
                        permanent_address
                    )
                VALUES (?, ?, ?, ?, ?);
                        """;
        String teacherAcademicSql = """
                        INSERT INTO
                    teacher_academic_background (
                        teacher_id,
                        highest_degree,
                        university,
                        year_completed,
                        specialization,
                        emergency_contact
                    )
                VALUES (?, ?, ?, ?, ?, ?);
                        """;
        String username = teacher.getTeacherCode();
        String email = teacher.getTeacherCode().toLowerCase().replace("-", "") + "@ytu.edu.mm";
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
        int teacherId = -1;
        try (PreparedStatement pstmtTeacher = conn.prepareStatement(teacherCreateSql,
                Statement.RETURN_GENERATED_KEYS)) {
            DepartmentDAO departmentDAO = new DepartmentDAO();
            int deptId = departmentDAO.getDeptIdByCodeOrName(conn, teacher.getDepartment());
            String rawDob = teacher.getDob();
            String formattedDob = rawDob;
            if (rawDob != null && rawDob.matches("\\d{2}-\\d{2}-\\d{4}")) {
                String[] parts = rawDob.split("-");
                formattedDob = parts[2] + "-" + parts[1] + "-" + parts[0];
            }
            pstmtTeacher.setInt(1, userId);
            pstmtTeacher.setString(2, teacher.getName());
            pstmtTeacher.setInt(3, deptId);
            pstmtTeacher.setString(4, teacher.getPosition());
            pstmtTeacher.setString(5, formattedDob);
            pstmtTeacher.setString(6, teacher.getGender());
            pstmtTeacher.setString(7, teacher.getNationality());
            pstmtTeacher.setString(8, teacher.getNrc());
            pstmtTeacher.setString(9, teacher.getPhotoPath());

            pstmtTeacher.executeUpdate();
            try (ResultSet generatedKeys = pstmtTeacher.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    teacherId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating teacher failed, no student ID obtained.");
                }
            }
        }
        try (PreparedStatement pstmtContact = conn.prepareStatement(teacherContactSql)) {
            pstmtContact.setInt(1, teacherId);
            pstmtContact.setString(2, teacher.getPhoneNo());
            pstmtContact.setString(3, teacher.getEmail());
            pstmtContact.setString(4, teacher.getCurrentAddress());
            pstmtContact.setString(5, teacher.getPermanentAddress());

            pstmtContact.executeUpdate();
        }
        try (PreparedStatement pstmtAcademic = conn.prepareStatement(teacherAcademicSql)) {
            pstmtAcademic.setInt(1, teacherId);
            pstmtAcademic.setString(2, teacher.getDegree());
            pstmtAcademic.setString(3, teacher.getUniversity());
            pstmtAcademic.setString(4, teacher.getYearCompleted()); // Ensure your Teacher model has this getter
            pstmtAcademic.setString(5, teacher.getSpecialization());
            pstmtAcademic.setString(6, teacher.getEmergencyContact());

            pstmtAcademic.executeUpdate();
        }
        return teacherId;
    }

    public boolean modifyTeacher(Teacher teacher) {
        Connection conn = null;
        String teacherSql = """
                        UPDATE teacher
                SET
                    full_name = ?,
                    dept_id = ?,
                    position = ?,
                    date_of_birth = ?,
                    gender = ?,
                    nationality = ?,
                    nrc = ?,
                    profile_image_url = ?
                WHERE
                    teacher_id = ?
                        """;
        String teacherContactSql = """
                        UPDATE teacher_contact_information
                SET
                    primary_phone_number = ?,
                    email_address = ?,
                    current_address = ?,
                    permanent_address = ?
                WHERE
                    teacher_id = ?
                        """;
        String teacherAcademicSql = """
                        UPDATE teacher_academic_background
                SET
                    highest_degree = ?,
                    university = ?,
                    year_completed = ?,
                    specialization = ?,
                    emergency_contact = ?
                WHERE
                    teacher_id = ?;
                        """;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtStudent = conn.prepareStatement(teacherSql)) {
                DepartmentDAO departmentDAO = new DepartmentDAO();
                int deptId = departmentDAO.getDeptIdByCodeOrName(conn, teacher.getDepartment());

                String rawDob = teacher.getDob();
                String formattedDob = rawDob;
                if (rawDob != null && rawDob.matches("\\d{2}-\\d{2}-\\d{4}")) {
                    String[] parts = rawDob.split("-");
                    formattedDob = parts[2] + "-" + parts[1] + "-" + parts[0];
                }
                pstmtStudent.setString(1, teacher.getName());
                pstmtStudent.setInt(2, deptId);
                pstmtStudent.setString(3, teacher.getPosition());
                pstmtStudent.setString(4, formattedDob);
                pstmtStudent.setString(5, teacher.getGender());
                pstmtStudent.setString(6, teacher.getNationality());
                pstmtStudent.setString(7, teacher.getNrc());
                pstmtStudent.setString(8, teacher.getPhotoPath());
                pstmtStudent.setInt(9, teacher.getNo());
                pstmtStudent.executeUpdate();
            }
            try (PreparedStatement pstmtContact = conn.prepareStatement(teacherContactSql)) {
                pstmtContact.setString(1, teacher.getPhoneNo());
                pstmtContact.setString(2, teacher.getEmail());
                pstmtContact.setString(3, teacher.getCurrentAddress());
                pstmtContact.setString(4, teacher.getPermanentAddress());
                pstmtContact.setInt(5, teacher.getNo());
                pstmtContact.executeUpdate();
            }
            try (PreparedStatement pstmtAcademic = conn.prepareStatement(teacherAcademicSql)) {
                int yearCompInt = 0;
                if (teacher.getYearCompleted() != null && teacher.getYearCompleted().trim().matches("\\d+")) {
                    yearCompInt = Integer.parseInt(teacher.getYearCompleted().trim());
                }
                pstmtAcademic.setString(1, teacher.getDegree());
                pstmtAcademic.setString(2, teacher.getUniversity());
                pstmtAcademic.setInt(3, yearCompInt);
                pstmtAcademic.setString(4, teacher.getSpecialization());
                pstmtAcademic.setString(5, teacher.getEmergencyContact());
                pstmtAcademic.setInt(6, teacher.getNo());
                pstmtAcademic.executeUpdate();
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

    public boolean removeTeacher(int teacherId) {
        String sql = """
                        UPDATE user_account
                SET
                    `status` = FALSE
                WHERE
                    user_id = (
                        SELECT user_id
                        FROM teacher
                        WHERE
                            teacher_id = ?
                    );
                        """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teacherId);
            pstmt.executeUpdate();

            int deletedRows = pstmt.executeUpdate();
            return deletedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeBatchTeacher(List<Integer> teacherIds) {
        String sql = """
                UPDATE user_account
                        SET
                            `status` = FALSE
                        WHERE
                            user_id = (
                                SELECT user_id
                                FROM teacher
                                WHERE
                                    teacher_id = ?
                            );
                """;
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Integer teacherId : teacherIds) {
                    pstmt.setInt(1, teacherId);
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

    public boolean restoreTeacher(List<Integer> teacherIds) {
        String sql = """
                UPDATE user_account
                SET `status` = TRUE
                WHERE user_id IN (
                SELECT user_id FROM teacher
                WHERE teacher_id = ?);
                """;
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Integer teacherId : teacherIds) {
                    pstmt.setInt(1, teacherId);
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