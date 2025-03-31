package services;

import db.DatabaseManager;
import models.Book;
import models.Guest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GuestService extends PersonService{

    //In-memory tracking of what guest is currently reading
    private final List<Book> guestReadingList = new ArrayList<>();

    // Start a guest session
    public Guest startSession(String name, String contact) {
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
                    return new Guest(name, contact);
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Guest with this contact already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    //Guest starts reading a book
    public void startReading(int guestId, int bookID) {
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
            guestReadingList.add(to_add);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Guest returns a book
    public boolean returnBook(int guestId, int bookId) {

        int i = 0;
        while (guestReadingList.size() > i) {

            if (guestReadingList.get(i).getId() == bookId) {
                break;
            }
        }
        guestReadingList.remove(i);

        String query = "UPDATE guest_book_usage SET end_time = CURRENT_TIMESTAMP WHERE guest_id = ? AND book_id = ? AND end_time IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, guestId);
            stmt.setInt(2, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void logoutGuest(int guestId) {
        double fine = calculateFine(guestId);

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
            e.printStackTrace();
        }
    }


    public double calculateFine(int guestId) {
        String query = """
        SELECT SUM(GREATEST(TIMESTAMPDIFF(MINUTE, start_time, COALESCE(end_time, NOW())) - 30, 0) / 10 * 5) AS total_fine
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
            e.printStackTrace();
        }
        return 0.0;
    }

    //Currently reading books
    //public  void currentReading(int guestId);

}
