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
            System.out.println("1.View Books 2.Return Book 3.LogOut Guest");
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
                    GuestService.logoutGuest(guest);
                    break;
                    default:break;
            }

        }while(choice != 3);
    }

    public static void SubscriptionUser(){
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
