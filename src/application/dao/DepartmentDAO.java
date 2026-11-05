package application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DepartmentDAO {
    public int getDeptIdByCodeOrName(Connection conn, String majorInput) throws SQLException {
        String sql = """
                SELECT dept_id
                FROM department
                WHERE dept_code = ? OR dept_name = ?
                LIMIT 1;
                """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, majorInput);
            pstmt.setString(2, majorInput);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                } else {
                    throw new SQLException("Department not found for input: " + majorInput);
                }
            }
        }
    }
}

