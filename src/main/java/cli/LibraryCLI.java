package cli;

import java.util.ArrayList;
import java.util.Scanner;

import db.DatabaseManager;
import services.GuestService;
import services.PersonService;
import services.AdminService;
import services.LibrarianService;
import services.UserService;
import models.Guest;
import models.Person;
import models.Transaction;
import models.User;
import models.Book;
import models.Admin;
import models.Staff;
import models.Librarian;




public class LibraryCLI {
    public static void Guest(){
        Scanner sc = new Scanner(System.in);
        System.out.println("Please authenticate yourself by logging in :");
        System.out.println("Please enter your name : ");
        String name = sc.nextLine();
        System.out.println("Please enter your contact number :");
        String contactNo = sc.nextLine();
        Guest guest = GuestService.guestLogin(name,contactNo);
        int choice ;
        do{
            System.out.println("Guest Menu:");
            System.out.println("1.View Books 2.Return Book 3.Currently reading books 4.LogOut Guest");
            choice = Integer.parseInt(sc.nextLine());
            switch (choice) {
                case 1:
                    System.out.println("1.View Books by genre");
                    PersonService.viewGenres();
                    String genreName = sc.nextLine();
                    System.out.println(genreName);
                    PersonService.viewBooksByGenre(genreName);
                    System.out.println("Enter book Id of the book you wish to read");
                    int bookId = Integer.parseInt(sc.nextLine());
                    GuestService.startReading(guest,bookId);
                    break;

                case 2:
                    System.out.println("All the books owed by you is as follows: (Enter the book - id of the one you wish to return)");
                    if (guest.displayBooks()) {
                        int bookID = Integer.parseInt(sc.nextLine());
                        GuestService.returnBook(guest,bookID);
                    } else {
                        System.out.println("Enter your choice: ");
                    }

                    break;

                case 3:
                    guest.displayBooks();
                    break;
                case 4:
                    GuestService.logoutGuest(guest);
                    break;

                default:
                    System.out.println("Invalid choice");
                    break;
            }

        }while(choice != 3);
    }

    public static void SubscriptionUser(){

        Scanner sc = new Scanner(System.in);
        User user = null;

        // Authentication loop
        while (user == null) {
            System.out.println("\n=== Library User System ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            int authChoice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (authChoice) {
                case 1:
                    System.out.print("Enter email: ");
                    String email = sc.nextLine();
                    System.out.print("Enter password: ");
                    String password = sc.nextLine();
                    user = UserService.loginUser(email, password);
                    if (user != null) {
                        UserService.getListOfUnfulfilledTransactions(user);
                        UserService.getListOfBorrowedBooks(user);
                    }
                    break;

                case 2:
                    System.out.print("Enter name: ");
                    String name = sc.nextLine();
                    System.out.print("Enter email: ");
                    String newEmail = sc.nextLine();
                    System.out.print("Enter password: ");
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
                    System.out.println("Invalid choice!");
            }
        }

        // Main menu loop
        int choice;
        do {
            System.out.println("\n=== User Menu ===");
            System.out.println("1. View Borrowed Books");
            System.out.println("2. Return a Book");
            System.out.println("3. Request a Book");
            System.out.println("4. View My Requests/Reservations");
            System.out.println("5. Cancel a Book Request");
            System.out.println("6. Cancel a Reservation");
            System.out.println("7. Delete Account");
            System.out.println("8. Logout");
            System.out.print("Enter choice: ");
            choice = sc.nextInt();
            sc.nextLine();  // Consume newline

            switch (choice) {
                case 1:
                    if (!user.getBooks_borrowed().isEmpty()) {
                        System.out.println("\n--- Borrowed Books ---");
                        for (Book book : user.getBooks_borrowed()) {
                            System.out.println("ID: " + book.getId() + " | Title: " + book.getTitle());
                        }
                    } else {
                        System.out.println("\n--- No Books Borrowed ---");
                    }
                    break;

                case 2:
                    if (user.getBooks_borrowed().isEmpty()) {
                        System.out.println("\n--- No Borrowed Books ---");
                        for (Book book : user.getBooks_borrowed()) {
                            System.out.println("ID: " + book.getId() + " | Title: " + book.getTitle());
                        }
                    } else {
                        System.out.println("\n--- Return Book ---");
                        System.out.print("Enter Book ID to return: ");
                        int returnId = sc.nextInt();
                        if (UserService.returnBook(user, returnId)) {
                            System.out.println("Book returned successfully!");
                        } else {
                            System.out.println("Return failed. Please try again.");
                        }
                    }
                    break;

                case 3:
                    System.out.println("\n--- Request Book ---");
                    System.out.println("View Books by genre");
                    PersonService.viewGenres();
                    System.out.println("Enter the genre name: ");
                    String genreName = sc.nextLine();
                    System.out.println(genreName);
                    PersonService.viewBooksByGenre(genreName);
                    System.out.println("Enter book Id of the book you wish to read");
                    int bookId = Integer.parseInt(sc.nextLine());
                    if (UserService.requestBook(user, bookId)) {
                        System.out.println("Request submitted successfully!");
                    }
                    break;

                case 4:
                    System.out.println("\n--- My Requests ---");
                    UserService.viewBookRequests(user);
                    break;

                case 5:
                    UserService.viewBookRequests(user);
                    System.out.print("Enter Request ID to cancel: ");
                    int reqId = sc.nextInt();
                    UserService.CancelBookRequest(user, reqId);
                    break;

                case 6:
                    UserService.viewBookRequests(user);
                    System.out.print("Enter Reservation ID to cancel: ");
                    int resId = sc.nextInt();
                    UserService.CancelReservationRequest(user, resId);
                    break;

                case 7:
                    if (UserService.deleteUser(user)) {
                        System.out.println("Account deleted successfully!");
                        return;
                    } else {
                        System.out.println("Cannot delete account. Check for pending books/fines.");
                    }
                    break;

                case 8:
                    System.out.println("Logging out...");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        } while (choice!=8);
}

    public static void cliMaster() {
        System.out.println("Welcome to Library CLI");
        Scanner scanner = new Scanner(System.in);
        int choice = 0 ;
        do{
            System.out.println("Who are you???");
            System.out.println("1. Admin\n2. Librarian\n3. User\n4. Exit");
            choice = scanner.nextInt();
            switch(choice){
                case 3:
                    int userChoice;
                    do {
                        System.out.println("1.Guest\n2. SubscriptionUser\n3. Exit\n");
                        userChoice = scanner.nextInt();
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
                                System.out.println("Invalid choice, try again.");
                        }
                    } while(userChoice != 3);
                    break;
                case 4:
                    System.out.println("Exiting Library System...");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid choice, try again.");
            }

        }while(true);

    }
}
