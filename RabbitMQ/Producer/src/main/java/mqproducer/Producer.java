package mqproducer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Producer {
	
	
	private static String IP_BROKER;
	
	private static String exchangeName = "tp2-ex";
	private static String routingKey = "ex-workers";
	static Logger logger = new SimpleLoggerFactory().getLogger("RabbitMQ-Producer");
	
	static void sendMessage(Channel channel) throws IOException {
		String message = readline("Message: ");
		
		//Thread.sleep(1000);
		channel.basicPublish(
				exchangeName,
				routingKey,
				true,
				null,
				message.getBytes()
		);
		logger.info("Message Sent:{}", message);
	}
	
	static Channel buildConnection(String brokerIp) throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(brokerIp);
		factory.setPort(5672);
		
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		channel.addReturnListener(new ReturnedMessage());

		return channel;
	}
	
	public static void main(String[] args) {
		
		try {
			if (args.length > 0) {
				IP_BROKER = args[0];
			}
			Channel channel = buildConnection(IP_BROKER);
			sendMessage(channel);
			channel.close();
			channel.getConnection().close();
			
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
	}
	
	
	public static String readline(String msg) {
		Scanner scanner = new Scanner(System.in);
		logger.info(msg);
		String s = scanner.nextLine();
		return s == null ? "" : s;
	}
	
}
