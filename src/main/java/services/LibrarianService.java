package services;

import db.DatabaseManager;
import models.Librarian;

import java.sql.*;

public class LibrarianService {
    public static Librarian loginLibrarian(String email, String password) {
        String login = "select * from staff where email = ? and password_hash = ?";
        String auditLogin = "insert into audit_log(staff_id, action) values(?,?) ";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(login);
             PreparedStatement auditStmt = conn.prepareStatement(auditLogin)) {

            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            FineService.populateFineTable();

            if (rs.next()) {

                auditStmt.setString(1, rs.getString("staff_id"));
                auditStmt.setString(2, "Librarian " + rs.getString("name") + " logged in");

                return new Librarian(rs.getInt("id"),
                                     rs.getString("name"),
                                     rs.getString("email"),
                                     rs.getString("password_hash"),
                                     rs.getTime("shift_start").toLocalTime(),
                                     rs.getTime("shift_end").toLocalTime());
            }
            else {
                System.out.println("Login Failed, user not found!");
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Librarian login failed. Error: " + e.getMessage());
            return null;
        }
    }

    public static void viewPendingFineDetails (Librarian librarian) {
        String viewPending = "select users.id, users.name, users.email, users.date_of_joining, fines.transaction_id, fines.amount from users join fines on users.id = fines.user_id where users.id = ?";
        String auditLog = "";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement viewStmt = conn.prepareStatement(viewPending);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

        } catch (SQLException e) {
            System.err.println("Error encountered while viewing pending users: " + e.getMessage());
            e.printStackTrace();
        }
    }
}