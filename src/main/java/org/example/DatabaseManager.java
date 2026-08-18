package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/memory_db";
    private static final String USER = "rag_user";
    private static final String PASSWORD = "rag_password";

    public void saveMemory(String summary, List<Double> embedding) {
        String sql = "INSERT INTO memories (summary_text, embedding) VALUES (?, ?::vector);";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, summary);
            pstmt.setString(2, embedding.toString());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Database error on write: " + e.getMessage());
        }
    }
    public List<String> getRelevantSummaries(List<Double> embedding) {
        List<String> results = new ArrayList<>();

        String sql = "SELECT summary_text FROM memories ORDER BY embedding <-> ?::vector LIMIT 3;";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, embedding.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(rs.getString("summary_text"));
            }

        } catch (SQLException e) {
            System.err.println("Database error on read: " + e.getMessage());
        }
        return results;
    }
}