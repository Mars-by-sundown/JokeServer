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
 *  custom container to hold info related to each joke or proverb, with a constructor for easy creation
 */
class JokeProverb implements Serializable{
    Boolean isJoke;
    Boolean isLast = false; //will be false unless JokeManager changes to notify client
    String prefix;
    String body;

    public JokeProverb(){
        //empty constructor
        this.isJoke = true;
        this.prefix = "";
        this.body = "";
    }
    //to make a copy of another JokeProverb
    public JokeProverb(JokeProverb otherJokeProverb){
        this(otherJokeProverb.isJoke, otherJokeProverb.prefix, otherJokeProverb.body);
    }
    public JokeProverb(Boolean isJoke, String prefix, String body){
        this.isJoke = isJoke;
        this.prefix = prefix;
        this.body = body;
    }
}


/**
 *  @implNote This is instantiated in the JokeServer method each JokeWorker will receive the 'reference' to this class.
 *  @implNote This class is specifically setup for 4 jokes and then 4 proverbs to be entered in order
 *  @implNote Randomizing for each client is automatically handled in this class
 */
class JokeManager{
    private ArrayList<JokeProverb> jokeList = new ArrayList<>();
    private ArrayList<JokeProverb> proverbList = new ArrayList<>();
    private ArrayList<ArrayList<JokeProverb>> JPList = new ArrayList<>();
    private HashMap<String, ArrayList<String>> clientData = new HashMap<>();

    JokeManager(){
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
        clientData.put(clientID, clientStateData);
    }

    public boolean clientExists(String clientID){
        return clientData.containsKey(clientID);
    }

