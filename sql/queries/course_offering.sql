--Course Offering

--CREATING COURSE OFFERING
--Insert course offering
INSERT INTO
    course_offering (
        teacher_id,
        course_id,
        `year`,
        total_sessions,
        term
    )
VALUES (?, ?, ?, ?, ?);

--Insert Session Query
INSERT INTO
    class_session (offering_id, session_date)
VALUES (?, ?);

--Update Total Session
UPDATE course_offering SET total_sessions = ? WHERE offering_id = ?;

--All Active Course Available RIGHT NOW
SELECT DISTINCT
    course_name
FROM course
WHERE
    is_active = TRUE
ORDER BY course_name ASC;