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
            System.out.println("10. Logout");
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
                    System.out.println("Logging out...");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        } while (choice != 10);
    }



    public static void cliMaster(String[] args) {
        System.out.println("Welcome to Library CLI");
        Scanner scanner = new Scanner(System.in);
        int choice = 0 ;
        do{
            System.out.println("Who are you???");
            System.out.println("1. Admin\n2. Librarian\n3. User\n4. Exit");
            choice = scanner.nextInt();
            switch(choice){
                case 2:
                    System.out.println("Welcome Librarian : ");
                    Librarian();
                    break;
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
