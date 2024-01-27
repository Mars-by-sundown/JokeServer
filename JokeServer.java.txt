// 1. Name: Nicholas Ragano

// 2. Date: 2024-01-26

// 3. Java version: 21

// 4. Precise command-line compilation examples / instructions: 
// To complile all: 
//  >javac *.java


// 5. Precise examples / instructions to run this program:

// In separate shell windows:

// JOKESERVER (removes brackets around 'secondary' to start the secondary server)
//      each server will need its own shell window

//      >java JokeServer [secondary]

// JOKECLIENT (0, 1, or 2 optional arguments can be provided)
//      if none are provided it will use InetAddress.getLocalHost() as its IP address

//      >java JokeClient [IPAddr] [IPAddr]

// JOKECLIENTADMIN (0, 1, or 2 optional arguments can be provided)
//      if none are provided it will use InetAddress.getLocalHost() as the target IP address

//      >java JokeClientAdmin [IPAddr] [IPAddr]

// 6. Full list of files needed for running the program: 

//  a. JokeServer.java

// 7. Notes: 

// My implementation first sends the jokes IN ORDER for each new client. Once a client has seen all Jokes or Proverbs once
//      every subsequent cycle will be a random order. so a client should first see this order
//
//      JA, JB, JC, JD, JOKE CYCLE COMPLETED
//
//      Then every time after the jokes will be served in a random order


import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
/*
 *  Thanks to John O'Hanley at http://www.javapractices.com/topic/TopicAction.do?Id=12 for reference material on how to make a shallow copy of an object (used in JokeManager)
 *  Thanks to Readers Digest for the jokes, as I am not a comedian https://www.rd.com/list/short-jokes/
 *  
 */


/**
 *  Serializable Object to hold data passed between Joke Server and Client
 */
class ClientData implements Serializable {
    String userName; //holds the username of the client
    String clientID; //client needs to be able to send and receive its unique ID
    JokeProverb message; //client needs to be able to receive the message object
}

/**
 *  Serializable Object to hold data passed between Joke Server and Admin Clients
 */
class AdminData implements Serializable{
    String serverHost;
    int serverPort;
    String adminHost;
    int adminPort;
    boolean serverMode; 
} 

/**
 *  custom container to hold info related to each joke or proverb
 *  overloaded constructor, either empty or with arguments
 *  also a constructor that takes another object of its own type to allow for shallow copies
 */
class JokeProverb implements Serializable{
    Boolean isJoke;
    Boolean isLast = false; //will be false unless JokeManager changes to notify client
    String prefix; //JA, PA, etc.
    String body; //the joke or proverb

    public JokeProverb(){
        //empty constructor
        this.isJoke = true;
        this.prefix = "";
        this.body = "";
    }

    public JokeProverb(Boolean isJoke, String prefix, String body){
        this.isJoke = isJoke;
        this.prefix = prefix;
        this.body = body;
    }

    //to make a copy of another JokeProverb
    public JokeProverb(JokeProverb otherJokeProverb){
        this(otherJokeProverb.isJoke, otherJokeProverb.prefix, otherJokeProverb.body);
    }

}


/**
 *  This is instantiated in the JokeServer method each JokeWorker will receive the 'reference' to this class.
 *  This class is specifically setup for 4 jokes and then 4 proverbs to be entered in order
 *  Randomizing for each client is automatically handled in this class
 */
class JokeManager{
    private ArrayList<JokeProverb> jokeList = new ArrayList<>();
    private ArrayList<JokeProverb> proverbList = new ArrayList<>();
    private ArrayList<ArrayList<JokeProverb>> JPList = new ArrayList<>();
    private HashMap<String, ArrayList<String>> clientData = new HashMap<>();

    JokeManager(){
        //initial setup of our nested arrays upon creation
        JPList.add(jokeList);
        JPList.add(proverbList);
    }

    public void addJokeProverb(boolean isJoke, String prefix, String body){
        JokeProverb jp = new JokeProverb(isJoke, prefix, body);
        if(isJoke){
            JPList.get(0).add(jp); //add to the joke list
        }else{
            JPList.get(1).add(jp); //add to the proverb list
        }
    }

    public void addNewClient(String clientID){
        ArrayList<String> clientStateData = new ArrayList<>();
        clientStateData.add("00000"); //add the base joke state
        clientStateData.add("00000"); //add the base proverb state
        clientData.put(clientID, clientStateData); // clientData[clientID] = ["00000", "00000"]
    }

