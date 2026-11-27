USE sims_db;

-- Disable foreign key checks temporarily for clean insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Clear existing data if re-running
DELETE FROM student_grade;

DELETE FROM assessment;

DELETE FROM assessment_weight;

DELETE FROM attendance;

DELETE FROM course_enrollment;

DELETE FROM class_session;

DELETE FROM course_offering;

DELETE FROM student_education_background;

DELETE FROM student_guardian;

DELETE FROM student_contact_information;

DELETE FROM student;

DELETE FROM teacher_academic_background;

DELETE FROM teacher_contact_information;

DELETE FROM teacher;

DELETE FROM prerequisite_course;

DELETE FROM course;

DELETE FROM department;

DELETE FROM user_account;

DELETE FROM master_admin;

-- ==========================================
-- 1. MASTER ADMIN
-- ==========================================
INSERT INTO
    master_admin (
        admin_id,
        email,
        password_hash,
        status
    )
VALUES (
        1,
        'admin.master@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        TRUE
    );

-- ==========================================
-- 2. USER ACCOUNTS (Teachers & Students)
-- ==========================================
INSERT INTO
    user_account (
        user_id,
        username,
        email,
        password_hash,
        user_role,
        status
    )
VALUES
    -- Teachers User Accounts (Username = Full Name)
    (
        1,
        'Sadayo Kawakami',
        't001@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'TEACHER',
        TRUE
    ),
    (
        2,
        'Sojiro Sakura',
        't002@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'TEACHER',
        TRUE
    ),
    (
        3,
        'Tae Takemi',
        't003@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'TEACHER',
        TRUE
    ),
    (
        4,
        'Ichiko Ohya',
        't004@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'TEACHER',
        TRUE
    ),

    -- Students User Accounts (Username = Full Name, Email = formatted roll_no placeholder)
    (
        5,
        'Ren Amamiya',
        'iiiceit1@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'STUDENT',
        TRUE
    ),
    (
        6,
        'Ryuji Sakamoto',
        'iiiceit2@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'STUDENT',
        TRUE
    ),
    (
        7,
        'Ann Takamaki',
        'iiiceit3@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'STUDENT',
        TRUE
    ),
    (
        8,
        'Yusuke Kitagawa',
        'iiimech1@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'STUDENT',
        TRUE
    ),
    (
        9,
        'Makoto Niijima',
        'iiimech2@ytu.edu.mm',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J08H9xY4e',
        'STUDENT',
        TRUE
    );

-- ==========================================
-- 3. DEPARTMENTS
-- ==========================================
INSERT INTO
    department (
        dept_id,
        dept_head_id,
        dept_code,
        dept_name,
        established_date,
        dept_location
    )
VALUES (
        1,
        NULL,
        'CEIT',
        'Computer Engineering and Information Technology',
        '1998-09-01',
        'Main Campus - Building A'
    ),
    (
        2,
        NULL,
        'Mech',
        'Mechanical Engineering',
        '1955-06-15',
        'Main Campus - Building B'
    ),
    (
        3,
        NULL,
        'Arch',
        'Architecture',
        '1960-06-15',
        'Main Campus - Building C'
    ),
    (
        4,
        NULL,
        'EC',
        'Electronic Engineering',
        '1980-01-10',
        'Main Campus - Building D'
    ),
    (
        5,
        NULL,
        'EP',
        'Electrical Power Engineering',
        '1955-06-15',
        'Main Campus - Building E'
    ),
    (
        6,
        NULL,
        'ChE',
        'Chemical Engineering',
        '1975-04-20',
        'Main Campus - Building F'
    ),
    (
        7,
        NULL,
        'PE',
        'Petroleum Engineering',
        '2012-08-10',
        'Main Campus - Building G'
    ),
    (
        8,
        NULL,
        'C',
        'Civil Engineering',
        '1915-01-01',
        'Main Campus - Building H'
    ),
    (
        9,
        NULL,
        'FE',
        'Food Engineering',
        '2015-09-01',
        'Main Campus - Building I'
    ),
    (
        10,
        NULL,
        'McE',
        'Mechatronic Engineering',
        '2008-03-12',
        'Main Campus - Building J'
    ),
    (
        11,
        NULL,
        'Mn',
        'Mining Engineering',
        '1955-06-15',
        'Main Campus - Building K'
    ),
    (
        12,
        NULL,
        'Tele',
        'Telecommunication Engineering',
        '2001-11-05',
        'Main Campus - Building L'
    ),
    (
        13,
        NULL,
        'Tex',
        'Textile Engineering',
        '1995-07-20',
        'Main Campus - Building M'
    ),
    (
        14,
        NULL,
        'Met',
        'Metallurgy Engineering and Material Science',
        '1990-05-14',
        'Main Campus - Building N'
    ),
    (
        15,
        NULL,
        'Math',
        'Department of Engineering Mathematics',
        '1955-06-15',
        'Science Building - Floor 1'
    ),
    (
        16,
        NULL,
        'Eng',
        'Department of Engineering English',
        '1955-06-15',
        'Science Building - Floor 2'
    );

