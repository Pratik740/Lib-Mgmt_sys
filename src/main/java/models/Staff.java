package models;

import java.time.LocalTime;

public class Staff extends Person {
    protected String role;
    protected LocalTime shiftStart;
    protected LocalTime shiftEnd;

    public Staff(int id, String name, String email, String passwordHash, String role, LocalTime shiftStart, LocalTime shiftEnd) {
        super(id, name, email, passwordHash);
        this.role = role;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
    }
    public String getRole() { return role; }
    public LocalTime getShiftStart() { return shiftStart; }
    public LocalTime getShiftEnd() { return shiftEnd; }
}
