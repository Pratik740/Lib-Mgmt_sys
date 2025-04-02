package Schedulers;


import db.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;


public class ReservationService {
    public static void reserveToReqPopulate() {
        String reserveTraversalQuery = "select user_id, book_id, id, date(expected_availability) from reservations";
        String availFinder = "select id from book_copies where book_id = ? and available = true limit 1";
        String fineCounter = "select count(*) as count from fines where user_id = ?";
        String appendRequest = "insert into book_requests(user_id, book_id, book_copy_id, status) values(?, ?, ?, ?)";
        String reservationDelQuery = "delete from reservations where id = ?";
        String availNotFound = "update reservations set expected_availability = date_add(current_timestamp, INTERVAL FLOOR(RAND()*14) + 1 DAY) where id = ?";
        String messageToUser = "insert into messages(user_id, description)  value(?, ?)";
        String bookNameQuery = "select title from books where id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement resTrav = conn.prepareStatement(reserveTraversalQuery);
             PreparedStatement availFind = conn.prepareStatement(availFinder);
             PreparedStatement fineCount = conn.prepareStatement(fineCounter);
             PreparedStatement appendReq = conn.prepareStatement(appendRequest);
             PreparedStatement reservationDel = conn.prepareStatement(reservationDelQuery);
             PreparedStatement availNF = conn.prepareStatement(availNotFound);
             PreparedStatement text = conn.prepareStatement(messageToUser);
             PreparedStatement bookName = conn.prepareStatement(bookNameQuery)) {

            ResultSet res = resTrav.executeQuery();

            if (res.next()) {
                bookName.setString(1, res.getString(2));
                ResultSet book = bookName.executeQuery();

                do {
                    //More than 3 overdue currently hence user cannot borrow this book
                    fineCount.setInt(1, res.getInt(1));

                    ResultSet fine = fineCount.executeQuery();

                    if (fine.getInt(1) > 3) {

                        appendReq.setInt(1, res.getInt(1));
                        appendReq.setInt(2, res.getInt(2));
                        appendReq.setNull(3, Types.INTEGER);
                        appendReq.setString(4, "Rejected");

                        appendReq.execute();

                        text.setInt(1, res.getInt(1));
                        text.setString(2, "Borrow request for book " + book.getString(1) +
                                " has been rejected due to more than 3 outstanding fines.");
                    } else {

                        availFind.setInt(1, res.getInt(2));
                        ResultSet avail = availFind.executeQuery();
                        if (avail.next()) {

                            //If book is available.

                            appendReq.setInt(1, res.getInt(1));
                            appendReq.setInt(2, res.getInt(2));
                            appendReq.setInt(3, avail.getInt(1));
                            appendReq.setString(4, "Pending");

                            appendReq.execute();

                            reservationDel.setInt(1, res.getInt(3));
                            reservationDel.executeUpdate();


                            text.setInt(1, res.getInt(1));
                            text.setString(2, "Borrow request for book " + book.getString(1) +
                                    " is awaititing on approval now as requested book is now available.");
                        } else {

                            if (LocalDate.now().isAfter(res.getDate(4).toLocalDate())) {

                                //If book isn't available but the expected date of availability we had given to user has passed.

                                availNF.setInt(1, res.getInt(3));
                                availNF.executeUpdate();
                            }

                        }
                    }
                } while (res.next());
            }

        } catch (SQLException e) {
            System.err.println("Reservation table traversal and Book - Request population failed, Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}