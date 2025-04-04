package cli;

import java.util.Scanner;

import models.*;
import services.GuestService;
import services.PersonService;
import services.AdminService;
import services.LibrarianService;
import services.UserService;


public class LibraryCLI {
    public static void Guest(){
        Scanner sc = new Scanner(System.in);

        System.out.println("=====================================");
        System.out.println("|       🔒 Secure Login Portal      |");
        System.out.println("=====================================");
        System.out.println("👉 Enter your name: ");
        String name = sc.nextLine();
        System.out.print("👉 Enter your contact number: ");
        String contactNo = sc.nextLine();
        System.out.print("\n✅ Login successful! Welcome, " + name + ".\n\n\n");
        Guest guest = GuestService.guestLogin(name,contactNo);
        int choice ;
        do{
            System.out.println("=================================");
            System.out.println("|         📖 Guest Menu         |");
            System.out.println("=================================");
            System.out.println("| 1️⃣  View Books               |");
            System.out.println("| 2️⃣  Return Book              |");
            System.out.println("| 3️⃣  Currently Reading Books  |");
            System.out.println("| 4️⃣  Log Out                  |");
            System.out.println("=================================");
            System.out.print("👉 Enter your choice: ");
            choice = Integer.parseInt(sc.nextLine());
            switch (choice) {
                case 1:
                    PersonService.viewGenres();
                    System.out.print("\n\n\n👉 Enter the genre name you feel like reading about: ");
                    String genreName = sc.nextLine();
                    System.out.println(genreName);
                    PersonService.viewBooksByGenre(genreName);
                    System.out.print("👉 Enter book Id of the book you wish to read: ");
                    int bookId = Integer.parseInt(sc.nextLine());
                    System.out.println("\n\n✅ Book successfully added!\n\n");
                    GuestService.startReading(guest,bookId);
                    break;

                case 2:
                    System.out.println("\n\nAll the books owed by you is as follows: (Enter the book - id of the one you wish to return)");
                    if (guest.displayBooks()) {
                        int bookID = Integer.parseInt(sc.nextLine());
                        GuestService.returnBook(guest,bookID);
                    } else {
                        System.out.println("👉 Enter your choice: ");
                    }

                    break;

                case 3:
                    guest.displayBooks();
                    break;
                case 4:
                    GuestService.logoutGuest(guest);
                    return;

                default:
                    System.err.println("\n\nInvalid choice, try again.\n\n\n");
                    break;
            }

        }while(choice != 4);
    }

