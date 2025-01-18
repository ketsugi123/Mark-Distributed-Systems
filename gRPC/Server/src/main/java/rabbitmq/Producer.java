package rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static server.Server.*;

public class Producer {
	private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(Producer.class.getName());
	
	static void sendMessage(Channel channel, String message) throws IOException, InterruptedException {
		
		Thread.sleep(1000);
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

		logger.info("Connected Successfully");

		return channel;
	}

	public static void produce(String message) {
		try {
			logger.info("About to produce");
			Channel channel = buildConnection(IP_BROKER);
			sendMessage(channel, message);
			channel.close();
			channel.getConnection().close();
		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}
	}
}