    //check if the client exists
    public boolean clientExists(String clientID){
        return clientData.containsKey(clientID);
    }

    public JokeProverb getNextForClient(String clientID, boolean proverbMode){
        JokeProverb jp_return = new JokeProverb(); //the object to hold our returned value
        int JPIDX; //used to select either the list of jokes or list of proverbs, also allows us to get the correct clientID state index
        if(proverbMode){
            JPIDX = 1; //proverbs, AKA the second "00000" String in the array
        }else{
            JPIDX = 0; //jokes
        }
        // gets the length 2 array of client state
        // clientData layout
        // key = clientID
        // value = ArrayList where
        // index = 0, is the Joke state data for that client
        // index = 1, is the Proverb state data for that client
        // clientData {clientID: ["00000", "00000"]}
        String clientStateString = (clientData.get(clientID)).get(JPIDX); 
        StringBuilder CSB = new StringBuilder(clientStateString); //a string builder to manipulate the client state data

        /*
         *  The state data can be decoded as follows
         *  index 0-3: each is a binary representation of if it has been sent or not already
         *      0 indicates it has not been sent, 1 indicates it has
         *  
         *  index 4: indicates whether the client has seen all in order once already
         *      0 indicates to sent responses in order
         *      1 indicates we have seen all atleast once, and will be randomized from here on out
         */

        if (CSB.charAt(4) == '0'){
            //this is our first time through, so send in order
            for(int i = 0; i < 4; i++){
                if(CSB.charAt(i) == '0'){
                    //send this one!
                    jp_return = new JokeProverb(JPList.get(JPIDX).get(i));
                    if(i == 3){
                        //if this is the last possible one to send we need to do some additional work to ready the state for next time
                        jp_return.isLast = true; //we set true, so client knows to send the notification of cycle completion
                        CSB.replace(0, CSB.length(),"00001"); //set all back to zero, and set last digit to 1 to force randomization from now on

                    }else{
                        CSB.setCharAt(i, '1'); //mark it as seen
                    }
                    break;
                }
            }
        }else{
            //this is NOT our first time through, so we need to return a random response that we have not previously sent this cycle
            ArrayList<Integer> idxList = new ArrayList<>();
            //record the indexes that we find an unsent response at
            for(int i = 0; i < 4; i++){
                if(CSB.charAt(i) == '0'){
                    idxList.add(i);
                }
            }

            if(idxList.size() == 1){
                //only one left so return whatever that index is
                jp_return = new JokeProverb(JPList.get(JPIDX).get(idxList.get(0)));
                jp_return.isLast = true; //notify of cycle completion
                CSB.replace(0,CSB.length(), "00001"); //reset
            }else{
                //RANDOMIZE
                Random randomizer = new Random();
                //pick a random index in the list of available indexes we have previously found
                int randIdx = randomizer.nextInt(idxList.size());
                jp_return = new JokeProverb(JPList.get(JPIDX).get(idxList.get(randIdx)));
                CSB.setCharAt(idxList.get(randIdx), '1'); //set the state at that index to 1
            }
        }

        clientData.get(clientID).set(JPIDX, CSB.toString()); //update our client data with the new state data
        return jp_return;
    }
}

/**
 *  Keeps information about which server the client and admin are currently using
 *  Provides functionality to toggle servers and get info about which server to use
 */
class ClientServerManager {
    private InetAddress prim_host;
    private int prim_port = 4545;
    private InetAddress sec_host; //will be overwritten when IP is provided
    private int sec_port = 4546;
    private boolean allowSecondary = false; //do not allow secondary until one is provided
    private boolean useSecondary = false; //if true the secondary server is in use

    public void setPrimaryServer(InetAddress hostname){
        prim_host = hostname;
    }

    public void setSecondaryServer(InetAddress hostname){
        sec_host = hostname;
        allowSecondary = true;
    }

    //what is the IP of the server we are using?
    public InetAddress getServer(){
        if(useSecondary && allowSecondary){
            return sec_host;
        }else{
            return prim_host;
        }
    }

    public String getPrimaryServerString(){
        return prim_host.toString();
    }

    public String getPrimaryPortString(){
        return Integer.toString(prim_port);
    }

