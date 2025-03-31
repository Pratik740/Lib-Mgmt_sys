package models;

import java.util.ArrayList;
import models.Book;

public class Guest {
    private String name;
    private String contact;
    private ArrayList<Book> currently_reading_books = new ArrayList<Book>();

    public Guest(String name, String contact) {
        this.name = name;
        this.contact = contact;
    }

    public String getName() { return name; }
    public String getContact() { return contact; }
}