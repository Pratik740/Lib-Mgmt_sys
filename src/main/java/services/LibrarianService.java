package services;

import Schedulers.FineService;
import db.DatabaseManager;
import models.Librarian;
import models.Staff;

import java.sql.*;

public class LibrarianService {
    public static Librarian loginLibrarian(String email, String password) {
        String login = "select * from staff where email = ? and password_hash = ?";
        String auditLogin = "insert into audit_log(staff_id, action) values(?,?)";

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
                auditStmt.executeUpdate();

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

    public static void viewPendingFineDetails (Staff staff) {
        String viewPending = """                
                             SELECT
                                 users.id AS user_id,
                                 users.name,
                                 users.email,
                                 books.title,
                                 transactions.issue_date,
                                 transactions.due_date,
                                 fines.amount
                             FROM users
                                      JOIN fines ON users.id = fines.user_id
                                      JOIN transactions ON fines.transaction_id = transactions.id
                                      JOIN book_copies ON transactions.book_copy_id = book_copies.id
                                      JOIN books ON book_copies.book_id = books.id
                             WHERE fines.status = 'Pending';
                             """;
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement viewStmt = conn.prepareStatement(viewPending);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

             viewStmt.executeQuery();
             ResultSet rs = viewStmt.executeQuery();

            System.out.printf("%-5s | %-20s | %-25s | %-30s | %-12s | %-12s | %-7s%n",
                    "ID", "Name", "Email", "Book Title", "Issue Date", "Due Date", "Fine");
            System.out.println("-------------------------------------------------------------------------------------------------------------");

            // Print table rows
            while (rs.next()) {
                System.out.printf("%-5d | %-20s | %-25s | %-30s | %-12s | %-12s | $%-6.2f%n",
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("title"),
                        rs.getDate("issue_date"),
                        rs.getDate("due_date"),
                        rs.getDouble("amount")
                );
            }

            auditStmt.setInt(1, staff.getId());
            auditStmt.setString(2, staff.getRole() + " " + staff.getName() + " viewed pending fine details of all users.");
        } catch (SQLException e) {
            System.err.println("Error encountered while viewing pending users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void viewAvailableBookCopies (Staff staff) {
        String availQuery = """
                            SELECT books.id, books.title,books.author, COUNT(book_copies.id) AS total_copies
                            FROM books
                            JOIN book_copies ON books.id = book_copies.book_id
                            GROUP BY books.id;
                            """;


        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement availStmt = conn.prepareStatement(availQuery);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

            availStmt.executeQuery();
            ResultSet rs = availStmt.executeQuery();

            System.out.println("+------+--------------------------------+----------------------+--------+");
            System.out.printf("| %-4s | %-30s | %-20s | %-6s |\n", "ID", "Title", "Author", "Copies");
            System.out.println("+------+--------------------------------+----------------------+--------+");

            while (rs.next()) {
                int bookId = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                int totalCopies = rs.getInt("total_copies");

                System.out.printf("| %-4d | %-30s | %-20s | %-6d |\n", bookId, title, author, totalCopies);
            }

            System.out.println("+------+--------------------------------+----------------------+--------+");

            auditStmt.setInt(1, staff.getId());
            auditStmt.setString(2, staff.getRole() + " " + staff.getName() + " viewed all available books and their copies.");
        } catch (SQLException e) {
            System.err.println("Error encountered while viewing available book copies: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void provideApprovals(Staff staff) {
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";
        String pendingRequests = """
                    UPDATE book_requests
                    SET status = 'Approved'
                    WHERE status = 'pending';
                   """;
        try(Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(pendingRequests);
        PreparedStatement stmt1 = conn.prepareStatement(auditLog)) {
            stmt.executeUpdate();
            System.out.println("All pending book requests have been approved.");
            stmt1.setInt(1, staff.getId());
            stmt1.setString(2, staff.getRole() + " " + staff.getName() + " approved all pending book requests.");
            stmt1.executeUpdate();
        }
        catch (SQLException e) {
            System.err.println("Error encountered while providing approvals: " + e.getMessage());
        }
    }



    public static void guestDetails(Staff staff) {
        String allGuests = "select * from guests";

        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";

        try(Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(allGuests);
        PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {
            ResultSet rs = stmt.executeQuery();
            System.out.println("+----+----------------+----------------+-----------------+---------------------+");
            System.out.println("| ID | Name           | Contact        | Visit Time      |");
            System.out.println("+----+----------------+----------------+-----------------+---------------------+");

            while (rs.next()) {
                System.out.printf("| %-2d | %-14s | %-14s | %-15s | %-19s |\n",
                        rs.getInt("id"),           // ID
                        rs.getString("name"),       // Name
                        rs.getString("contact"),    // Contact
                        rs.getTimestamp("visit_time")); // Visit Time (formatted timestamp)
            }

            System.out.println("+----+----------------+----------------+-----------------+---------------------+");

            auditStmt.setInt(1, staff.getId());
            auditStmt.setString(2, staff.getRole() + " " + staff.getName() + " viewed all guests' details.");
            auditStmt.executeUpdate();
        }
        catch (SQLException e) {
            System.err.println("Error encountered while guest details: " + e.getMessage());
        }
    }

    public static void guestBookDetails (Staff staff) {
        String guestBookDetailsQuery = """
                                       SELECT
                                           guests.id,
                                           guests.name,
                                           books.title,
                                           DATE_FORMAT(guest_book_usage.start_time, '%Y-%m-%d %H:%i:%s') as start_time,
                                           DATE_FORMAT(guest_book_usage.end_time, '%Y-%m-%d %H:%i:%s') as end_time,
                                           TIMESTAMPDIFF(MINUTE, guest_book_usage.start_time, IFNULL(guest_book_usage.end_time, NOW())) AS amount
                                       FROM guests
                                       JOIN guest_book_usage ON guests.id = guest_book_usage.guest_id
                                       JOIN books ON guest_book_usage.book_id = books.id;
                                       """;

        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement guestBookDetailStmt = conn.prepareStatement(guestBookDetailsQuery);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

            ResultSet rs = guestBookDetailStmt.executeQuery();

            // Print table header
            System.out.println("+----+----------------+-----------------------------+---------------------+---------------------+--------+");
            System.out.println("| ID | Guest Name     | Book Title                  | Start Time          | End Time            | Minutes|");
            System.out.println("+----+----------------+-----------------------------+---------------------+---------------------+--------+");

            // Print rows
            while (rs.next()) {


                Object endTimeObj = rs.getObject("end_time");
                String endtime = (endTimeObj == null) ? "Not Ended Yet" : rs.getString("end_time");

                System.out.printf("| %-2d | %-14s | %-27s | %-19s | %-19s | %-6d |\n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("title"),
                        rs.getTimestamp("start_time"),
                        endtime,
                        rs.getInt("amount"));
            }

            System.out.println("+----+----------------+-----------------------------+---------------------+---------------------+--------+");

            auditStmt.setInt(1, staff.getId());
            auditStmt.setString(2, staff.getRole() + " " + staff.getName() + " viewed the book reading details of each guest.");
            auditStmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error encountered while trying to view guest book details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void viewUserDetails(Staff staff) {
        String userDetailsQuery = "SELECT users.id, users.name, users.email, users.date_of_joining FROM users";
        String auditLog = "INSERT INTO audit_log(staff_id, action) VALUES(?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement userDetailsStmt = conn.prepareStatement(userDetailsQuery);
             PreparedStatement auditStmt = conn.prepareStatement(auditLog)) {

            // Execute the query to retrieve user details
            ResultSet rs = userDetailsStmt.executeQuery();

            // Print table header
            System.out.println("+----+----------------+-------------------------+---------------------+");
            System.out.println("| ID | Name           | Email                   | Date of Joining     |");
            System.out.println("+----+----------------+-------------------------+---------------------+");

            // Iterate through the result set and print each user's details
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                Date dateOfJoining = rs.getDate("date_of_joining");

                // Print each row of the user details
                System.out.printf("| %-2d | %-14s | %-23s | %-19s |\n",
                        id, name, email, dateOfJoining);
            }

            System.out.println("+----+----------------+-------------------------+---------------------+");

            // Log the action in the audit log (log the librarian's action)
            auditStmt.setInt(1, staff.getId());
            auditStmt.setString(2, staff.getRole() + " " + staff.getName() + " viewed user details.");
            auditStmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error encountered when librarian tried to view user details: " + e.getMessage());
            e.printStackTrace();
        }
    }
}