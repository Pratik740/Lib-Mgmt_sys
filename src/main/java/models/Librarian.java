package models;

import java.time.LocalTime;

public class Librarian extends Staff {
    public Librarian(int id, String name, String email, String passwordHash, LocalTime shiftStart, LocalTime shiftEnd) {
        super(id, name, email, passwordHash, "Librarian", shiftStart, shiftEnd);
    }

    public void manageBooks() {
        System.out.println("Managing books...");
    }
}