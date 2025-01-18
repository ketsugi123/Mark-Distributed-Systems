package mqproducer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static mqproducer.Producer.readline;

public class ProducerWithHeaders {
    
    private static String IP_BROKER;
	static Logger logger = new SimpleLoggerFactory().getLogger(ProducerWithHeaders.class.getName());
    
    public static void main(String[] args) {
        try {
            // Set broker IP if passed as an argument
            if (args.length > 0) {
                IP_BROKER = args[0];
            }
            
            // Setup RabbitMQ connection and channel
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IP_BROKER);
            factory.setPort(5672);
            
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            
            // Prompt for exchange name
			String exchangeName = readline("Exchange name:");
            
            while (true) {
                // Prompt for message body
                String messageBody = readline("Enter message text (type 'exit' to finish):");
                if (messageBody.equalsIgnoreCase("exit")) {
                    break; // Exit loop if user types "exit"
                }
                
                // Add headers to the message
                Map<String, Object> headersBinding = new HashMap<>();
                while (true) {
                    String headerKey = readline("Enter header key (type 'exit' to finish):");
                    if (headerKey.equalsIgnoreCase("exit")) {
                        break; // Exit loop if user types "exit"
                    }
                    String headerValue = readline("Enter value for header '" + headerKey + "':");
                    headersBinding.put(headerKey, headerValue); // Add key-value pair to headers
                }
                
                // Set message properties with headers
                AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                        .headers(headersBinding)
                        .build();
                
                // Publish the message to the exchange
                channel.basicPublish(exchangeName, "", properties, messageBody.getBytes());
				logger.info("Message sent with headers: {}", properties.getHeaders());
            }
            
            // Close the channel and connection
            channel.close();
            connection.close();
            
        } catch (Exception ex) {
            logger.error(ex.getMessage()); // Handle exceptions
        }
    }
}
