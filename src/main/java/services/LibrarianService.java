package services;

import Schedulers.*;
import db.DatabaseManager;
import models.Librarian;
import models.Staff;

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


            if (rs.next()) {

                auditStmt.setString(1, rs.getString("id"));
                auditStmt.setString(2, "Librarian " + rs.getString("name") + " logged in");
                auditStmt.executeUpdate();

                Librarian lib =  new Librarian(rs.getInt("id"),
                                     rs.getString("name"),
                                     rs.getString("email"),
                                     rs.getString("password_hash"),
                                     rs.getTime("shift_start").toLocalTime(),
                                     rs.getTime("shift_end").toLocalTime());

                FineService.populateFineTable();
                ReservationService.reserveToReqPopulate();

                return lib;
            }
            else {
                System.out.println("Login Failed, user not found!");
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Librarian login failed. Error: " + e.getMessage());
            e.printStackTrace();
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
            auditStmt.setString(2, staff.getRole() + " "  + staff.getName() + "viewed all available books and their copies.");
        } catch (SQLException e) {
            System.err.println("Error encountered while viewing available book copies: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static void provideApprovals(Staff staff) {
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";
        String pendingRequests = """
                    UPDATE book_requests
                    SET status = 'Approved'
                    WHERE status = 'Pending';
                   """;

        String appendToTransactions = "insert into transactions(user_id, book_copy_id, issue_date, due_date)" +
                                      "values (?, ?, current_date, date_add(current_date, INTERVAL 14 day))";

        String selectPendingQuery = "select user_id, book_copy_id from book_requests where status = 'Pending'";

        String messageToUser = "insert into messages(user_id, description)  value(?, ?)";

        String bookNameQuery = "select books.title from books join book_copies on books.id = book_copies.book_id where book_copies.id = ?";

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(pendingRequests);
            PreparedStatement stmt1 = conn.prepareStatement(auditLog);
            PreparedStatement appendStmt = conn.prepareStatement(appendToTransactions);
            PreparedStatement selectPendingStmt = conn.prepareStatement(selectPendingQuery);
            PreparedStatement text = conn.prepareStatement(messageToUser);
            PreparedStatement bookNameStmt = conn.prepareStatement(bookNameQuery);) {


            ResultSet rs = selectPendingStmt.executeQuery();

            while (rs.next()) {
                appendStmt.setInt(1, rs.getInt("user_id"));
                appendStmt.setInt(2, rs.getInt("book_copy_id"));
                appendStmt.executeQuery();
            }

            bookNameStmt.setInt(1, rs.getInt(2));
            ResultSet book = bookNameStmt.executeQuery();

            text.setInt(1, rs.getInt("user_id"));
            text.setString(2, "Request for book " + book.getString("title") + " has been approved.");

            stmt.executeUpdate();

            System.out.println("All pending book requests have been approved.");
            stmt1.setInt(1, staff.getId());
            stmt1.setString(2, staff.getRole() + " " + staff.getName() + " approved all pending book requests.");
            stmt1.executeUpdate();

        } catch (SQLException e) {
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
            auditStmt.setString(2, staff.getRole() + " "  + staff.getName() + "viewed the book reading details of each guest.");
            auditStmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error encountered while trying to view guest book details: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void checkoutUser(Staff staff,int userId) {
        String getUser = "select * from users where id = ?";
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";
        try(Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(getUser);
        PreparedStatement auditStmt = conn.prepareStatement(auditLog)){
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("Id : "+userId +
                                " Name : "+rs.getString("name")+
                                " Email : "+rs.getString("email")+
                                " Date of Joining : "+rs.getDate("date_of_joining"));
            auditStmt.setInt(1, staff.getId());
            auditStmt.setString(2,staff.getRole() + " " +staff.getName()+" viewed the details " +
                    "of User "+rs.getString("name"));
            auditStmt.executeUpdate();
        }
        catch (SQLException e) {
            System.err.println("Error encountered while trying to checkout user: " + e.getMessage());
        }
    }
    // It will contain all the books that are overdue(Book is with him/her) by any user.
    public static void overdueBooksOfUser(Staff staff,int userId){
            String getOverDue = """
                    SELECT title,due_date,amount from
                    fines as f INNER JOIN transactions as t ON f.transaction_id = t.id
                    INNER JOIN book_copies as bc ON bc.id = t.book_copy_id 
                    INNER JOIN books as b ON b.id = bc.book_id
                    where f.user_id = ? AND f.status = 'Pending';
                    """;
            String auditLog = "insert into audit_log(staff_id, action) values(?,?)";
            try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(getOverDue);
            PreparedStatement auditstmt = conn.prepareStatement(auditLog)){
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                System.out.println("Overdue Books of User " + userId + " :");
                System.out.println("+-------------------------------+------------+--------+");
                System.out.println("| Title                         | Due Date   | Amount |");
                System.out.println("+-------------------------------+------------+--------+");

                while (rs.next()) {
                    System.out.printf("| %-29s | %-10s | %-6d |\n",
                            rs.getString("title"),
                            rs.getDate("due_date"),
                            rs.getInt("amount"));
                }

                System.out.println("+-------------------------------+------------+--------+");
                auditstmt.setInt(1,staff.getId());
                auditstmt.setString(2,staff.getRole() + " " +staff.getName()+" viewed the overdue books of User "+userId);
                auditstmt.executeUpdate();
            }
            catch (SQLException e) {
                System.err.println("Error encountered while trying to overdue books of user: " + e.getMessage());
            }
    }

    public static void returnedBooksOnCurrentDate(Staff staff) {
        String returned = """
                select title , amount , book_copy_id , user_id from fines as f
                INNER JOIN transactions as t ON f.transaction_id = t.id
                INNER JOIN book_copies as bc ON bc.id = t.book_copy_id
                INNER JOIN books as b ON b.id = bc.book_id
                where f.status = 'Paid' AND t.return_date = ?;                
                """;
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";
        try(Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(returned);
        PreparedStatement auditlog = conn.prepareStatement(auditLog)){
            Date currentdate = new Date(System.currentTimeMillis());
            stmt.setDate(1,currentdate );
            ResultSet rs = stmt.executeQuery();
            System.out.println("Returned Books on Current Date:");
            System.out.println("+---------+-------------------------------+--------------+--------+");
            System.out.println("| User ID | Title                         | Book Copy ID | Amount |");
            System.out.println("+---------+-------------------------------+--------------+--------+");

            while (rs.next()) {
                System.out.printf("| %-7d | %-29s | %-12s | %-6s |\n",
                        rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getString("book_copy_id"),
                        rs.getString("amount"));
            }
            System.out.println("+---------+-------------------------------+--------------+--------+");

            auditlog.setInt(1, staff.getId());
            auditlog.setString(2,staff.getRole() + " " +staff.getName()+" viewed returned books on date : "+currentdate);
            auditlog.executeUpdate();
        }
        catch (SQLException e) {
            System.err.println("Error encountered while trying to view returned books : " + e.getMessage());
        }
    }

    public static void getMyProfile(Staff staff) {
        System.out.println("+----------------------+-----+---------------------------+------------+-------------+------------+");
        System.out.println("| Name                 | ID  | Email                     | Role       | Shift Start | Shift End  |");
        System.out.println("+----------------------+-----+---------------------------+------------+-------------+------------+");

        System.out.printf("| %-20s | %-3s | %-25s | %-10s | %-11s | %-10s |\n",
                staff.getName(),
                staff.getId(),
                staff.getEmail(),
                staff.getRole(),
                staff.getShiftStart(),
                staff.getShiftEnd());

        System.out.println("+----------------------+-----+---------------------------+------------+-------------+------------+");
    }

    public static void viewPendingApprovals(Staff staff) {
        String requests = "Select * from book_requests where status = 'Pending'";
        String auditLog = "insert into audit_log(staff_id, action) values(?,?)";
        try(Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(requests);
        PreparedStatement auditstmt = conn.prepareStatement(auditLog)){
            stmt.executeQuery();
            ResultSet rs = stmt.executeQuery();
            System.out.println("All pending Approvals are as follows:");
            System.out.println("+----+---------+---------+--------------+---------------------+");
            System.out.println("| ID | User ID | Book ID | Book Copy ID | Request Date        |");
            System.out.println("+----+---------+---------+--------------+---------------------+");

            while (rs.next()) {
                System.out.printf("| %-2d | %-7d | %-7d | %-12d | %-19s |\n",
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getInt("book_copy_id"),
                        rs.getTimestamp("request_date"));
            }
            System.out.println("+----+---------+---------+--------------+---------------------+");
            auditstmt.setInt(1, staff.getId());
            auditstmt.setString(2, staff.getRole() + " " +staff.getName()+" has viewed all pending approvals");
            auditstmt.executeUpdate();
        }
        catch (SQLException e) {
            System.err.println("Error encountered while trying to view Pending approvals : " + e.getMessage());
        }
    }

}