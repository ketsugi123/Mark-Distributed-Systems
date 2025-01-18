package mqconsumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.*;
import markApp.MarkApp;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;
import spread.GroupMember;
import spread.SpreadException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

import static utils.JsonParserUtils.getStringProperty;
import static utils.JsonParserUtils.parseJsonString;

public class Worker {
	static Logger logger = new SimpleLoggerFactory().getLogger("Worker");

	// Spread
	private static final int daemonPort = 4803;
	public static String spreadGroup = "tp2-spread";

	// RabbitMQ
	static String IP_BROKER;
	private static String queueName = "workers";

	public static String workCompleteJsonMessage(String imageId) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("work", "Work Complete: Image " + imageId + " is marked");
		return new Gson().toJson(jsonObject);
	}
	
	public static void main(String[] args) {
		try {
			String username;
			String daemonIP;

			if (args.length > 0) {
				username = args[0];
				daemonIP = args[1];
				IP_BROKER = args[2];
				if (args.length >= 5) {
					queueName = args[3];
					spreadGroup = args[4];
				}
			} else {
				return;
			}

			// Join Spread Group
			GroupMember member = new GroupMember(username, daemonIP, daemonPort);
			member.joinToGroup(spreadGroup);

			// Setup RabbitMQ connection and channel
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(IP_BROKER);
			factory.setPort(5672);
			
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();
			
			Scanner scan = new Scanner(System.in);
			
			// Callback to handle consumer cancellation
			CancelCallback cancelCallback = (consumerTag) -> logger.info("CANCEL Received! {}", consumerTag);
			
			// Callback to handle messages without auto-ack
			DeliverCallback deliverCallbackWithoutAck = (consumerTag, delivery) -> {
				String recMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
				String routingKey = delivery.getEnvelope().getRoutingKey();
				long deliveryTag = delivery.getEnvelope().getDeliveryTag();
				logger.info("{}: Message Received:{}:{}", consumerTag, routingKey, recMessage);
				
				// Acknowledge or reject the message
				if (recMessage.equals("nack")) {
					channel.basicNack(deliveryTag, false, true); // Requeue message
				} else {
					// Parse the received message
					JsonObject jsonObject = parseJsonString(recMessage);
					// Obtain the image ID and keywords
					String imageId = getStringProperty(jsonObject, "name");
					String keyword = getStringProperty(jsonObject, "keyword");
					// Create input and output path
					String inputPath = "/var/sharedfiles/" + imageId + ".png";
					String outputPath = "/var/sharedfiles/" + imageId + "-marks" + ".png";
					// Mark the image and save it inside the shared directory
					MarkApp.markAndSaveImage(inputPath, outputPath, keyword);
					logger.info("Image " + imageId + " marked and saved inside: " + outputPath);
					try {
						member.sendMessage(spreadGroup, workCompleteJsonMessage(imageId));
					} catch (SpreadException e) {
						throw new RuntimeException(e);
					}
					channel.basicAck(deliveryTag, false); // Acknowledge message
				}
				logger.info("Acknowledged message: {}", recMessage);
			};

			String consumerTag = channel.basicConsume(
					queueName,
					false,
					deliverCallbackWithoutAck,
					cancelCallback
			);
			
			logger.info("{}: Waiting for messages. Press any key to exit.", consumerTag);
			scan.nextLine(); // Wait for user input to terminate
			member.close();
		} catch (Exception ex) {
			logger.error(Arrays.toString(ex.getStackTrace()), ex);
		}
	}
	
}
