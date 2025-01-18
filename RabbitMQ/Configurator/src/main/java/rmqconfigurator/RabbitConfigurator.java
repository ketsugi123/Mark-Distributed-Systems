package rmqconfigurator;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static utils.IpUtils.getExternalIp;

public class RabbitConfigurator {
	
	private static final Logger logger = LoggerFactory.getLogger(RabbitConfigurator.class);
	
	private static void buildConnection(String brokerIp) throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(brokerIp);
		factory.setPort(PORT_BROKER);
		connection = factory.newConnection();
		channel = connection.createChannel();
		logger.info("Connected to RabbitMQ at {}:{}", brokerIp, PORT_BROKER);
	}
	
	static Connection connection = null;
	static Channel channel = null;
	static String IP_BROKER = getExternalIp();
	static String RABBIT_HOST = getExternalIp();
	static String EXCHANGE_NAME = "tp2-ex";
	static String QUEUE_NAME = "workers";
	static String ROUTING_KEY = "ex-workers";
	static final int PORT_BROKER = 5672;
	
	public static void main(String[] args) {
		try {
			if (args.length > 0) {
				IP_BROKER = args[0];
				RABBIT_HOST = args[1];
				EXCHANGE_NAME = args[2];
				QUEUE_NAME = args[3];
				ROUTING_KEY = args[4];
			}
			logger.info("Initializing RabbitMQ configuration...");
			buildConnection(IP_BROKER);
			
			ConfigurationOperations.CreateExchange(EXCHANGE_NAME);
			ConfigurationOperations.CreateQueue(QUEUE_NAME);
			ConfigurationOperations.BindQueue2Ex(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
			
			logger.info("Configuration complete. Exchange: {}, Queue: {}, Routing Key: {}", EXCHANGE_NAME, QUEUE_NAME, ROUTING_KEY);
			
			channel.close();
			connection.close();
			logger.info("Connection closed successfully.");
		} catch (Exception ex) {
			logger.error("Error occurred: {}", ex.getMessage(), ex);
		}
	}
	
	static String getRabbitHost() {
		return RABBIT_HOST;
	}
	
	static Channel getChannel() {
		return channel;
	}
	
	static Connection getConnection() {
		return connection;
	}
}
