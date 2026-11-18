package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import application.dto.AttendanceTrendRecord;
import application.dto.EnrollmentTrendRecord;
import javafx.scene.chart.PieChart.Data;

public class DashboardDAO {
    public List<EnrollmentTrendRecord> getHistoricalTrend(int numYears) {
        int lookback = numYears - 1;
        String sql = """
                        WITH
                    CurrentCycle AS (
                        SELECT
                            CASE
                                WHEN (MONTH(CURDATE()) >= 11) THEN (YEAR(CURDATE() + 1))
                                ELSE YEAR(CURDATE())
                            END AS curr_acad_year
                    ),
                    ProcessedEnrollments AS (
                        SELECT
                            s.student_id,
                            CASE UPPER(s.current_year)
                                WHEN 'FIRST YEAR' THEN 1
                                WHEN 'SECOND YEAR' THEN 2
                                WHEN 'THIRD YEAR' THEN 3
                                WHEN 'FOURTH YEAR' THEN 4
                                WHEN 'FIFTH YEAR' THEN 5
                                ELSE NULL
                            END AS current_student_year,
                            CASE
                                WHEN MONTH(ce.enrollment_date) >= 11 THEN YEAR(ce.enrollment_date) + 1
                                ELSE YEAR(ce.enrollment_date)
                            END AS academic_school_year
                        FROM
                            course_enrollment ce
                            JOIN student s ON ce.student_id = s.student_id
                        WHERE
                            ce.status IN ('ENROLLED', 'COMPLETED')
                    )
                SELECT
                    pe.academic_school_year,
                    pe.current_student_year - (
                        cc.curr_acad_year - pe.academic_school_year
                    ) AS historical_year_level,
                    COUNT(DISTINCT pe.student_id) AS student_count
                FROM
                    ProcessedEnrollments pe
                    CROSS JOIN CurrentCycle cc
                WHERE
                    pe.current_student_year IS NOT NULL
                    AND pe.academic_school_year BETWEEN cc.curr_acad_year - ? AND cc.curr_acad_year
                GROUP BY
                    pe.academic_school_year,
                    historical_year_level
                HAVING
                    historical_year_level BETWEEN 1 AND 5
                ORDER BY pe.academic_school_year ASC, historical_year_level ASC;
                        """;
        List<EnrollmentTrendRecord> records = new ArrayList<>();
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, lookback);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int academicSchoolYear = rs.getInt("academic_school_year");
                    int historicalYearLevel = rs.getInt("historical_year_level");
                    int studentCount = rs.getInt("student_count");
                    records.add(new EnrollmentTrendRecord(academicSchoolYear, historicalYearLevel, studentCount));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public List<AttendanceTrendRecord> getDailyAttendance(String deptCode) {
        List<AttendanceTrendRecord> records = new ArrayList<>();

        // Base SQL query
        StringBuilder sql = new StringBuilder(
                "SELECT cs.period, (" +
                        "    COUNT(DISTINCT ce.enrollment_id) - COUNT(a.attendance_id)" +
                        ") * 100.0 / NULLIF(" +
                        "    COUNT(DISTINCT ce.enrollment_id), 0" +
                        ") AS attendance_percentage " +
                        "FROM class_session cs " +
                        "JOIN course_offering co ON cs.offering_id = co.offering_id " +
                        "JOIN course c ON co.course_id = c.course_id ");

        boolean hasDeptFilter = (deptCode != null && !deptCode.isEmpty() && !deptCode.equals("All Majors"));
        if (hasDeptFilter) {
            sql.append("JOIN department d ON c.dept_id = d.dept_id ");
        }

        sql.append(
                "JOIN course_enrollment ce ON cs.offering_id = ce.offering_id AND ce.status = 'ENROLLED' " +
                        "LEFT JOIN attendance a ON cs.session_id = a.session_id AND a.enrollment_id = ce.enrollment_id "
                        +
                        "WHERE cs.is_cancelled = FALSE AND cs.session_date = CURDATE() ");

        if (hasDeptFilter) {
            sql.append("AND d.dept_code = ? ");
        }

        sql.append("GROUP BY cs.period ORDER BY cs.period ASC;");

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (hasDeptFilter) {
                pstmt.setString(1, deptCode);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int period = rs.getInt("period");
                    double percentage = rs.getDouble("attendance_percentage");
                    records.add(new AttendanceTrendRecord(period, percentage));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }

    public List<AttendanceTrendRecord> getWeeklyAttendance(LocalDate startDate, LocalDate endDate, String deptCode) {
        List<AttendanceTrendRecord> records = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT cs.session_date, (" +
                        "    COUNT(DISTINCT ce.enrollment_id) - COUNT(a.attendance_id)" +
                        ") * 100.0 / NULLIF(" +
                        "    COUNT(DISTINCT ce.enrollment_id), 0" +
                        ") AS attendance_percentage " +
                        "FROM class_session cs " +
                        "JOIN course_offering co ON cs.offering_id = co.offering_id " +
                        "JOIN course c ON co.course_id = c.course_id ");

        // Dynamically append the department join if a filter is active
        boolean hasDeptFilter = (deptCode != null && !deptCode.isEmpty() && !deptCode.equals("All Majors"));
        if (hasDeptFilter) {
            sql.append("JOIN department d ON c.dept_id = d.dept_id ");
        }

        sql.append(
                "JOIN course_enrollment ce ON cs.offering_id = ce.offering_id AND ce.status = 'ENROLLED' " +
                        "LEFT JOIN attendance a ON cs.session_id = a.session_id AND a.enrollment_id = ce.enrollment_id "
                        +
                        "WHERE cs.is_cancelled = FALSE " +
                        "AND cs.session_date BETWEEN ? AND LEAST(?, CURDATE()) ");

        if (hasDeptFilter) {
            sql.append("AND d.dept_code = ? ");
        }

        sql.append("GROUP BY cs.session_date ORDER BY cs.session_date ASC;");

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            // Bind parameters safely in sequence
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            if (hasDeptFilter) {
                pstmt.setString(3, deptCode);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate sessionDate = rs.getDate("session_date").toLocalDate();
                    double percentage = rs.getDouble("attendance_percentage");
                    records.add(new AttendanceTrendRecord(sessionDate, percentage));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }

    public List<AttendanceTrendRecord> getMonthlyAttendanceTrend(String deptCode) {
        List<AttendanceTrendRecord> records = new ArrayList<>();

        // Base SQL query using StringBuilder
        StringBuilder sql = new StringBuilder(
                "SELECT " +
                        "    WEEK(cs.session_date, 1) - WEEK(" +
                        "        DATE_SUB(" +
                        "            cs.session_date," +
                        "            INTERVAL DAYOFMONTH(cs.session_date) - 1 DAY" +
                        "        )," +
                        "        1" +
                        "    ) + 1 AS week_number," +
                        "    (" +
                        "        COUNT(DISTINCT ce.enrollment_id) - COUNT(a.attendance_id)" +
                        "    ) * 100.0 / NULLIF(" +
                        "        COUNT(DISTINCT ce.enrollment_id)," +
                        "        0" +
                        "    ) AS attendance_percentage " +
                        "FROM class_session cs " +
                        "JOIN course_offering co ON cs.offering_id = co.offering_id " +
                        "JOIN course c ON co.course_id = c.course_id ");

        boolean hasDeptFilter = (deptCode != null && !deptCode.isEmpty() && !deptCode.equals("ALL"));
        if (hasDeptFilter) {
            sql.append("JOIN department d ON c.dept_id = d.dept_id ");
        }

        sql.append(
                "JOIN course_enrollment ce ON cs.offering_id = ce.offering_id AND ce.status = 'ENROLLED' " +
                        "LEFT JOIN attendance a ON cs.session_id = a.session_id AND a.enrollment_id = ce.enrollment_id "
                        +
                        "WHERE cs.is_cancelled = FALSE " +
                        "AND YEAR(cs.session_date) = YEAR(CURDATE()) " +
                        "AND MONTH(cs.session_date) = MONTH(CURDATE()) " +
                        "AND cs.session_date <= CURDATE() ");

        if (hasDeptFilter) {
            sql.append("AND d.dept_code = ? ");
        }

        sql.append("GROUP BY week_number ORDER BY week_number ASC;");

        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (hasDeptFilter) {
                pstmt.setString(1, deptCode);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int weekNum = rs.getInt("week_number");
                    double attendance = rs.getDouble("attendance_percentage");
                    records.add(new AttendanceTrendRecord(weekNum, attendance, true));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }
}
