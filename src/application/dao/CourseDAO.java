package application.dao;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {
    public List<String> getAllActiveCourseName() {
        List<String> courseList = new ArrayList<>();
        String sql = """
                        SELECT DISTINCT
                    course_name
                FROM course
                WHERE
                    is_active = TRUE
                ORDER BY course_name ASC;
                        """;
        try (Connection conn = DatabaseConnector.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                courseList.add(rs.getString("course_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseList;
    }
}
