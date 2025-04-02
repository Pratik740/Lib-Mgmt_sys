package services;

import java.sql.*;

import Schedulers.FineService;
import db.DatabaseManager;
import models.Admin;

public class AdminService extends LibrarianService{

    public static Admin adminLogin(String email, String password) {
        String adminLoginQuery = "SELECT * FROM staff WHERE email = ? AND password_hash = ?";
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement loginStmt = conn.prepareStatement(adminLoginQuery);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

            FineService.populateFineTable();

            loginStmt.setString(1, email);
            loginStmt.setString(2, password);
            ResultSet rs = loginStmt.executeQuery();

            if (rs.next()) {

                auditStmt.setString(1, rs.getString("staff_id"));
                auditStmt.setString(2, "ADMIN: " + rs.getString("name") + " logged in");
                auditStmt.executeUpdate();

                return new Admin(rs.getInt("id"),
                                 rs.getString("name"),
                                 rs.getString("email"),
                                 rs.getString("password_hash"));
            }
            else {
                System.out.println("Admin login Failed!!!");
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Error occurred during admin login: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void viewStaff(Admin admin) {
        String staffQuery = "SELECT * FROM staff where id != ?";
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement staffStmt = conn.prepareStatement(staffQuery);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

            staffStmt.setInt(1, admin.getId());
            ResultSet rs = staffStmt.executeQuery();

            // Print table header
            System.out.println("+----+----------------+-------------------------+-------------------+---------------+----------------+-----------------+");
            System.out.println("| ID | Name           | Email                   | Role              | Shift Start   | Shift End      |");
            System.out.println("+----+----------------+-------------------------+-------------------+---------------+----------------+-----------------+");

            // Iterate over the result set and print each row
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                String role = rs.getString("role");
                Time shiftStart = rs.getTime("shift_start");
                Time shiftEnd = rs.getTime("shift_end");

                // Format shift times and handle null values
                String shiftStartFormatted = (shiftStart == null) ? "N/A" : shiftStart.toString();
                String shiftEndFormatted = (shiftEnd == null) ? "N/A" : shiftEnd.toString();

                // Print each staff member's details
                System.out.printf("| %-2d | %-14s | %-23s | %-17s | %-13s | %-14s |\n",
                        id, name, email, role, shiftStartFormatted, shiftEndFormatted);
            }

            System.out.println("+----+----------------+-------------------------+-------------------+---------------+----------------+-----------------+");

            // Log the action in the audit log
            auditStmt.setInt(1, admin.getId());
            auditStmt.setString(2, "Admin " + admin.getName() + " viewed all staff details.");
            auditStmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error occurred when admin went to view all staff details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void removeStaff(Admin admin, int staffId) {
        String staffQuery = "DELETE FROM staff WHERE id = ?";
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement staffStmt = conn.prepareStatement(staffQuery);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

            staffStmt.setInt(1, staffId);
            staffStmt.executeUpdate();

            System.out.println("Removed staff: " + staffId);

            auditStmt.setInt(1, admin.getId());
            auditStmt.setString(2, "Admin " + admin.getName() + " removed " + staffId + " no. staff details.");
            auditStmt.executeUpdate();

        } catch ( SQLException e ) {
            System.err.println("Error occurred when admin went to remove staff: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void viewAuditLog(Admin admin) {
        String actionView = "SELECT * FROM audit_log";
        String auditLog = "INSERT INTO audit_log(staff_id, action) VALUES(?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement actionStmt = conn.prepareStatement(actionView);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

            // Execute the query to retrieve audit log entries
            ResultSet rs = actionStmt.executeQuery();

            // Print table header
            System.out.println("+----+------------+----------------------------+-----------------------------+---------------------+");
            System.out.println("| ID | Staff ID   | Action                     | Action Description          | Timestamp           |");
            System.out.println("+----+------------+----------------------------+-----------------------------+---------------------+");

            // Iterate through the ResultSet and print each audit log entry
            while (rs.next()) {
                int id = rs.getInt("id");
                int staffId = rs.getInt("staff_id");
                String action = rs.getString("action");
                String actionDescription = rs.getString("action");
                Timestamp timestamp = rs.getTimestamp("timestamp");

                // Print each row of the audit log
                System.out.printf("| %-2d | %-10d | %-26s | %-27s | %-19s |\n",
                        id, staffId, action, actionDescription, timestamp);
            }

            System.out.println("+----+------------+----------------------------+-----------------------------+---------------------+");

            // Log the action in the audit log (logging the admin's action)
            auditStmt.setInt(1, admin.getId());
            auditStmt.setString(2, "Admin " + admin.getName() + " viewed the audit log.");
            auditStmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error occurred when admin went to view audit_log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void viewUserDetails(Admin admin) {
        String userViewQuery = "SELECT * FROM users";
        String auditLog = "INSERT INTO audit_log(staff_id, action) VALUES(?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement auditStmt = conn.prepareStatement(auditLog);
             PreparedStatement userStmt = conn.prepareStatement(userViewQuery)) {

            ResultSet rs = userStmt.executeQuery();

            // Print header
            System.out.printf("%-5s | %-20s | %-30s | %-64s | %-15s%n",
                    "ID", "Name", "Email", "Password Hash", "Date of Joining");
            System.out.println("=".repeat(140));

            // Print each row
            while (rs.next()) {
                System.out.printf("%-5d | %-20s | %-30s | %-64s | %-15s%n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getDate("date_of_joining").toString());
            }

            // Log the action
            auditStmt.setInt(1, admin.getId());
            auditStmt.setString(2, admin.getRole() + " " + admin.getName() + " viewed the user details.");
            auditStmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error occurred when admin went to view user details: " + e.getMessage());
            e.printStackTrace();
        }
    }

}