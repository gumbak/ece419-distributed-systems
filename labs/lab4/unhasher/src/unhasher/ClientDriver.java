package unhasher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;


public class ClientDriver {

	static BufferedReader br = null;

	static Integer port;
	
	// ZooKeeper resources 
	ZkConnector zkc;
	static ZooKeeper zk;
	static String zkhost;
	static Integer zkport;
	static String lpath;
	
	// ZooKeeper directories
	static String ZK_TASK = "/tasks";
	static String ZK_SUBTASK = "/t";



	static boolean debug = true;

	public ClientDriver() {

		//connect to ZooKeeper
		zkc = new ZkConnector();
		try {			
			debug("Connecting to ZooKeeper instance zk");
			String hosts = String.format("%s:%d", zkhost, zkport);
			debug(hosts);
			zkc.connect(hosts);
			zk = zkc.getZooKeeper();
			debug("Connected to ZooKeeper instance zk");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendPacket(TaskPacket p) {
		String data = p.taskToString();
		
		Code ret = createTaskPath(data);
		
		if (ret == Code.OK) debug("task sent!"); 
	}
	
	private KeeperException.Code createTaskPath(String data) {
        try {
            byte[] byteData = null;
            if(data != null) {
                byteData = data.getBytes();
            }
            lpath = zk.create(ZK_TASK + ZK_SUBTASK, 
            		byteData, 
            		ZooDefs.Ids.OPEN_ACL_UNSAFE, 
            		CreateMode.PERSISTENT_SEQUENTIAL);
            
        } catch(KeeperException e) {
            return e.code();
        } catch(Exception e) {
            return KeeperException.Code.SYSTEMERROR;
        }
        return KeeperException.Code.OK;
	}
	
	@SuppressWarnings("unused")
	private String waitForStatus() {
		String path = lpath + "/res";
		
		byte [] data;
		String result = null;
		Stat stat = null;


		try {
			// wait for query result
			zkc.listenToPath(path);
			
			//result is back, get it
			data = zk.getData(path, false, stat);
			result = byteToString(data);
			zk.delete(path, 0);		//delete result from /tasks/t#
			zk.delete(lpath, 0);	// delete query /tasks
			
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public String byteToString(byte[] b) {
		String s = null;
		if (b != null) {
			try {
				s = new String(b, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return s;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		if(args.length == 2) {
			zkhost = args[0];
			zkport = Integer.parseInt(args[1]);

		} else {
			System.err.println("ERROR: Invalid arguments!");
			System.exit(-1);
		}
		
		ClientDriver cd = new ClientDriver();

		br = new BufferedReader (new InputStreamReader(System.in));

		// in loop, wait for client input 
		String buf, cmd, hash, result;
		TaskPacket toZk = null;

		System.out.print("> ");
		while ((buf = br.readLine()) != null ) {
			String[] tokens = buf.split("[ ]+");
			cmd = tokens[0].toLowerCase();
			
			debug("client command is -> " + buf);
			
			if (cmd.equals("quit") || cmd.equals("q") ) {
				debug("quitting...");
				return;
			}
			
			hash = tokens[1];
			
			if (cmd.equals("job")) {
				toZk = new TaskPacket(TaskPacket.TASK_SUBMIT, hash);
				cd.sendPacket(toZk);
			} else if (cmd.equals("status")) {
				toZk = new TaskPacket(TaskPacket.TASK_QUERY, hash);
				cd.sendPacket(toZk);
				result = cd.waitForStatus();
				System.out.println(result);
			} 
			System.out.print("> ");
		}
	}

	private static void debug (String s) {
		if (debug) {
			System.out.println(String.format("CLIENT: %s", s ));
		}
	}


}