    public String getSecondaryServerString(){
        if(allowSecondary)
            return sec_host.toString();
        else{
            return "None";
        }
    }

    public String getSecondaryPortString(){
        if(allowSecondary)
            return Integer.toString(sec_port);
        else{
            return "None";
        }
    }
    public void setPrimaryPort(int portNum){
        prim_port = portNum;
    }

    public void setSecondaryPort(int portNum){
        sec_port = portNum;
    }

    //what port are we on?
    public int getPort(){
        if(useSecondary && allowSecondary){
            return sec_port;
        }else{
            return prim_port;
        }
    }

    public int toggleServer(){
        if(allowSecondary){
            useSecondary = !useSecondary; //toggle server use
            return 1; //successfully toggled
        }else{
            return 0; //no secondary server provided
        }
        
    }

    //Were we given a secondary server?
    public boolean hasSecondary(){
        return allowSecondary;
    }

    //Are we using the secondary server?
    public boolean isSecondary(){
        return useSecondary;
    }


}

/**
 *  Joke Client, called from command line with >java JokeClient [IPAddr] [IPAddr]
 *     [IPAddr] are optional and not required, will default to the localhost IP
 */
class JokeClient {
    ClientServerManager CSM = new ClientServerManager();
    String ClientID;
    String userName;

    public static void main(String argv[]) {
        JokeClient cc = new JokeClient(argv);
        cc.run(argv);
    }

    public JokeClient(String argv[]) {
        ClientID = this.generateClientID(); //generate this clients unique identifier
    }


    //generates a random client ID for this instance
    private String generateClientID(){
        StringBuilder generatedID = new StringBuilder();
        Random generator = new Random();
        int randStrLen = 16; //length of the random string we want to generate for the UUID, arbitrarily picked 16
        for(int i = 0; i < randStrLen; i++){
            generatedID.append(generator.nextInt(9));
        }
        return generatedID.toString();
    }

    public void run(String argv[]) {
        try{
            if(argv.length == 0){
                //use default localhost w/o secondary
                CSM.setPrimaryServer(InetAddress.getLocalHost());
            }else if(argv.length == 1){
                //change the primary, still no secondary
                CSM.setPrimaryServer(InetAddress.getByName(argv[0]));
            }else if(argv.length == 2){
                //change the primary, set the secondary
                CSM.setPrimaryServer(InetAddress.getByName(argv[0]));
                CSM.setSecondaryServer(InetAddress.getByName(argv[1]));
            }else{
                //wrong number of args were entered
                System.out.println("Wrong number of arguments entered please enter in the form of >Java JokeClientAdmin <IPaddr> <IPaddr> ");
            }
            //set the ports to the required admin ports
            CSM.setPrimaryPort(4545);
            CSM.setSecondaryPort(4546);

            //print some information about our servers, we havent actually tried to connect to them at all yet!
            StringBuilder connInfo = new StringBuilder();
            connInfo.append("Server one: " + CSM.getPrimaryServerString() + ", port: " + CSM.getPrimaryPortString());
            if(CSM.hasSecondary()){
                connInfo.append(" Server two: " + CSM.getSecondaryServerString() + ", port: " + CSM.getSecondaryPortString());
            }
            System.out.println(connInfo.toString());

            //Get the name of the user and say hello!
            Scanner clientIn = new Scanner(System.in);
            System.out.println("Enter your name: ");
            System.out.flush();
            this.userName = clientIn.nextLine();
            System.out.println("Hi " + this.userName + "!\n");


            //get Joke loop, this takes new Joke requests until quit is typed
            String clientStr;
            do{
                System.out.println("Hit ENTER to get a new response or type quit to exit...");
                clientStr = clientIn.nextLine();

                //handle request to switch servers
                if(clientStr.indexOf("s") >= 0){
                    if(CSM.toggleServer() == 0){
                        //if it failed to switch
                        System.out.println("No Secondary Server was provided at client startup, unable to switch");
                    }
                    //print the server info regardless
                    System.out.println("Now communicating with: " + CSM.getServer().toString() + ",  Port " + CSM.getPort());
                } else if(clientStr.indexOf("quit") < 0){
                    queryServer();
                }
            }while (clientStr.indexOf("quit") < 0);

            System.out.println("Cancelled by user request.");
            clientIn.close(); //make sure we close our scanner

        }catch(UnknownHostException UH){
            //Thrown when IP address of the host could not be determined, possibly isnt online
            System.out.println("\nUnknown Host problem.\n"); 
            UH.printStackTrace();
        }
    }

