package models;

import java.util.ArrayList;
import models.Book;
import services.GuestService;

public class Guest {
    private int id;
    private String name;
    private String contact;
    private ArrayList<Book> currently_reading_books = new ArrayList<Book>();

    public Guest(int id, String name, String contact) {
        this.id = id;
        this.name = name;
        this.contact = contact;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getContact() { return contact; }

    public void displayBooks() {
        if (!this.currently_reading_books.isEmpty()) {

            System.out.println(this.name + " has the following books currently: -\n");
            for (Book book : this.currently_reading_books) {
                System.out.println(book);
            }
        }
        else {
            System.out.println(this.name + " is not reading any books currently.");
        }
    }

    public void addBook(Book book) {
        this.currently_reading_books.add(book);
    }

    public void removeBook(int bookID) {
        int i = 0;
        while (i < this.currently_reading_books.size()) {
            if (this.currently_reading_books.get(i).getId() == bookID) {
                this.currently_reading_books.remove(i);
                break;
            }
            i += 1;
        }
    }
}