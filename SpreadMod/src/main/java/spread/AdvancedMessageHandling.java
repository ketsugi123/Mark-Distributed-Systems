package spread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancedMessageHandling implements AdvancedMessageListener {
    private static final Logger logger = LogManager.getLogger(AdvancedMessageHandling.class);
    
    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        logger.info("Regular message received (Thread ID: {})", Thread.currentThread().getId());
        logger.info("Message content: {}", new String(spreadMessage.getData()));
    }
    
    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        logger.info("Membership message received (Thread ID: {})", Thread.currentThread().getId());
        MembershipInfo info = spreadMessage.getMembershipInfo();
        
        if (info.isSelfLeave()) {
            logger.info("Left group: {}", info.getGroup());
        } else {
            SpreadGroup[] members = info.getMembers();
            logger.info("Group members in {}: {}", info.getGroup(), (Object) members);
        }
    }
}
