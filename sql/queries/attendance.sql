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
                    FROM
                        class_session cs2
                        JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
                    WHERE
                        ce2.student_id = s.student_id
                ) AS sub
        )
    ) AND COALESCE(?, CURDATE())
    AND (c.course_name LIKE ?)
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
    AND cs.session_date BETWEEN COALESCE(
        ?,
        SELECT MIN(cs2.session_date)
        FROM
            class_session cs2
            JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
        WHERE
            ce2.student_id = s.student_id
    ) AND COALESCE(?, CURDATE())
    AND cs.is_cancelled = FALSE
    AND ce.status = 'ENROLLED';

--Finding the total absent attendance session
SELECT COUNT(a.attendance_id) AS total_absent
FROM
    attendance a
    JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
    JOIN class_session cs ON a.session_id = cs.session_id
WHERE
    ce.student_id = s.student_id
    AND cs.session_date BETWEEN COALESCE(
        ?,
        SELECT MIN(cs2.session_date)
        FROM
            class_session cs2
            JOIN course_enrollment ce2 ON cs2.offering_id = ce2.offering_id
        WHERE
            ce2.student_id = s.student_id
    ) AND COALESCE(?, CURDATE());

--Daily Log
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
ORDER BY cs.period ASC;
--Append dynamically when searched by searchBar
AND ( s.full_name LIKE ? OR c.course_name LIKE OR s.roll_no LIKE ? )

--Append dynamically when attendance status is checkbox
AND COALESCE(a.status, 'Present') = ?

--Append dyamically when period is filtered
AND cs.period = ?

--IF attendance status is edited,
--MARK ALL PRESENT QUERY
DELETE FROM attendance a
JOIN class_session cs ON a.session_id = cs.session_id
JOIN course_enrollment ce ON a.enrollment_id = ce.enrollment_id
JOIN course_offering co ON ce.offering_id = co.offering_id
JOIN course c ON co.course_id = c.course_id
WHERE
    cs.session_date = ?
    AND c.course_name = ?
    AND cs.period = ?
    --If Attendance Status is changed into "Present",
DELETE FROM attendance
WHERE
    session_id = ?
    AND enrollment_id = ?;

--If attendance status is changed into "Present"
DELETE FROM attendance WHERE session_id = ? AND enrollment_id = ?

--If attendance status is changed into "Absent" or "Excused"
INSERT INTO
    attendance (
        session_id,
        enrollment_id,
        status,
        reason
    )
VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    reason = VALUES(reason);

--Individual Student View
--After searching students from searchbar
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
            ci.email,
            ci.address
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
    email,
    address
FROM student_matrix;
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
ORDER BY cs.session_date DESC, cs.period ASC
LIMIT ?
OFFSET
    ?;

-- Optional dynamic filters
-- AND c.course_name = ?
-- AND COALESCE(a.status, 'Present') = ?