-- ==========================================
-- 4. TEACHERS & RELATED TABLES
-- ==========================================
INSERT INTO
    teacher (
        teacher_id,
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
VALUES (
        1,
        1,
        'Sadayo Kawakami',
        1,
        'Professor & Head',
        '1984-01-12',
        'Female',
        'Japanese',
        '12/MaGaTa(N)123456',
        NULL
    ),
    (
        2,
        2,
        'Sojiro Sakura',
        1,
        'Lecturer',
        '1965-03-25',
        'Male',
        'Japanese',
        '12/DaGaNa(N)654321',
        NULL
    ),
    (
        3,
        3,
        'Tae Takemi',
        2,
        'Professor',
        '1983-04-18',
        'Female',
        'Japanese',
        '9/MaHaMa(N)112233',
        NULL
    ),
    (
        4,
        4,
        'Ichiko Ohya',
        15,
        'Tutor',
        '1988-09-09',
        'Female',
        'Japanese',
        '5/LaGaNa(N)998877',
        NULL
    );

-- Update Department Heads
UPDATE department SET dept_head_id = 1 WHERE dept_id = 1;

UPDATE department SET dept_head_id = 3 WHERE dept_id = 2;

-- Teacher Contact Info
INSERT INTO
    teacher_contact_information (
        teacher_id,
        primary_phone_number,
        email_address,
        current_address,
        permanent_address
    )
VALUES (
        1,
        '09-971234567',
        't001@ytu.edu.mm',
        'Yongen, Tokyo',
        'Tokyo'
    ),
    (
        2,
        '09-421987654',
        't002@ytu.edu.mm',
        'Leblanc, Tokyo',
        'Tokyo'
    ),
    (
        3,
        '09-798887766',
        't003@ytu.edu.mm',
        'Takemi Clinic, Tokyo',
        'Tokyo'
    ),
    (
        4,
        '09-250112233',
        't004@ytu.edu.mm',
        'Shinjuku, Tokyo',
        'Tokyo'
    );

-- Teacher Academic Background
INSERT INTO
    teacher_academic_background (
        teacher_id,
        highest_degree,
        university,
        year_completed,
        specialization,
        emergency_contact
    )
VALUES (
        1,
        'Ph.D. in Computer Science',
        'University of Tokyo',
        2010,
        'Distributed Systems',
        'Sojiro Sakura - 09-971111111'
    ),
    (
        2,
        'M.E. in Information Technology',
        'Kyoto University',
        1995,
        'Software Engineering',
        'Futaba Sakura - 09-422222222'
    ),
    (
        3,
        'Ph.D. in Mechanical Engineering',
        'Tokyo Institute of Technology',
        2008,
        'Thermal Dynamics',
        'Tae Takemi Clinic - 09-793333333'
    ),
    (
        4,
        'B.E. (Hons) in Engineering Mathematics',
        'Waseda University',
        2015,
        'Applied Mathematics',
        'Mishima - 09-25444444'
    );

-- ==========================================
-- 5. COURSES & PREREQUISITES
-- ==========================================
INSERT INTO
    course (
        course_id,
        course_code,
        course_name,
        dept_id,
        course_type,
        credit,
        is_active
    )
VALUES (
        1,
        'CEIT-3101',
        'Advanced Database Systems',
        1,
        'Bachelor',
        3.0,
        TRUE
    ),
    (
        2,
        'CEIT-3102',
        'Object-Oriented Programming with Java',
        1,
        'Bachelor',
        3.0,
        TRUE
    ),
    (
        3,
        'ME-3101',
        'Advanced Thermodynamics',
        2,
        'Bachelor',
        3.0,
        TRUE
    );

INSERT INTO
    prerequisite_course (
        course_id,
        prerequisite_course_id
    )
VALUES (1, 2);

-- ==========================================
-- 6. STUDENTS & RELATED TABLES
-- Format: <roman_year>-<dept_code>-<no>
-- ==========================================
INSERT INTO
    student (
        student_id,
        user_id,
        roll_no,
        full_name,
        dept_id,
        current_year,
        total_credit,
        gpa,
        date_of_birth,
        gender,
        nationality,
        nrc,
        profile_image_url
    )
VALUES (
        1,
        5,
        'III-CEIT-1',
        'Ren Amamiya',
        1,
        'Third year',
        15.0,
        3.85,
        '2006-05-10',
        'Male',
        'Japanese',
        '12/BaBaNa(N)111111',
        NULL
    ),
    (
        2,
        6,
        'III-CEIT-2',
        'Ryuji Sakamoto',
        1,
        'Third year',
        15.0,
        2.70,
        '2006-07-03',
        'Male',
        'Japanese',
        '12/YaKaNa(N)222222',
        NULL
    ),
    (
        3,
        7,
        'III-CEIT-3',
        'Ann Takamaki',
        1,
        'Third year',
        15.0,
        3.20,
        '2006-11-12',
        'Female',
        'Japanese',
        '12/DaGaNa(N)333333',
        NULL
    ),
    (
        4,
        8,
        'III-Mech-1',
        'Yusuke Kitagawa',
        2,
        'Third year',
        15.0,
        3.90,
        '2005-02-28',
        'Male',
        'Japanese',
        '12/SaKaNa(N)444444',
        NULL
    ),
    (
        5,
        9,
        'III-Mech-2',
        'Makoto Niijima',
        2,
        'Third year',
        15.0,
        4.00,
        '2005-08-23',
        'Female',
        'Japanese',
        '12/MaGaTa(N)555555',
        NULL
    );

-- Student Contact Info
INSERT INTO
    student_contact_information (
        student_id,
        primary_phone_number,
        secondary_phone_number,
        email_address,
        current_address,
        permanent_address
    )
VALUES (
        1,
        '09-765432101',
        NULL,
        'iiiceit1@ytu.edu.mm',
        'Yongen-Jaya, Tokyo',
        'Tokyo'
    ),
    (
        2,
        '09-765432102',
        NULL,
        'iiiceit2@ytu.edu.mm',
        'Shibuya, Tokyo',
        'Tokyo'
    ),
    (
        3,
        '09-765432103',
        NULL,
        'iiiceit3@ytu.edu.mm',
        'Aoyama-Itchome, Tokyo',
        'Tokyo'
    ),
    (
        4,
        '09-765432104',
        NULL,
        'iiimech1@ytu.edu.mm',
        'Ueno, Tokyo',
        'Tokyo'
    ),
    (
        5,
        '09-765432105',
        NULL,
        'iiimech2@ytu.edu.mm',
        'Kanda, Tokyo',
        'Tokyo'
    );

-- Student Guardian Info
INSERT INTO
    student_guardian (
        student_id,
        father_full_name,
        father_occupation,
        father_phone_number,
        father_email_address,
        mother_full_name,
        mother_occupation,
        mother_phone_number,
        mother_email_address
    )
VALUES (
        1,
        'Guardian Uncle',
        'Business',
        '09-400000001',
        'uncle@gmail.com',
        'Guardian Aunt',
        'Housewife',
        '09-400000002',
        'aunt@gmail.com'
    ),
    (
        2,
        'Tatsumi Sakamoto',
        'Worker',
        '09-400000003',
        'tatsumi@gmail.com',
        'Mrs. Sakamoto',
        'Employee',
        '09-400000004',
        'mrs.sakamoto@gmail.com'
    ),
    (
        3,
        'Mr. Takamaki',
        'Designer',
        '09-400000005',
        'takamaki.sr@gmail.com',
        'Mrs. Takamaki',
        'Model',
        '09-400000006',
        'takamaki.m@gmail.com'
    ),
    (
        4,
        'Madarame (Foster)',
        'Artist',
        '09-400000007',
        'madarame@gmail.com',
        'N/A',
        'N/A',
        '09-400000008',
        'na@gmail.com'
    ),
    (
        5,
        'Director Niijima',
        'Prosecutor',
        '09-400000009',
        'niijima@gmail.com',
        'Mrs. Niijima',
        'Teacher',
        '09-400000010',
        'mrs.niijima@gmail.com'
    );

-- Student Education Background
INSERT INTO
    student_education_background (
        student_id,
        previous_school,
        year_completed,
        additional_info
    )
VALUES (
        1,
        'Aoharu High School',
        2023,
        'Transferred from countryside'
    ),
    (
        2,
        'Shujin Academy',
        2023,
        'Track star team member'
    ),
    (
        3,
        'Shujin Academy',
        2023,
        'Student model'
    ),
    (
        4,
        'Kosei High School',
        2022,
        'Art scholarship'
    ),
    (
        5,
        'Shujin Academy',
        2022,
        'Student Council President'
    );

-- ==========================================
-- 7. COURSE OFFERINGS & ASSESSMENT WEIGHTS
-- ==========================================
INSERT INTO
    course_offering (
        offering_id,
        teacher_id,
        course_id,
        create_dept_id,
        `year`,
        academic_year,
        total_sessions,
        term,
        max_student,
        card_symbol,
        badge_color
    )
VALUES (
        1,
        1,
        1,
        1,
        'Second Year',
        2026,
        6,
        'First Semester',
        60,
        '💾',
        '#2563eb'
    ), -- Advanced DB (Taught by Sadayo Kawakami)
    (
        2,
        2,
        2,
        1,
        'Third Year',
        2026,
        6,
        'First Semester',
        60,
        '☕',
        '#7c3aed'
    ), -- Java OOP (Taught by Sojiro Sakura)
    (
        3,
        3,
        3,
        1,
        'Fourth Year',
        2026,
        3,
        'First Semester',
        50,
        '🔥',
        '#ea580c'
    ); -- Thermodynamics (Taught by Tae Takemi)

INSERT INTO
    assessment_weight (
        offering_id,
        tutorial_weight,
        assignment_weight,
        lab_test_weight,
        practical_weight,
        final_exam_weight
    )
VALUES (
        1,
        15.00,
        15.00,
        10.00,
        0.00,
        60.00
    ),
    (
        2,
        10.00,
        10.00,
        15.00,
        15.00,
        50.00
    ),
    (
        3,
        15.00,
        15.00,
        0.00,
        0.00,
        70.00
    );

-- ==========================================
-- 8. CLASS SESSIONS (Updated with CURDATE() for Today's Attendance)
-- ==========================================
INSERT INTO
    class_session (
        session_id,
        offering_id,
        session_date,
        period,
        is_cancelled
    )
VALUES
    -- Advanced DB Sessions (Offering 1)
    (1, 1, '2026-06-01', 1, FALSE),
    (2, 1, '2026-06-03', 1, FALSE),
    (3, 1, '2026-06-08', 1, FALSE),
    (4, 1, '2026-06-10', 1, FALSE),
    (5, 1, '2026-06-15', 1, FALSE),
    -- Java OOP Sessions (Offering 2)
    (6, 2, '2026-06-02', 2, FALSE),
    (7, 2, '2026-06-04', 2, FALSE),
    (8, 2, '2026-06-09', 2, FALSE),
    (9, 2, '2026-06-11', 2, FALSE),
    (10, 2, '2026-06-16', 2, FALSE),
    -- Thermodynamics Sessions (Offering 3)
    (11, 3, '2026-06-01', 3, FALSE),
    (12, 3, '2026-06-05', 3, FALSE),

    -- TODAY'S SESSIONS (CURDATE()) for Testing Daily Logs & Today's Attendance
    (13, 1, CURDATE(), 1, FALSE), -- Advanced DB (Period 1)
    (14, 2, CURDATE(), 2, FALSE), -- Java OOP (Period 2)
    (15, 3, CURDATE(), 3, FALSE); -- Thermodynamics (Period 3)

-- ==========================================
-- 9. COURSE ENROLLMENTS
-- ==========================================
INSERT INTO
    course_enrollment (
        enrollment_id,
        offering_id,
        student_id,
        enrollment_date,
        status
    )
VALUES (
        1,
        1,
        1,
        '2026-05-20',
        'ENROLLED'
    ), -- Ren in Advanced DB
    (
        2,
        2,
        1,
        '2026-05-20',
        'ENROLLED'
    ), -- Ren in Java OOP
    (
        3,
        1,
        2,
        '2026-05-20',
        'ENROLLED'
    ), -- Ryuji in Advanced DB
    (
        4,
        2,
        2,
        '2026-05-20',
        'ENROLLED'
    ), -- Ryuji in Java OOP
    (
        5,
        1,
        3,
        '2026-05-20',
        'ENROLLED'
    ), -- Ann in Advanced DB (Low attendance test case)
    (
        6,
        3,
        4,
        '2026-05-20',
        'ENROLLED'
    ), -- Yusuke in Thermodynamics
    (
        7,
        3,
        5,
        '2026-05-20',
        'ENROLLED'
    ); -- Makoto in Thermodynamics

-- ==========================================
-- 10. ATTENDANCE (Expanded Sparse Exception Model)
-- ==========================================
INSERT INTO
    attendance (
        attendance_id,
        session_id,
        enrollment_id,
        status,
        reason
    )
VALUES
    -- Ren's absence (Enrollment 1 - Advanced DB)
    (
        1,
        2,
        1,
        'EXCUSED',
        'Metaverse Operation'
    ),

    -- Ryuji's absence (Enrollment 3 - Advanced DB)
    (
        2,
        1,
        3,
        'UNEXCUSED',
        'Overslept / Track Practice'
    ),

    -- Ann's multiple absences to test < 75% filter (Enrollment 5 - Advanced DB)
    (
        3,
        1,
        5,
        'UNEXCUSED',
        'Modeling Photo Shoot'
    ),
    (
        4,
        3,
        5,
        'UNEXCUSED',
        'Skipped class'
    ),
    (
        5,
        4,
        5,
        'EXCUSED',
        'Agency Work'
    ),

    -- Ren's absence in Java OOP (Enrollment 2)
    (
        6,
        7,
        2,
        'EXCUSED',
        'Mementos Mission'
    ),

    -- TODAY'S TEST ATTENDANCE RECORDS (Matches CURDATE() sessions 13, 14, 15)
    -- Session 13 (Advanced DB): Ryuji (Enrollment 3) is absent (Unexcused)
    (
        7,
        13,
        3,
        'UNEXCUSED',
        'Overslept'
    ),

    -- Session 14 (Java OOP): Ren (Enrollment 2) is excused with a medical reason
    (
        8,
        14,
        2,
        'EXCUSED',
        'Medical Checkup - Fever'
    ),

    -- Session 15 (Thermodynamics): Makoto (Enrollment 7) is absent (Unexcused)
    (
        9,
        15,
        7,
        'UNEXCUSED',
        'Student Council Meeting'
    );

-- ==========================================
-- 11. ASSESSMENTS & STUDENT GRADES
-- ==========================================
INSERT INTO
    assessment (
        assessment_id,
        offering_id,
        title,
        assessment_type,
        max_marks
    )
VALUES (
        1,
        1,
        'Database Lab Test 1',
        'LAB_TEST',
        20.00
    ),
    (
        2,
        1,
        'Final Examination',
        'FINAL',
        100.00
    ),
    (
        3,
        2,
        'Java Midterm Project',
        'ASSIGNMENT',
        50.00
    );

INSERT INTO
    student_grade (
        grade_id,
        enrollment_id,
        assessment_id,
        marks_obtained
    )
VALUES
    (1, 1, 1, 19.00), -- Ren lab score in Advanced DB
    (2, 3, 1, 12.00), -- Ryuji lab score in Advanced DB
    (3, 5, 1, 14.50), -- Ann lab score in Advanced DB
    (4, 2, 3, 48.00); -- Ren project score in Java OOP

-- Restore foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

UPDATE course_offering
SET `year` = 'Third Year'
WHERE offering_id IN (1, 3);