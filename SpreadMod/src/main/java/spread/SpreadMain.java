package spread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;

public class SpreadMain {
    private static final Logger logger = LogManager.getLogger(SpreadMain.class);
    private static String daemonIP = "localhost"; // Node2
    private static final int daemonPort = 4803;
    
    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                daemonIP = args[0];
            }
            logger.info("Connecting to daemon at {}", daemonIP);
            
            Scanner scanner = new Scanner(System.in);
            String userName = readInput("MemberApp name? ", scanner);
            GroupMember member = new GroupMember(userName, daemonIP, daemonPort);
            
            boolean end = false;
            while (!end) {
                int option = displayMenu();
                handleMenuOption(option, member, scanner);
                if (option == 99) {
                    end = true;
                }
            }
            
            member.close();
        } catch (Exception e) {
            logger.error("An error occurred in the main application.", e);
        }
    }
    
    private static int displayMenu() {
        Scanner scanner = new Scanner(System.in);
        logger.info("MENU:\n1 - Join a Group\n2 - Send Message to Group\n3 - Send N Messages to Group\n4 - Leave a Group\n99 - Exit");
        return scanner.nextInt();
    }
    
    private static void handleMenuOption(int option, GroupMember member, Scanner scanner) throws SpreadException {
        switch (option) {
            case 1 -> member.joinToGroup(readInput("Join to group named? ", scanner));
            case 2 -> member.sendMessage(readInput("Group to send message: ", scanner), readInput("Message: ", scanner));
            case 3 -> {
                String group = readInput("Group to send messages: ", scanner);
                for (int i = 1; i <= 20; i++) {
                    member.sendMessage(group, "Message " + i);
                }
            }
            case 4 -> member.leaveGroup(readInput("Group name to leave: ", scanner));
            case 99 -> logger.info("Exiting application.");
            default -> logger.warn("Invalid menu option selected.");
        }
    }
    
    private static String readInput(String prompt, Scanner scanner) {
        logger.info(prompt);
        return scanner.nextLine();
    }
}
