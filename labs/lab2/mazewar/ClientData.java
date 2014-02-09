import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

/* ClientData
 * wrapper for clientTable entries
 * each clientTable key holds maps to ClientData 
 */
public class ClientData implements Serializable {
    public static final int ROBOT = 1;
    public static final int REMOTE = 2;

    Point client_location;
    Direction client_direction;
    int client_type;
}
