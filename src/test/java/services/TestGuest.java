//package services;
//
//import services.GuestService;
//import java.util.Scanner;
//
//public class TestGuest {
//    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//        GuestService guestService = new GuestService();
//
//        int guestId = -1;
//        boolean sessionActive = false;
//
//        while (true) {
//            System.out.println("\n--- Guest Service Test CLI ---");
//            System.out.println("1. Register Guest");
//            System.out.println("2. Borrow a Book (Simulated)");
//            System.out.println("3. Return a Book");
//            System.out.println("4. Check Fine Amount");
//            System.out.println("5. Logout Guest");
//            System.out.println("6. Exit");
//            System.out.print("Choose an option: ");
//
//            int choice = scanner.nextInt();
//            scanner.nextLine(); // Consume newline
//
//            switch (choice) {
//                case 1 -> {
//                    System.out.print("Enter Guest Name: ");
//                    String name = scanner.nextLine();
//                    System.out.print("Enter Contact: ");
//                    String contact = scanner.nextLine();
//
//                    if (guestService.startSession(name, contact) != null) {
//                        System.out.println("Guest session started successfully.");
//                        sessionActive = true;
//                    } else {
//                        System.out.println("Failed to start guest session.");
//                    }
//                }
//                case 2 -> {
//                    if (!sessionActive) {
//                        System.out.println("Start a guest session first!");
//                        continue;
//                    }
//                    System.out.print("Enter Book ID to borrow: ");
//                    int bookId = scanner.nextInt();
//                    scanner.nextLine();
//
//                    // Simulate borrowing by inserting into `guest_book_usage`
//                    String borrowQuery = "INSERT INTO guest_book_usage (guest_id, book_id, start_time) VALUES (?, ?, NOW())";
//                    try (var conn = db.DatabaseManager.getConnection();
//                         var stmt = conn.prepareStatement(borrowQuery)) {
//                        stmt.setInt(1, guestId);
//                        stmt.setInt(2, bookId);
//                        if (stmt.executeUpdate() > 0) {
//                            System.out.println("Book borrowed successfully.");
//                        } else {
//                            System.out.println("Failed to borrow book.");
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                case 3 -> {
//                    if (!sessionActive) {
//                        System.out.println("Start a guest session first!");
//                        continue;
//                    }
//                    System.out.print("Enter Book ID to return: ");
//                    int bookId = scanner.nextInt();
//                    scanner.nextLine();
//
//                    if (guestService.returnBook(guestId, bookId)) {
//                        System.out.println("Book returned successfully.");
//                    } else {
//                        System.out.println("Failed to return book.");
//                    }
//                }
//                case 4 -> {
//                    if (!sessionActive) {
//                        System.out.println("Start a guest session first!");
//                        continue;
//                    }
//                    double fine = guestService.calculateFine(guestId);
//                    System.out.println("Total Fine Amount: ₹" + fine);
//                }
//                case 5 -> {
//                    if (!sessionActive) {
//                        System.out.println("No active session to logout.");
//                        continue;
//                    }
//                    guestService.logoutGuest(guestId);
//                    sessionActive = false;
//                    guestId = -1;
//                }
//                case 6 -> {
//                    System.out.println("Exiting Guest Service Test...");
//                    scanner.close();
//                    return;
//                }
//                default -> System.out.println("Invalid option. Try again.");
//            }
//        }
//    }
//}
