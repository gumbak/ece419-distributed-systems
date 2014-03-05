import java.net.*;
import java.io.*; 
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;


/* 
   Lookup / Naming service
   Stores key associated with broker, IP, and port.

   Hastable format
   key (broker name) String, IP (ex. localhost), port (ex. 8000)
*/
public class BrokerLookupServer {
    public static void main (String[] args) throws IOException {
        ServerSocket lookupSocket = null;
        boolean listening = true;
    
        /* Create Online Broker Lookup server socket */
        try { 
            if (args.length == 1) {
                lookupSocket = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("Error: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        /* Store loopup table into a hashmap */
        ConcurrentHashMap<String, String> lookup = new ConcurrentHashMap<String, String>();

	// Create file if it doesn't exist
	File lookupfile = new File("lookuptable");
	if(lookupfile.exists())
	    lookupfile.delete();

	lookupfile.createNewFile();


        BufferedReader in = new BufferedReader(new FileReader("lookuptable"));
        String line = "";
        Long quote;
        while ((line = in.readLine()) != null) {
            String parts[] = line.split(" ");
            lookup.put(parts[0], parts[1] + " " + parts[2]);    
        }
        in.close();

        /* Set table quotes in OnlineLookupHandlerThread */
        OnlineLookupHandlerThread.setTable(lookup);

        /* Listen for clients */
        while (listening) {
            new OnlineLookupHandlerThread(lookupSocket.accept()).start();
        }
    }
}