    void queryServer(){
        try{
            //create a ClientData object to pass the data to and from the server
            ClientData ClientDataObj = new ClientData();
            ClientDataObj.clientID = this.ClientID;
            ClientDataObj.userName = this.userName;

            //Establish a connection to the server
            Socket socket = new Socket(CSM.getServer(), CSM.getPort());

            //establish an output stream using our socket add send our info
            OutputStream outStream = socket.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeObject(ClientDataObj);

            //Establish an input stream to listen for a response
            InputStream inStream = socket.getInputStream();
            ObjectInputStream objectInStream = new ObjectInputStream(inStream);

            //Read the serialized ClientData response sent by the JokeServer
            ClientData inObject = (ClientData) objectInStream.readObject();
            
            //Build out our printed response so the user knows whats happening
            StringBuilder respStr = new StringBuilder();

            if(CSM.isSecondary()){
                respStr.append("<S2> ");
            }

            //build our, PA username: proverbGoesHere, format
            respStr.append(inObject.message.prefix + " ");
            respStr.append(this.userName + ": ");
            respStr.append(inObject.message.body + "\n");
            System.out.println(respStr.toString());

            //if we were notified that this was the last joke/ proverb then build out our cycle complete response
            if(inObject.message.isLast){
                respStr.delete(0, respStr.length());
                if(inObject.message.isJoke){
                    respStr.append("\nJOKE ");
                }else{
                    respStr.append("\nPROVERB ");
                }
                respStr.append("CYCLE COMPLETED\n");
                System.out.println(respStr.toString());   
            }

            socket.close(); //close our socket

        }catch(ConnectException CE){
            //will be thrown if server is not open, also thrown if request is refused (server backlog queue is exceeded)
            System.out.println("\nOh no. The JokeServer refused our connection! Is it running?\n");
            CE.printStackTrace();

        }catch(UnknownHostException UH){
            //Thrown when IP address of the host could not be determined, possibly isnt online
            System.out.println("\nUnknown Host problem.\n"); 
            UH.printStackTrace();

        }catch(ClassNotFoundException CNF){
            //This will be thrown if there is an issue with the Serialized ClientData class we are passing back and forth
            CNF.printStackTrace();

        }catch(IOException IOE){
            IOE.printStackTrace(); 
        }
    }
}

/**
 *  Joke Admin Client, called from command line with >java JokeClientAdmin
 */
class JokeClientAdmin {
    ClientServerManager CSM = new ClientServerManager(); //the server manager for our admin client
    public static void main(String argv[]) {
        JokeClientAdmin cc = new JokeClientAdmin(argv);
        cc.run(argv);
    }
    public JokeClientAdmin(String argv[]) {
        //nothing to do here
    }

    public void run(String argv[]) {
        try{
            //get the server name or address we were passed
            if(argv.length == 0){
                //use default localhost w/o secondary
                CSM.setPrimaryServer(InetAddress.getLocalHost());
            }else if(argv.length == 1){
                //change the primary, still no secondary
                CSM.setPrimaryServer(InetAddress.getByName(argv[0]));
            }else if(argv.length == 2){
                //change the primary, set the secondary
                CSM.setPrimaryServer(InetAddress.getByName(argv[0]));
                CSM.setSecondaryServer(InetAddress.getByName(argv[1]));
            }else{
                //wrong number of args were entered
                System.out.println("Wrong number of arguments entered please enter in the form of >Java JokeClientAdmin <IPaddr> <IPaddr> ");
            }
            //set the ports to the admin ports
            CSM.setPrimaryPort(5050);
            CSM.setSecondaryPort(5051);

            //Scanner Object to allow input from client console
            Scanner clientIn = new Scanner(System.in);

            //Get the name of the user
            System.out.println("Admin Client Started...\n");
            StringBuilder connInfo = new StringBuilder();
            connInfo.append("Server one: " + CSM.getPrimaryServerString() + ", port: " + CSM.getPrimaryPortString());
            if(CSM.hasSecondary()){
                connInfo.append(" Server two: " + CSM.getSecondaryServerString() + ", port: " + CSM.getSecondaryPortString());
            }
            System.out.println(connInfo.toString());

            //get Joke loop, this takes new Joke requests until quit is typed
            String clientStr = "";
            do{
                System.out.println("Hit ENTER to change server mode");
                clientStr = clientIn.nextLine();
                //handle requests to switch the server
                if(clientStr.indexOf("s") >= 0){
                    if(CSM.toggleServer() == 0){
                        //if it failed to switch
                        System.out.println("No Secondary Server was provided at client startup, unable to switch");
                    }
                    //print the server info regardless
                    System.out.println("Now communicating with: " + CSM.getServer().toString() + ",  Port " + CSM.getPort());
                } else if(clientStr.indexOf("quit") < 0){
                    queryServer(CSM);
                }
            }while (clientStr.indexOf("quit") < 0);
            System.out.println("Cancelled by user request.");
            clientIn.close(); //close our scanner

        }catch(UnknownHostException UH){
            //Thrown when IP address of the host could not be determined, possibly isnt online
            System.out.println("\nUnknown Host problem.\n"); 
            UH.printStackTrace();
        }
    }

