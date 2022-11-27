import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


/** This class is for starting a server. */
public class IrcServer {

    private ServerSocket ss;
    private static ArrayList<ConnectionHandler> users = new ArrayList<ConnectionHandler>();
    private static HashMap<String, ArrayList<ConnectionHandler>> channels = new HashMap<String, ArrayList<ConnectionHandler>>();

/**
 * Constructor for IrcServer.
 * @param serverName name of the server
 * @param port port number of the server listening to
 */
    public IrcServer(String serverName, int port) {
        try {
            ss = new ServerSocket(port);
            System.out.println(serverName + "server started ... listening on port " + port + " ...");
            while (true) {
                // waits until client requests a connection, then returns connection (socket)
                Socket conn = ss.accept();
                System.out.println(serverName + "server got new connection request from " + conn.getInetAddress());
                // create new handler for the connection
                new ConnectionThread(this, conn, serverName).start();
            }
        } catch (IOException ioe) {
            System.out.println("Ooops " + ioe.getMessage());
        }
    }
/**
 * This method is to add a new connectionhandler of a client to the client list.
 * @param client the connencitonhandler of a client
 */
    public void addUser(ConnectionHandler client) {
        users.add(client);
    }
/**
 * This method is to add a new connectionhandler of a client to the client list.
 * @return an arraylist of all the connection handlers of current clients
 */
    public ArrayList<ConnectionHandler> getUsers() {
        return users;
    }
/**
 * This method is to get the channels stored in the server.
 * @return the hashmap of channels and their users
 */
    public HashMap<String, ArrayList<ConnectionHandler>> getChannels() {
        return channels;
    }
/**
 * This method is to add a new channel.
 * @param name name of the channel
 * @param user the client creating it
 */
    public void addChannel(String name, ConnectionHandler user) {
        ArrayList<ConnectionHandler> chan = new ArrayList<ConnectionHandler>();
        chan.add(user);
        channels.put(name, chan);
    }
/**
 * This method is to add a new client to a channel.
 * @param client the connencitonhandler of a client
 * @param channelName name of the channel
 */
    public void addChannelUser(ConnectionHandler client, String channelName) {
        channels.get(channelName).add(client);
    }
/**
 * This method is to list the nicknames of all the current clients.
 * @return arraylist of all nicknames of clients
 */
    public ArrayList<String> nameList() {
        ArrayList<String> nameList = new ArrayList<String>();
        for (ConnectionHandler each : users) {
            nameList.add(each.getNickname());
        }
        return nameList;
    }

}

