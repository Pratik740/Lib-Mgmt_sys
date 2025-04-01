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
    public static User registerUser(String name, String email, String passwordHash) {
        String query = "INSERT INTO users (name, email, password_hash, date_of_joining) VALUES (?, ?, ?, current_timestamp())";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);

            stmt.executeUpdate();

            return loginUser(name, email);

        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("Enter an unique email address, current one already exists!");
        } catch (SQLException e) {
            System.err.println("User registration failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Authenticate user login
    public static User loginUser(String email, String passwordHash) {
        String query = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, passwordHash);
            ResultSet rs = stmt.executeQuery();

            FineService.populateFineTable();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getDate("date_of_joining").toLocalDate()
                );
            }
            else {
                System.out.println("User not found!");
                return null;
            }


        } catch (SQLException e) {
            System.err.println("User login failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    //This is needed to have a set of transactions from which fine computation is done.
    public static void getListOfUnfulfilledTransactions(User user) {
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
            System.err.println(" Failed to fetch the list of unfulfilled transactions: " + e.getMessage());
            e.printStackTrace();
        }

    }


    //Arraylist of borrowed books for a particular user
    public static void getListOfBorrowedBooks(User user) {
        String query = """
                       select id, title, author, isbn, genre_id from
                       (select b.book_id as book_id from book_copies b join transactions t on t.book_copy_id = b.id where t.user_id = ? and t.return_date is null)
                       as a
                       join books on a.book_id = books.id;
                       """;

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

    public static boolean returnBook(User user , int bookId) {

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
                       SET return_date = now(),
                       fine_amount = DATEDIFF(now(), due_date) * 10
                       WHERE id = ?;
                       """;

        String fineQuery = """
                           UPDATE fines
                           SET amount = ?, status = 'Paid'
                           WHERE user_id = ? and transaction_id = ?;
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

            stmt2.setInt(1, transactionId);
            stmt2.executeUpdate();

            if (overdue) {
                stmt3.setInt(1, (int) ChronoUnit.DAYS.between(LocalDate.now(), dueDate) * 10);
                stmt3.setInt(2, user.getId());
                stmt3.setInt(3, transactionId);
                stmt3.executeUpdate();
            }



        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        user.UpdateTransaction(transactionId);
        return true;


    }


    //Deletes any user
    public static boolean deleteUser(User user) {
        if (!user.getBooks_borrowed().isEmpty()) {
            System.out.println("Cannot delete user: Books are still borrowed.");
            return true;
        }

        if (FineService.pendingFines(user)) {
            System.out.println("Cannot delete user: Fines are still pending.");
            return true;
        }

        String delQuery = "delete from users where id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(delQuery)) {

            if (stmt.executeUpdate() > 0) {
                return true;
            }
            else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Cannot delete user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean requestBook(User user , int bookId) {

        for (Book book : user.getBooks_borrowed()) {
            if (book.getId() == bookId) {
                System.out.println("Cannot request book as user posses a copy already!.");
                return true;
            }
        }

        String fineCount = "select count(*) as count from fines where user_id = ?";
        String insertionQuery = "insert into book_requests(user_id, book_id, book_copy_id, request_date, status) values(?, ?, ?, current_date, ?)";
        String copyIdFinder = "select copy_number from book_copies where book_id = ? AND available = true ";
        String reservationInsert = "insert into reservations(user_id,book_id,request_date,expected_availability) values(?,?,current_date,?) ";
        String oldestTransaction = "select ";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(fineCount);
             PreparedStatement stmt2 = conn.prepareStatement(insertionQuery);
             PreparedStatement stmt3 = conn.prepareStatement(copyIdFinder);
             PreparedStatement stmt4 = conn.prepareStatement(reservationInsert);
             PreparedStatement stmt5 = conn.prepareStatement(oldestTransaction)) {

            stmt1.setInt(1, user.getId());
            ResultSet rs1 = stmt1.executeQuery();
            if (rs1.next()) {
                if (rs1.getInt("count") > 3) {
                    System.out.println("Cannot request book as user has more than 3 pending fines!.");
                    stmt2.setInt(1, user.getId());
                    stmt2.setInt(2, bookId);
                    stmt2.setNull(3, Types.INTEGER);
                    stmt2.setString(4, "Rejected");
                    stmt2.executeUpdate();
                    return true;
                }
            }
            stmt3.setInt(1, bookId);
            ResultSet rs3 = stmt3.executeQuery();
            if(rs3.next()){
                System.out.println("Your request has been processed in the Book Requests!!!");
                stmt2.setInt(1, user.getId());
                stmt2.setInt(2, bookId);
                stmt2.setInt(3, rs3.getInt("copy_number"));
                stmt2.setString(4, "Pending");
            }
            else{
                System.out.println("Oops!!! We are fresh out of that book copies.");




            }








        } catch (SQLException e) {
            System.err.println("User not able to request book: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    public boolean requestBookRef(int userId, int bookId) {
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
