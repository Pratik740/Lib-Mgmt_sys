package services;

import db.DatabaseManager;
import java.sql.*;

public class PersonService {

    // View all available genres
    public void viewGenres() {
        String query = "SELECT name FROM genres";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("Available Genres:");
            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // View available books by genre
    public void viewBooksByGenre(String genreName) {
        String query = """
            SELECT DISTINCT b.title
            FROM books b
            JOIN genres g ON b.genre_id = g.id
            JOIN book_copies bc ON b.id = bc.book_id
            WHERE g.name = ? AND bc.available = TRUE
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, genreName);
            ResultSet rs = stmt.executeQuery();

            System.out.printf("Available Books in Genre '%s':%n", genreName);
            while (rs.next()) {
                System.out.println(rs.getString("title"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
