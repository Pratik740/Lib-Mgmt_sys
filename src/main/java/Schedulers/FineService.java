package Schedulers;

import java.sql.*;

import db.DatabaseManager;
import models.User;


public class FineService {

    public static void populateFineTable() {
        String selectTransactions = """
                                    select transactions.user_id as user_id, transactions.id as transaction_id, datediff(current_date, transactions.due_date)*10 as amount
                                    from transactions left join fines on transactions.id = fines.transaction_id
                                    where fines.id is null and transactions.due_date < current_date and transactions.return_date is null;
                                    """;

        String populationQuery = "insert into fines(user_id, transaction_id, amount, status) values(?,?,?,'Pending')";

        String updTransactions = """
                                 update transactions set fine_amount = datediff(return_date, issue_date)*10 where return_date = current_date;
                                 """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(selectTransactions);
             PreparedStatement stmt2 = conn.prepareStatement(populationQuery);
             PreparedStatement stmt3 = conn.prepareStatement(updTransactions);){

            ResultSet rs = stmt1.executeQuery();

            while (rs.next()) {
                stmt2.setInt(1, rs.getInt("user_id"));
                stmt2.setInt(2, rs.getInt("transaction_id"));
                stmt2.setDouble(3, rs.getDouble("amount"));
                stmt2.executeUpdate();
            }

            stmt3.executeUpdate();

            updateFinesTable();

        } catch (SQLException e) {
            System.err.println("User fine population failed! Exception: " + e.getMessage());
            e.printStackTrace();
        }

    }


    public static void updateFinesTable() {
        String fineUpdateQuery = """
                                 UPDATE fines
                                     JOIN (
                                         SELECT fines.transaction_id, DATEDIFF(CURRENT_DATE, t.due_date) AS diff_in_dates
                                         FROM (
                                                  SELECT distinct transactions.id AS transaction_id, transactions.due_date
                                                  FROM users
                                                           JOIN transactions ON users.id = transactions.user_id
                                                  WHERE transactions.due_date < CURRENT_DATE
                                                    AND transactions.return_date IS NULL
                                              ) AS t
                                                  JOIN fines ON t.transaction_id = fines.transaction_id
                                         WHERE fines.status = 'Pending'
                                     ) AS subquery
                                     ON fines.transaction_id = subquery.transaction_id
                                                 SET fines.amount = subquery.diff_in_dates * 10;
                                 """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(fineUpdateQuery)) {

            stmt1.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error occured when updating fines.");
            e.printStackTrace();
        }
    }

    public static boolean pendingFines(User user) {
        String pending = "select * from fines where user_id = ? and status = 'Pending'";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(pending)) {

            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error occured when selecting pending fines for user: " + user.getId());
            e.printStackTrace();
            return false;
        }
    }
}