    public JokeProverb getNextForClient(String clientID, boolean proverbMode){
        JokeProverb jp_return = new JokeProverb(); //the object to hold our returned value
        int JPIDX; //used to select either the list of jokes or list of proverbs, also allows us to get the correct clientID state index
        String clientStateString;
        if(proverbMode){
            JPIDX = 1;
        }else{
            JPIDX = 0;
        }
        // gets the length 2 array of client state
        // clientData layout
        // key = clientID
        // value = ArrayList where
        // index = 0, is the Joke state data for that client
        // index = 1, is the Proverb state data for that client
        // clientData {clientID: ["00000", "00000"]}
        clientStateString = (clientData.get(clientID)).get(JPIDX); 
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
            //this is NOT our first time through, so we need to return a random response
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
 *  Serializable Object to hold data passed between Joke Server and Client
 */
class ClientData implements Serializable {
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
 *  Joke Client, called from command line with >java JokeClient
 */
class JokeClient {
    public static void main(String argv[]) {
        JokeClient cc = new JokeClient(argv);
        cc.run(argv);
    }

    public JokeClient(String argv[]) {
        System.out.println("\nThis is the Constructor\n");
    }

    public void run(String argv[]) {
        ClientServerManager CSM = new ClientServerManager();

        //get the server name or address we were passed
        if(argv.length == 1) {
            //only different primary was provided so update that
            CSM.setPrimaryServer(argv[0]);

        }else if (argv.length == 2){
            CSM.setPrimaryServer(argv[0]);
            CSM.setPrimaryServer(argv[1]);
        }else{
            //use defaults of the CSM class
        }

        //Scanner Object to allow input from client console
        Scanner clientIn = new Scanner(System.in);

        //Get the name of the user
        System.out.println("Enter your name: ");
        System.out.flush();
        String userName = clientIn.nextLine();
        System.out.println("Hi " + userName + "!\n    If a second server was provided you can type s to switch between them\n");

        //get Joke loop, this takes new Joke requests until quit is typed
        String clientStr = "";
        do{
            System.out.println("Hit ENTER to get a new response or type quit to exit...");
            clientStr = clientIn.nextLine();
            if(clientStr.indexOf("s") >= 0){
                if(CSM.toggleServer() == 0){
                    //if it failed to switch
                    System.out.println("No Secondary Server was provided at client startup, unable to switch");
                }
                //print the server info regardless
                System.out.println("Server: " + CSM.getServer() + "  Port: " + CSM.getPort());
            } else if(clientStr.indexOf("quit") < 0){
                queryServer(userName, CSM);
            }
        }while (clientStr.indexOf("quit") < 0);
        System.out.println("Cancelled by user request.");
        clientIn.close(); //Added this as it was not closed in the original JokeClient code
        

    }

    void queryServer(String userName, ClientServerManager CSM){
        try{

            //create a ClientData object to pass the data to and from the server
            ClientData ClientDataObj = new ClientData();
            ClientDataObj.clientID = userName;

            //Establish a connection to the server
            Socket socket = new Socket(CSM.getServer(), CSM.getPort());
            System.out.println("\n");

            //establish an output stream using our socket add send our info
            OutputStream outStream = socket.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeObject(ClientDataObj);

            //Establish an input stream to listen for a response
            InputStream inStream = socket.getInputStream();
            ObjectInputStream objectInStream = new ObjectInputStream(inStream);

            //Read the serialized ClientData response sent by the JokeServer
            ClientData inObject = (ClientData) objectInStream.readObject();
            
            StringBuilder respStr = new StringBuilder();
            respStr.append(inObject.message.prefix + " ");
            respStr.append(userName + ": ");
            respStr.append(inObject.message.body);
            System.out.println(respStr.toString());
            if(inObject.message.isLast){
                respStr.delete(0, respStr.length());
                if(inObject.message.isJoke){
                    respStr.append("JOKE ");
                }else{
                    respStr.append("PROVERB ");
                }
                respStr.append("CYCLE COMPLETED");
                System.out.println(respStr.toString());   
            }
            socket.close();

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
 *  Keeps information about which server the client is currently using
 *  Provides functionality to toggle servers
 */
class ClientServerManager {
    private String prim_host = "localhost";
    private int prim_port = 4545;
    private String sec_host = "localhost"; //will be overwritten when IP is provided
    private int sec_port = 4546;
    private boolean allowSecondary = false; //do not allow secondary until one is provided
    private boolean useSecondary = false;

    public void setPrimaryServer(String hostname){
        prim_host = hostname;
    }

    public void setSecondaryServer(String hostname){
        sec_host = hostname;
        allowSecondary = true;
    }
    public String getServer(){
        if(useSecondary && allowSecondary){
            return sec_host;
        }else{
            return prim_host;
        }
    }

    public void setPrimaryPort(int portNum){
        prim_port = portNum;
    }

    public void setSecondaryPort(int portNum){
        sec_port = portNum;
    }

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


}

/**
 *  Joke Admin Client, called from command line with >java JokeClientAdmin
 */
class JokeClientAdmin {
    public static void main(String argv[]) {
        JokeClientAdmin cc = new JokeClientAdmin(argv);
        cc.run(argv);
    }
    public JokeClientAdmin(String argv[]) {
        System.out.println("\nThis is the Constructor\n");
    }

    public void run(String argv[]) {
        ClientServerManager CSM = new ClientServerManager();
        CSM.setPrimaryPort(5050);
        CSM.setSecondaryPort(5051);
        //get the server name or address we were passed
        if(argv.length == 1) {
            //only different primary was provided so update that
            CSM.setPrimaryServer(argv[0]);
        }else if (argv.length == 2){
            CSM.setPrimaryServer(argv[0]);
            CSM.setPrimaryServer(argv[1]);
        }else{
            //use defaults          
        }

        //Scanner Object to allow input from client console
        Scanner clientIn = new Scanner(System.in);

        //Get the name of the user
        System.out.println("Admin Client Started...\n    If a second server was provided you can type s to switch between them\n");

        //get Joke loop, this takes new Joke requests until quit is typed
        String clientStr = "";
        do{
            System.out.println("Hit ENTER to change server mode");
            clientStr = clientIn.nextLine();
            if(clientStr.indexOf("s") >= 0){
                if(CSM.toggleServer() == 0){
                    //if it failed to switch
                    System.out.println("No Secondary Server was provided at client startup, unable to switch");
                }
                //print the server info regardless
                System.out.println("Server: " + CSM.getServer() + "  Port: " + CSM.getPort());
            } else if(clientStr.indexOf("quit") < 0){
                queryServer(CSM);
            }
        }while (clientStr.indexOf("quit") < 0);
        System.out.println("Cancelled by user request.");
        clientIn.close(); //Added this as it was not closed in the original JokeClient code
        

    }

    void queryServer(ClientServerManager CSM){
        try{

            //create a AdminData object to rec
            AdminData adminDataObj = new AdminData();
            //Establish a connection to the server
            Socket sock = new Socket(CSM.getServer(), CSM.getPort());

            OutputStream outStream = sock.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            adminDataObj.adminHost = CSM.getServer();
            adminDataObj.adminPort = CSM.getPort();
            objectOutStream.writeObject(adminDataObj);

            InputStream InStream = sock.getInputStream();
            ObjectInputStream ObjectInStream = new ObjectInputStream(InStream);
            adminDataObj = (AdminData) ObjectInStream.readObject();
            System.out.println("    Server proverb mode is now: " + adminDataObj.serverMode);
            sock.close();

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
            
            System.out.println("Data received from Admin client: ");
            System.out.println("    Admin Host: " + adminDataObj.adminHost + " Admin Port: " + adminDataObj.adminPort);
            al.toggleMode();
            System.out.println("    Admin changed server proverb mode to: " + al.getMode());

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
    private boolean isPrimary;
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

/**
 *  Processes requests for each Client
 */
class JokeWorker extends Thread {
    Socket sock;
    boolean proverbMode;
    JokeManager jm;
    //Constructor

    JokeWorker(Socket s, AdminListener admin, JokeManager serverJM){

        //takes an arg of socket type and assigns it to local JokeWorker member, sock
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

            System.out.println("Client Data Received, Client ID: " + clientTransObj.clientID);
            

            //if this is a new client then create an entry for them
            if(!jm.clientExists(clientTransObj.clientID)){
                jm.addNewClient(clientTransObj.clientID);
                System.out.println(clientTransObj.clientID + " is a new client, created a new client entry in the JokeManager");
            }

            //get the next joke or proverb
            clientTransObj.message = jm.getNextForClient(clientTransObj.clientID, proverbMode);

            OutputStream outStream = sock.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            System.out.println("    Sending " + clientTransObj.message.prefix + " to " + clientTransObj.clientID);
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
 *  JokeServer that manages creation of JokeWorker threads
 */
public class JokeServer {

    

    public static void main(String[] args) throws Exception {
        int q_len = 6; /*Maximum number of requests to queue in the backlog, additional requests will be refused if full */
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
        ServerSocket serverSock = new ServerSocket(serverPort, q_len, InetAddress.getLocalHost());
        System.out.println("Server open on port " + serverPort + " and awaiting connections from clients...");
        while(true){
            //Listen until a request comes in, accept it and spin up a worker thread to handle it
            sock = serverSock.accept(); //accept creates a new Socket and returns it
            System.out.println("Connection from: " + sock);
            new JokeWorker(sock, admin, JokeProverbManager).start();
        }
    }

    
}