    void queryServer(ClientServerManager CSM){
        try{
            AdminData adminDataObj = new AdminData();

            //Establish a connection to the server and send our command
            Socket sock = new Socket(CSM.getServer(), CSM.getPort());
            OutputStream outStream = sock.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            adminDataObj.adminHost = CSM.getServer().toString();
            adminDataObj.adminPort = CSM.getPort();
            objectOutStream.writeObject(adminDataObj);

            //Wait/ Receive response
            InputStream InStream = sock.getInputStream();
            ObjectInputStream ObjectInStream = new ObjectInputStream(InStream);
            adminDataObj = (AdminData) ObjectInStream.readObject();

            //construct the print out of our response
            StringBuilder respStr = new StringBuilder();
            if(CSM.isSecondary()){
                respStr.append("<S2> ");
            }
            if(adminDataObj.serverMode){
                respStr.append("    Server is now in proverb mode");
            }else{
                respStr.append("    Server is now in joke mode");
            }
            
            System.out.println(respStr.toString());
            sock.close(); //dont forget to close our socket

        }catch(ConnectException CE){
            //will be thrown if server is not open, also thrown if request is refused (server backlog queue is exceeded)
            System.out.println("\nOh no. The JokeServer refused our connection! Is it running?\n");
            CE.printStackTrace();

        }catch(UnknownHostException UH){
            //Thrown when IP address of the host could not be determined, possibly isnt online
            System.out.println("\nUnknown Host problem.\n"); 
            UH.printStackTrace();

        }catch(ClassNotFoundException CNF){
            //This will be thrown if there is an issue with the Serialized ClientData class we are passing back and forth
            CNF.printStackTrace();

        }catch(IOException IOE){
            IOE.printStackTrace(); 
        }
    }
}

/**
 *  Worker thread that communicates with the Chosen Joke Server
 */
class JokeAdminServerWorker extends Thread{
    Socket sock;
    AdminListener al;
    JokeAdminServerWorker(Socket s, AdminListener admin){
        sock = s;
        al = admin;
    }

    public void run(){
        try{
            //connect and listen for instructions from client
            InputStream InStream = sock.getInputStream();
            ObjectInputStream ObjectInStream = new ObjectInputStream(InStream);
            AdminData adminDataObj = (AdminData) ObjectInStream.readObject();
            
            //print out some information in the terminal
            System.out.println("Data received from Admin client: ");
            System.out.println("    Admin Host: " + adminDataObj.adminHost + " Admin Port: " + adminDataObj.adminPort);
            al.toggleMode();
            if(al.getMode()){
                System.out.println("    Admin changed server mode to: Proverb");
            }else{
                System.out.println("    Admin changed server mode to: Joke");
            }

            //send our data, inlcudes the mode the server should be in
            OutputStream outStream = sock.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            adminDataObj.serverMode = al.getMode();
            objectOutStream.writeObject(adminDataObj);
            sock.close(); //close the connection, we are done

        }catch(IOException IOE){
            IOE.printStackTrace();
        }catch(ClassNotFoundException CNF){
            CNF.printStackTrace();
        }
    }
}

/**
 *  Listener for Admin Client connections, spawned as a thread from the Joke Server
 */
