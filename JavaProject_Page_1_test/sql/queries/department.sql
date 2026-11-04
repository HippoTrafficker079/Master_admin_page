--Finding Department_ID by its code or name query

SELECT dept_id
FROM department
WHERE
    dept_code = ?
    OR dept_name = ?
LIMIT 1;