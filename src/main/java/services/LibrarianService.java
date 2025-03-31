package services;

import db.DatabaseManager;
import models.Librarian;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LibrarianService {

    public Librarian login(String email, String hash_pass) {
        String query = """
                SELECT id, name, shift_start, shift_end
                FROM staff WHERE email = ? AND password_hash = ? AND role = 'Librarian'
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, hash_pass);

            ResultSet rs = stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}