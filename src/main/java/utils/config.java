package utils;

import db.DatabaseManager;
import db.SchemaInitializer;

import java.sql.*;

public class config {
    public static void preConfiguration() {
        SchemaInitializer.initialize();


        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            String populateGuests = """
                                INSERT INTO guests (name, contact) VALUES
                                        ('Alice Johnson', '1234567890'),
                                        ('Bob Smith', '0987654321'),
                                        ('Charlie Brown', '1122334455'),
                                        ('David Miller', '2233445566'),
                                        ('Emma Wilson', '3344556677'),
                                        ('Sophia Lee', '4455667788'),
                                        ('Michael Johnson', '5566778899'),
                                        ('Olivia Martinez', '6677889900'),
                                        ('William Anderson', '7788990011'),
                                        ('Isabella Thomas', '8899001122');
                                """;
            stmt.executeUpdate(populateGuests);

            String populateUsers = """
                                   INSERT INTO users (name, email, password_hash, date_of_joining) VALUES
                                           ('John Doe', 'johndoe1@example.com', 'hashedpassword1', '2024-03-10'),
                                           ('Jane Smith', 'janesmith2@example.com', 'hashedpassword2', '2024-07-10'),
                                           ('Michael Johnson', 'michaelj3@example.com', 'hashedpassword3', '2024-02-10'),
                                           ('Emily Davis', 'emilyd4@example.com', 'hashedpassword4', '2024-10-10'),
                                           ('Daniel Brown', 'danielb5@example.com', 'hashedpassword5', '2024-08-10'),
                                           ('Jessica Wilson', 'jessicaw6@example.com', 'hashedpassword6', '2024-01-10'),
                                           ('David Martinez', 'davidm7@example.com', 'hashedpassword7', '2024-09-10'),
                                           ('Sarah Taylor', 'saraht8@example.com', 'hashedpassword8', '2024-05-10'),
                                           ('James Anderson', 'jamesa9@example.com', 'hashedpassword9', '2024-03-10'),
                                           ('Laura Thomas', 'laurat10@example.com', 'hashedpassword10', '2024-11-10');
                                   """;
            stmt.executeUpdate(populateUsers);

            String populateGenres = """
                                    INSERT INTO genres (name) VALUES
                                                  ('Fiction'), ('Non-Fiction'), ('Sci-Fi'), ('Fantasy'), ('Mystery'),
                                                  ('Biography'), ('Self-Help'), ('History'), ('Technology'), ('Philosophy');
                                    """;
            stmt.executeUpdate(populateGenres);

            String populateBooks = """
                                   INSERT INTO books (title, author, isbn, genre_id) VALUES
                                               ('To Kill a Mockingbird', 'Harper Lee', 'ISBN-1001', 1),
                                               ('The Great Gatsby', 'F. Scott Fitzgerald', 'ISBN-1002', 1),
                                               ('1984', 'George Orwell', 'ISBN-1003', 1),
                                               ('Sapiens: A Brief History of Humankind', 'Yuval Noah Harari', 'ISBN-2001', 2),
                                               ('Becoming', 'Michelle Obama', 'ISBN-2002', 2),
                                               ('Silent Spring', 'Rachel Carson', 'ISBN-2003', 2),
                                               ('Dune', 'Frank Herbert', 'ISBN-3001', 3),
                                               ('The Hitchhiker\\'s Guide to the Galaxy', 'Douglas Adams', 'ISBN-3002', 3),
                                               ('Neuromancer', 'William Gibson', 'ISBN-3003', 3),
                                               ('The Lord of the Rings', 'J.R.R. Tolkien', 'ISBN-4001', 4),
                                               ('Harry Potter and the Philosopher\\'s Stone', 'J.K. Rowling', 'ISBN-4002', 4),
                                               ('A Game of Thrones', 'George R.R. Martin', 'ISBN-4003', 4),
                                               ('The Girl with the Dragon Tattoo', 'Stieg Larsson', 'ISBN-5001', 5),
                                               ('Gone Girl', 'Gillian Flynn', 'ISBN-5002', 5),
                                               ('And Then There Were None', 'Agatha Christie', 'ISBN-5003', 5),
                                               ('Steve Jobs', 'Walter Isaacson', 'ISBN-6001', 6),
                                               ('The Diary of a Young Girl', 'Anne Frank', 'ISBN-6002', 6),
                                               ('Elon Musk', 'Walter Isaacson', 'ISBN-6003', 6),
                                               ('Atomic Habits', 'James Clear', 'ISBN-7001', 7),
                                               ('The 7 Habits of Highly Effective People', 'Stephen R. Covey', 'ISBN-7002', 7),
                                               ('Man\\'s Search for Meaning', 'Viktor E. Frankl', 'ISBN-7003', 7),
                                               ('Guns, Germs, and Steel', 'Jared Diamond', 'ISBN-8001', 8),
                                               ('A People\\'s History of the United States', 'Howard Zinn', 'ISBN-8002', 8),
                                               ('The Silk Roads', 'Peter Frankopan', 'ISBN-8003', 8),
                                               ('The Innovators', 'Walter Isaacson', 'ISBN-9001', 9),
                                               ('Superintelligence', 'Nick Bostrom', 'ISBN-9002', 9),
                                               ('The Age of AI', 'Henry Kissinger, Eric Schmidt, and Daniel Huttenlocher', 'ISBN-9003', 9),
                                               ('Meditations', 'Marcus Aurelius', 'ISBN-10001', 10),
                                               ('Beyond Good and Evil', 'Friedrich Nietzsche', 'ISBN-10002', 10),
                                               ('The Republic', 'Plato', 'ISBN-10003', 10),
                                               ('Mystic River', 'Dennis Lehane', 'ISBN-003', 5),
                                               ('A Brief History of Time', 'Stephen Hawking', 'ISBN-004', 9),
                                               ('Meditations', 'Marcus Aurelius', 'ISBN-005', 10),
                                               ('The Biography of Tesla', 'John O’Neill', 'ISBN-006', 6),
                                               ('The Self-Help Guide', 'Dale Carnegie', 'ISBN-007', 7),
                                               ('The Ancient World', 'Herodotus', 'ISBN-008', 8),
                                               ('Fantasy Realms', 'J.R.R. Tolkien', 'ISBN-009', 4),
                                               ('The Hidden Truth', 'Malcolm Gladwell', 'ISBN-010', 2);
                                   """;
            stmt.executeUpdate(populateBooks);

            String populateBook_Copies = """
                                        INSERT INTO book_copies (book_id, copy_number, available) VALUES
                                        -- Books 1-10 (already in your query)
                                        (1, 1, TRUE), (1, 2, TRUE), (1, 3, TRUE), (1, 4, TRUE), (1, 5, TRUE),
                                        (2, 1, TRUE), (2, 2, TRUE), (2, 3, TRUE), (2, 4, TRUE), (2, 5, TRUE),
                                        (3, 1, TRUE), (3, 2, FALSE), (3, 3, TRUE), (3, 4, TRUE), (3, 5, FALSE),
                                        (4, 1, TRUE), (4, 2, TRUE), (4, 3, TRUE), (4, 4, FALSE), (4, 5, TRUE),
                                        (5, 1, TRUE), (5, 2, TRUE), (5, 3, FALSE), (5, 4, TRUE), (5, 5, TRUE),
                                        (6, 1, TRUE), (6, 2, TRUE), (6, 3, FALSE), (6, 4, TRUE), (6, 5, TRUE),
                                        (7, 1, TRUE), (7, 2, TRUE), (7, 3, FALSE), (7, 4, TRUE), (7, 5, TRUE),
                                        (8, 1, TRUE), (8, 2, TRUE), (8, 3, TRUE), (8, 4, FALSE), (8, 5, TRUE),
                                        (9, 1, TRUE), (9, 2, FALSE), (9, 3, TRUE), (9, 4, FALSE), (9, 5, TRUE),
                                        (10, 1, TRUE), (10, 2, TRUE), (10, 3, TRUE), (10, 4, TRUE), (10, 5, TRUE),
                                        
                                        -- Books 11-20
                                        (11, 1, TRUE), (11, 2, TRUE), (11, 3, TRUE), (11, 4, TRUE), (11, 5, TRUE),
                                        (12, 1, TRUE), (12, 2, TRUE), (12, 3, TRUE), (12, 4, TRUE), (12, 5, TRUE),
                                        (13, 1, TRUE), (13, 2, TRUE), (13, 3, TRUE), (13, 4, TRUE), (13, 5, TRUE),
                                        (14, 1, TRUE), (14, 2, TRUE), (14, 3, TRUE), (14, 4, TRUE), (14, 5, TRUE),
                                        (15, 1, TRUE), (15, 2, TRUE), (15, 3, TRUE), (15, 4, TRUE), (15, 5, TRUE),
                                        (16, 1, TRUE), (16, 2, TRUE), (16, 3, TRUE), (16, 4, TRUE), (16, 5, TRUE),
                                        (17, 1, TRUE), (17, 2, TRUE), (17, 3, TRUE), (17, 4, TRUE), (17, 5, TRUE),
                                        (18, 1, TRUE), (18, 2, TRUE), (18, 3, TRUE), (18, 4, TRUE), (18, 5, TRUE),
                                        (19, 1, TRUE), (19, 2, TRUE), (19, 3, TRUE), (19, 4, TRUE), (19, 5, TRUE),
                                        (20, 1, TRUE), (20, 2, TRUE), (20, 3, TRUE), (20, 4, TRUE), (20, 5, TRUE),
                                        
                                        -- Books 21-30
                                        (21, 1, TRUE), (21, 2, TRUE), (21, 3, TRUE), (21, 4, TRUE), (21, 5, TRUE),
                                        (22, 1, FALSE), (22, 2, FALSE), (22, 3, FALSE), (22, 4, FALSE), (22, 5, FALSE),
                                        (23, 1, TRUE), (23, 2, TRUE), (23, 3, TRUE), (23, 4, TRUE), (23, 5, TRUE),
                                        (24, 1, TRUE), (24, 2, TRUE), (24, 3, TRUE), (24, 4, TRUE), (24, 5, TRUE),
                                        (25, 1, TRUE), (25, 2, TRUE), (25, 3, TRUE), (25, 4, TRUE), (25, 5, TRUE),
                                        (26, 1, TRUE), (26, 2, TRUE), (26, 3, TRUE), (26, 4, TRUE), (26, 5, TRUE),
                                        (27, 1, TRUE), (27, 2, TRUE), (27, 3, TRUE), (27, 4, TRUE), (27, 5, TRUE),
                                        (28, 1, TRUE), (28, 2, TRUE), (28, 3, TRUE), (28, 4, TRUE), (28, 5, TRUE),
                                        (29, 1, FALSE), (29, 2, FALSE), (29, 3, FALSE), (29, 4, FALSE), (29, 5, FALSE),
                                        (30, 1, TRUE), (30, 2, TRUE), (30, 3, TRUE), (30, 4, TRUE), (30, 5, TRUE),
                                        
                                        -- Books 31 to 
                                        (31, 1, TRUE), (31, 2, TRUE), (31, 3, TRUE), (31, 4, TRUE), (31, 5, TRUE),
                                        (32, 1, TRUE), (32, 2, TRUE), (32, 3, TRUE), (32, 4, TRUE), (32, 5, TRUE),
                                        (33, 1, TRUE), (33, 2, TRUE), (33, 3, TRUE), (33, 4, TRUE), (33, 5, TRUE),
                                        (34, 1, TRUE), (34, 2, TRUE), (34, 1, TRUE), (34, 2, TRUE), (34, 3, TRUE),
                                        (35, 1, TRUE), (35, 2, TRUE), (35, 3, TRUE), (35, 4, TRUE), (35, 5, TRUE),
                                        (36, 1, TRUE), (36, 2, TRUE),(36, 3, TRUE), (36, 4, TRUE), (36, 5, TRUE),
                                        (37, 1, TRUE), (37, 2, TRUE), (37, 3, TRUE), (37, 4, TRUE), (37, 5, TRUE),
                                        (38, 1, TRUE), (38, 2, TRUE), (38, 3, TRUE), (38, 4, TRUE), (38, 5, TRUE);
                                        """;
            stmt.executeUpdate(populateBook_Copies);

            String populateStaff = """
                                   INSERT INTO staff (name, email, password_hash, role, shift_start, shift_end) VALUES
                                  \s
                                   -- Librarians                                                                                \s
                                   ('Sarah Johnson', 'sjohnson@library.org', 'LibraryPass123!', 'Librarian', '08:00:00', '16:00:00'),
                                   ('Michael Chen', 'mchen@library.org', 'BookLover456@', 'Librarian', '09:00:00', '17:00:00'),
                                   ('Emily Rodriguez', 'erodriguez@library.org', 'ReadingIsFun789#', 'Librarian', '10:00:00', '18:00:00'),
                                   ('David Wilson', 'dwilson@library.org', 'Catalog2025$', 'Librarian', '08:00:00', '16:00:00'),
                                   ('Jennifer Lee', 'jlee@library.org', 'ShelfOrder321!', 'Librarian', '12:00:00', '20:00:00'),
                                   ('Robert Kim', 'rkim@library.org', 'LibraryScience567@', 'Librarian', '09:00:00', '17:00:00'),
                                   ('Maria Gonzalez', 'mgonzalez@library.org', 'BookWorm890#', 'Librarian', '11:00:00', '19:00:00'),
                                   ('James Taylor', 'jtaylor@library.org', 'Reference432!', 'Librarian', '08:00:00', '16:00:00'),
                                   ('Sophia Brown', 'sbrown@library.org', 'ArchiveAccess654@', 'Librarian', '10:00:00', '18:00:00'),
                                   ('Daniel Patel', 'dpatel@library.org', 'Collection987#', 'Librarian', '13:00:00', '21:00:00'),
                                   ('Temp', 'temp', 'temp','Librarian', '08:00:00', '16:00:00'),
                                  \s
                                   -- Admins
                                  ('Harsh Jaiswal', '112315070@cse.iiitp.ac.in', 'Master_Password_070', 'Admin', null, null),
                                  ('Ishan Nanglot', '112315074@cse.iiitp.ac.in', 'Master_Password_074', 'Admin', null, null),
                                  ('Pratik Jaiswal', '112315076@cse.iiitp.ac.in', 'Master_Password_076', 'Admin', null, null),
                                  ('Jayata Roy', '112315077@cse.iiitp.ac.in', 'Master_Password_077', 'Admin', null, null),
                                  ('Administrator', 'admin', 'admin', 'Admin', null, null);
                                  \s""";
            stmt.executeUpdate(populateStaff);


            String populateTransactions = """
                    INSERT INTO transactions (user_id, book_copy_id, issue_date, due_date, return_date) VALUES
                                          -- User 1 transactions
                                          (1, 4, '2025-03-01', '2025-03-15', '2025-03-14'),  -- Returned on time
                                          (1, 12, '2025-03-20', '2025-04-03', NULL),  -- Currently checked out
                                         \s
                                          -- User 2 transactions
                                          (2, 7, '2025-02-15', '2025-03-01', '2025-03-05'),  -- Returned late
                                          (2, 18, '2025-03-10', '2025-03-24', '2025-03-22'), -- Returned on time
                                          (2, 141, '2025-03-10', '2025-03-24', NULL),  -- Overdue, not returned
                                          (2, 23, '2025-03-25', '2025-04-08', NULL),  -- Currently checked out
                                         \s
                                          -- User 3 transactions
                                          (3, 9, '2025-01-20', '2025-02-03', '2025-02-01'),  -- Returned on time
                                          (3, 15, '2025-03-05', '2025-03-19', NULL),  -- Overdue, not returned yet
                                          (3, 10, '2025-03-15', '2025-03-29', NULL),
                                          (3, 107, '2025-03-15', '2025-03-29', NULL), -- Overdue, not returned
                                         \s
                                          -- User 4 transactions
                                          (4, 22, '2025-02-10', '2025-02-24', '2025-02-20'), -- Returned on time
                                          (4, 106, '2025-03-01', '2025-03-15', NULL),  -- Currently checked out
                                          (4, 31, '2025-03-15', '2025-03-29', '2025-03-29'), -- Returned on time
                                          (4, 142, '2025-03-15', '2025-03-29', NULL),  -- Overdue, not returned
                                          (4, 42, '2025-03-30', '2025-04-13', NULL),  -- Currently checked out
                                         \s
                                          -- User 5 transactions
                                          (5, 5, '2025-02-05', '2025-02-19', '2025-03-01'),  -- Returned late
                                          (5, 28, '2025-03-10', '2025-03-24', NULL),  -- Overdue, not returned yet
                                          (5, 109, '2025-03-20', '2025-04-03', NULL),  -- Currently checked out
                                         \s
                                          -- User 6 transactions
                                          (6, 13, '2025-01-25', '2025-02-08', '2025-02-06'), -- Returned on time
                                          (6, 37, '2025-03-01', '2025-03-15', '2025-03-18'), -- Returned late
                                          (6, 44, '2025-03-20', '2025-04-03', NULL),  -- Currently checked out
                                          (6, 144, '2025-03-22', '2025-04-05', NULL),  -- Currently checked out
                                         \s
                                          -- User 7 transactions
                                          (7, 2, '2025-02-20', '2025-03-06', '2025-03-05'),  -- Returned on time
                                          (7, 19, '2025-03-15', '2025-03-29', NULL),  -- Overdue, not returned yet
                                          (7, 109, '2025-03-25', '2025-04-08', NULL),  -- Currently checked out
                                         \s
                                          -- User 8 transactions
                                          (8, 11, '2025-02-01', '2025-02-15', '2025-02-10'), -- Returned on time
                                          (8, 26, '2025-03-05', '2025-03-19', '2025-03-15'), -- Returned on time
                                          (8, 39, '2025-03-25', '2025-04-08', NULL),  -- Currently checked out
                                          (8, 144, '2025-03-25', '2025-04-08', NULL),  -- Currently checked out
                                         \s
                                          -- User 9 transactions
                                          (9, 3, '2025-01-15', '2025-01-29', '2025-02-10'),  -- Returned very late
                                          (9, 27, '2025-03-01', '2025-03-15', '2025-03-14'), -- Returned on time
                                          (9, 108, '2025-03-18', '2025-04-01', NULL),        -- Slightly overdue
                                         \s
                                          -- User 10 transactions
                                          (10, 16, '2025-02-15', '2025-03-01', '2025-02-28'), -- Returned on time
                                          (10, 33, '2025-03-10', '2025-03-24', NULL),  -- Overdue, not returned yet
                                          (10, 143, '2025-03-20', '2025-04-03', NULL); -- Currently checked out
                                         \s
                                         \s""";
            stmt.executeUpdate(populateTransactions);


            String populateGuest_book_usage = """
                                              INSERT INTO guest_book_usage (guest_id, book_id, start_time, end_time, fine_amt) VALUES
                                                  (1, 3, '2025-03-28 10:00:00', '2025-03-28 12:30:00', 0.00),
                                                  (2, 5, '2025-03-28 14:15:00', '2025-03-28 16:00:00', 10.50),
                                                  (3, 7, '2025-03-29 09:45:00', NULL, 0.00), -- Ongoing session
                                                  (4, 2, '2025-03-29 13:30:00', NULL, 0.00), -- Ongoing session
                                                  (5, 8, '2025-03-30 08:00:00', '2025-03-30 10:45:00', 5.75),
                                                  (6, 1, '2025-03-30 15:10:00', NULL, 0.00), -- Ongoing session
                                                  (7, 4, '2025-03-31 11:30:00', NULL, 0.00), -- Ongoing session
                                                  (8, 6, '2025-03-31 17:00:00', NULL, 0.00), -- Ongoing session
                                                  (9, 9, '2025-04-01 09:00:00', '2025-04-01 10:30:00', 0.00),
                                                  (10, 10, '2025-04-01 14:00:00', NULL, 0.00); -- Ongoing session
                                              """;
            stmt.executeUpdate(populateGuest_book_usage);

            conn.commit();

            System.out.println("Database populated!!!");


        } catch (SQLException e) {
            System.err.println("An error occurred while populating the database. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void del() {

        String DB_URL = "jdbc:mysql://localhost:3306/";
        String USER = "root";
        String PASSWORD = "Jayata_roy@5077";
        String DATABASE_NAME = "library_db";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Drop database if it exists
            String dropDB = "DROP DATABASE IF EXISTS " + DATABASE_NAME;
            stmt.executeUpdate(dropDB);

            // Create a new database
            String createDB = "CREATE DATABASE " + DATABASE_NAME;
            stmt.executeUpdate(createDB);

        } catch (SQLException e) {
            System.err.println("An error occurred while deleting the database. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
