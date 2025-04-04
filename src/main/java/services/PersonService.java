package services;

import db.DatabaseManager;
import models.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonService {

    // View all available genres
    public static void viewGenres() {
        String query = "SELECT name FROM genres";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            System.out.println("\n====================================");
            System.out.println("|       📚 Available Genres       |");
            System.out.println("====================================");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("| %2d. %-25s |\n", count, rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // View available books by genre using Book model
    public static List<Book> getBooksByGenre(String genreName) {
        String query = """
            SELECT distinct b.id, b.title, b.author, b.isbn, b.genre_id
            FROM books b
            JOIN genres g ON b.genre_id = g.id
            WHERE g.name = ?
        """;

        List<Book> books = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, genreName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                books.add(new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getInt("genre_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    // Display books retrieved by getBooksByGenre
    public static void viewBooksByGenre(String genreName) {
        List<Book> books = getBooksByGenre(genreName);
        System.out.printf("Available Books in Genre '%s':%n", genreName);
        for (Book book : books) {
            System.out.printf("%s. %s by %s (ISBN: %s)%n",book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn());
        }
    }

}
