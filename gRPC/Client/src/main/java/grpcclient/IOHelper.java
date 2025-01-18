package grpcclient;

import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;

import java.util.Scanner;

public class IOHelper {
	
	private static final InternalLogger ioLogger = Log4J2LoggerFactory.getInstance("IOHelper");
	private final Scanner ioScanner;
	
	public IOHelper(Scanner scanner){
		ioScanner = scanner;
	}
	
	public String readString(String message) {
		ioLogger.info(message);
		return ioScanner.nextLine();
	}
	
	public int readOption(String message) {
		ioLogger.info(message);
		int value = ioScanner.nextInt();
		ioScanner.nextLine();
		return value;
	}
	
	public int readOption(String[] messages) {
		for(String message : messages) {
			ioLogger.info(message);
		}
		int value = ioScanner.nextInt();
		ioScanner.nextLine();
		return value;
	}
}

