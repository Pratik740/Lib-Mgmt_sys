package models;

public class Book {
    private int id;
    private String title;
    private String author;
    private String isbn;
    private int genreId;

    public Book(int id, String title, String author, String isbn, int genreId) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genreId = genreId;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getGenreId() { return genreId; }

    @Override
    public String toString() {
        return String.format("[Book ID: %d] \"%s\" by %s | ISBN: %s | Genre ID: %d", id, title, author, isbn, genreId);
    }



}