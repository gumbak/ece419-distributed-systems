import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.Serializable;
import java.util.concurrent.Semaphore;

public class ServerData implements Serializable {

    BlockingQueue<MazePacket> eventQueue = new LinkedBlockingQueue();
    ConcurrentHashMap<String, ClientData> clientTable = new ConcurrentHashMap(); //Might need reference to actual thread here, for dispatcher
    ConcurrentHashMap<Integer, ObjectOutputStream> socketOutList = new ConcurrentHashMap(); //Might need reference to actual thread here, for dispatcher
    //ArrayList socketOutList = new ArrayList();
    ConcurrentHashMap<String, Point> clientPosition = new ConcurrentHashMap();
    MazePacket [] eventArray = new MazePacket[20];

    ConcurrentHashMap<Integer, ClientData> lookupTable = new ConcurrentHashMap(); // Contains all client data
    int lamportClock; // This client's lamport clock
    Semaphore sem = new Semaphore(0); // Sempahore for client awknowledgement sinconization
    int myId;

    public void addEventToEventArray(MazePacket p) {
        if (p) {
            lamportArray[p.lamportClock] = p;
        }
    }

    public MazePacket getNextEvent() {
        // remove next event and return
        MazePacket p = new MazePacket();
        int i = -1;
        while (p == null && i < 20) {
            i++;
            p = eventArray[i];
        }
        eventArray[i] = null;
        return p;
    }


    public void setId(int num){
        myId = num;
    }

    public int getId(){
        return myId; 
    }

    public void acquireSemaphore(int num){

        try{
            sem.acquire(num);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void releaseSemaphore(int num){
        try{
            sem.release(num);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addLookupTable(ConcurrentHashMap lookupTable){
        this.lookupTable = lookupTable;
    }

    public void addClientToTable(String name, Point position, Direction direction, int type) {
        if (!clientTable.containsKey(name)) {
            /* Create ClientData */
            ClientData clientData = new ClientData();
            clientData.client_location = position;
            clientData.client_direction = direction;
            clientData.client_type = type;

            /* Add to table */
            clientTable.put(name, clientData);

        } else {
            System.out.println("Client with name " + name + " already exists.");
        }
    }

    public void removeClientFromTable(String name){
        if(clientTable.containsKey(name)){
            clientTable.remove(name);
            clientPosition.remove(name);
        }
    }

    public void addEventToQueue(MazePacket event){
        eventQueue.offer(event);
    }

    public void addSocketOutToList(Integer key, ObjectOutputStream out) {
        socketOutList.put(key, out);
    }

    public void removeSocketOutFromList(Integer key) {
        socketOutList.remove(key);
    }

    public boolean setPosition(String name, Point position){
        if(!clientPosition.containsValue(position)){
            clientPosition.put(name,position);	  	    
            return true;
        }else{
            Point clientPos = clientPosition.get(name);
            if(clientPos == position)
                return true;
            else
                return false;
        }
    }

    public void setLamportClock(int value){
        lamportClock = value;
    }

}