    public static void SubscriptionUser(){

        Scanner sc = new Scanner(System.in);
        User user = null;

        // Authentication loop
        while (user == null) {
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("        LIBRARY USER SYSTEM        ");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("  [1] Login");
            System.out.println("  [2] Register");
            System.out.println("  [3] Exit");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.print("➤ Enter your choice: ");
            int authChoice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (authChoice) {
                case 1:
                    System.out.print("👉 Enter email: ");
                    String email = sc.nextLine();
                    System.out.print("👉 Enter password: ");
                    String password = sc.nextLine();
                    user = UserService.loginUser(email, password);
                    if (user != null) {
                        UserService.getListOfUnfulfilledTransactions(user);
                        UserService.getListOfBorrowedBooks(user);
                    }
                    break;

                case 2:
                    System.out.print("👉 Enter name: ");
                    String name = sc.nextLine();
                    System.out.print("👉 Enter email: ");
                    String newEmail = sc.nextLine();
                    System.out.print("👉 Enter password: ");
                    String newPassword = sc.nextLine();
                    user = UserService.registerUser(name, newEmail, newPassword);
                    if (user != null) {
                        UserService.getListOfUnfulfilledTransactions(user);
                        UserService.getListOfBorrowedBooks(user);
                    }
                    break;

                case 3:
                    return;

                default:
                    System.err.println("\n\nInvalid choice, try again.\n\n\n");
            }
        }

        // Main menu loop
        int choice;
        do {
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("           USER MENU         ");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("  [1] View Borrowed Books");
            System.out.println("  [2] Total Fine owed to Library");
            System.out.println("  [3] Return a Book");
            System.out.println("  [4] Request a Book");
            System.out.println("  [5] View My Requests/Reservations");
            System.out.println("  [6] Cancel a Book Request");
            System.out.println("  [7] Cancel a Reservation");
            System.out.println("  [8] Delete Account");
            System.out.println("  [9] Logout");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.print("➤ Enter your choice: ");
            choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    if (!user.getBooks_borrowed().isEmpty()) {
                        System.out.println("\n--- Borrowed Books ---");
                        for (Book book : user.getBooks_borrowed()) {
                            System.out.println(book);
                        }
                    } else {
                        System.out.println("\n\n\n--- No Books Borrowed ---\n\n\n");
                    }
                    break;
                case 2:
                    System.out.println("Total amount owed by you to the library: " + user.ComputeFine());
                    break;
                case 3:
                    if (user.getBooks_borrowed().isEmpty()) {
                        System.out.println("\n\n\n--- No Borrowed Books ---\n\n\n");
                        for (Book book : user.getBooks_borrowed()) {
                            System.out.println(book);
                        }
                    } else {
                        System.out.println("\n--- Return Book ---");
                        System.out.print("➤ Enter Book ID to return: ");
                        int returnId = sc.nextInt();
                        if (UserService.returnBook(user, returnId)) {
                            System.out.println("Book returned successfully!");
                        } else {
                            System.out.println("Return failed. Please try again.");
                        }
                    }
                    break;

                case 4:
                    System.out.println("\n--- Request Book ---");
                    System.out.println("View Books by genre");
                    PersonService.viewGenres();
                    System.out.println("➤ Enter the genre name: ");
                    String genreName = sc.nextLine();
                    System.out.println(genreName);
                    PersonService.viewBooksByGenre(genreName);
                    System.out.println("➤ Enter book Id of the book you wish to read");
                    int bookId = Integer.parseInt(sc.nextLine());
                    if (UserService.requestBook(user, bookId)) {
                        System.out.println("Request submitted successfully!");
                    }
                    break;

                case 5:
                    System.out.println("\n--- My Requests ---");
                    UserService.viewBookRequests(user);
                    break;

                case 6:
                    UserService.viewBookRequests(user);
                    System.out.print("➤ Enter Request ID to cancel: ");
                    int reqId = sc.nextInt();
                    UserService.CancelBookRequest(user, reqId);
                    break;

                case 7:
                    UserService.viewBookRequests(user);
                    System.out.print("➤ Enter Reservation ID to cancel: ");
                    int resId = sc.nextInt();
                    UserService.CancelReservationRequest(user, resId);
                    break;

                case 8:
                    if (UserService.deleteUser(user)) {
                        System.out.println("Account deleted successfully!");
                        return;
                    } else {
                        System.out.println("Cannot delete account. Check for pending books/fines.");
                    }
                    break;

                case 9:
                    System.out.println("\n\nLogging out...\n\n");
                    return;

                default:
                    System.err.println("\n\nInvalid choice, try again.\n\n\n");
            }
        } while (choice!=9);
}


    public static void Librarian() {
        Scanner sc = new Scanner(System.in);
        Librarian librarian = null;

        // Login
        while (librarian == null) {
            System.out.println("\n=== Librarian Login ===");
            System.out.print("Enter email: ");
            String email = sc.nextLine();
            System.out.print("Enter password: ");
            String password = sc.nextLine();
            librarian = LibrarianService.loginLibrarian(email, password);
        }

        int choice;

        do {
            System.out.println("\n=== Librarian Menu ===");
            System.out.println("1. View Pending Fines");
            System.out.println("2. View Available Book Copies");
            System.out.println("3. Approve Pending Requests");
            System.out.println("4. View Guest Details");
            System.out.println("5. View Guest Reading History");
            System.out.println("6. Check User Overdue Books");
            System.out.println("7. View Today's Returns");
            System.out.println("8. View My Profile");
            System.out.println("9. View Pending Approvals");
            System.out.println("10. View User by id");
            System.out.println("11. Logout");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    System.out.println("\n--- Pending Fines ---");
                    LibrarianService.viewPendingFineDetails(librarian);
                    break;

                case 2:
                    System.out.println("\n--- Available Books ---");
                    LibrarianService.viewAvailableBookCopies(librarian);
                    break;

                case 3:
                    System.out.println("\n--- Approve Requests ---");
                    LibrarianService.provideApprovals(librarian);
                    break;

                case 4:
                    System.out.println("\n--- Guest Details ---");
                    LibrarianService.guestDetails(librarian);
                    break;

                case 5:
                    System.out.println("\n--- Guest Reading History ---");
                    LibrarianService.guestBookDetails(librarian);
                    break;

                case 6:
                    System.out.print("\nEnter User ID to check overdue books: ");
                    int userId = sc.nextInt();
                    LibrarianService.overdueBooksOfUser(librarian, userId);
                    break;

                case 7:
                    System.out.println("\n--- Today's Returns ---");
                    LibrarianService.returnedBooksOnCurrentDate(librarian);
                    break;

                case 8:
                    System.out.println("\n--- My Profile ---");
                    LibrarianService.getMyProfile(librarian);
                    break;

                case 9:
                    System.out.println("\n--- Pending Approvals ---");
                    LibrarianService.viewPendingApprovals(librarian);
                    break;

                case 10:
                    System.out.println("\n--- User by id ---");
                    System.out.println("Enter User ID to be viewed: ");
                    int currentUser = Integer.parseInt(sc.nextLine());
                    LibrarianService.checkoutUser(librarian, currentUser);
                    break;

                case 11:
                    System.out.println("Logging out...");
                    return;

                default:
                    System.out.println("Invalid choice!");
            }
        } while (choice != 11);
    }
    public static void admin() {
        Scanner sc = new Scanner(System.in);
        System.out.println("\nAdmin Login");
        System.out.print("Enter email: ");
        String email = sc.next();
        System.out.print("Enter password: ");
        String password = sc.next();

        Admin admin = AdminService.adminLogin(email, password);
        if (admin == null) {
            System.out.println("Login failed. Returning to main menu.");
            return;
        }

        int adminChoice;
        do {
            System.out.println("\nAdmin Menu");
            System.out.println("1. View Staff Details");
            System.out.println("2. Remove Staff");
            System.out.println("3. View Audit Log");
            System.out.println("4. View User Details");
            System.out.println("5. View Pending Fines");
            System.out.println("6. View available Book Copies");
            System.out.println("7. Approve Pending Requests");
            System.out.println("8. View Guest Details");
            System.out.println("9. View Guest Reading History");
            System.out.println("10. Check User Overdue Books");
            System.out.println("11. View Today's Returns");
            System.out.println("12. View My Profile");
            System.out.println("13. View pending approvals");
            System.out.println("14. View User by Id");
            System.out.println("15. Logout");
            System.out.print("Enter your choice: ");
            adminChoice = sc.nextInt();
            sc.nextLine();

            switch(adminChoice) {
                case 1:
                    AdminService.viewStaff(admin);
                    break;
                case 2:
                    System.out.print("Enter Staff ID to remove: ");
                    int staffId = sc.nextInt();
                    sc.nextLine();
                    AdminService.removeStaff(admin, staffId);
                    break;
                case 3:
                    AdminService.viewAuditLog(admin);
                    break;
                case 4:
                    AdminService.viewUserDetails(admin);
                    break;
                case 5:
                    System.out.println("\n--- Pending Fines ---");
                    LibrarianService.viewPendingFineDetails(admin);
                    break;

                case 6:
                    System.out.println("\n--- Available Books ---");
                    LibrarianService.viewAvailableBookCopies(admin);
                    break;

                case 7:
                    System.out.println("\n--- Approve Requests ---");
                    LibrarianService.provideApprovals(admin);
                    break;

                case 8:
                    System.out.println("\n--- Guest Details ---");
                    LibrarianService.guestDetails(admin);
                    break;

                case 9:
                    System.out.println("\n--- Guest Reading History ---");
                    LibrarianService.guestBookDetails(admin);
                    break;

                case 10:
                    System.out.print("\nEnter User ID to check overdue books: ");
                    int userId = sc.nextInt();
                    sc.nextLine();
                    LibrarianService.overdueBooksOfUser(admin, userId);
                    break;

                case 11:
                    System.out.println("\n--- Today's Returns ---");
                    LibrarianService.returnedBooksOnCurrentDate(admin);
                    break;

                case 12:
                    System.out.println("\n--- My Profile ---");
                    LibrarianService.getMyProfile(admin);
                    break;

                case 13:
                    System.out.println("\n--- Pending Approvals ---");
                    LibrarianService.viewPendingApprovals(admin);
                    break;

                case 14:
                    System.out.println("\n--- User by id ---");
                    System.out.println("Enter User ID to be viewed: ");
                    int currentUser = Integer.parseInt(sc.nextLine());
                    LibrarianService.checkoutUser(admin, currentUser);
                    break;

                case 15:
                    System.out.println("Logging out...");
                    break;
                default:
                    System.err.println("\n\nInvalid choice, try again.\n\n\n");
            }
        } while(adminChoice != 15);
    }



    public static void cliMaster() {
        String asciiArt = """
             .----------------.  .----------------.  .----------------.  .----------------.  .----------------.  .----------------.  .----------------. 
            | .--------------. || .--------------. || .--------------. || .--------------. || .--------------. || .--------------. || .--------------. |
            | |   _____      | || |     _____    | || |   ______     | || |  _______     | || |      __      | || |  _______     | || |  ____  ____  | |
            | |  |_   _|     | || |    |_   _|   | || |  |_   _ \\    | || | |_   __ \\    | || |     /  \\     | || | |_   __ \\    | || | |_  _||_  _| | |
            | |    | |       | || |      | |     | || |    | |_) |   | || |   | |__) |   | || |    / /\\ \\    | || |   | |__) |   | || |   \\ \\  / /   | |
            | |    | |   _   | || |      | |     | || |    |  __'.   | || |   |  __ /    | || |   / ____ \\   | || |   |  __ /    | || |    \\ \\/ /    | |
            | |   _| |__/ |  | || |     _| |_    | || |   _| |__) |  | || |  _| |  \\ \\_  | || | _/ /    \\ \\_ | || |  _| |  \\ \\_  | || |    _|  |_    | |
            | |  |________|  | || |    |_____|   | || |  |_______/   | || | |____| |___| | || ||____|  |____|| || | |____| |___| | || |   |______|   | |
            | |              | || |              | || |              | || |              | || |              | || |              | || |              | |
            | '--------------' || '--------------' || '--------------' || '--------------' || '--------------' || '--------------' || '--------------' |
             '----------------'  '----------------'  '----------------'  '----------------'  '----------------'  '----------------'  '----------------'  
        """;

        System.out.println(asciiArt);
        Scanner sc = new Scanner(System.in);
        int choice = 0;
        do{
            System.out.println("=======================================");
            System.out.println("|          ❓ Who are you? ❓          |");
            System.out.println("=======================================");
            System.out.println("|  Option  |        Role              |");
            System.out.println("|-------------------------------------|");
            System.out.println("|    1     |      🛠  Admin           |");
            System.out.println("|    2     |      📚  Librarian       |");
            System.out.println("|    3     |      👤  User            |");
            System.out.println("|    4     |      ❌  Exit            |");
            System.out.println("=======================================");
            System.out.print("👉 Enter your choice: ");
            choice = sc.nextInt();
            switch(choice){
                case 1:
                    admin();
                    break;
                case 2:
                    Librarian();
                    break;
                case 3:
                    int userChoice;
                    do {
                        System.out.println("==============================");
                        System.out.println("|       Select User Type     |");
                        System.out.println("==============================");
                        System.out.println("| 1️⃣  Guest                 |");
                        System.out.println("| 2️⃣  Subscription User      |");
                        System.out.println("==============================");
                        System.out.println("| 3️⃣  Exit                  |");
                        System.out.println("==============================");
                        System.out.print("👉 Enter your choice: ");
                        userChoice = sc.nextInt();
                        sc.nextLine();
                        switch (userChoice) {
                            case 1:
                                Guest();
                                break;
                            case 2:
                                SubscriptionUser();
                                break;
                            case 3:
                                System.out.println("Exiting User Menu...");
                                break;
                            default:
                                System.err.println("\n\nInvalid choice, try again.\n\n\n");
                        }
                    } while(userChoice != 3);
                    break;
                case 4:
                    System.out.println("Exiting Library System...");
                    sc.close();
                    return;

                default:
                    System.err.println("\n\nInvalid choice, try again.\n\n\n");
            }

        }while(true);

    }
}
