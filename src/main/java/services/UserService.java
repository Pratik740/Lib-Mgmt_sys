package services;

import db.DatabaseManager;
import models.User;
import java.sql.*;

public class UserService extends PersonService{

    // Register a new user
    public boolean registerUser(String name, String email, String passwordHash) {
        String query = "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Authenticate user login
    public User loginUser(String email, String passwordHash) {
        String query = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, passwordHash);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getDate("date_of_joining").toLocalDate()
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Fetch user profile by ID
    public User getUserById(int userId) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getDate("date_of_joining").toLocalDate()
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update user details
    public boolean updateUser(int userId, String newName, String newEmail, String newPasswordHash) {
        String query = "UPDATE users SET name = ?, email = ?, password_hash = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newName);
            stmt.setString(2, newEmail);
            stmt.setString(3, newPasswordHash);
            stmt.setInt(4, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete user account
    public boolean deleteUser(int userId) {
        String checkBorrowedBooksQuery = "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND return_date IS NULL";
        String checkFinesQuery = "SELECT COUNT(*) FROM fines WHERE user_id = ? AND status = 'Pending'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkBorrowedStmt = conn.prepareStatement(checkBorrowedBooksQuery);
             PreparedStatement checkFinesStmt = conn.prepareStatement(checkFinesQuery)) {

            checkBorrowedStmt.setInt(1, userId);
            checkFinesStmt.setInt(1, userId);

            ResultSet rsBooks = checkBorrowedStmt.executeQuery();
            ResultSet rsFines = checkFinesStmt.executeQuery();

            if (rsBooks.next() && rsBooks.getInt(1) > 0) {
                System.out.println("Cannot delete user: Books are still borrowed.");
                return false;
            }

            if (rsFines.next() && rsFines.getInt(1) > 0) {
                System.out.println("Cannot delete user: Pending fines exist.");
                return false;
            }

            // Now delete user safely
            String deleteUserQuery = "DELETE FROM users WHERE id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteUserQuery)) {
                deleteStmt.setInt(1, userId);
                return deleteStmt.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean requestBook(int userId, int bookId) {
        if (hasActiveLoan(userId, bookId)) {
            System.out.println("You already have this book borrowed. Return it first.");
            return false;
        }

        if (!isBookAvailable(bookId)) {
            System.out.println("No available copies for this book. Reserve it instead.");
            return false;
        }

        String query = "INSERT INTO book_requests (user_id, book_id, status) VALUES (?, ?, 'Pending')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean handleReservations(int userId, int bookId) {
        if (isBookAvailable(bookId)) {
            System.out.println("Book is available. No need for a reservation.");
            return false;
        }

        String dueDateQuery = """
        SELECT MIN(due_date) FROM transactions 
        WHERE book_copy_id IN (SELECT id FROM book_copies WHERE book_id = ?) 
        AND return_date IS NULL
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dueDateQuery)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getDate(1) != null) {
                Date estimatedAvailable = rs.getDate(1); // Latest due date

                String insertQuery = """
                INSERT INTO reservations (user_id, book_id, expected_availability) VALUES (?, ?, ?)
                """;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, bookId);
                    insertStmt.setDate(3, estimatedAvailable);
                    return insertStmt.executeUpdate() > 0;
                }
            } else {
                System.out.println("No active transactions found. Book availability unknown.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    // Return a book
    public boolean returnBook(int userId, int bookId) {
        checkAndApplyFine(userId, bookId);
        String query = """
        UPDATE transactions 
        SET return_date = CURDATE()
        WHERE user_id = ? 
        AND book_copy_id IN (SELECT id FROM book_copies WHERE book_id = ?) 
        AND return_date IS NULL
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            if (stmt.executeUpdate() > 0) {
                markBookAsAvailable(bookId);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Helper Methods
    private boolean isBookAvailable(int bookId) {
        String query = "SELECT COUNT(*) FROM book_copies WHERE book_id = ? AND available = TRUE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean hasActiveLoan(int userId, int bookId) {
        String query = """
        SELECT COUNT(*) FROM transactions 
        WHERE user_id = ? AND book_copy_id IN 
        (SELECT id FROM book_copies WHERE book_id = ?) 
        AND return_date IS NULL
    """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void checkAndApplyFine(int userId, int bookId) {
        String fineQuery = """
        INSERT INTO fines (user_id, transaction_id, amount, status)
        SELECT ?, id, 10.00 * DATEDIFF(CURDATE(), due_date), 'Pending'
        FROM transactions WHERE user_id = ? AND book_copy_id IN 
        (SELECT id FROM book_copies WHERE book_id = ?) 
        AND return_date IS NULL AND CURDATE() > due_date
    """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(fineQuery)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, bookId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void markBookAsAvailable(int bookId) throws SQLException {
        String updateQuery = "UPDATE book_copies SET available = TRUE WHERE book_id = ? AND available = FALSE LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setInt(1, bookId);
            stmt.executeUpdate();
        }
    }


    public void printUserBorrowedBooks(int userId) {
        String query = """
        SELECT DISTINCT b.title
        FROM transactions t
        JOIN book_copies bc ON t.book_copy_id = bc.id
        JOIN books b ON bc.book_id = b.id
        WHERE t.user_id = ? AND t.return_date IS NULL
        ORDER BY t.due_date ASC
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            System.out.println("Books currently borrowed by user ID " + userId + ":");

            boolean hasBooks = false;
            while (rs.next()) {
                hasBooks = true;
                System.out.println("- " + rs.getString("title"));
            }

            if (!hasBooks) {
                System.out.println("No books borrowed.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void printUserFines(int userId) {
        String query = """
        SELECT b.title, f.amount, f.status
        FROM fines f
        JOIN transactions t ON f.transaction_id = t.id
        JOIN book_copies bc ON t.book_copy_id = bc.id
        JOIN books b ON bc.book_id = b.id
        WHERE f.user_id = ?
        ORDER BY f.status ASC, f.amount DESC
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            System.out.println("Fines accumulated by user ID " + userId + ":");

            boolean hasFines = false;
            while (rs.next()) {
                hasFines = true;
                String bookTitle = rs.getString("title");
                double fineAmount = rs.getDouble("amount");
                String status = rs.getString("status");

                System.out.println("- Book: " + bookTitle + " | Fine: ₹" + fineAmount + " | Status: " + status);
            }

            if (!hasFines) {
                System.out.println("No fines recorded.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




}
