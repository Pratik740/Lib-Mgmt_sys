package models;

public class User extends Person {
    private boolean isSubscriptionUser; // Subscription or Guest

    public User(int id, String name, String email, String passwordHash, boolean isSubscriptionUser) {
        super(id, name, email, passwordHash);
        this.isSubscriptionUser = isSubscriptionUser;
    }

    public boolean isSubscriptionUser() { return isSubscriptionUser; }
}
