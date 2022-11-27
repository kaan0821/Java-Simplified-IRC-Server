import java.net.Socket;

/** This class is for opening a thread for handling each connection with the client. */
public class ConnectionThread extends Thread {

    private Socket conn;
    private String serverName;
    private IrcServer server;

/**
 * Constructor for ConnectionThread.
 * @param server the server clients are connecting to
 * @param socket the connection socket
 * @param serverName the name of the server
 */
    public ConnectionThread(IrcServer server, Socket socket, String serverName) {
        this.server = server;
        this.conn = socket;
        this.serverName = serverName;
    }
/**
 * This method is to start a thread for handling connection.
 */
    public void run() {
        ConnectionHandler ch = new ConnectionHandler(server, conn, serverName);
        ch.handleClientRequest(); // handle the client request
    }
}
