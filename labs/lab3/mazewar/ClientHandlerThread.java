import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.net.*;

/* Client handler 
 * Each client will be registered with a client handler
 * Listens for actions by GUI client and notifies server
 * Receives game events queue from server and executes events 
 * 
 */

public class ClientHandlerThread extends Thread {
    Socket cSocket;
    Client me;
    Maze maze;
    ObjectOutputStream out;
    ObjectInputStream in;
    BlockingQueue<MazeEvent> eventQueue;
    ConcurrentHashMap<String, Client> clientTable;
    int seqNum;
    MazePacket []eventArray = new MazePacket[21];
    boolean quitting = false;

    int lamportClock = 0;
    // Score table
    //ScoreTableModel scoreTable;


    MazePacket packetFromServer;

    public ClientHandlerThread(String lookup_host, int lookup_port, int client_port){
        /* Connect to naming service. */
        try {
            
            System.out.println("Connecting to Naming Service...");

            cSocket = new Socket(lookup_host,lookup_port);
            out = new ObjectOutputStream(cSocket.getOutputStream());
            in = new ObjectInputStream(cSocket.getInputStream());
            clientTable = new ConcurrentHashMap();
	    //scoreTable = st;
	    
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

	
    }

    public void registerMaze(Maze maze) {
        this.maze = maze;

	//sendPacketToServer(MazePacket.GET_SEQ_NUM);
    }

    public void getClients(){
	    // Get all existing clients
	    MazePacket packetToLookup = new MazePacket();

	    try{
		packetToLookup.packet_type = MazePacket.LOOKUP_GET;
		out.writeObject(packetToLookup);
	    } catch (IOException e){
		e.printStackTrace();
		System.out.println("ERROR: registering with server");
	    }
    }

    public void registerClientWithMazewar(int client_port){
        MazePacket packetToLookup = new MazePacket();

        try{
	    // Register self
	    packetToLookup.packet_type = MazePacket.LOOKUP_REGISTER;
	    packetToLookup.client_type = MazePacket.REMOTE;
	    packetToLookup.client_host = InetAddress.getLocalHost().getHostName();
	    packetToLookup.client_port = client_port;

	    out.writeObject(packetToLookup);

        }catch (IOException e){
            e.printStackTrace();
            System.out.println("ERROR: registering with server");
        }

    }

    public void registerRobotWithMazewar(Client name){
        MazePacket packetToLookup = new MazePacket();

        try{

            /* Initialize handshaking with server */
            Random rand = new Random();

            packetToLookup.packet_type = MazePacket.CLIENT_REGISTER;
            packetToLookup.client_name = me.getName();
            packetToLookup.client_location = maze.getClientPoint(name);
            packetToLookup.client_direction = me.getOrientation();
            packetToLookup.client_type = MazePacket.REMOTE;
            System.out.println("CLIENT REGISTER: " + me.getName());
            out.writeObject(packetToLookup);

            /* Init client table with yourself */
            clientTable.put(me.getName(), me);

        }catch (IOException e){
            e.printStackTrace();
            System.out.println("ERROR: registering with server");
        }

    }
    public void run() {
        /* Listen for packets */
        packetFromServer = new MazePacket();
	

        try {
            while(!quitting && (packetFromServer = (MazePacket) in.readObject()) != null) {

                switch (packetFromServer.packet_type) {
                    case MazePacket.LOOKUP_REGISTER:
			//???
                        //addClientEvent();
                        break;
                    case MazePacket.LOOKUP_GET:			
                        lookupGetEvent();
                        break;
                    default:
                        System.out.println("Could not recognize packet type");
			break; 
                }

                // switch (packet_type) {
                //     case MazePacket.CLIENT_REGISTER:
                //         addClientEvent();
                //         break;
                //     case MazePacket.CLIENT_FORWARD:			
                //         clientForwardEvent();
                //         break;
                //     case MazePacket.CLIENT_BACK:
                //         clientBackEvent();
                //         break;
                //     case MazePacket.CLIENT_LEFT:
                //         clientLeftEvent();
                //         break;
                //     case MazePacket.CLIENT_RIGHT:
                //         clientRightEvent();
                //         break;
                //     case MazePacket.CLIENT_FIRE:
                //         clientFireEvent();
                //         break;
		//     case MazePacket.CLIENT_RESPAWN:
		// 	clientRespawnEvent();
		//         break;
		//     case MazePacket.CLIENT_QUIT:
		// 	clientQuitEvent();
		// 	break; 
                //     default:
                //         System.out.println("Could not recognize packet type");
		// 	break; 
                // }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Store all clients
    private void lookupGetEvent(){
	// INCOMPLETE

    }

    //Remove the client that is quitting.
    private void clientQuitEvent(){	
        System.out.println("Remove quitting client");
        String name = packetFromServer.client_name;
        if (clientTable.containsKey(name)) { 
            Client c = clientTable.get(name);

	    maze.removeClient(c);
        } else {
            System.out.println("CLIENT: no client named " + name + " in backup");
        }
    }
    
    private void clientRespawnEvent(){
        System.out.println("Respawning client");
        String name = packetFromServer.tc;

        if (clientTable.containsKey(name)) { 
            Client tc = clientTable.get(name);
	    
	    tc.getLock();

	    Client sc = clientTable.get(packetFromServer.sc);
	    Point p = packetFromServer.client_location;
	    Direction d = packetFromServer.client_direction;

	    maze.setClient(sc, tc, p,d);

	    tc.setKilledTo(false);
	    tc.releaseLock();

        } else {
            System.out.println("CLIENT: no client named " + name + " in respawn");
        }
    }

    /**
     * Process server packet eventsi
     * */
    private void addClientEvent() {
        String name = packetFromServer.client_name;
        ConcurrentHashMap<String, ClientData> clientTableFromServer = packetFromServer.client_list;
        System.out.println("CLIENT: Server sent addClient event");

        if (name.equals(me.getName())) {
            System.out.println("CLIENT: Server added me!");
        }
        else {
            System.out.println("CLIENT: Server adding new client " + name);
            int clientType = packetFromServer.client_type;

            switch (clientType) {
                case ClientData.REMOTE:
                    //add remote client
                    RemoteClient c = new RemoteClient(name);
                    clientTable.put(name, c);
                    maze.addRemoteClient(c, packetFromServer.client_location, packetFromServer.client_direction);
                    break;
                case ClientData.ROBOT:
                    //add robot client
                    break;
                default:
                    System.out.println("CLIENT: no new clients on add client event");
                    break;
            }
        }
	
	seqNum = packetFromServer.sequence_num;

        // else server is telling you to add a new client
        // create new clients into clientTable based on any
        // new clients seen in clientTableFromServer
        for (Map.Entry<String, ClientData> entry : clientTableFromServer.entrySet()) {
            String key = entry.getKey();
            System.out.println(key);
            if (!clientTable.containsKey(key)) {
                ClientData cData = entry.getValue();
                
                switch (cData.client_type) {
                    case ClientData.REMOTE:
                        //add remote client
                        RemoteClient c = new RemoteClient(key);
                        clientTable.put(key, c);
                        maze.addRemoteClient(c, cData.client_location, cData.client_direction);
                        break;
                    case ClientData.ROBOT:
                        //add robot client
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void clientForwardEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name) && !clientTable.get(name).isKilled()) { 
            Client c = clientTable.get(name);

	    c.getLock();
	    c.forward();
	    c.releaseLock();

        } else {
            System.out.println("CLIENT: no client named " + name + " in forward");
        }
    }

    private void clientBackEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name) && !clientTable.get(name).isKilled()) { 
            Client c = clientTable.get(name);

	    c.getLock();
	    c.backup();
	    c.releaseLock();

        } else {
            System.out.println("CLIENT: no client named " + name + " in backup");
        }
    }

    private void clientLeftEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name) && !clientTable.get(name).isKilled()) { 
            Client c = clientTable.get(name);

	    c.getLock();
	    c.turnLeft();
	    c.releaseLock();

        } else {
            System.out.println("CLIENT: no client named " + name + " in left");
        }
    }

    private void clientRightEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name) && !clientTable.get(name).isKilled()) { 
            Client c = clientTable.get(name);

	    c.getLock();
	    c.turnRight();
	    c.releaseLock();

        } else {
            System.out.println("CLIENT: no client named " + name + " in right");
        }
    }


    private void clientFireEvent() {
        // get client with name from client list
        // client.foward
        String name = packetFromServer.client_name;

        if (clientTable.containsKey(name) && !clientTable.get(name).isKilled()) { 
            Client c = clientTable.get(name);

	    c.getLock();
	    c.fire();
	    c.releaseLock();

	    // Decrement score.
	    //scoreTable.clientFired(clientTable.get(name));

        } else {
            System.out.println("CLIENT: no client named " + name + " in fire");
        }
    }

    /**
     * Listen for client keypress and send server packets 
     * */
    public void handleKeyPress(KeyEvent e) {
        // If the user pressed Q, invoke the cleanup code and quit. 
        if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
	    System.out.println("CLIENT: Quitting");

	    quitting = true;
	    sendPacketToServer(MazePacket.CLIENT_QUIT);

	    try{
		out.close();
		in.close();
		cSocket.close();
	    } catch(Exception e1){
		System.out.println("CLIENT: Couldn't close sockets...");
	    }
	    
            Mazewar.quit();
            // Up-arrow moves forward.
        } else if(e.getKeyCode() == KeyEvent.VK_UP && !me.isKilled()) {
            sendPacketToServer(MazePacket.CLIENT_FORWARD);
            // Down-arrow moves backward.
        } else if(e.getKeyCode() == KeyEvent.VK_DOWN && !me.isKilled()) {
            sendPacketToServer(MazePacket.CLIENT_BACK);
            //backup();
            // Left-arrow turns left.
        } else if(e.getKeyCode() == KeyEvent.VK_LEFT && !me.isKilled()) {
            sendPacketToServer(MazePacket.CLIENT_LEFT);
            //turnLeft();
            // Right-arrow turns right.
        } else if(e.getKeyCode() == KeyEvent.VK_RIGHT && !me.isKilled()) {
            sendPacketToServer(MazePacket.CLIENT_RIGHT);
            //turnRight();
            // Spacebar fires.
        } else if(e.getKeyCode() == KeyEvent.VK_SPACE && !me.isKilled()) {
            sendPacketToServer(MazePacket.CLIENT_FIRE);
            //fire();
        }
    }

    private void sendPacketToServer(int packetType) {
        try {
            MazePacket packetToLookup = new MazePacket();
            packetToLookup.packet_type = packetType;
            packetToLookup.client_name = me.getName();
            out.writeObject(packetToLookup);
	    // //Wait... Else If another remote client is in front of you, it will glitch!
	    // Thread.sleep(200);
	
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Try and reserve a point!
    public boolean reservePoint(Point point){
       MazePacket packetToLookup = new MazePacket();

        try{
            packetToLookup.packet_type = MazePacket.RESERVE_POINT;
            packetToLookup.client_name = me.getName();
            packetToLookup.client_location = point;
            packetToLookup.client_direction = null;
            packetToLookup.client_type = MazePacket.REMOTE;
            System.out.println("CLIENT " + me.getName() + " RESERVING POINT");
            out.writeObject(packetToLookup);

	    packetFromServer = new MazePacket();
	    packetFromServer = (MazePacket) in.readObject();

	    int error_code = packetFromServer.error_code;

	    if(error_code == 0)
		return true;
	    else
	    	return false;
	 

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("ERROR: reserving point");
	    return false;
        }

    }

    public boolean clientIsMe(Client c){
	if(c == me)
	    return true;
	else
	    return false;
    }


    public void sendClientRespawn(String sc, String tc, Point p, Direction d) {
        try {
            MazePacket packetToLookup = new MazePacket();
            packetToLookup.packet_type = MazePacket.CLIENT_RESPAWN;
            packetToLookup.sc = sc;
	    packetToLookup.tc = tc;
	    packetToLookup.client_location = p;
	    packetToLookup.client_direction = d;
            out.writeObject(packetToLookup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


