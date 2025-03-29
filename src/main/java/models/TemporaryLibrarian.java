package models;

import java.time.LocalTime;
import java.time.LocalDate;

public class TemporaryLibrarian extends Librarian {
    private LocalDate contractEndDate;

    public TemporaryLibrarian(int id, String name, String email, String passwordHash, LocalTime shiftStart, LocalTime shiftEnd, LocalDate contractEndDate) {
        super(id, name, email, passwordHash, shiftStart, shiftEnd);
        this.contractEndDate = contractEndDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public boolean isContractExpired() {
        return LocalDate.now().isAfter(contractEndDate);
    }

    @Override
    public void manageBooks() {
        if (isContractExpired()) {
            System.out.println("Temporary librarian contract expired. Cannot manage books.");
        } else {
            super.manageBooks();
        }
    }
}