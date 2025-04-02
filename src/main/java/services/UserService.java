package services;

import Schedulers.*;
import db.DatabaseManager;
import models.Transaction;
import models.User;
import models.Book;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class UserService extends PersonService {

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
            return null;
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



            if (rs.next()) {
                User user =  new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getDate("date_of_joining").toLocalDate()
                );

                FineService.populateFineTable();
                ReservationService.reserveToReqPopulate();

                return user;
            } else {
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
            while (rs.next()) {
                user.SetTransaction(new Transaction(rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_copy_id"),
                        rs.getDate("issue_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        null,
                        LocalDate.now().isAfter(rs.getDate("due_date").toLocalDate()) ? ((int) ChronoUnit.DAYS.between(rs.getDate("due_date").toLocalDate(), LocalDate.now()) * 10) : 0));
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

        String transactionFinder =""" 
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

            stmt2.


                    setInt(1, transactionId);
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
            System.out.println("Cannot delete u e still pending.");
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


    public static boolean requestBook(User user , int bookId) {

        for (Book book : user.getBooks_borrowed()) {
            if (book.getId() == bookId) {
                System.out.println("Cannot request book as user posses a copy already!.");
                return true;
            }
        }

        String fineCount = "select count(*) as count from fines where user_id = ?";
        String reqInsertQuery = "insert into book_requests(user_id, book_id, book_copy_id, status) values(?, ?, ?, ?)";
        String copyIdFinder = "select id from book_copies where book_id = ? AND available = true ";
        String reservationInsert = "insert into reservations(user_id,book_id,expected_availability) values(?,?,?) ";
        String oldestTransaction = "select due_date from book_copies as b INNER JOIN transactions as t ON b.id = t.book_copy_id WHERE b.book_id = ? ORDER BY due_date ASC LIMIT 1";
        String updateCopyAvailability = "update book_copies set available = false where id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement fineStmt = conn.prepareStatement(fineCount);
             PreparedStatement reqInsertStmt = conn.prepareStatement(reqInsertQuery);
             PreparedStatement availFind = conn.prepareStatement(copyIdFinder);
             PreparedStatement reservationStmt = conn.prepareStatement(reservationInsert);
             PreparedStatement stmt5 = conn.prepareStatement(oldestTransaction);
             PreparedStatement updateCopyStmt = conn.prepareStatement(updateCopyAvailability)) {

            fineStmt.setInt(1, user.getId());
            ResultSet rs1 = fineStmt.executeQuery();
            if (rs1.next()) {
                if (rs1.getInt("count") > 3) {
                    System.out.println("Cannot request book as user has more than 3 pending fines!.");
                    reqInsertStmt.setInt(1, user.getId());
                    reqInsertStmt.setInt(2, bookId);
                    reqInsertStmt.setNull(3, Types.INTEGER);
                    reqInsertStmt.setString(4, "Rejected");
                    reqInsertStmt.executeUpdate();
                    return true;
                }
            }

            //Find available copy
            availFind.setInt(1, bookId);
            ResultSet availCopy = availFind.executeQuery();

            if(availCopy.next()) {
                System.out.println("Your request has been processed in the Book Requests!!!");
                reqInsertStmt.setInt(1, user.getId());
                reqInsertStmt.setInt(2, bookId);
                reqInsertStmt.setInt(3, availCopy.getInt("id"));
                reqInsertStmt.setString(4, "Pending");
                reqInsertStmt.executeUpdate();

                updateCopyStmt.setInt(1, availCopy.getInt("id"));
                updateCopyStmt.executeUpdate();

                return true;
            } else {
                //Since book is unavailable we find the expected availability date by the earliest approaching due date
                stmt5.setInt(1, bookId);
                ResultSet rs5 = stmt5.executeQuery();

                System.out.println("Oops!!! We are fresh out of that book copies.");
                rs5.next();
                reservationStmt.setInt(1,user.getId());
                reservationStmt.setInt(2,bookId);
                reservationStmt.setDate(3,rs5.getDate("due_date"));
                reservationStmt.executeUpdate();

                return true;
            }
        } catch (SQLException e) {
            System.err.println("User not able to request book: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }



    public static void viewBookRequests(User user){
        String fromBookRequest = "select * from book_requests where user_id = ?";
        String fromReservations = "select * from reservations where user_id = ?";
        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(fromBookRequest);
            PreparedStatement stmt1 = conn.prepareStatement(fromReservations)){
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            System.out.println("Book Requests waiting for approvals : ");
            System.out.printf("%-5s %-10s %-15s %-10s%n", "ID", "Book ID", "Request Date", "Status");
            System.out.println("--------------------------------------------------");
            while(rs.next()){
                System.out.printf("%-5d %-10d %-15s %-10s%n",
                        rs.getInt("id"),
                        rs.getInt("book_id"),
                        rs.getDate("request_date"),
                        rs.getString("status"));
            }
            stmt1.setInt(1, user.getId());
            ResultSet rs1 = stmt1.executeQuery();
            if(rs1.next()){
                System.out.println("Books that you wanted to borrow but are currently unavailable: -");
                System.out.printf("%-5s %-10s %-25s%n", "ID", "Book ID", "Expected Availability");
                System.out.println("--------------------------------------------------");
                do{
                    System.out.printf("%-5d %-10d %-25s%n",
                            rs1.getInt("id"),
                            rs1.getInt("book_id"),
                            rs1.getDate("expected_availability")
                    );
                }while(rs1.next());
            }

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void CancelBookRequest(User user, int requestId){
        String delRequest = "delete from book_requests where user_id = ? AND id = ?";
        String copyId = "select book_copy_id from book_requests where user_id = ? and  id = ?";
        String updateAvail = "update book_copies set available = true where id = ?";

        try(Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(delRequest);
            PreparedStatement copyIdFinderStmt = conn.prepareStatement(copyId);
            PreparedStatement deallocCopy = conn.prepareStatement(updateAvail)) {

            copyIdFinderStmt.setInt(1, user.getId());
            copyIdFinderStmt.setInt(2, requestId);

            ResultSet rs = copyIdFinderStmt.executeQuery();

            markBookAsAvailable(rs.getInt(1));

            deallocCopy.setInt(1, rs.getInt(1));
            deallocCopy.executeQuery();


            stmt.setInt(1, user.getId());
            stmt.setInt(2, requestId);
            int rowsAffected = stmt.executeUpdate(); // Check how many rows were deleted
            if (rowsAffected > 0) {
                System.out.println("Book request with ID " + requestId + " has been successfully cancelled.");
            } else {
                System.out.println("No book request found with ID " + requestId + " for this user.");
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static void CancelReservationRequest(User user, int requestId){
        String delRequest = "delete from reservations where user_id = ? AND id = ?";
        try(Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(delRequest)){
            stmt.setInt(1, user.getId());
            stmt.setInt(2,requestId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Book request with ID " + requestId + " has been successfully removed from the reservations.");
            } else {
                System.out.println("No book request found with ID " + requestId + " for this user.");
            }
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static void markBookAsAvailable(int bookCopyId) {
        String updateQuery = "UPDATE book_copies SET available = true WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
            stmt.setInt(1, bookCopyId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to update book availability: " + e.getMessage());
            e.printStackTrace();
        }
    }

}