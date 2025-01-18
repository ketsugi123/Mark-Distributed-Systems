package spread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupMember {
    private static final Logger logger = LogManager.getLogger(GroupMember.class);
    private SpreadConnection connection;
    private final Map<String, SpreadGroup> groupsBelonging = new HashMap<>();
    private RegistryMessageHandling msgHandling;
    
    public GroupMember(String user, String address, int port) {
        try {
            connection = new SpreadConnection();
            connection.connect(InetAddress.getByName(address), port, user, false, true);
            
            msgHandling = new RegistryMessageHandling(connection);
            connection.add(msgHandling);
        } catch (SpreadException e) {
            logger.error("Error connecting to the daemon.", e);
            System.exit(1);
        } catch (UnknownHostException e) {
            logger.error("Cannot find daemon at address: {}", address);
            System.exit(1);
        }
    }
    
    public List<String> getNamesOfBelongingGroups() {
        return new ArrayList<>(groupsBelonging.keySet());
    }
    
    public void joinToGroup(String groupName) throws SpreadException {
        SpreadGroup newGroup = new SpreadGroup();
        newGroup.join(connection, groupName);
        groupsBelonging.put(groupName, newGroup);
        logger.info("Joined group: {}", groupName);
    }
    
    public void sendMessage(String groupToSend, String txtMessage) throws SpreadException {
        SpreadMessage msg = new SpreadMessage();
        msg.setSafe();
        msg.addGroup(groupToSend);
        msg.setData(txtMessage.getBytes());
        connection.multicast(msg);
        logger.info("Sent message to group {}: {}", groupToSend, txtMessage);
    }
    
    public void leaveGroup(String nameToLeave) throws SpreadException {
        SpreadGroup group = groupsBelonging.get(nameToLeave);
        if (group != null) {
            group.leave();
            groupsBelonging.remove(nameToLeave);
            logger.info("Left group: {}", nameToLeave);
        } else {
            logger.warn("No group found with name: {}", nameToLeave);
        }
    }
    
    public void close() throws SpreadException {
        connection.remove(msgHandling);
        connection.disconnect();
        logger.info("Disconnected from Spread.");
    }
}
