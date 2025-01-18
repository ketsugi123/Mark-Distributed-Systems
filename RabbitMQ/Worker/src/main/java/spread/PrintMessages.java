package spread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrintMessages {
	
	private static final Logger logger = LogManager.getLogger(PrintMessages.class);
	
	// Entry point for printing message details
	public static void displayMessageDetails(SpreadMessage msg) {
		try {
			if (msg.isRegular()) {
				handleRegularMessage(msg);
			} else if (msg.isMembership()) {
				handleMembershipMessage(msg);
			} else if (msg.isReject()) {
				handleRejectedMessage(msg);
			} else {
				handleUnknownMessage(msg);
			}
		} catch (Exception e) {
			logger.error("Error processing message", e);
			System.exit(1);
		}
	}
	
	// Handles regular messages
	private static void handleRegularMessage(SpreadMessage msg) {
		getInfo(msg);
	}
	
	private static void getInfo(SpreadMessage msg) {
		logger.info("The message is: {}", new String(msg.getData()));
	}
	
	// Handles membership messages
	private static void handleMembershipMessage(SpreadMessage msg) {
		MembershipInfo info = msg.getMembershipInfo();
		printMembershipInfo(info);
	}
	
	// Handles rejected messages
	private static void handleRejectedMessage(SpreadMessage msg) {
		logger.warn("Received a REJECTED {} message.", getMessageReliability(msg));
		logger.info("Sent by: {}", msg.getSender());
		printMessageGroups(msg);
		getInfo(msg);
	}
	
	// Handles unknown message types
	private static void handleUnknownMessage(SpreadMessage msg) {
		logger.warn("Message is of unknown type: {}", msg.getServiceType());
	}
	
	// Prints membership information
	public static void printMembershipInfo(MembershipInfo info) {
		SpreadGroup group = info.getGroup();
		
		if (info.isRegularMembership()) {
			printRegularMembershipInfo(info, group);
		} else if (info.isTransition()) {
			logger.info("TRANSITIONAL membership for group {}", group);
		} else if (info.isSelfLeave()) {
			logger.info("SELF-LEAVE message for group {}", group);
		}
	}
	
	private static void printRegularMembershipInfo(MembershipInfo info, SpreadGroup group) {
		SpreadGroup[] members = info.getMembers();
		MembershipInfo.VirtualSynchronySet[] virtualSynchronySets = info.getVirtualSynchronySets();
		MembershipInfo.VirtualSynchronySet mySet = info.getMyVirtualSynchronySet();
		
		logger.info("REGULAR membership for group {} with {} members:", group, members.length);
		for (SpreadGroup member : members) {
			logger.info("\t{}", member);
		}
		
		printMembershipCause(info);
		
		if (info.isCausedByNetwork()) {
			printVirtualSynchronySets(virtualSynchronySets, mySet);
		}
	}
	
	private static void printMembershipCause(MembershipInfo info) {
		if (info.isCausedByJoin()) {
			logger.info("\tDue to: JOIN of {}", info.getJoined());
		} else if (info.isCausedByLeave()) {
			logger.info("\tDue to: LEAVE of {}", info.getLeft());
		} else if (info.isCausedByDisconnect()) {
			logger.info("\tDue to: DISCONNECT of {}", info.getDisconnected());
		} else if (info.isCausedByNetwork()) {
			logger.info("\tDue to: NETWORK change");
		}
	}
	
	private static void printVirtualSynchronySets(MembershipInfo.VirtualSynchronySet[] sets, MembershipInfo.VirtualSynchronySet mySet) {
		for (int i = 0; i < sets.length; i++) {
			MembershipInfo.VirtualSynchronySet set = sets[i];
			SpreadGroup[] setMembers = set.getMembers();
			
			logger.info("\t\t{}Virtual Synchrony Set {} has {} members:",
					(set == mySet ? "(LOCAL) " : "(OTHER) "), i, set.getSize());
			
			for (SpreadGroup member : setMembers) {
				logger.info("\t\t\t{}", member);
			}
		}
	}
	
	private static String getMessageReliability(SpreadMessage msg) {
		if (msg.isUnreliable()) return "UNRELIABLE";
		if (msg.isReliable()) return "RELIABLE";
		if (msg.isFifo()) return "FIFO";
		if (msg.isCausal()) return "CAUSAL";
		if (msg.isAgreed()) return "AGREED";
		if (msg.isSafe()) return "SAFE";
		return "UNKNOWN";
	}
	
	private static void printMessageGroups(SpreadMessage msg) {
		SpreadGroup[] groups = msg.getGroups();
		logger.info("to {} groups:", groups.length);
		for (SpreadGroup group : groups) {
			logger.info("\t{}", group);
		}
	}
}
