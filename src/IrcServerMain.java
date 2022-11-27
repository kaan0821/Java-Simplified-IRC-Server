/** This class is to get user input for setting up the server. */
public class IrcServerMain {
/**
 * This method is to get userinput and setting up the server.
 * @param args the user's input specifying the server details
 */
    public static void main(String[] args) {
        try {
            IrcServer s = new IrcServer(args[0], Integer.valueOf(args[1]));
        } catch (Exception e) {
            System.out.println("Usage: java IrcServerMain <server_name> <port>");
        }
    }
}