class AdminListener implements Runnable{
    public static boolean serverProverbMode = false; //server starts in joke mode by default
    private boolean isPrimary; //true if this is for the primary server
    private InetAddress hostIP; 
    private int serverPort;
    public AdminListener(InetAddress hostIP, boolean isPrimary){
        this.isPrimary = isPrimary;
        this.hostIP = hostIP;
    }
    
    public void run(){
        int q_len = 6; /*Maximum number of requests to queue in the backlog, additional requests will be refused if full */
        if(isPrimary){
            serverPort = 5050;
        }else{
            serverPort = 5051;
        }
        Socket adminSock;

        //each server, Primary and Secondary, each will have its own listener
        try{
            ServerSocket adminSocket = new ServerSocket(serverPort, q_len, hostIP );
            while(true){
                adminSock = adminSocket.accept();
                System.out.println("##Admin connection from: " + hostIP + ": " + adminSock);
                new JokeAdminServerWorker(adminSock, this).start();
            }
        }catch(IOException IOE){
            IOE.printStackTrace();
        }
    }
    public boolean getMode(){
        return serverProverbMode;
    }

    public void setMode(boolean proverbMode){
        serverProverbMode = proverbMode;
    }

    public void toggleMode(){
        serverProverbMode = !serverProverbMode;
    }
}

/*
 *  Worker thread that is spun by the server to process a client request
 */
class JokeWorker extends Thread {
    Socket sock;
    boolean proverbMode; //true if the server is found to be in proverbMode, otherwise false
    JokeManager jm;

    //Constructor
    JokeWorker(Socket s, AdminListener admin, JokeManager serverJM){
        //assign our socket, admin listener and JokeManager
        // all threads share a reference to the same admin and jokemanager instance so all updates are centralized
        sock = s;
        proverbMode = admin.getMode();
        jm = serverJM;
    }

    public void run(){
        try{
            //listen for incoming client info
            InputStream InStream = sock.getInputStream();
            ObjectInputStream ObjectInStream = new ObjectInputStream(InStream);
            ClientData clientTransObj = (ClientData) ObjectInStream.readObject();
            System.out.println("    Client Data Received from user: " + clientTransObj.userName + ", Client ID - " + clientTransObj.clientID);

            //if this is a new client then create an entry for them
            if(!jm.clientExists(clientTransObj.clientID)){
                jm.addNewClient(clientTransObj.clientID);
                System.out.println("    " + clientTransObj.userName + " is a new client, created a new client entry in the JokeManager");
            }

            //get the next joke or proverb from the Joke Manager
            clientTransObj.message = jm.getNextForClient(clientTransObj.clientID, proverbMode);

            //send our response and clean up/ close our connection
            OutputStream outStream = sock.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            System.out.println("    Sending " + clientTransObj.message.prefix + " to " + clientTransObj.userName);
            objectOutStream.writeObject(clientTransObj);
            System.out.println("Closing connection...\n");
            sock.close(); //close the connection, we are done

        }catch( ClassNotFoundException CNF){
            CNF.printStackTrace();
        }catch( IOException x){
            System.out.println("Server error.");
            x.printStackTrace();
        }
    }
} 

/**
 *  The Joke Server
 */
public class JokeServer {
    public static void main(String[] args) throws Exception {
        int q_len = 6; /*Maximum number of requests to queue in the backlog, additional requests will be refused if full */
        InetAddress servAddress = InetAddress.getLocalHost();
        int serverPort = 4545;
        boolean isPrimary = true;

        if(args.length == 1 && args[0].equals("secondary")){
            serverPort = 4546;
            isPrimary = false; //indicate this is a secondary server
        }
        Socket sock;

        //create a thread to go and listen for admin connections
        AdminListener admin = new AdminListener(InetAddress.getLocalHost(), isPrimary);
        Thread adminThread = new Thread(admin);
        adminThread.start();
        //start our jokemanager and load in jokes/ proverbs
        JokeManager JokeProverbManager = new JokeManager();
        JokeProverbManager.addJokeProverb(true, "JA", "Why dont scientists trust atoms? Because they make up everything.");
        JokeProverbManager.addJokeProverb(true, "JB", "What do you call a fake noodle? An impasta.");
        JokeProverbManager.addJokeProverb(true, "JC", "What did the pirate say when he turned 80? Aye matey!");
        JokeProverbManager.addJokeProverb(true, "JD", "What did the buffalo say when his son left for college?  Bison.");
        JokeProverbManager.addJokeProverb(false, "PA", "If you want to go fast, go alone. If you want to go far, go together.");
        JokeProverbManager.addJokeProverb(false, "PB", "A man who uses force is afraid of reasoning.");
        JokeProverbManager.addJokeProverb(false, "PC", "A spoon does not know the taste of soup, nor a learned fool the taste of wisdom.");
        JokeProverbManager.addJokeProverb(false, "PD", "The reputation of a thousand years may be determined by the conduct of one hour.");

        //Create our server socket using our port and allowed queue length
        ServerSocket serverSock = new ServerSocket(serverPort, q_len, servAddress);
        System.out.println("Server " + servAddress + " open on port " + serverPort + " and awaiting connections from clients...");
        while(true){
            //Listen until a request comes in, accept it and spin up a worker thread to handle it
            sock = serverSock.accept(); //accept creates a new Socket and returns it
            System.out.println("Connection from: " + sock);
            new JokeWorker(sock, admin, JokeProverbManager).start();
        }
    }
}

