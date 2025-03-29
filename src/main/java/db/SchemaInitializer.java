package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    public static void initialize() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            // 1. Users Table (Contains Subscription Users & Guests)
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    type ENUM('Subscription', 'Guest') NOT NULL DEFAULT 'Subscription'
                );
            """;
            stmt.execute(createUsersTable);

            // 2. Staff Table (Separate from Users)
            String createStaffTable = """
                CREATE TABLE IF NOT EXISTS staff (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    role ENUM('Admin', 'Librarian') NOT NULL,
                    shift_start TIME NULL,
                    shift_end TIME NULL
                );
            """;
            stmt.execute(createStaffTable);

            // 3. Genres Table
            String createGenresTable = """
                CREATE TABLE IF NOT EXISTS genres (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) UNIQUE NOT NULL
                );
            """;
            stmt.execute(createGenresTable);

            // 4. Books Table
            String createBooksTable = """
                CREATE TABLE IF NOT EXISTS books (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    author VARCHAR(255) NOT NULL,
                    isbn VARCHAR(20) UNIQUE NOT NULL,
                    genre_id INT,
                    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE SET NULL
                );
            """;
            stmt.execute(createBooksTable);

            // 5. Book Copies Table
            String createBookCopiesTable = """
                CREATE TABLE IF NOT EXISTS book_copies (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    book_id INT NOT NULL,
                    copy_number INT NOT NULL,
                    available BOOLEAN DEFAULT TRUE,
                    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createBookCopiesTable);

            // 6. Transactions Table (For Subscription Users)
            String createTransactionsTable = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    book_copy_id INT NOT NULL,
                    issue_date DATE NOT NULL,
                    due_date DATE NOT NULL,
                    return_date DATE,
                    fine_amount DECIMAL(10,2) DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (book_copy_id) REFERENCES book_copies(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createTransactionsTable);

            // 7. Reservations Table (For Subscription Users)
            String createReservationsTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    book_id INT NOT NULL,
                    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status ENUM('Pending', 'Completed', 'Cancelled') DEFAULT 'Pending',
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createReservationsTable);

            // 8. Book Requests Table (For Subscription Users & Guests)
            String createBookRequestsTable = """
                CREATE TABLE IF NOT EXISTS book_requests (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NULL,  -- Nullable for Guests
                    guest_name VARCHAR(100) NULL,
                    guest_contact VARCHAR(20) NULL,
                    title VARCHAR(255) NOT NULL,
                    author VARCHAR(255) NOT NULL,
                    status ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
                    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
                );
            """;
            stmt.execute(createBookRequestsTable);

            // 9. Guest Book Usage Table (Tracks Guest Reading Time & Charges)
            String createGuestBookUsageTable = """
                CREATE TABLE IF NOT EXISTS guest_book_usage (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    guest_name VARCHAR(100) NOT NULL,
                    guest_contact VARCHAR(20) NOT NULL,
                    book_id INT NOT NULL,
                    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    end_time TIMESTAMP NULL,
                    total_hours DECIMAL(5,2) GENERATED ALWAYS AS (TIMESTAMPDIFF(SECOND, start_time, end_time) / 3600.0) VIRTUAL,
                    amount_due DECIMAL(10,2) DEFAULT 0,
                    payment_status ENUM('Pending', 'Paid') DEFAULT 'Pending',
                    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createGuestBookUsageTable);

            // 10. Fines Table (For Subscription Users)
            String createFinesTable = """
                CREATE TABLE IF NOT EXISTS fines (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    transaction_id INT NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    status ENUM('Pending', 'Paid') DEFAULT 'Pending',
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createFinesTable);

            // 11. Audit Log Table (For Staff Actions)
            String createAuditLogTable = """
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    staff_id INT NOT NULL,
                    action VARCHAR(255) NOT NULL,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
                );
            """;
            stmt.execute(createAuditLogTable);

            conn.commit();

            System.out.println("Database schema initialized successfully!");

        } catch (SQLException e) {
            e.printStackTrace(); // Proper logging needed in production
        }
    }
}
