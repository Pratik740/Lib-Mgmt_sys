package models;

import java.time.LocalDate;
import java.util.ArrayList;
import models.Book;
import models.Transaction;

public class User extends Person {
    private LocalDate dateOfJoining;
    private ArrayList<Book> books_borrowed = new ArrayList<Book>();
    private ArrayList<Transaction> transactions = new ArrayList<Transaction>();

    public User(int id, String name, String email, String passwordHash, LocalDate dateOfJoining) {
        super(id, name, email, passwordHash);
        this.dateOfJoining = dateOfJoining;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDate dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public void displayBorrowedBooks() {
        for (Book book : books_borrowed) {
            System.out.println(book);
        }
    }

    public void borrowingTransactions() {
        for (Transaction transaction : transactions) {
            System.out.println(transaction);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ", Date of Joining: " + dateOfJoining;
    }

    public void SetTransaction(Transaction transaction) {
        transactions.add(transaction);
    }
    public void SetBook(Book book) {
        books_borrowed.add(book);
    }
    public double ComputeFine(){
        double sum = 0;
        for(Transaction t: transactions){
            sum += t.getfineAmount();
        }
        return sum;
    }
    public void removeBook(int bookId) {
        for(Book b: books_borrowed){
            if(b.getId() == bookId){
                books_borrowed.remove(b);
                break;
            }
        }
    }
    public void UpdateTransaction(int transactionId) {
        for(Transaction t: transactions){
            if(t.getId() == transactionId){
                t.setReturnDate(LocalDate.now());
                break;
            }
        }
    }

    //User selects book_copy_id to
    public int helper1()

}
