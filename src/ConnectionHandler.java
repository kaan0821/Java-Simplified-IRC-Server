import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;

/** This class is for handling each connection with the client. */
public class ConnectionHandler {

/** The length of nickname allowed. */
    public static final int MAX_LENGTH = 10;
/** The length of USER command after split. */
    public static final int USER_COMMAND_LENGTH = 5;
/** The length of PRIVMSG command after split. */
    public static final int PRIV_COMMAND_LENGTH = 3;
/** The index of USER command to split to store real name. */
    public static final int USER_COMMAND_INDEX = 4;

    private Socket conn;
    private String serverName;
    private IrcServer server;
    private String username; //Client's username
    private String realname; //Client's real name
    private boolean dummy; //A dummy variable to prevent empty statement
    private boolean registered = false; //Keep track of whether client is registered
    private ArrayList<String> groupsJoined = new ArrayList<String>(); //Keep track of which channels client joined
    private InputStream is;
    private OutputStream os;
    private BufferedReader br; // use buffered reader to read client data
    private PrintWriter out;
    private String nickname = "*";

/**
 * Constructor for ConnectionHandler.
 * @param server the server clients are connecting to
 * @param conn the connection socket
 * @param serverName the name of the server
 */
    public ConnectionHandler(IrcServer server, Socket conn, String serverName) {
        this.server = server;
        this.conn = conn;
        this.serverName = serverName;
        try {
            is = conn.getInputStream(); // get data from server on this input stream
            os = conn.getOutputStream(); // to send data back to server on this stream
            br = new BufferedReader(new InputStreamReader(is)); // use buffered reader to read data
            out = new PrintWriter(os, true); //writer
        } catch (IOException ioe) {
            System.out.println("ConnectionHandler: " + ioe.getMessage());
        }
    }

/**
 * This method is to handle all client requests.
 */
    public void handleClientRequest() {
        System.out.println("new ConnectionHandler constructed .... ");
        try {
            while (true) { //keep on getting request command from the client
                String line = br.readLine();
                String[] command = line.split(" ");
                //TIME command
                if (command[0].equals("TIME") && command.length == 1) {
                    printTime();
                }
                //QUIT command
                if (command[0].equals("QUIT") && command.length == 1) {
                    quit();
                }
                //NICK command
                if (command[0].equals("NICK") & command.length == 2) {
                    setNickname(command[1]);
                }
                //INFO command
                if (command[0].equals("INFO") && command.length == 1) {
                    out.println(":" + this.serverName + " 371 " + nickname + " :This code is written by the Magnificent 220011642");
                }
                //USER command
                if (command[0].equals("USER") && command.length >= USER_COMMAND_LENGTH) {
                    boolean checkUsername = command[1].matches("[A-Za-z0-9_]+");
                    boolean checkRealname = command[USER_COMMAND_INDEX].substring(1).matches("[A-Za-z]+");
                    if (registered) {
                        out.println(":" + this.serverName + " 400" + nickname + " :You are already registered");
                    } else {
                        //If arguments are valid
                        if (checkUsername && checkRealname) {
                            String real = "";
                            for (int i = USER_COMMAND_INDEX; i < command.length; i++) {
                                real = real + " " + command[i];
                            }
                            registerUser(command[1], real.substring(1));
                        } else {
                            //If arguments are not valid
                            out.println(":" + this.serverName + " 400" + nickname + " :Invalid arguments to USER command");
                        }
                    }
                    //If not enough arguments
                } else if (command[0].equals("USER") && command.length < USER_COMMAND_LENGTH) {
                    out.println(":" + this.serverName + " 400" + nickname + " :Not enough arguments");
                } else {
                    //This is playing with a dummy variable just to do something, prevent stylecheck from caliming error
                    dummy = true;
                }
                //PING command
                if (command[0].equals("PING")) {
                    pong(line);
                }
                //PART command
                if (command[0].equals("PART") && command.length == 2) {
                    if (!registered) {
                        out.println(":" + this.serverName + " 400 " + nickname + " :You need to register first");
                    } else {
                        //If channel exists
                        if (this.server.getChannels().containsKey(command[1])) {
                            partChannel(command[1]);
                        } else {
                        //If channel doesn't exist
                            out.println(":" + this.serverName + " 400 " + nickname + " :No channel exists with that name");
                        }
                    }
                }
                //PRIVMSG command
                //If arguments are valid
                if (command[0].equals("PRIVMSG") && command.length >= PRIV_COMMAND_LENGTH) {
                    if (!registered) {
                        out.println(":" + this.serverName + " 400 " + nickname + " :You need to register first");
                    } else {
                        //If sending group message
                        if (command[1].matches("^#[A-Za-z0-9_]+")) {
                            //If group exists
                            if (this.server.getChannels().containsKey(command[1])) {
                                String msg = "";
                                for (int i = 2; i < command.length; i++) {
                                    msg = msg + " " + command[i];
                                }
                                msg = ":" + nickname + " PRIVMSG " + command[1] + msg;
                                groupSend(command[1], msg);
                            //If group doesn't exist
                            } else {
                                out.println(":" + this.serverName + " 400 " + nickname + " :No channel exists with that name");
                            }
                        //If sending private message
                        } else if (command[1].matches("^[A-Za-z_][A-Za-z0-9_]+")) {
                            //If client exists
                            if (this.server.nameList().contains(command[1])) {
                                String msg = "";
                                for (int i = 2; i < command.length; i++) {
                                    msg = msg + " " + command[i];
                                }
                                msg = ":" + nickname + " PRIVMSG " + command[1] + msg;
                                send(command[1], msg);
                            //If client doesn't exist
                            } else {
                                out.println(":" + this.serverName + " 400 " + nickname + " :No user exists with that name");
                            }
                        }
                    }
                //If arguments are invalid
                } else if (command[0].equals("PRIVMSG") && command.length < PRIV_COMMAND_LENGTH) {
                    out.println(":" + this.serverName + " 400" + nickname + " :Invalid arguments to PRIVMSG command");
                } else {
                    //Prevent doing nothing for stylecheck
                    dummy = true;
                }
                //JOIN command
                if (command[0].equals("JOIN") && command.length == 2) {
                    if (!registered) {
                        out.println(":" + this.serverName + " 400 " + nickname + " :You need to register first");
                    //If command matches a channel
                    } else if (!command[1].matches("^#[A-Za-z0-9_]+")) {
                        out.println(":" + this.serverName + " 400 * :Invalid channel name");
                    } else {
                        //If everything works right
                        joinChannel(command[1]);
                    }
                }
                //LIST command
                if (command[0].equals("LIST") && command.length == 1) {
                    if (!registered) {
                        out.println(":" + this.serverName + " 400 " + nickname + " :You need to register first");
                    } else {
                        list();
                    }
                }
                //NAMES command
                if (command[0].equals("NAMES") && command.length == 2) {
                    if (!registered) {
                        out.println(":" + this.serverName + " 400 " + nickname + " :You need to register first");
                    } else {
                        names(command[1]);
                    }
                }
            }

        } catch (Exception e) { // exit cleanly for any Exception
            System.out.println("ConnectionHandler.handleClientRequest: " + e.getMessage());
            cleanup(); // cleanup and exit
        }
    }
/**
 * This method is for a client to part channels.
 * @param channelName the channel I'm leaving from
 */
    public void partChannel(String channelName) {
        ArrayList<ConnectionHandler> members = this.server.getChannels().get(channelName);
        //If channel does exist
        if (members.contains(this)) {
            String msg = ":" + nickname + " PART " + channelName;
            groupSend(channelName, msg);
            this.server.getChannels().get(channelName).remove(this);
            if (this.server.getChannels().get(channelName).size() == 0) {
                //If this is last member in the group, delete channel
                this.server.getChannels().remove(channelName);
            }
        } else {
            //If channel doesn't exist, do nothing
            dummy = true;
        }
    }
/**
 * This method is to register a user and store their details, add them to the server user list.
 * @param theUsername the username
 * @param theRealname their real name
 */
    public void registerUser(String theUsername, String theRealname) {
        username = theUsername;
        realname = theRealname;
        registered = true;
        this.server.addUser(this);
        out.println(":" + this.serverName + " 001 " + nickname + " :Welcome to the IRC network, " + nickname);
    }
/**
 * This method is to print the current time.
 */
    private void printTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        out.println(":" + this.serverName + " 391 " + nickname + " :" + currentTime);

    }
