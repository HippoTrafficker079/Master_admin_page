package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import application.AttendanceRecord;
import application.Student;
import application.dto.StudentProfile;

public class AttendanceDAO {
    private int lastFetchedTotalCount = 0;

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
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();

        parameters.add(startDate != null ? startDate.toString() : null);
        parameters.add(endDate != null ? endDate.toString() : null);

        parameters.add(startDate != null ? startDate.toString() : null);
        parameters.add(endDate != null ? endDate.toString() : null);

        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND ( full_name LIKE ? OR roll_no LIKE ? ) ");
            parameters.add("%" + searchText.toLowerCase() + "%");
            parameters.add("%" + searchText.toLowerCase() + "%");
        }
        if (selectedDept != null && !selectedDept.trim().isEmpty()) {
            sql.append(" AND dept_code = ? ");
            parameters.add(selectedDept);
        }
        if (selectedYear != null && !selectedYear.trim().isEmpty()) {
            sql.append(" AND current_year = ? ");
            parameters.add(selectedYear);
        }
        if (lowOnly) {
            sql.append(" HAVING overall_percentage < 75 ");
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
                    AND 1 = 1
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
        sql.append("""
                GROUP BY
                        s.student_id,
                        s.roll_no,
                        s.full_name,
                        d.dept_code,
                        s.current_year;""");
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
                                    cs.session_id,
                                    ce.enrollment_id,
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
                    WHEN a.status = 'EXCUSED' THEN 'Excused'
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
            parameters.add(Integer.parseInt(selectedPeriod.trim()));
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
                                        WHEN a.status = 'EXCUSED' THEN 'Excused'
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
                    int studentId = rs.getInt("student_id");
                    String rollNo = rs.getString("roll_no");
                    String studentName = rs.getString("full_name");
                    String department = rs.getString("dept_code");
                    String year = rs.getString("current_year");
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
                            rs.getInt("session_id"),
                            rs.getInt("enrollment_id"),
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dailyLogs;
    }

    public void updateAttendanceStatus(AttendanceRecord record, String newStatus, String reason) {
        System.out.println("DAO called! Session ID: " + record.getSessionId() + ", Enrollment ID: "
                + record.getEnrollmentId() + ", Status: " + newStatus);
        int sessionId = record.getSessionId();
        int enrollmentId = record.getEnrollmentId();

        if ("Present".equalsIgnoreCase(newStatus)) {
            String deleteSql = "DELETE FROM attendance WHERE session_id = ? AND enrollment_id = ?";

            try (Connection conn = DatabaseConnector.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, sessionId);
                pstmt.setInt(2, enrollmentId);
                int rowsAffected = pstmt.executeUpdate();
                System.out.println("DELETE executed. Rows removed: " + rowsAffected);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String dbStatus = "Absent".equalsIgnoreCase(newStatus) ? "UNEXCUSED" : "EXCUSED";
            String cleanReason = (reason != null && !reason.trim().isEmpty() && !reason.trim().equals("-"))
                    ? reason.trim()
                    : ("Absent".equalsIgnoreCase(newStatus) ? "Absent Today" : "");

            String upsertSql = """
                        INSERT INTO attendance (session_id, enrollment_id, status, reason)
                        VALUES (?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                            status = VALUES(status),
                            reason = VALUES(reason)
                    """;

            try (Connection conn = DatabaseConnector.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {
                pstmt.setInt(1, sessionId);
                pstmt.setInt(2, enrollmentId);
                pstmt.setString(3, dbStatus);
                pstmt.setString(4, cleanReason);
                int rowsAffected = pstmt.executeUpdate();
                System.out.println("UPSERT executed. Rows affected: " + rowsAffected);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Student> studentListForSearchBar(String searchText, String selectedDept, String selectedYear) {
        StringBuilder sql = new StringBuilder(
                """
                        SELECT s.student_id, s.roll_no, s.full_name, d.dept_code, s.current_year, ci.primary_phone_number
                                FROM
                                    student s
                                    JOIN user_account ua ON s.user_id = ua.user_id
                                    JOIN department d ON s.dept_id = d.dept_id
                                    JOIN student_contact_information ci ON s.student_id = ci.student_id
                                WHERE
                                    ua.status = TRUE
                                    AND 1 = 1
                                        """);
        List<Object> parameters = new ArrayList<>();
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
        List<Student> studentList = new ArrayList<>();

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    String rollNo = rs.getString("roll_no");
                    String name = rs.getString("full_name");
                    String deptCode = rs.getString("dept_code");
                    String currentYear = rs.getString("current_year");
                    String phoneNumber = rs.getString("primary_phone_number");

                    studentList.add(new Student(studentId, rollNo, name,
                            deptCode, currentYear, null, phoneNumber));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return studentList;
    }

    public StudentProfile getStudentProfile(int studentId) {
        String sql = """
                                                    WITH
                                student_matrix AS (
                                    SELECT
                                        s.user_id,
                                        s.student_id,
                                        s.roll_no,
                                        s.full_name,
                                        d.dept_code,
                                        s.current_year,
                                        s.profile_image_url,
                                        s.nrc,
                                        ua.status AS user_status,
                                        ci.primary_phone_number,
                                        ci.email_address,
                                        ci.permanent_address,
                                        (
                                            SELECT COUNT(cs.session_id)
                                            FROM
                                                class_session cs
                                                JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
                                            WHERE
                                                ce.student_id = s.student_id
                                                AND cs.session_date BETWEEN (
                                                    SELECT MIN(cs2.session_date)
                                                    FROM
                                                        class_session cs2
                                                        JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                                                    WHERE
                                                        ce2.student_id = s.student_id
                                                ) AND (CURDATE())
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
                                                AND cs.session_date BETWEEN (
                                                    SELECT MIN(cs2.session_date)
                                                    FROM
                                                        class_session cs2
                                                        JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                                                    WHERE
                                                        ce2.student_id = s.student_id
                                                ) AND (CURDATE())
                                        ) AS total_absent
                                    FROM
                                        student s
                                        JOIN user_account ua ON s.user_id = ua.user_id
                                        JOIN department d ON s.dept_id = d.dept_id
                                        JOIN course_enrollment ce ON s.student_id = ce.student_id
                                        JOIN student_contact_information ci ON s.student_id = ci.student_id
                                    WHERE
                                        ce.status = 'ENROLLED'
                                        AND s.student_id = ?
                                    GROUP BY
                                        s.user_id,
                                        s.student_id,
                                        s.roll_no,
                                        s.full_name,
                                        d.dept_code,
                                        s.current_year,
                                        s.profile_image_url,
                                        s.nrc,
                                        ua.status,
                                        ci.primary_phone_number,
                                        ci.email_address,
                                        ci.permanent_address
                                )
                    SELECT
                    user_id,
                    student_id,
                    roll_no,
                    full_name,
                    dept_code,
                    current_year,
                    profile_image_url,
                    nrc,
                    user_status,
                    total_sessions,
                    total_absent,
                    total_sessions - total_absent AS total_attended,
                    -- Calculate attendance percentage
                    CASE
                        WHEN total_sessions = 0 THEN 100.0
                        ELSE (
                            (total_sessions - total_absent) * 100.0 / total_sessions
                        )
                    END AS attendance_percentage,
                    -- Determine academic standing based on percentage threshold (e.g., < 75% is Warning)
                    CASE
                        WHEN total_sessions = 0 THEN 'Good'
                        WHEN (
                            (total_sessions - total_absent) * 100.0 / total_sessions
                        ) < 75.0 THEN 'Warning'
                        ELSE 'Good'
                    END AS academic_standing,
                    primary_phone_number,
                    email_address,
                    permanent_address
                FROM student_matrix;
                                                    """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new StudentProfile(
                            rs.getInt("user_id"),
                            rs.getInt("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("full_name"),
                            rs.getString("dept_code"),
                            rs.getString("current_year"),
                            rs.getString("profile_image_url"),
                            rs.getString("nrc"),
                            rs.getBoolean("user_status"),
                            rs.getInt("total_sessions"),
                            rs.getInt("total_absent"),
                            rs.getInt("total_attended"),
                            rs.getDouble("attendance_percentage"),
                            rs.getString("academic_standing"),
                            rs.getString("primary_phone_number"),
                            rs.getString("email_address"),
                            rs.getString("permanent_address"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<AttendanceRecord> getStudentCourseBreakdown(int studentId) {
        List<AttendanceRecord> breakdownList = new ArrayList<>();
        String sql = """
                        SELECT
                    s.student_id,
                    c.course_code,
                    c.course_name,
                    COUNT(cs.session_id) AS total_sessions,
                    COUNT(a.attendance_id) AS total_absent,
                    COUNT(cs.session_id) - COUNT(a.attendance_id) AS total_attended,
                    ROUND(
                        (
                            COUNT(cs.session_id) - COUNT(a.attendance_id)
                        ) * 100.0 / NULLIF(COUNT(cs.session_id), 0),
                        2
                    ) AS attendance_percentage
                FROM
                    student s
                    JOIN user_account ua ON s.user_id = ua.user_id
                    JOIN course_enrollment ce ON s.student_id = ce.student_id
                    JOIN course_offering co ON ce.offering_id = co.offering_id
                    JOIN course c ON co.course_id = c.course_id
                    JOIN class_session cs ON co.offering_id = cs.offering_id
                    LEFT JOIN attendance a ON cs.session_id = a.session_id
                    AND ce.enrollment_id = a.enrollment_id
                WHERE
                    ua.status = TRUE
                    AND ce.status = 'ENROLLED'
                    AND cs.is_cancelled = FALSE
                    AND cs.session_date BETWEEN (
                        SELECT MIN(cs2.session_date)
                        FROM
                            class_session cs2
                            JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                        WHERE
                            ce2.student_id = s.student_id
                    ) AND CURDATE()
                    AND s.student_id = ?
                GROUP BY
                    s.student_id,
                    c.course_code,
                    c.course_name
                        """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String courseName = rs.getString("course_name");
                    String courseCode = rs.getString("course_code");
                    int totalSessions = rs.getInt("total_sessions");
                    int totalAbsent = rs.getInt("total_absent");
                    double percentage = rs.getDouble("attendance_percentage");

                    AttendanceRecord record = new AttendanceRecord(
                            studentId,
                            courseName,
                            courseCode,
                            totalSessions,
                            totalAbsent,
                            percentage,
                            null // Let the constructor auto-generate the remarks/standing based on percentage!
                    );

                    breakdownList.add(record);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return breakdownList;
    }

    public List<AttendanceRecord> detailedChronoLog(int studentId, String selectedCourse, String status,
            LocalDate startDate, LocalDate endDate, int offsetNum) {
        int totalRecords = 0;
        StringBuilder sql = new StringBuilder("""
                        SELECT
                            s.student_id,
                            cs.session_date,
                            cs.period,
                            c.course_name,
                            c.course_code,
                            COALESCE(a.status, 'Present') AS attendance_status,
                            t.full_name AS teacher_name,
                            COALESCE(a.reason, '-') AS reason,
                            COUNT(*) OVER() AS total_count
                        FROM
                            student s
                            JOIN user_account ua ON s.user_id = ua.user_id
                            JOIN course_enrollment ce ON s.student_id = ce.student_id
                            JOIN course_offering co ON ce.offering_id = co.offering_id
                            JOIN class_session cs ON co.offering_id = cs.offering_id
                            JOIN course c ON co.course_id = c.course_id
                            JOIN teacher t ON co.teacher_id = t.teacher_id
                            LEFT JOIN attendance a ON cs.session_id = a.session_id
                            AND ce.enrollment_id = a.enrollment_id
                        WHERE
                            ua.status = TRUE
                            AND ce.status = 'ENROLLED'
                            AND cs.is_cancelled = FALSE
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
                            AND s.student_id = ?
                """);

        List<Object> parameters = new ArrayList<>();
        parameters.add(startDate != null ? startDate.toString() : null);
        parameters.add(endDate != null ? endDate.toString() : null);
        parameters.add(studentId);

        if (selectedCourse != null && !selectedCourse.trim().isEmpty() && !selectedCourse.equals("All Courses")) {
            sql.append(" AND c.course_name = ? ");
            parameters.add(selectedCourse);
        }

        if (status != null && !status.trim().isEmpty() && !status.equals("All Status")) {
            sql.append(" AND COALESCE(a.status, 'Present') = ? ");
            if (status.equalsIgnoreCase("Absent")) {
                parameters.add("UNEXCUSED");
            } else {
                parameters.add(status);
            }
        }
        sql.append("""
                    ORDER BY cs.session_date DESC, cs.period ASC
                    LIMIT 6
                    OFFSET ?
                """);
        parameters.add(offsetNum);

        List<AttendanceRecord> logList = new ArrayList<>();

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRecords = rs.getInt("total_count");
                    do {
                        LocalDate sessionDate = rs.getDate("session_date") != null
                                ? rs.getDate("session_date").toLocalDate()
                                : null;
                        int period = rs.getInt("period");
                        String courseName = rs.getString("course_name");
                        String courseCode = rs.getString("course_code");
                        String dbStatus = rs.getString("attendance_status");

                        String uiStatus = dbStatus.equals("UNEXCUSED") ? "Absent" : dbStatus;

                        String teacherName = rs.getString("teacher_name");
                        String reason = rs.getString("reason");

                        AttendanceRecord record = new AttendanceRecord(
                                studentId,
                                "", // rollNo
                                "", // studentName
                                "", // department
                                "", // year
                                "", // semester
                                sessionDate, // dateObj
                                period, // period
                                courseName, // subject
                                courseCode, // courseCode
                                uiStatus, // status
                                teacherName, // markedBy
                                reason // remarks
                        );

                        logList.add(record);
                    } while (rs.next());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.lastFetchedTotalCount = totalRecords;
        return logList;
    }

    public double getTodayAttendancePercentage() {
        String sql = """
                    SELECT
                        COUNT(CASE WHEN COALESCE(a.status, 'Present') IN ('Present', 'EXCUSED') THEN 1 END) AS present_count,
                        COUNT(*) AS total_count
                    FROM class_session cs
                    JOIN course_offering co ON cs.offering_id = co.offering_id
                    JOIN course_enrollment ce ON co.offering_id = ce.offering_id
                    LEFT JOIN attendance a ON cs.session_id = a.session_id AND ce.enrollment_id = a.enrollment_id
                    WHERE cs.session_date = CURDATE() AND ce.status = 'ENROLLED'
                """;

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int presentCount = rs.getInt("present_count");
                int totalCount = rs.getInt("total_count");

                if (totalCount == 0) {
                    return 100.0;
                }
                return ((double) presentCount / totalCount) * 100.0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0.0;
    }

    public int getLastFetchedTotalCount() {
        return lastFetchedTotalCount;
    }
}