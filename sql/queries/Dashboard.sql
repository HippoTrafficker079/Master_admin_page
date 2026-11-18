--Student Enrollment Trend
WITH
    CurrentCycle AS (
        SELECT
            CASE
                WHEN (MONTH(CURDATE()) >= 11) THEN (YEAR(CURDATE() + 1))
                ELSE YEAR(CURDATE())
            END AS cur_academic_year
    ),
    ProceseedEnrollment AS (
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
            END AS academic_school_year --2026, 2027, 2028 for example
        FROM
            course_enrollment ce
            JOIN student s ON ce.student_id = s.student_id
        WHERE
            ce.status IN ('ENROLLED', 'COMPLETED')
    )
SELECT
    pe.academic_school_year,
    pe.current_student_year - (
        cc.cur_academic_year - pe.academic_school_year
    ) AS historial_year_level,
    COUNT(DISTINCT pe.student_id) AS student_count
FROM
    ProcessedEnrollments pe
    CROSS JOIN CurrentCycle cc
WHERE
    pe.current_student_year IS NOT NULL
    AND pe.academic_school_year BETWEEN cc.curr_acad_year - ? AND cc.curr_acad_year -- ? = last (? + 1) year
GROUP BY
    pe.academic_school_year,
    historical_year_level
HAVING
    historical_year_level BETWEEN 1 AND 5
ORDER BY pe.academic_school_year ASC, historical_year_level ASC;

--Total Daily Attendance Percentage in each period
SELECT cs.period, (
        COUNT(DISTINCT ce.enrollment_id) - COUNT(a.attendance_id)
    ) * 100.0 / NULLIF(
        COUNT(DISTINCT ce.enrollment_id), 0
    ) AS attendance_percentage
FROM
    class_session cs
    JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
    AND ce.status = 'ENROLLED'
    LEFT JOIN attendance a ON cs.session_id = a.session_id
    AND a.enrollment_id = ce.enrollment_id
WHERE
    cs.is_cancelled = FALSE
    AND cs.session_date = CURDATE()
GROUP BY
    cs.period
ORDER BY cs.period ASC;

--Weekly
SELECT cs.session_date, (
        COUNT(DISTINCT ce.enrollment_id) - COUNT(a.attendance_id)
    ) * 100.0 / NULLIF(
        COUNT(DISTINCT ce.enrollment_id), 0
    ) AS attendance_percentage
FROM
    class_session cs
    JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
    AND ce.status = 'ENROLLED'
    LEFT JOIN attendance a ON cs.session_id = a.session_id
    AND a.enrollment_id = ce.enrollment_id
WHERE
    cs.is_cancelled = FALSE
    AND cs.session_date BETWEEN ? AND LEAST(?, CURDATE())
GROUP BY
    cs.session_date
ORDER BY cs.session_date ASC;

--Monthly
SELECT
    WEEK(cs.session_date, 1) - WEEK(
        DATE_SUB(
            cs.session_date,
            INTERVAL DAYOFMONTH(cs.session_date) -1 DAY
        ),
        1
    ) + 1 AS week_number,
    (
        COUNT(DISTINCT ce.enrollment_id) - COUNT(a.attendance_id)
    ) * 100.0 / NULLIF(
        COUNT(DISTINCT ce.enrollment_id),
        0
    ) AS attendance_percentage
FROM
    class_session cs
    JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
    AND ce.status = 'ENROLLED'
    LEFT JOIN attendance a ON cs.session_id = a.session_id
    AND a.enrollment_id = ce.enrollment_id
WHERE
    cs.is_cancelled = FALSE
    AND YEAR(cs.session_date) = YEAR(CURDATE())
    AND MONTH(cs.session_date) = MONTH(CURDATE())
    AND cs.session_date <= CURDATE()
GROUP BY
    week_number
ORDER BY week_number ASC;