package models;

public class Admin extends Staff {
    public Admin(int id, String name, String email, String passwordHash) {
        super(id, name, email, passwordHash, "Admin", null, null);
    }

    public void hireLibrarian() {
        System.out.println("Hiring a librarian...");
    }
}
