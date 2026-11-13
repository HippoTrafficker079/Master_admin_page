package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import application.AttendanceRecord;

public class AttendanceDAO {
    public List<AttendanceRecord> AtteandanceMatrixSummary(String searchText, String selectedDept,
            String selectedYear,
            String SelectedSubject, LocalDate startDate, LocalDate endDate, boolean lowOnly) {
        List<AttendanceRecord> attendanceStudents = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                                                            WITH
                    student_attendance_metric AS (
                        SELECT
                            s.user_id,
                            s.student_id,
                            s.roll_no,
                            s.full_name,
                            d.dept_code,
                            s.current_year,
                            (
                                SELECT COUNT(cs.session_id)
                                FROM
                                    class_session cs
                                    JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
                                WHERE
                                    ce.student_id = s.student_id
                                    AND cs.session_date BETWEEN COALESCE(
                                        ?,
                                        (
                                            SELECT MIN(cs2.session_date)
                                            FROM
                                                class_session cs2
                                                JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                                            WHERE
                                                ce2.student_id = s.student_id
                                        )
                                    ) AND COALESCE(?, CURDATE())
                                    AND cs.is_cancelled = FALSE
                                    AND ce.status = 'ENROLLED'
                            ) AS total_sessions,
                            (
                                SELECT COUNT(a.attendance_id)
                                FROM
                                    attendance a
                                    JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
                                    JOIN class_session cs ON a.session_id = cs.session_id
                                WHERE
                                    ce.student_id = s.student_id
                                    AND cs.session_date BETWEEN COALESCE(
                                        ?,
                                        (
                                            SELECT MIN(cs2.session_date)
                                            FROM
                                                class_session cs2
                                                JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                                            WHERE
                                                ce2.student_id = s.student_id
                                        )
                                    ) AND COALESCE(?, CURDATE())
                            ) AS total_absent
                        FROM
                            student s
                            JOIN user_account ua ON s.user_id = ua.user_id
                            JOIN department d ON s.dept_id = d.dept_id
                            JOIN course_enrollment ce ON s.student_id = ce.student_id
                        WHERE
                            ce.status = 'ENROLLED'
                            AND ua.status = TRUE
                        GROUP BY
                            s.user_id,
                            s.student_id,
                            s.roll_no,
                            s.full_name,
                            d.dept_code,
                            s.current_year
                    )
                SELECT
                    user_id,
                    student_id,
                    roll_no,
                    full_name,
                    dept_code,
                    current_year,
                    total_sessions,
                    total_absent,
                    ROUND(
                        (total_sessions - total_absent) * 100.0 / NULLIF(total_sessions, 0),
                        2
                    ) AS overall_percentage
                FROM student_attendance_metric
                WHERE
                    1 = 1
                        """);
        List<Object> parameters = new ArrayList<>();

        parameters.add(startDate != null ? startDate.toString() : null);
        parameters.add(endDate != null ? endDate.toString() : null);

        parameters.add(startDate != null ? startDate.toString() : null);
        parameters.add(endDate != null ? endDate.toString() : null);
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append("""
                    AND ( full_name LIKE ? OR roll_no LIKE ? )
                    """);
            parameters.add("%" + searchText.toLowerCase() + "%");
            parameters.add("%" + searchText.toLowerCase() + "%");
        }
        if (selectedDept != null && !selectedDept.trim().isEmpty()) {
            sql.append("""
                    AND dept_code = ?
                    """);
            parameters.add(selectedDept);
        }
        if (selectedYear != null && !selectedYear.trim().isEmpty()) {
            sql.append("""
                    AND current_year = ?
                    """);
            parameters.add(selectedYear);
        }
        if (lowOnly) {
            sql.append("AND overall_percentage < 75");
        }
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int userId = rs.getInt("user_id");
                    int studentId = rs.getInt("student_id");
                    String rollNo = rs.getString("roll_no");
                    String name = rs.getString("full_name");
                    String deptCode = rs.getString("dept_code");
                    String currentYear = rs.getString("current_year");
                    int totalSessions = rs.getInt("total_sessions");
                    int totalAbsent = rs.getInt("total_absent");
                    double overallPercentage = rs.getDouble("overall_percentage");

                    AttendanceRecord attendanceStudent = new AttendanceRecord(userId, studentId, rollNo, name,
                            deptCode, currentYear, totalSessions, totalAbsent, overallPercentage);
                    attendanceStudents.add(attendanceStudent);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attendanceStudents;
    }
}
