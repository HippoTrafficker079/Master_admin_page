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
                                            SELECT MIN(sub.session_date)
                                            FROM (
                                                SELECT cs2.session_date
                                                FROM class_session cs2
                                                JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                                                WHERE ce2.student_id = s.student_id
                                            ) AS sub
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
                                            SELECT MIN(sub.session_date)
                                            FROM (
                                                SELECT cs2.session_date
                                                FROM class_session cs2
                                                JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                                                WHERE ce2.student_id = s.student_id
                                            ) AS sub
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
        return executeAttendanceQuery(sql, parameters);
    }

    public List<AttendanceRecord> SubjectFilterMatrixSummary(String searchText, String selectedDept,
            String selectedYear,
            String SelectedSubject, LocalDate startDate, LocalDate endDate, boolean lowOnly, String selectedCourse) {
        StringBuilder sql = new StringBuilder("""
                        SELECT
                    s.user_id,
                    s.student_id,
                    s.roll_no,
                    s.full_name,
                    d.dept_code AS dept_code,
                    s.current_year,
                    COUNT(cs.session_id) AS total_sessions,
                    COUNT(a.attendance_id) AS total_absent,
                    ROUND(
                        (
                            COUNT(cs.session_id) - COUNT(a.attendance_id)
                        ) * 100.0 / NULLIF(COUNT(cs.session_id), 0),
                        2
                    ) AS overall_percentage
                FROM
                    student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN department d ON s.dept_id = d.dept_id
                    JOIN course_enrollment ce ON s.student_id = ce.student_id
                    JOIN class_session cs ON ce.offering_id = cs.offering_id
                    JOIN course_offering co ON ce.offering_id = co.offering_id
                    JOIN course c ON co.course_id = c.course_id
                    LEFT JOIN attendance a ON ce.enrollment_id = a.enrollment_id
                    AND cs.session_id = a.session_id
                WHERE
                    ce.status = 'ENROLLED'
                    AND ua.status = TRUE
                    AND cs.is_cancelled = FALSE
                    AND cs.session_date BETWEEN COALESCE(
                        ?,
                        (
                            SELECT MIN(sub.session_date)
                            FROM (
                                SELECT cs2.session_date
                                FROM class_session cs2
                                JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                                WHERE ce2.student_id = s.student_id
                            ) AS sub
                        )
                    ) AND COALESCE(?, CURDATE())
                    AND (
                        c.course_name LIKE ?
                    )
                GROUP BY
                    s.student_id,
                    s.roll_no,
                    s.full_name,
                    d.dept_code,
                    s.current_year;
                    """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(startDate != null ? startDate.toString() : null);
        parameters.add(endDate != null ? endDate.toString() : null);
        parameters.add("%" + selectedCourse + "%");
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
            sql.append(" HAVING overall_percentage < 75");
        }
        return executeAttendanceQuery(sql, parameters);
    }

    private List<AttendanceRecord> executeAttendanceQuery(StringBuilder sql, List<Object> parameters) {
        List<AttendanceRecord> studentList = new ArrayList<>();

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

                    studentList.add(new AttendanceRecord(userId, studentId, rollNo, name,
                            deptCode, currentYear, totalSessions, totalAbsent, overallPercentage));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return studentList;
    }

    public List<AttendanceRecord> dailyLog(LocalDate selectedDate, String selectedPeriod, String searchText,
            boolean pChecked, boolean aChecked, boolean eChecked) {
        List<AttendanceRecord> dailyLogs = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                                            SELECT
                    s.student_id,
                    s.roll_no,
                    s.full_name,
                    d.dept_code,
                    s.current_year,
                    cs.session_date,
                    cs.period,
                    c.course_name,
                    CASE
                        WHEN a.attendance_id IS NULL THEN 'Present'
                        WHEN LOWER(a.reason) LIKE '%medical%'
                        OR LOWER(a.reason) LIKE '%family%' THEN 'Excused'
                        ELSE 'Absent'
                    END AS computed_status,
                    t.full_name AS teacher_name,
                    COALESCE(a.reason, 'On Time') AS reason
                FROM
                    student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN course_enrollment ce ON s.student_id = ce.student_id
                    JOIN department d ON s.dept_id = d.dept_id
                    JOIN course_offering co ON ce.offering_id = co.offering_id
                    JOIN class_session cs ON co.offering_id = cs.offering_id
                    JOIN course c ON co.course_id = c.course_id
                    JOIN teacher t ON co.teacher_id = t.teacher_id
                    LEFT JOIN attendance a ON cs.session_id = a.session_id
                    AND ce.enrollment_id = a.enrollment_id
                WHERE
                    ua.status = TRUE
                    AND cs.session_date = COALESCE(?, CURDATE())
                    AND 1 = 1
                                        """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(selectedDate != null ? selectedDate.toString() : null);

        // 2. Period Filter
        if (selectedPeriod != null && !selectedPeriod.equals("All Periods")) {
            sql.append(" AND cs.period = ? ");
            parameters.add(selectedPeriod);
        }

        // 3. Search Text Filter (Student Name, Roll No, or Course Name)
        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND (s.full_name LIKE ? OR s.roll_no LIKE ? OR c.course_name LIKE ?) ");
            String searchPattern = "%" + searchText.toLowerCase().trim() + "%";
            parameters.add(searchPattern);
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }

        // 4. Status / Remark Checkbox Filters (Present, Absent, Excuse)
        List<String> statusFilters = new ArrayList<>();
        if (pChecked)
            statusFilters.add("Present");
        if (aChecked)
            statusFilters.add("Absent");
        if (eChecked)
            statusFilters.add("Excused");

        if (!statusFilters.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < statusFilters.size(); i++) {
                if (i > 0)
                    sql.append(" OR ");
                sql.append(
                        """
                                    CASE
                                        WHEN a.attendance_id IS NULL THEN 'Present'
                                        WHEN LOWER(a.reason) LIKE '%medical%' OR LOWER(a.reason) LIKE '%family%' THEN 'Excused'
                                        ELSE 'Absent'
                                    END = ?
                                """);
                parameters.add(statusFilters.get(i));
            }
            sql.append(") ");
        }

        sql.append(" ORDER BY cs.session_date DESC, cs.period ASC, s.roll_no ASC");
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    while (rs.next()) {
                        int studentId = rs.getInt("student_id");
                        String rollNo = rs.getString("roll_no");
                        String studentName = rs.getString("full_name");

                        String department = rs.getString("dept_code");
                        String year = rs.getString("current_year");

                        // Fallback semester string if your query doesn't select it directly
                        String semester = "First Semester";

                        LocalDate dateObj = rs.getDate("session_date") != null
                                ? rs.getDate("session_date").toLocalDate()
                                : null;
                        int period = rs.getInt("period");
                        String subject = rs.getString("course_name");
                        String status = rs.getString("computed_status");
                        String markedBy = rs.getString("teacher_name");
                        String remarks = rs.getString("reason");

                        AttendanceRecord record = new AttendanceRecord(
                                studentId,
                                rollNo,
                                studentName,
                                department != null ? department : "",
                                year != null ? year : "",
                                semester,
                                dateObj,
                                period,
                                subject,
                                status,
                                markedBy != null ? markedBy : "Unmarked",
                                remarks != null ? remarks : "On Time");

                        dailyLogs.add(record);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dailyLogs;
    }
}
