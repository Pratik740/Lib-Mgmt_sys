package models;

import java.time.LocalDate;

public class User extends Person {
    private LocalDate dateOfJoining;

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

    @Override
    public String toString() {
        return super.toString() + ", Date of Joining: " + dateOfJoining;
    }
}