/*
 * JokeLog.txt output
 */
// ####################################JOKESERVER LOG####################################
// >java JokeServer
// Server DESKTOP-1MRMOOR/192.168.1.77 open on port 4545 and awaiting connections from clients...
// Connection from: Socket[addr=/192.168.1.77,port=61630,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Nick is a new client, created a new client entry in the JokeManager
//     Sending JA to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61634,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending JB to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61635,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending JC to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61636,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending JD to Nick
// Closing connection...

// ##Admin connection from: DESKTOP-1MRMOOR/192.168.1.77: Socket[addr=/192.168.1.77,port=61643,localport=5050]
// Data received from Admin client: 
//     Admin Host: DESKTOP-1MRMOOR/192.168.1.77 Admin Port: 5050
//     Admin changed server mode to: Proverb
// Connection from: Socket[addr=/192.168.1.77,port=61645,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PA to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61647,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PB to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61648,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PC to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61649,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PD to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61651,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PD to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61652,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PA to Nick
// Closing connection...

// ##Admin connection from: DESKTOP-1MRMOOR/192.168.1.77: Socket[addr=/192.168.1.77,port=61654,localport=5050]
// Data received from Admin client:
//     Admin Host: DESKTOP-1MRMOOR/192.168.1.77 Admin Port: 5050
//     Admin changed server mode to: Joke
// Connection from: Socket[addr=/192.168.1.77,port=61656,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending JD to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61657,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending JA to Nick
// Closing connection...

// ##Admin connection from: DESKTOP-1MRMOOR/192.168.1.77: Socket[addr=/192.168.1.77,port=61659,localport=5050]
// Data received from Admin client:
//     Admin Host: DESKTOP-1MRMOOR/192.168.1.77 Admin Port: 5050
//     Admin changed server mode to: Proverb
// Connection from: Socket[addr=/192.168.1.77,port=61660,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PB to Nick
// Closing connection...

// Connection from: Socket[addr=/192.168.1.77,port=61662,localport=4545]
//     Client Data Received from user: Nick, Client ID - 6534821780573318
//     Sending PC to Nick
// Closing connection...



// ####################################JOKECLIENT OUTPUT LOG####################################
// >java JokeClient

// Server one: DESKTOP-1MRMOOR/192.168.1.77, port: 4545
// Enter your name: 
// Nick
// Hi Nick!

// Hit ENTER to get a new response or type quit to exit...

// JA Nick: Why dont scientists trust atoms? Because they make up everything.

// Hit ENTER to get a new response or type quit to exit...

// JB Nick: What do you call a fake noodle? An impasta.

// Hit ENTER to get a new response or type quit to exit...

// JC Nick: What did the pirate say when he turned 80? Aye matey!

// Hit ENTER to get a new response or type quit to exit...

// JD Nick: What did the buffalo say when his son left for college?  Bison.


// JOKE CYCLE COMPLETED

// Hit ENTER to get a new response or type quit to exit...

// PA Nick: If you want to go fast, go alone. If you want to go far, go together.

// Hit ENTER to get a new response or type quit to exit...

// PB Nick: A man who uses force is afraid of reasoning.

// Hit ENTER to get a new response or type quit to exit...

