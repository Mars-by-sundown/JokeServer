 import java.io.*;
 import java.net.*;
 import java.util.Scanner;
 import java.util.ArrayList;

//test
class JokeClient {

    private static int clientColorCount = 0;
    //MODIFICATION: will track all colors sent and received by client
    //  uses an ArrayList to hold Arrays of strings, a new Array will be added to the ledger after each transaction
    private static ArrayList<String[]> colorLedger = new ArrayList<String[]>();
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

        //get color loop, this takes new color requests until quit is typed
        String colorFromClient = "";
        do{
            System.out.println("Enter a color, or type quit to end: ");
            colorFromClient = consoleIn.nextLine();
            if(colorFromClient.indexOf("quit") < 0){
                getColor(userName, colorFromClient, serverName);
            }
        }while (colorFromClient.indexOf("quit") < 0);
        consoleIn.close(); //Added this as it was not closed in the original JokeClient code
        System.out.println("Cancelled by user request.");
        System.out.println(userName + ", You completed " + clientColorCount + " color transactions");

        for(String[] s: colorLedger){
            System.out.println("Transaction: " + (colorLedger.indexOf(s) + 1) + ", Color Sent: " + s[0] + ", Color Received: " + s[1]);
        }
    }

    void getColor(String userName, String colorFromClient, String serverName){
        try{

            //create a ColorData object to pass the data to and from the server
            ColorData colorObj = new ColorData();
            colorObj.userName = userName;
            colorObj.colorSentFromClient = colorFromClient;
            colorObj.colorCount = clientColorCount;

            //Establish a connection to the server
            Socket socket = new Socket(serverName, 45565);
            System.out.println("\nWe have successfully connected to the ColorServer at port 45565");

            //establish an output stream using our socket
            OutputStream outStream = socket.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);

            //serializes and sends the colordata object to the server
            objectOutStream.writeObject(colorObj);
            System.out.println("We have sent the serialized values to the ColorServer's server socket");

            //Establish an input stream to listen for a response
            InputStream inStream = socket.getInputStream();
            ObjectInputStream objectInStream = new ObjectInputStream(inStream);

            //Read the serialized ColorData response sent by the ColorServer
            ColorData inObject = (ColorData) objectInStream.readObject();

            //record our colorcount in our client instance
            clientColorCount = inObject.colorCount;
            String[] colorRecordPair = new String[2];
            colorRecordPair[0] = inObject.colorSentFromClient;
            colorRecordPair[1] = inObject.colorSentFromServer;

            colorLedger.add(colorRecordPair);

            System.out.println("\nFROM THE SERVER:");
            System.out.println(inObject.messageToClient);
            System.out.println("The color sent back is: " + inObject.colorSentFromServer);
            System.out.println("The color count is: " + inObject.colorCount + "\n");

            //Be sure to close our connection out when we are done
            System.out.println("Closing the connection to the ColorServer.\n");
            socket.close();

        }catch(ConnectException CE){
            //will be thrown if server is not open, also thrown if request is refused (server backlog queue is exceeded)
            System.out.println("\nOh no. The ColorServer refused our connection! Is it running?\n");
            CE.printStackTrace();

        }catch(UnknownHostException UH){
            //Thrown when IP address of the host could not be determined, possibly isnt online
            System.out.println("\nUnknown Host problem.\n"); 
            UH.printStackTrace();

        }catch(ClassNotFoundException CNF){
            //This will be thrown if there is an issue with the Serialized ColorData class we are passing back and forth
            CNF.printStackTrace();

        }catch(IOException IOE){
            IOE.printStackTrace(); 
        }
    }
}

class ColorData implements Serializable {
    //Allows this data to be serialized so that it can used by the object stream and sent over network
    String userName;
    String colorSentFromClient;
    String colorSentFromServer;
    String messageToClient;
    int colorCount;
}

class ColorWorker extends Thread {
    Socket sock;

    //Constructor
    ColorWorker(Socket s){
        //takes an arg of socket type and assigns it to local ColorWorker member, sock
        sock = s;
    }

    public void run(){
        try{
            //Creates an Object input stream allowing us to read in Serialized data
            //  ObjectInputStream Deserializes this data sent to us
            InputStream InStream = sock.getInputStream();
            ObjectInputStream ObjectInStream = new ObjectInputStream(InStream);

            //Responsible for reading the incoming serialized data and reconstructing the Colordata object
            ColorData InObject = (ColorData) ObjectInStream.readObject();

            //set up and prepare our output stream so we can send data back to the client
            OutputStream outStream = sock.getOutputStream();
            ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);

            System.out.println("\nFROM THE CLIENT:\n");
            System.out.println("Username: " + InObject.userName);
            System.out.println("Color sent from the client: " + InObject.colorSentFromClient);
            System.out.println("Connections count (State!): " + (InObject.colorCount + 1));

            InObject.colorSentFromServer = getRandomColor();
            InObject.colorCount++;
            InObject.messageToClient = 
                String.format("Thanks %s for sending the color %s", InObject.userName, InObject.colorSentFromClient);

            //Serialize our ColorData Object with the updated data and send it back to the client
            objectOutStream.writeObject(InObject);

            System.out.println("Closing the client socket connection...");
            sock.close(); //close the connection, we are done

        } catch(ClassNotFoundException CNF){
            CNF.printStackTrace();
        } catch( IOException x){
            System.out.println("Server error.");
            x.printStackTrace();
        }
    }

    String getRandomColor(){
        String[] colorArray = new String[]
        {
            "Red","Blue","Green","Yellow", "Magenta", "Silver", "Aqua", "Gray", "Peach", "Orange"
        };
        int randomArrayIndex = (int) (Math.random() * colorArray.length);
        return (colorArray[randomArrayIndex]);
    }
} 

public class JokeServer {
    public static void main(String[] args) throws Exception {
        int q_len = 6; /*Maximum number of requests to queue in the backlog, additional requests will be refused if full */
        int serverPort = 45565;
        Socket sock;

        //split into two lines because I think it looks nicer
        System.out.println("Nicholas Ragano's Color Server 1.0 starting, Listening on port " + serverPort + ".\n");

        //Create our server socket using our port and allowed queue length
        ServerSocket serverSock = new ServerSocket(serverPort, q_len);
        System.out.println("Server open and awaiting connections...");

        while(true){
            //Listen until a request comes in, accept it and spin up a worker thread to handle it
            sock = serverSock.accept(); //accept creates a new Socket and returns it
            System.out.println("Connection from: " + sock);
            new ColorWorker(sock).start();
        }
    }
}
