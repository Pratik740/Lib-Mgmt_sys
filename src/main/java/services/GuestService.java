package services;

import java.sql.*;
import java.util.ArrayList;

import db.DatabaseManager;
import models.Book;
import models.Guest;

public class GuestService extends PersonService{

    // Start a guest session
    public static Guest guestLogin(String name, String contact) {
        String query = "INSERT INTO guests (name, contact) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, contact);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int guestId = generatedKeys.getInt(1);
                    System.out.println("Guest session started. Guest ID: " + guestId);
                    return new Guest(guestId, name, contact);
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Guest with this contact already exists.");
        } catch (SQLException e) {
            System.err.println("Some error occurred while trying to insert a guest.");
            e.printStackTrace();
        }
        return null;
    }


    //Guest starts reading a book
    public static void startReading(Guest guest, int bookID) {
        int guestId = guest.getId();
        String query = "INSERT INTO guest_book_usage (guest_id, book_id, start_time) VALUES (?, ?, CURRENT_TIMESTAMP)";
        String book_find = "SELECT * FROM books WHERE books.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             PreparedStatement book = conn.prepareStatement(book_find)) {

            stmt.setInt(1, guestId);
            stmt.setInt(2, bookID);
            stmt.execute();

            book.setInt(1, bookID);
            ResultSet rs = book.executeQuery();

            Book to_add = new Book(rs.getInt("id"),
                                    rs.getString("title"),
                                    rs.getString("author"),
                                    rs.getString("isbn"),
                                    rs.getInt("genre_id"));


            guest.addBook(to_add);
        } catch (SQLException e) {
            System.err.println("Some error occurred while trying to start reading a new book.");
            e.printStackTrace();
        }
    }

    // Guest returns a book
    public static boolean returnBook(Guest guest, int bookId) {

        int guestId = guest.getId();

        guest.removeBook(bookId);

        String query = "UPDATE guest_book_usage SET end_time = CURRENT_TIMESTAMP, fine_amt = GREATEST(TIMESTAMPDIFF(MINUTE, start_time, NOW()), 0) WHERE guest_id = ? AND book_id = ? AND end_time IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, guestId);
            stmt.setInt(2, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Book could not be returned. Error: " + e.getMessage());
            return false;
        }
    }

    public static void logoutGuest(Guest guest) {
        int guestId = guest.getId();

        //Return all presently reading books as on logout all books would be auto submitted to the librarian
        ArrayList<Book> temp = guest.getCurrently_reading_books();
        for (Book book : temp) {
            returnBook(guest, book.getId());
        }
        temp.clear();

        double fine = calculateFine(guestId); //

        String query = "DELETE FROM guests WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, guestId);
            if (stmt.executeUpdate() > 0) {
                System.out.println("Guest logged out successfully.");
                System.out.println("Total amount due: ₹" + fine);
            } else {
                System.out.println("Guest not found.");
            }
        } catch (SQLException e) {
            System.err.println("Logout not successful. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static double calculateFine(int guestId) {
        String query = """
        SELECT SUM(fine_amt) AS total_fine
        FROM guest_book_usage WHERE guest_id = ?
    """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, guestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total_fine");
            }
        } catch (SQLException e) {
            System.err.println("Fine calculation failed. Error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

}
