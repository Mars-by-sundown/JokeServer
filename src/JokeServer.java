import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;


/**
 *  custom container to hold info related to each joke or proverb, with a constructor for easy creation
 */
class JokeProverb{
    Boolean isJoke;
    String prefix;
    String body;
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
    private ArrayList<JokeProverb> jokesAndProverbs = new ArrayList<>();
    private HashMap<String, String> clientData = new HashMap<>();
    /* clientData stores as follows:
     *      key: clientID
     *      value: String of encoded binary values 0 or 1 in the form of ##########
     *          digits 0-3: seen jokes (1 if seen)
     *          digit 4: all jokes seen atleast once if set to 1
     *          digit 5-8: seen proverbs (1 if seen)
     *          digit 9: all proverbs seen atleast once if set to 1
     */

    public void addJokeProverb(String prefix, String body){
        JokeProverb j = new JokeProverb(true, prefix, body);
        jokesAndProverbs.add(j);
    }

    /**
     * @param proverbMode - True if server is in Proverb mode
     * @param clientID - Unique ID of client
     * @return A Joke or Proverb that the client has not seen 
     */
    public JokeProverb getJokeProverb(Boolean proverbMode, String clientID){

        StringBuilder str = new StringBuilder(clientData.get(clientID)); //get saved client cookie with encoded history of sent jokes
        int idxShift = 0; //default to checking joke values
        JokeProverb returnValue;
    
        //if we are in proverb mode then shift add an additional 5 to the index shift
        if(proverbMode){
            idxShift = idxShift + 5; //move 5 to the right to look at joke values
        }

        if(str.charAt(4 + idxShift) == '0'){

            //first time serving this client because 5th digit of the series is a zero (####0), so go in order
            for(int i = 0; i < 4; i++){
                //count to 4
                if(str.charAt(i + idxShift) == '0'){
                    //return first unseen joke or proverb we come across
                    returnValue = jokesAndProverbs.get(i + idxShift);
                }
                if(i == 3){
                    //we have reached the final entry and it was not a zero, thus we have 11110
                    //this means this is the first time completing the set and we need to 
                }
            }

        }
        //if we make it here then all values were seen

        
        return returnValue;
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
            System.out.println(inObject.message);
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
    public static boolean serverProverbMode = false;
    public void run(){
        int q_len = 6; /*Maximum number of requests to queue in the backlog, additional requests will be refused if full */
        int serverPort = 5050;
        Socket adminSock;

        try{
            ServerSocket adminSocket = new ServerSocket(serverPort, q_len);
            while(true){
                adminSock = adminSocket.accept();
                System.out.println("Admin connection from: " + adminSock);
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
            ClientData InObject = (ClientData) ObjectInStream.readObject();

            System.out.println("Client Data Received: ");
            System.out.println("    ClientID: " + InObject.clientID);


            OutputStream outStream = sock.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeObject(InObject);
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
        Socket sock;


        //create a thread to go and listen for admin connections
        AdminListener admin = new AdminListener();
        Thread adminThread = new Thread(admin);
        adminThread.start();
        
        //create our clientMap to hold client joke history
        JokeManager JokeProverbManager = new JokeManager();

        //Create our server socket using our port and allowed queue length
        ServerSocket serverSock = new ServerSocket(serverPort, q_len);
        System.out.println("Server open and awaiting connections from clients...");
        while(true){
            //Listen until a request comes in, accept it and spin up a worker thread to handle it
            sock = serverSock.accept(); //accept creates a new Socket and returns it
            System.out.println("Connection from: " + sock);
            new JokeWorker(sock, admin, JokeProverbManager).start();
        }
    }

    
}
