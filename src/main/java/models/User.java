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

    //public ArrayList<Book> getBooks_borrowed() {}

    @Override
    public String toString() {
        return super.toString() + ", Date of Joining: " + dateOfJoining;
    }
}