/**
 * This method is to list all the channels.
 */
    public void list() {
        for (String channelName : this.server.getChannels().keySet()) {
            out.println(":" + this.serverName + " 322 " + nickname + " " + channelName);
        }
        out.println(":" + this.serverName + " 323 " + nickname + " :End of LIST");
    }
/**
 * This method is to join a channel.
 * If exists, join. If doesn't exist, create.
 * @param channelName name of the channel
 */
    public void joinChannel(String channelName) {
        //If channel does exist, join it
        if (this.server.getChannels().containsKey(channelName)) {
            this.server.addChannelUser(this, channelName);
            groupsJoined.add(channelName);
            String msg = ":" + nickname + " JOIN " + channelName;
            groupSend(channelName, msg);
        } else {
            //If doesn't exist, create new channel
            this.server.addChannel(channelName, this);
            out.println(":" + nickname + " JOIN " + channelName);
            groupsJoined.add(channelName);
        }
    }
/**
 * This method is to get the nickname of the client.
 * @return the nickname
 */
    public String getNickname() {
        return nickname;
    }
/**
 * This method is to set a new nickname.
 * @param newName the new name
 */
    private void setNickname(String newName) {

        boolean lengthCheck = newName.length() > 0 && newName.length() < MAX_LENGTH;
        boolean nameCheck = newName.matches("^[A-Za-z_][A-Za-z0-9_]+");
        if (lengthCheck && nameCheck) {
            nickname = newName;
        } else {
            out.println(":" + this.serverName + " 400 * :Invalid nickname");
        }

    }