// PC Nick: A spoon does not know the taste of soup, nor a learned fool the taste of wisdom.

// Hit ENTER to get a new response or type quit to exit...

// PD Nick: The reputation of a thousand years may be determined by the conduct of one hour.


// PROVERB CYCLE COMPLETED

// Hit ENTER to get a new response or type quit to exit...

// PD Nick: The reputation of a thousand years may be determined by the conduct of one hour.

// Hit ENTER to get a new response or type quit to exit...

// PA Nick: If you want to go fast, go alone. If you want to go far, go together.

// Hit ENTER to get a new response or type quit to exit...

// JD Nick: What did the buffalo say when his son left for college?  Bison.

// Hit ENTER to get a new response or type quit to exit...

// JA Nick: Why dont scientists trust atoms? Because they make up everything.

// Hit ENTER to get a new response or type quit to exit...

// PB Nick: A man who uses force is afraid of reasoning.

// Hit ENTER to get a new response or type quit to exit...

// PC Nick: A spoon does not know the taste of soup, nor a learned fool the taste of wisdom.


// PROVERB CYCLE COMPLETED

// Hit ENTER to get a new response or type quit to exit...
// quit
// Cancelled by user request.

// ####################################JOKE ADMIN LOG #######################################
// >java JokeClientAdmin
// Admin Client Started...

// Server one: DESKTOP-1MRMOOR/192.168.1.77, port: 5050
// Hit ENTER to change server mode

//     Server is now in proverb mode
// Hit ENTER to change server mode

//     Server is now in joke mode
// Hit ENTER to change server mode

//     Server is now in proverb mode
// Hit ENTER to change server mode
// quit
// Cancelled by user request.

/*
 * DISCUSSION POSTINGS
 * 
 * Reply to: Robinkumar Ramanbhai Macwan
 *  I set mine up so that the first time through they are sent in order so...

    JA,JB,JC,JD, etc.

    and after the first pass through every other pass will be randomized

* Question Asked on Forum:
    In the instructions and requirements document in the first bullet of JokeServer conventions it states we are not allowed to use InetServer code. 
    does this mean we cannot use the InetAddress argument of the ServerSocket?
    I am trying to get/pass the IP to server sockets and in the process am using InetAddress.getLocalHost() 
    in order to find the IP address of the machine the server is on. I am then passing this InetAddress to the thread that listens 
    for the admin connections as well as to the server connection listeners so that I can use it as an argument when creating a socket. 
    Is this allowed or is the INetServer code that is mentioned referring to something else (perhaps to another assignment from past years?)

* Reply to: Syed Saifuddin

    I agree and that is what I am doing, essentially all my client needs to pass is its client ID. 
    I have the server use that client ID as the key in a hashmap that pulls up the state information. 
    I am currently working on the encoding and manipulation of that state data and am trying to use a method 
    similar to the 8 binary digit example in the reference materials for the assignment. 
    However I am currently using 10 digits (a set of 5 for the jokes, and a set of 5 for the proverbs), 
    I am using the 5th digit of each set to indicate if its the first pass through for that client or if 
    it has seen all the jokes/proverbs atleast once. so the first time through I can ensure they are in order and then randomize them every other time

* Reply to: Robinkumar Ramanbhai Macwan

    I believe the case for this assignment is that each time you run 

    >java JokeClient

    it is to always be treated as a new client, as like you pointed out we are not implementing a way to identify the machine, location, terminal window, etc. 
    that the client is connecting from. I would have to imagine this is to simplify things and the fact that 99% of the students will 
    likely be running the multiple terminals from the same machine.My understanding is that when it is mentioned to save the client state 
    it is more focused on the scope of the clients lifespan. Since every time you request a new joke the client reconnects to the server, 
    goes through the process of getting a new joke, then disconnecting. This is repeated every time you hit enter. 
    The ID is sent from the client upon each reconnection (when enter is pressed) to allow the server to serve the correct joke/proverb to that specific client instance. 
    I would imagine that in a real implementation of this code you would indeed send additional information that would identify perhaps 
    the machine or some even more granular identifier to restore the session data. (much like loading chrome or firefox after it crashes, 
    all your tabs are restored, etc.)

    Edit: After some additional thought the only way I could see to save state between entire cmd window close/reopen cycles would be to use the username you will need to enter when starting a client instance. However I dont think thats required or in the scope of the assignment.
    */