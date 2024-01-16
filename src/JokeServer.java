 import java.io.*;
 import java.net.*;
 import java.util.Scanner;
 import java.util.ArrayList;

 class JokeClientAdmin {
    public static void main(String argv[]) {
        JokeClient cc = new JokeClient(argv);
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

            //create a JokeData object to pass the data to and from the server
            JokeData JokeObj = new JokeData();
            JokeObj.clientID = userName;

            //Establish a connection to the server
            Socket socket = new Socket(CSM.getServer(), CSM.getPort());
            System.out.println("\n");

            //establish an output stream using our socket add send our info
            OutputStream outStream = socket.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeObject(JokeObj);

            //Establish an input stream to listen for a response
            InputStream inStream = socket.getInputStream();
            ObjectInputStream objectInStream = new ObjectInputStream(inStream);

            //Read the serialized JokeData response sent by the JokeServer
            JokeData inObject = (JokeData) objectInStream.readObject();
            System.out.println(inObject.body);
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
            //This will be thrown if there is an issue with the Serialized JokeData class we are passing back and forth
            CNF.printStackTrace();

        }catch(IOException IOE){
            IOE.printStackTrace(); 
        }
    }
}
 
class JokeClient {
    private static String clientCookie = "0000";
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

            //create a JokeData object to pass the data to and from the server
            JokeData JokeObj = new JokeData();
            JokeObj.clientID = userName;

            //Establish a connection to the server
            Socket socket = new Socket(CSM.getServer(), CSM.getPort());
            System.out.println("\n");

            //establish an output stream using our socket add send our info
            OutputStream outStream = socket.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
            objectOutStream.writeObject(JokeObj);

            //Establish an input stream to listen for a response
            InputStream inStream = socket.getInputStream();
            ObjectInputStream objectInStream = new ObjectInputStream(inStream);

            //Read the serialized JokeData response sent by the JokeServer
            JokeData inObject = (JokeData) objectInStream.readObject();
            System.out.println(inObject.body);
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
            //This will be thrown if there is an issue with the Serialized JokeData class we are passing back and forth
            CNF.printStackTrace();

        }catch(IOException IOE){
            IOE.printStackTrace(); 
        }
    }
}

/**
 *  Keeps information about which server the client is currently using
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

class JokeData implements Serializable {
    String clientID;
    String cookie;
    int serverMode;
    String prefix;
    String body;
}

class JokeWorker extends Thread {
    Socket sock;

    //Constructor
    JokeWorker(Socket s){
        //takes an arg of socket type and assigns it to local JokeWorker member, sock
        sock = s;
    }

    public void run(){
        try{
            //listen for incoming client info
            InputStream InStream = sock.getInputStream();
            ObjectInputStream ObjectInStream = new ObjectInputStream(InStream);
            JokeData InObject = (JokeData) ObjectInStream.readObject();

            System.out.println("Client Data Received: ");
            System.out.println("    Username: " + InObject.clientID);

            InObject.body = "This is a test Joke";

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

public class JokeServer {
    
    public static void main(String[] args) throws Exception {
        int q_len = 6; /*Maximum number of requests to queue in the backlog, additional requests will be refused if full */
        int serverPort = 4545;
        int adminPort = 5050;
        Socket sock;
        
        //Create our server socket using our port and allowed queue length
        ServerSocket serverSock = new ServerSocket(serverPort, q_len);
        ServerSocket adminSock = new ServerSocket(adminPort, q_len);
        System.out.println("Server open and awaiting connections...");

        while(true){
            //Listen until a request comes in, accept it and spin up a worker thread to handle it
            sock = serverSock.accept(); //accept creates a new Socket and returns it
            System.out.println("Connection from: " + sock);
            new JokeWorker(sock).start();
        }
    }
}
