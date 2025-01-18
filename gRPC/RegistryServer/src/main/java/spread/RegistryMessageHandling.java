package spread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistryMessageHandling implements BasicMessageListener {
    private static final Logger logger = LogManager.getLogger(RegistryMessageHandling.class);
    private final SpreadConnection connection;
    
    public RegistryMessageHandling(SpreadConnection connection) {
        this.connection = connection;
    }
    
    @Override
    public void messageReceived(SpreadMessage spreadMessage) {
        try {
            logger.info("Message received (Thread ID: {})", Thread.currentThread().getId());
            PrintMessages.displayMessageDetails(spreadMessage);
            
            if (!spreadMessage.isMembership()) {
                handleNonMembershipMessage(spreadMessage);
            }
        } catch (Exception e) {
            logger.error("Error processing received message", e);
        }
    }
    
    private void handleNonMembershipMessage(SpreadMessage spreadMessage) throws Exception {
        SpreadGroup myPrivateGroup = connection.getPrivateGroup();
        SpreadGroup senderPrivateGroup = spreadMessage.getSender();
        
        if (!myPrivateGroup.equals(senderPrivateGroup)) {
            String txtMsg = new String(spreadMessage.getData());
            if ("request".equalsIgnoreCase(txtMsg)) {
                SpreadMessage reply = new SpreadMessage();
                reply.setSafe();
                reply.addGroup(senderPrivateGroup.toString());
                reply.setData(("Hello, I am " + myPrivateGroup + ": I received your request").getBytes());
                logger.info("Sending reply message to {}", senderPrivateGroup);
                connection.multicast(reply);
            }
        }
    }
}
