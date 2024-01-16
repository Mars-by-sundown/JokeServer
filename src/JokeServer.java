 import java.io.*;
 import java.net.*;
 import java.util.Scanner;
 import java.util.ArrayList;

//test
class JokeClient {


    private static int clientJokeCount = 0;
    //MODIFICATION: will track all Jokes sent and received by client
    //  uses an ArrayList to hold Arrays of strings, a new Array will be added to the ledger after each transaction
    private static ArrayList<String[]> JokeLedger = new ArrayList<String[]>();
    public static void main(String argv[]) {
        JokeClient cc = new JokeClient(argv);
        cc.run(argv);
    }

    public JokeClient(String argv[]) {
        System.out.println("\nThis is the Constructor\n");
    }

    public void run(String argv[]) {
        String serverName;
        
        //get the server name or address we were passed
        if(argv.length < 1) {
            serverName = "localhost";
        }else{
            serverName = argv[0];
        }

        //Scanner Object to allow input
        Scanner consoleIn = new Scanner(System.in);

        //Get the name
        System.out.println("Enter your name: ");
        System.out.flush();
        String userName = consoleIn.nextLine();
        System.out.println("Hi " + userName);

        //get Joke loop, this takes new Joke requests until quit is typed
        String JokeFromClient = "";
        do{
            System.out.println("Enter a Joke, or type quit to end: ");
            JokeFromClient = consoleIn.nextLine();
            if(JokeFromClient.indexOf("quit") < 0){
                getJoke(userName, JokeFromClient, serverName);
            }
        }while (JokeFromClient.indexOf("quit") < 0);
        consoleIn.close(); //Added this as it was not closed in the original JokeClient code
        System.out.println("Cancelled by user request.");
        System.out.println(userName + ", You completed " + clientJokeCount + " Joke transactions");

        for(String[] s: JokeLedger){
            System.out.println("Transaction: " + (JokeLedger.indexOf(s) + 1) + ", Joke Sent: " + s[0] + ", Joke Received: " + s[1]);
        }
    }

    void getJoke(String userName, String JokeFromClient, String serverName){
        try{

            //create a JokeData object to pass the data to and from the server
            JokeData JokeObj = new JokeData();
            JokeObj.userName = userName;
            JokeObj.JokeSentFromClient = JokeFromClient;
            JokeObj.JokeCount = clientJokeCount;

            //Establish a connection to the server
            Socket socket = new Socket(serverName, 45565);
            System.out.println("\nWe have successfully connected to the JokeServer at port 45565");

            //establish an output stream using our socket
            OutputStream outStream = socket.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);

            //serializes and sends the Jokedata object to the server
            objectOutStream.writeObject(JokeObj);
            System.out.println("We have sent the serialized values to the JokeServer's server socket");

            //Establish an input stream to listen for a response
            InputStream inStream = socket.getInputStream();
            ObjectInputStream objectInStream = new ObjectInputStream(inStream);

            //Read the serialized JokeData response sent by the JokeServer
            JokeData inObject = (JokeData) objectInStream.readObject();

            //record our Jokecount in our client instance
            clientJokeCount = inObject.JokeCount;

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

class JokeData implements Serializable {
    //Allows this data to be serialized so that it can used by the object stream and sent over network
    String userName;
    String JokeSentFromClient;
    String JokeSentFromServer;
    String messageToClient;
    int JokeCount;
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



            System.out.println("Joke Here");
            sock.close(); //close the connection, we are done

        }catch( IOException x){
            System.out.println("Server error.");
            x.printStackTrace();
        }
    }
} 

public class JokeServer {
    public static void main(String[] args) throws Exception {
        int q_len = 6; /*Maximum number of requests to queue in the backlog, additional requests will be refused if full */
        int serverPort = 45565;
        Socket sock;
        
        //Create our server socket using our port and allowed queue length
        ServerSocket serverSock = new ServerSocket(serverPort, q_len);
        System.out.println("Server open and awaiting connections...");

        while(true){
            //Listen until a request comes in, accept it and spin up a worker thread to handle it
            sock = serverSock.accept(); //accept creates a new Socket and returns it
            System.out.println("Connection from: " + sock);
            new JokeWorker(sock).start();
        }
    }
}