/**
 * This method is to list all the nicknames of users in a channel.
 * @param channelName the name of the channels
 */
    private void names(String channelName) {
        //If channel does exist
        if (this.server.getChannels().containsKey(channelName)) {
            ArrayList<String> names = new ArrayList<String>();
            //Fill the list with all nicknames in that channel
            for (ConnectionHandler users : this.server.getChannels().get(channelName)) {
                names.add(users.nickname);
            }
            out.print(":" + this.serverName + " 353 " + nickname + " = " + channelName + " :");
            //Print all the names out
            for (String name : names) {
                out.print(name + " ");
            }
            out.println("");
        //If doesn't exist
        } else {
            out.println(":" + this.serverName + " 400 " + nickname + " :No channel exists with that name");
        }
    }
/**
 * This method is to test connection by sending the same line back to client.
 * @param line what the client sent
 */
    private void pong(String line) {
        //Simply change the second character and send back
        String newLine = "";
        for (int i = 0; i < line.length(); i++) {
            if (i == 1) {
                newLine = newLine + "O";
            } else {
                newLine = newLine + line.charAt(i);
            }
        }
        out.println(newLine);
    }
/**
 * This method is to quit the connection.
 * Quit all the channels, send a message to all current clients.
 */
    private void quit() {
        ArrayList<ConnectionHandler> users = this.server.getUsers();
        for (ConnectionHandler user : users) {
            user.printMessage(":" + nickname + " QUIT");
        }
        for (String group : groupsJoined) {
            this.server.getChannels().get(group).remove(this);
        }
        cleanup();
    }
/**
 * This method is to send message to a channel.
 * @param channel the name of the channel
 * @param msg the message I'm sending
 */
    private void groupSend(String channel, String msg) {
        try {
            //Send message to all members in a channel
            ArrayList<ConnectionHandler> channelUsers = this.server.getChannels().get(channel);
            for (ConnectionHandler each : channelUsers) {
                each.printMessage(msg);
            }
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }
/**
 * This method is to send message to a client, by asking server to transfer it.
 * @param name nickname of the client
 * @param msg messaging I'm sending
 */
    private void send(String name, String msg) {
        try {
            ArrayList<ConnectionHandler> users = this.server.getUsers();
            for (ConnectionHandler each : users) {
                //Try to find that user from the current client list in the server
                if (each.nickname.equals(name)) {
                    each.printMessage(msg);
                }
            }
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }
/**
 * This method is to ask server to send out the message through a connection.
 * @param msg the message being sent
 */
    private void printMessage(String msg) {
        out.println(msg);
    }

/**
 * This method is to clean up all connections and readers writers.
 */
    private void cleanup() {
        System.out.println("ConnectionHandler:" + nickname + " ... cleaning up and exiting ... ");
        try {
            br.close();
            is.close();
            conn.close();
        } catch (IOException ioe) {
            System.out.println("ConnectionHandler:cleanup " + ioe.getMessage());
        }
    }
}
