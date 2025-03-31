package services;

import db.DatabaseManager;
import models.Transaction;
import models.User;
import models.Book;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class UserService extends PersonService{

    // Register a new user
    public User registerUser(String name, String email, String passwordHash) {
        String query = "INSERT INTO users (name, email, password_hash, date_of_joining) VALUES (?, ?, ?, current_timestamp())";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);

            stmt.executeUpdate();

            return loginUser(name, email);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
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

    public void addTransaction(User user) {
        String query = "SELECT * FROM transactions WHERE user_id = ? AND return_date is null";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);) {
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            while(rs.next()) {
                user.SetTransaction(new Transaction(rs.getInt("id"),
                                                    rs.getInt("user_id"),
                                                    rs.getInt("book_copy_id"),
                                                    rs.getDate("issue_date").toLocalDate(),
                                                    rs.getDate("due_date").toLocalDate(),
                                                    null,
                                                    LocalDate.now().isAfter(rs.getDate("due_date").toLocalDate()) ? ((int) ChronoUnit.DAYS.between(rs.getDate("due_date").toLocalDate(), LocalDate.now())*10): 0));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void addBorrowedBook(User user) {
        String query = "SELECT b.id AS book_id, b.title, b.author, b.isbn, b.genre_id " +
                "FROM transactions t " +
                "JOIN book_copies bc ON t.book_copy_id = bc.id " +
                "JOIN books b ON bc.book_id = b.id " +
                "WHERE t.user_id = ? AND t.return_date IS NULL";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getInt("genre_id"));  // Using genreId as int
                user.SetBook(book);  // Assuming User class has addBook method
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void returnBook(User user , int bookId) {

        user.removeBook(bookId);

        int transactionId = 0;

        boolean overdue = false;
        LocalDate dueDate = null;

        String transactionFinder = """
                                        SELECT a.book_id, a.title, a.author, a.isbn, a.genre_id,
                                               a.copy_number, a.available,
                                               t.id AS transaction_id, t.user_id, t.issue_date, t.due_date,
                                               t.return_date, t.fine_amount
                                        FROM (
                                                 SELECT books.id AS book_id, books.title, books.author, books.isbn, books.genre_id,
                                                        book_copies.id AS copy_id, book_copies.copy_number, book_copies.available
                                                 FROM books
                                                          JOIN book_copies ON books.id = book_copies.book_id
                                                 WHERE books.id = ?
                                             ) AS a
                                                 JOIN transactions t ON a.copy_id = t.book_copy_id WHERE t.user_id = ?;
                                        
                                    """;
        String query = """
                       UPDATE transactions
                       SET return_date = ?,
                       fine_amount = 0
                       WHERE id = ?;
                       """;

        String fineQuery = """
                           UPDATE fines
                           SET amount = ?, status = 'Paid'
                           WHERE id = ?;
                           """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(transactionFinder);
             PreparedStatement stmt2 = conn.prepareStatement(query);
             PreparedStatement stmt3 = conn.prepareStatement(fineQuery)) {

            stmt1.setInt(1, bookId);
            stmt1.setInt(2, user.getId());
            ResultSet rs1 = stmt1.executeQuery();

            if (rs1.next()) {
                transactionId = rs1.getInt("transaction_id");

                if (LocalDate.now().isAfter(rs1.getDate("due_date").toLocalDate())) {
                    overdue = true;
                    dueDate = rs1.getDate("due_date").toLocalDate();
                }
            }

            stmt2.setDate(1, new Date(System.currentTimeMillis()));
            stmt2.setInt(2, transactionId);
            stmt2.executeUpdate();

            if (overdue) {
                stmt3.setInt(1, (int) ChronoUnit.DAYS.between(LocalDate.now(), dueDate) * 10);
                stmt3.setInt(2, user.getId());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        user.UpdateTransaction(transactionId);


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
