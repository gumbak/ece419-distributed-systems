package unhasher;

import java.io.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

// Hashing library
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;


// Each WorkerHandler is assigned a job
// Try and find if the password hash is equal to the word hash in worker's dictionary partition
public class WorkerHandler extends Thread{
	
    static String myPath = "/Workers";

    Socket cSocket = null;
    ObjectInputStream cin;
    ObjectOutputStream cout;
    
    TaskPacket packetFromCD;
    ZooKeeper zk;
    Lock zklock;

    ZkConnector zkc;
    String path;

    ConcurrentHashMap <String,String> dictionary;
    Lock dlock = new ReentrantLock();

    boolean isNewPartition = false;
    List <String> workers;

    static String mode;
    static boolean debug = true;

    /**
     * @param args
     */
    public WorkerHandler (ZkConnector zkc, String path) throws IOException {
        super("WorkerHandler");

	debug("WorkherHandler thread created for " + path);

	// Save variables
	this.zkc = zkc;
	zk = zkc.getZooKeeper();

	this.path = path;

	// Request for a library partition
	//getDictPartition();

	// Keep a watch on the workers for any changes
	listenToPathChildren(myPath);
    }

    //Get to work
    // Start traversing through the partition and find the hash!
    public void run(){
	// Hash each word in the partition.
	// Check if it exists
	Object[] keys = dictionary.keySet().toArray();
	int size = dictionary.size(); 

	for(int i = 0; i < size; i++){
	    if(isNewPartition){
		dlock.lock();

		i = 0;

		keys = dictionary.keySet().toArray();
		size = dictionary.size();

		isNewPartition = false;

		listenToPathChildren(myPath);

		dlock.unlock();
	    }

	    String key = keys[i].toString();
	    String word = dictionary.get(key);

	    String hash = getHash(word);

	    // if(hash == client_hash){
	    // 	// The client's hash is the same as one in the dictionary!
	    // 	// Return the password!
		
	    // }
	}

    }


    

    // Place a watch on the children of a given path
    // Watch the other workers
    private void listenToPathChildren(final String p){
	debug("listenToPathChildren: " + p);

	try {
	    workers = zk.getChildren(
		      p, 
		      new Watcher() {       
			  @Override
			      public void process(WatchedEvent event) {
			      
			      // dlock.lock();
			      
			      // isNewPartition = true;

			      // // Oh no! The amount of workers has scaled.
			      // // Request a new patition.
			      // //getDictPartition();

			      
			      // dlock.unlock();

			      }
			  
		      });

	    debug("listenToPathChildren: Created a watch on " + path + " children.");
	} catch(Exception e) {
	    e.printStackTrace();
	}                          
    }

    public static String getHash(String word) {

        String hash = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
            hash = hashint.toString(16);
            while (hash.length() < 32) hash = "0" + hash;
        } catch (NoSuchAlgorithmException nsae) {
            // ignore
        }
        return hash;
    }


    private static void debug (String s) {
	if (debug && mode != null) {
	    System.out.println(String.format("TRACKER_%s: %s", mode.toUpperCase(), s));
	} else {
	    System.out.println(String.format("TRACKER_?: %s", s));		
	}
    }
}
