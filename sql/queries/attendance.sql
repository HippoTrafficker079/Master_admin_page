--MASTER_ADMIN VIEW

--CLASS VIEW
--Matrix Summary

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
                    AND cs.session_date BETWEEN ? AND ?
                    AND cs.is_cancelled = FALSE
                    AND ce.status = 'ENROLLED'
            ) AS total_sessions,
            (
                SELECT COUNT(a.attendance_id)
                FROM
                    attendance a
                    JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
                WHERE
                    ce.student_id = s.student_id
                    AND a.absent_date BETWEEN ? AND ?
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
    1 = 1;

--Append year dynamically
AND current_year = ?

--Append Department dynamically
AND dept_code = ?

--For Filtering Attendance<75%
AND overall_percentage < 75

--Search with student_name
AND ( full_name LIKE ? OR roll_no LIKE ? )

--IF FILTERED WITH SUBJECT
SELECT
    s.student_id,
    s.roll_no,
    s.full_name,
    d.dept_code,
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
    JOIN department d ON s.dept_id = d.dept_id
    JOIN course_enrollment ce ON s.student_id = ce.student_id
    JOIN class_session cs ON ce.offering_id = cs.offering_id
    LEFT JOIN attendance a ON ce.offering_id = a.offering_id
    AND cs.session_id = a.session_id
WHERE
    ce.status = 'ENROLLED'
    AND cs.is_cancelled = FALSE
    AND cs.session_date BETWEEN ? AND ?
    AND ce.offering_id = ?
GROUP BY
    s.student_id,
    s.roll_no,
    s.full_name,
    d.dept_code,
    s.current_year;

--Finding the total session of all subjects
SELECT COUNT(cs.session_id) AS total_sessions
FROM
    class_session cs
    JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
WHERE
    ce.student_id = s.student_id
    AND cs.session_date BETWEEN ? AND ?
    AND cs.is_cancelled = FALSE
    AND ce.status = 'ENROLLED';

--Finding the total absent attendance session
SELECT COUNT(a.attendance_id) AS total_absent
FROM
    attendance a
    JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
WHERE
    ce.student_id = s.student_id
    AND a.absent_date BETWEEN ? AND ?;

--Daily Log
SELECT
    s.student_id,
    s.roll_no,
    s.full_name,
    cs.session_date,
    cs.period,
    c.course_name,
    COALESCE(a.status, 'Present') AS attendance_status,
    t.full_name AS teacher_name,
    COALESCE(a.reason, 'On Time')
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
    AND cs.session_date = ?
    AND 1 = 1
ORDER BY cs.period ASC;
--Append dynamically when subject is filtered
AND c.course_id = ?

--Append dynamically when attendance status is checkbox
AND COALESCE(a.status, 'Present') = ?

--Append dyamically when period is filtered
AND cs.period = ?

--IF attendance status is edited,
UPDATE attendance a
JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
JOIN class_session cs ON a.session_id = cs.session_id
SET
    a.status = ?,
    a.reason = ?
WHERE
    ce.student_id = ?
    AND cs.session_date = ?
    AND cs.period = ?

--Individual Student View
--After searching studnents from searchbar
SELECT s.student_id, s.roll_no, s.full_name, d.dept_code, s.current_year, ci.primary_phone_number
FROM
    student s
    JOIN user_account ua ON s.user_id = ua.user_id
    JOIN department d ON s.dept_id = d.dept_id
    JOIN student_contact_information ci ON s.student_id = ci.student_id
WHERE
    ua.status = TRUE
    AND 1 = 1

--Must be appended at least
AND ( s.roll_no LIKE ? OR s.full_name LIKE ? )

--Append Dynamically When year is filtered
AND s.current_year = ?

--Append Dynamically When Department is filtered
AND d.dept_id = ?

--After Selecting a student
--Student Profile Card & Overall University Metric
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
            (
                SELECT COUNT(cs.session_id)
                FROM
                    class_session cs
                    JOIN course_enrollment ce ON cs.offering_id = ce.offering_id
                WHERE
                    ce.student_id = s.student_id
                    AND cs.session_date BETWEEN ce.enrollment_date AND CURRENT_DATE
                    AND cs.is_cancelled = FALSE
                    AND ce.status = 'ENROLLED'
            ) AS total_sessions,
            (
                SELECT COUNT(a.attendance_id)
                FROM
                    attendance a
                    JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
                WHERE
                    ce.student_id = s.student_id
                    AND a.absent_date BETWEEN ce.enrollment_date AND CURRENT_DATE
            ) AS total_absent,
            ci.primary_phone_number AS contact
        FROM
            student s
            JOIN user_account ua ON s.user_id = ua.user_id
            JOIN department d ON s.dept_id = d.dept_id
            JOIN course_enrollment ce ON s.student_id = ce.student_id
            JOIN student_contact_information ci ON s.student_id = ci.student_id
        WHERE
            ce.status = 'ENROLLED'
            AND ua.status = TRUE
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
            ci.primary_phone_number
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
    total_sessions,
    total_absent,
    total_sessions - total_absent AS total_attended,
    contact
FROM student_matrix

--Subject Breakdown
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
    AND cs.session_date BETWEEN ce.enrollment_date AND CURRENT_DATE
    AND cs.is_cancelled = FALSE
    AND s.student_id = ?
GROUP BY
    s.student_id,
    c.course_code,
    c.course_name

--Detailed Chronological log
SELECT
    s.student_id,
    cs.session_date,
    cs.period,
    c.course_name,
    COALESCE(a.status, 'Present') AS attendance_status,
    t.full_name AS teacher_name,
    COALESCE(a.reason, 'On Time')
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
    AND cs.session_date BETWEEN ce.enrollment_date AND CURRENT_DATE
    AND s.student_id = ?
ORDER BY cs.session_date DESC, cs.period ASC
LIMIT ?
OFFSET
    ?;

-- Optional dynamic filters
-- AND c.course_id = ?
-- AND COALESCE(a.status, 'Present') = ?