import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class FrontDeskApp {
    /*
     * Notes:
     * Small box storage's limit is 20 stored boxes
     * Medium box storage's limit is 10 stored boxes
     * Large box storage's limit is 5 stored boxes
     */
    final static int smallBoxLimit = 20;
    final static int mediumBoxLimit = 10;
    final static int largeBoxLimit = 5;

    // Available slots for each storage
    static int smallBoxAvailableSlots = 0;
    static int mediumBoxAvailableSlots = 0;
    static int largeBoxAvailableSlots = 0;

    final static Scanner s = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        // CHANGE LATER
        int currentUserID = 0;

        // Database Details
        // h2 Server mode
        String jdbcURL = "jdbc:h2:tcp://localhost/~/FrontDeskAppDB;AUTO_SERVER=TRUE";
        String dbUsername = "FrontDeskAdmin";
        String dbPassword = "admin";

        try {
            // Connecting to h2 DB
            Connection connection = DriverManager.getConnection(jdbcURL, dbUsername, dbPassword);
            System.out.println("---------Connected to Database.---------");

            // Console app
            while (true) {
                if (currentUserID != 0) {
                    printLoggedInAs(currentUserID, connection);
                }
                System.out.println("Welcome to the x Logistics Company!");
                System.out.println("Enter the following numbers to interact with the system: ");
                System.out.println("1 - Register a customer.");
                System.out.println("2 - Login as a customer.");
                System.out.println("3 - Store or retrieve a box.");
                System.out.println("4 - Check your records.");
                System.out.println("5 - Check each storage's boxes' status.");
                System.out.println("6 - Exit the application.");
                System.out.print("Input: ");
                int input = s.nextInt();
                clearTerminal();
                s.nextLine();

                // Register a customer
                if (input == 1) {
                    registerCustomer(s, connection);
                }

                // Login as a customer
                else if (input == 2) {
                    System.out.print("Enter your first name: ");
                    String firstNameInput = s.nextLine();
                    String query = "SELECT * FROM front_desk_schema.customers WHERE first_name=\'" + firstNameInput
                            + "\'";
                    Statement statement = connection.createStatement();
                    ResultSet rs = statement.executeQuery(query);

                    // User not registered
                    if (rs.getMetaData().getColumnCount() == 0) {
                        System.out.println("User not found! Please register first before logging in.");
                        currentUserID = 0;
                    }

                    // Get user ID
                    while (rs.next()) {
                        currentUserID = Integer.parseInt(rs.getString(1));
                    }

                    // Print user's first and last name
                    if (currentUserID != 0) {
                        printLoggedInAs(currentUserID, connection);
                    }

                    rs.close();
                }

                // Store or retrieve a box
                else if (input == 3) {
                    if (currentUserID != 0) {
                        printLoggedInAs(currentUserID, connection);

                        // Input whether storing or retrieving a box
                        System.out.print("Enter 1 to store your box, 2 to retrieve your box: ");
                        int storeOrRetrieveInput = s.nextInt();
                        s.nextLine();

                        // Check if the input is correct
                        if (storeOrRetrieveInput != 1 && storeOrRetrieveInput != 2) {
                            System.out.println("Invalid Input!");
                            pressEnterToContinue();
                            return;
                        }

                        // Storing Box
                        if (storeOrRetrieveInput == 1) {
                            // Query variables
                            String boxID = "";
                            String boxSize = "";
                            String boxDescription = "";
                            String boxStatus = "";
                            clearTerminal();

                            // Input small of the box and the description of what is inside
                            // Checking if the inputs are correct
                            System.out.println("Storing a Box.");
                            System.out.print("Enter what size your box is (small, medium, large): ");
                            boxSize = s.nextLine();
                            boxSize = boxSize.toLowerCase();
                            if (boxSize.isEmpty() && !boxSize.equals("small") && !boxSize.equals("medium")
                                    && !boxSize.equals("large")) {
                                System.out.println("Invalid box size input!");
                                return;
                            }
                            System.out.print("Enter what is inside the box: ");
                            boxDescription = s.nextLine();
                            if (boxDescription.length() > 32) {
                                System.out.println("Please limit the description to less than 32 characters.");
                                return;
                            }

                            // Checking the availability of the specific box storage
                            switch (boxSize) {
                                case "small": {
                                    smallBoxAvailableSlots = checkStorageAvailability(boxSize, connection);
                                    if (smallBoxAvailableSlots == 0) {
                                        System.out.println(
                                                "No available slots for the small box storage. Declining the box.");
                                        return;
                                    }
                                    break;
                                }
                                case "medium": {
                                    mediumBoxAvailableSlots = checkStorageAvailability(boxSize, connection);
                                    if (mediumBoxAvailableSlots == 0) {
                                        System.out.println(
                                                "No available slots for the medium box storage. Declining the box.");
                                        return;
                                    }
                                    break;
                                }
                                case "large": {
                                    largeBoxAvailableSlots = checkStorageAvailability(boxSize, connection);
                                    if (largeBoxAvailableSlots == 0) {
                                        System.out.println(
                                                "No available slots for the large box storage. Declining the box.");
                                        return;
                                    }
                                    break;
                                }
                            }

                            // If there are available slots, confirm if the user is sure to store the box
                            System.out.println();
                            System.out.println(
                                    "Trying to store a " + boxSize + " box with " + boxDescription + " inside.");
                            System.out.print("Enter 'confirm' to proceed: ");
                            String confirmInput = s.nextLine();
                            if (confirmInput.equals("confirm")) {
                                System.out.println();
                                System.out.println(
                                        "Storing a " + boxSize + " with " + boxDescription + " in the storage.");
                                String query1 = "";
                                String query2 = "";
                                switch (boxSize) {
                                    case "small": {
                                        query1 = "SELECT * FROM front_desk_schema.small_box_storage";
                                        query2 = "INSERT INTO front_desk_schema.small_box_storage (id, description, status, owner_id) VALUES ";
                                        boxID = "S";
                                        break;
                                    }
                                    case "medium": {
                                        query1 = "SELECT * FROM front_desk_schema.medium_box_storage";
                                        query2 = "INSERT INTO front_desk_schema.medium_box_storage (id, description, status, owner_id) VALUES ";
                                        boxID = "M";
                                        break;
                                    }
                                    case "large": {
                                        query1 = "SELECT * FROM front_desk_schema.large_box_storage";
                                        query2 = "INSERT INTO front_desk_schema.large_box_storage (id, description, status, owner_id) VALUES ";
                                        boxID = "L";
                                        break;
                                    }
                                }

                                // Getting the box ID
                                Statement statement = connection.createStatement();
                                ResultSet rs = statement.executeQuery(query1);
                                int rowCount = 0;
                                while (rs.next()) {
                                    rowCount++;
                                }
                                boxID += (rowCount + 1);

                                // Getting the status and the date of the box
                                boxStatus = "STORED - "
                                        + DateTimeFormatter.ofPattern("YYYY/MM/dd").format(LocalDateTime.now());
                                query2 += ("(\'" + boxID + "\'" + "," + "\'" + boxDescription + "\'" + "," + "\'"
                                        + boxStatus + "\'" + "," + currentUserID + ")");
                                statement = connection.createStatement();
                                statement.executeUpdate(query2);

                                // Successfully stored the box
                                System.out.println(
                                        "Your " + boxSize + " box has been stored. The box id is " + boxID + ".");
                            } else {
                                System.out.println("Canceled the transaction.");
                            }
                        }

                        // Retrieving Box
                        else if (storeOrRetrieveInput == 2) {
                            // Qiery Variables
                            String boxID = "";
                            String boxSize = "";
                            String boxDescription = "";
                            String boxStatus = "";
                            clearTerminal();

                            // Put all box IDs that the current user has stored/retrieved
                            List<String> listOfOwnedBoxIDs = new ArrayList<>();
                            System.out.println("Retrieving a box.");
                            System.out.println("Listing all stored boxes: ");
                            System.out.println();

                            System.out.println("Small Boxes: ");
                            listOfOwnedBoxIDs = getAllStoredBoxes("small", currentUserID, listOfOwnedBoxIDs,
                                    connection);
                            System.out.println();

                            System.out.println("Medium Boxes: ");
                            listOfOwnedBoxIDs = getAllStoredBoxes("medium", currentUserID, listOfOwnedBoxIDs,
                                    connection);
                            System.out.println();

                            System.out.println("Large Boxes: ");
                            listOfOwnedBoxIDs = getAllStoredBoxes("large", currentUserID, listOfOwnedBoxIDs,
                                    connection);
                            System.out.println();

                            // Ask the user what box ID they want to retrieve
                            System.out.print("Enter the ID of the box you want to retrieve: ");
                            String idInput = s.nextLine();
                            String query = "";
                            if (idInput.contains("S")) {
                                query = "SELECT * FROM front_desk_schema.small_box_storage WHERE id=\'" + idInput
                                        + "\'";
                                boxSize = "small";
                            } else if (idInput.contains("M")) {
                                query = "SELECT * FROM front_desk_schema.medium_box_storage WHERE id=\'" + idInput
                                        + "\'";
                                boxSize = "medium";
                            } else if (idInput.contains("L")) {
                                query = "SELECT * FROM front_desk_schema.large_box_storage WHERE id=\'" + idInput
                                        + "\'";
                                boxSize = "large";
                            } else {
                                System.out.println("Invalid Input!");
                                return;
                            }

                            // Getting box's details
                            Statement statement = connection.createStatement();
                            ResultSet rs = statement.executeQuery(query);
                            while (rs.next()) {
                                boxID = rs.getString("id");
                                boxDescription = rs.getString("description");
                                boxStatus = rs.getString("status");
                            }
                            System.out.println("Trying to retrieve a " + boxSize + " with " + boxDescription
                                    + " inside and its ID is " + boxID + ".");
                            System.out.println();

                            // Confirming the details of the box they want to retrieve
                            System.out.print("Enter 'confirm' to proceed: ");
                            String confirmInput = s.nextLine();
                            if (confirmInput.equals("confirm")) {
                                System.out.println("Retrieving a a " + boxSize + " with " + boxDescription
                                        + " inside and its ID is " + boxID + ".");
                                boxStatus = "RETRIEVED - "
                                        + DateTimeFormatter.ofPattern("YYYY/MM/dd").format(LocalDateTime.now());
                                switch (boxSize) {
                                    case "small": {
                                        query = "UPDATE front_desk_schema.small_box_storage SET STATUS = \'" + boxStatus
                                                + "\' WHERE owner_id=\'" + currentUserID + "\'";
                                        break;
                                    }
                                    case "medium": {
                                        query = "UPDATE front_desk_schema.medium_box_storage SET STATUS = \'"
                                                + boxStatus
                                                + "\' WHERE owner_id=\'" + currentUserID + "\'";
                                        break;
                                    }
                                    case "large": {
                                        query = "UPDATE front_desk_schema.large_box_storage SET STATUS = \'" + boxStatus
                                                + "\' WHERE owner_id=\'" + currentUserID + "\'";
                                        break;
                                    }
                                }
                                statement = connection.createStatement();
                                statement.executeUpdate(query);

                                // Successfully retrieved the box
                                System.out.println("Successfully retrieved box ID: " + boxID);
                            } else {
                                System.out.println("Canceled the transaction.");
                                return;
                            }
                        }
                    } else {
                        System.out.println(
                                "No user currently logged in, please register and/or login first to store or retrieve boxes.");
                    }

                }

                // Check records of the user
                else if (input == 4) {
                    if (currentUserID == 0) {
                        System.out.println(
                                "No user currently logged in, please register and/or login first to check for records.");
                    } else {
                        getAllOwnedBoxes(currentUserID, connection);
                    }

                }

                // Check storage's available slots
                else if (input == 5) {
                    System.out.print(
                            "Enter what storage slot availability you want to check (small, medium, large, all): ");
                    String checkInput = s.nextLine();
                    switch (checkInput) {
                        case "small": {
                            smallBoxAvailableSlots = checkStorageAvailability("small", connection);
                            break;
                        }
                        case "medium": {
                            mediumBoxAvailableSlots = checkStorageAvailability("medium", connection);
                            break;
                        }
                        case "large": {
                            largeBoxAvailableSlots = checkStorageAvailability("large", connection);
                            break;
                        }
                        case "all": {
                            smallBoxAvailableSlots = checkStorageAvailability("small", connection);
                            mediumBoxAvailableSlots = checkStorageAvailability("medium", connection);
                            largeBoxAvailableSlots = checkStorageAvailability("large", connection);
                            break;
                        }
                    }

                }

                // Exit the application
                else if (input == 6) {
                    break;
                }
                pressEnterToContinue();
            }

            // CLosing the connection
            connection.close();
            s.close();
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database.");
            e.printStackTrace();
        }
    }

    public static void registerCustomer(Scanner s, Connection connection) throws Exception {
        System.out.print("Enter your First Name: ");
        String firstName = s.nextLine();
        System.out.print("Enter your Last Name: ");
        String lastName = s.nextLine();
        System.out.print("Enter your Contact Number (Example: 09123456789): ");
        String contactNumber = s.nextLine();

        if (firstName.isEmpty() && lastName.isEmpty() && contactNumber.length() != 11) {
            System.out.println("Invalid Details!");
        } else {
            String query = "INSERT INTO front_desk_schema.customers (first_name, last_name, contact_number) VALUES "
                    + "(\'" + firstName + "\',\'" + lastName + "\', \'" + contactNumber + "\')";
            Statement statement = connection.createStatement();
            System.out.println(query);
            statement.executeUpdate(query);
            System.out.println("Customer " + firstName + " " + lastName + " has been registered.");
        }
    }

    static int checkStorageAvailability(String size, Connection connection) throws Exception {
        String query = "";
        int limit = 0;
        switch (size) {
            case "small": {
                query = "SELECT * FROM front_desk_schema.small_box_storage";
                limit = smallBoxLimit;
                break;
            }
            case "medium": {
                query = "SELECT * FROM front_desk_schema.medium_box_storage";
                limit = mediumBoxLimit;
                break;
            }
            case "large": {
                query = "SELECT * FROM front_desk_schema.large_box_storage";
                limit = largeBoxLimit;
                break;
            }
        }
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        int storedCount = 0;
        while (rs.next()) {
            if (rs.getString(3).contains("STORED")) {
                storedCount++;
            }
        }
        int availableSlots = limit - storedCount;
        if (availableSlots < 0) {
            availableSlots = 0;
        }
        System.out.print(StringUtils.capitalize(size) + " Box Storage has " + availableSlots);
        System.out.println((availableSlots == 1) ? " slot available." : " slots available.");
        return availableSlots;
    }

    static void clearTerminal() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c",
                        "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void pressEnterToContinue() {
        System.out.println();
        System.out.println("Press Enter key to continue...");
        try {
            System.in.read();
            s.nextLine();
            clearTerminal();
        } catch (Exception e) {
        }
    }

    static void printLoggedInAs(int currentUserID, Connection connection) throws Exception {
        String query = "SELECT * FROM front_desk_schema.customers WHERE id=" + currentUserID;
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        while (rs.next()) {
            System.out.println("Logged in as " + rs.getString(2) + " " + rs.getString(3) + ".");
        }
    }

    static List<String> getAllStoredBoxes(String boxSize, int owner_id, List<String> list, Connection connection)
            throws Exception {
        String query = "SELECT * FROM ";
        Statement statement = connection.createStatement();
        switch (boxSize) {
            case ("small"): {
                query += "front_desk_schema.small_box_storage WHERE owner_id=" + owner_id;
                ResultSet rs = statement.executeQuery(query);
                rs.last();
                if (rs.getRow() == 0) {
                    System.out.println("No stored small boxes.");
                } else {
                    rs = statement.executeQuery(query);
                    while (rs.next()) {
                        list.add(rs.getString("id"));
                    }
                    rs = statement.executeQuery(query);
                    printResultSetTable(rs);
                }
                break;
            }
            case ("medium"): {
                query += "front_desk_schema.medium_box_storage WHERE owner_id=" + owner_id;
                ResultSet rs = statement.executeQuery(query);
                rs.last();
                if (rs.getRow() == 0) {
                    System.out.println("No stored medium boxes.");
                } else {
                    rs = statement.executeQuery(query);
                    while (rs.next()) {
                        list.add(rs.getString("id"));
                    }
                    rs = statement.executeQuery(query);
                    printResultSetTable(rs);
                }
                break;
            }
            case ("large"): {
                query += "front_desk_schema.large_box_storage WHERE owner_id=" + owner_id;
                ResultSet rs = statement.executeQuery(query);
                rs.last();
                if (rs.getRow() == 0) {
                    System.out.println("No stored large boxes.");
                } else {
                    rs = statement.executeQuery(query);
                    while (rs.next()) {
                        list.add(rs.getString("id"));
                    }
                    rs = statement.executeQuery(query);
                    printResultSetTable(rs);
                }
                break;
            }
        }
        return list;
    }

    static void printResultSetTable(ResultSet rs) throws Exception {
        String leftAlignFormat = "| %-4s | %-32s | %-23s |%n";
        System.out.format("+------+----------------------------------+-------------------------+%n");
        System.out.format("| ID   | Description                      | Status                  |%n");
        System.out.format("+------+----------------------------------+-------------------------+%n");
        while (rs.next()) {
            System.out.format(leftAlignFormat, rs.getString("id"), rs.getString("description"), rs.getString("status"));
        }
        System.out.format("+------+----------------------------------+-------------------------+%n");
    }

    static void getAllOwnedBoxes(int owner_id, Connection connection) throws Exception {
        // Get and print all small boxes that with the given owner id
        String query = "SELECT * FROM front_desk_schema.small_box_storage WHERE owner_id=" + owner_id;
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        rs.last();
        if (rs.getRow() == 0) {
            System.out.println("No owned small boxes.");
        } else {
            rs = statement.executeQuery(query);
            printResultSetTable(rs);
        }
        System.out.println();

        // Get and print all medium boxes that with the given owner id
        query = "SELECT * FROM front_desk_schema.medium_box_storage WHERE owner_id=" + owner_id;
        statement = connection.createStatement();
        rs = statement.executeQuery(query);
        rs.last();
        if (rs.getRow() == 0) {
            System.out.println("No owned medium boxes.");
        } else {
            rs = statement.executeQuery(query);
            printResultSetTable(rs);
        }
        System.out.println();

        // Get and print all large boxes that with the given owner id
        query = "SELECT * FROM front_desk_schema.large_box_storage WHERE owner_id=" + owner_id;
        statement = connection.createStatement();
        rs = statement.executeQuery(query);
        rs.last();
        if (rs.getRow() == 0) {
            System.out.println("No owned large boxes.");
        } else {
            rs = statement.executeQuery(query);
            printResultSetTable(rs);
        }
    }
}