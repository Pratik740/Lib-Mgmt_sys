package models;

import java.time.LocalDate;

public class Transaction {
    private int id;
    private int userId;
    private int bookCopyId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;

    public Transaction(int id, int userId, int bookCopyId, LocalDate issueDate, LocalDate dueDate, LocalDate returnDate, double fineAmount) {
        this.id = id;
        this.userId = userId;
        this.bookCopyId = bookCopyId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
    }

    public boolean isOverdue() {
        return returnDate == null && LocalDate.now().isAfter(dueDate);
    }
}
