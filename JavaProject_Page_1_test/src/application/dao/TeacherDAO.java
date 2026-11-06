package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import application.Teacher;

public class TeacherDAO {

    public List<Teacher> getAllTeacher() {
        List<Teacher> teachers = new ArrayList<>();
        String sql = """
                SELECT t.teacher_id, t.full_name, d.dept_code, t.position, tci.primary_phone_number
                FROM teacher t
                JOIN user_account ua ON t.user_id = ua.user_id
                JOIN department d ON t.dept_id = d.dept_id
                JOIN teacher_contact_information tci ON t.teacher_id = tci.teacher_id
                WHERE ua.status = TRUE
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

                Teacher teacher = new Teacher(teacherId, teacherCode, name, dept, position, phone);
                teachers.add(teacher);
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
}
