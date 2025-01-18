package rmqconfigurator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ConfigurationOperations {
	
	
	static Logger logger = new SimpleLoggerFactory().getLogger(RabbitConfigurator.class.getName());
	static Channel channel = RabbitConfigurator.getChannel();
	static Connection connection = RabbitConfigurator.getConnection();
	static final String QUEUE_ROUTE = "/api/queues";
	static final String EXCHANGE_ROUTE = "/api/exchanges";
	static final ObjectMapper mapper = new ObjectMapper();
	
	static void CreateExchange(String exchangeName) throws IOException {
		BuiltinExchangeType exchangeType = BuiltinExchangeType.FANOUT;
		channel.exchangeDeclare(exchangeName, exchangeType, true);
	}
	
	static void CreateExchangeWithAlternateExchange() throws IOException {
		// Create the alternate exchange
		boolean isExchangeDurable = Boolean.parseBoolean(readline("Exchange Durable?"));
		boolean isQueueDurable = Boolean.parseBoolean(readline("Queue Durable?"));
		boolean isQueueExclusive = Boolean.parseBoolean(readline("Queue Exclusive?"));
		boolean doesQueueAutoDelete = Boolean.parseBoolean(readline("Queue Auto Delete?"));
		String alternateExchangeName = readline("Alternate Exchange Name?");
		
		channel.exchangeDeclare(alternateExchangeName, BuiltinExchangeType.FANOUT, isExchangeDurable);
		channel.queueDeclare("alternate-queue", isQueueDurable, isQueueExclusive, doesQueueAutoDelete, null);
		channel.queueBind("alternate-queue", alternateExchangeName, "");
		
		// Create a main exchange with the alternate exchange linked
		String exchangeName = readline("Exchange Name?");
		Map<String, Object> args = new HashMap<>();
		args.put("alternate-exchange", alternateExchangeName);
		
		boolean isDurable = Boolean.parseBoolean(readline("Durable?"));
		boolean isAutoDelete = Boolean.parseBoolean(readline("Auto-Delete?"));
		channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, isDurable, isAutoDelete, args);
	}
	
	static void CreateQueue(String queueName) throws IOException {
		// Create a durable, non-exclusive, non-auto-delete queue
		channel.queueDeclare(queueName, true, false, false, null);
	}
	
	static void GetQueues() throws IOException, InterruptedException {
		String username = readline("Username?");
		String password = readline("Password?");
		
		URI rabbitUri = URI.create(RabbitConfigurator.getRabbitHost() + QUEUE_ROUTE);
		String response = makeRequestSimplified(username, password, rabbitUri);
		logger.info(response);
	}
	
	static void GetExchanges() throws IOException, InterruptedException {
		String username = readline("Username?");
		String password = readline("Password?");
		URI exchangeURI = URI.create(RabbitConfigurator.getRabbitHost() + EXCHANGE_ROUTE);
		String response = makeRequestSimplified(username, password, exchangeURI);
		logger.info(response);
	}
	
	static String buildRabbitHeaderString(String username, String password) {
		return "Basic " + java.util.Base64.getEncoder()
				.encodeToString((username + ":" + password).getBytes());
	}
	
	static void BindQueue2Ex(String queueName, String exchangeName, String routingKey) throws IOException {
		// Bind a queue to an exchange using a routing key
		channel.queueBind(queueName, exchangeName, routingKey);
	}
	
	static void BindExToEx() throws IOException {
		// Bind one exchange to another using a routing key
		String destinationExchangeName = readline("Destination Exchange name?");
		String sourceExchangeName = readline("Source Exchange name?");
		String routingKey = readline("Routing Key?");
		channel.exchangeBind(destinationExchangeName, sourceExchangeName, routingKey);
	}
	
	static void BindQueuesToExWHeaders() throws IOException {
		// Bind queues to an exchange using headers
		
		String exchangeName = readline("Exchange Name?");
		String queueNameAny = readline("Queue Name (for messages with <any> headers)?");
		String queueNameAll = readline("Queue Name (for messages with <all> headers)?");
		
		Map<String, Object> headersAny = new HashMap<>();
		Map<String, Object> headersAll = new HashMap<>();
		
		// Read header values and prepare header maps
		String headersString = readline("Enter header values separated by spaces:");
		String[] headerList = headersString.split(" ");
		
		headersAny.put("x-match", "any"); // Matches any header
		headersAll.put("x-match", "all"); // Matches all headers
		
		for (int i = 0; i < headerList.length; i++) {
			headersAny.put("h" + i, headerList[i]);
			headersAll.put("h" + i, headerList[i]);
		}
		
		// Bind queues with header conditions
		channel.queueBind(queueNameAny, exchangeName, "", headersAny);
		channel.queueBind(queueNameAll, exchangeName, "", headersAll);
		
		// Bind queue with a specific header
		Map<String, Object> specificHeader = new HashMap<>();
		specificHeader.put("x-match", "all");
		
		String specificQueue = readline("Queue Name (for messages with specific header)?");
		String headerKey = readline("Specific header key?");
		String headerValue = readline("Specific header value?");
		specificHeader.put(headerKey, headerValue);
		
		channel.queueBind(specificQueue, exchangeName, "", specificHeader);
	}
	
	
	private static String makeRequest(String username, String password, URI uri) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		String headerType = "Authorization";
		HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.header(headerType, buildRabbitHeaderString(username, password))
				.GET()
				.build();
		
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		Object parsedJson = mapper.readValue(response.body(), Object.class);
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
		return writer.writeValueAsString(parsedJson);
		
	}
	
	private static  String makeRequestSimplified(String username, String password, URI uri) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		String headerType = "Authorization";
		HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.header(headerType, buildRabbitHeaderString(username, password))
				.GET()
				.build();
		
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode parsedJson = mapper.readTree(response.body());
		ArrayNode responseJson= mapper.createArrayNode();
		for(JsonNode node: parsedJson) {
			ObjectNode responseJsonNode = mapper.createObjectNode();
			JsonNode headersNode = node.path("arguments");
			String headers = headersNode.isObject() ? headersNode.toString() : "None";
			responseJsonNode.put("headers", headers);
			responseJsonNode.put("name", node.get("name").asText());
			responseJsonNode.put("type", node.get("type").asText());
			responseJson.add(responseJsonNode);
		}
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
		return writer.writeValueAsString(responseJson);
	}
	
	private static String readline(String msg) {
		Scanner scanner = new Scanner(System.in);
		logger.info(msg);
		String s = scanner.nextLine();
		return s == null ? "" : s;
	}
	
	private static String readline(String msg, Object... args) {
		Scanner scanner = new Scanner(System.in);
		logger.info(msg, args);
		String s = scanner.nextLine();
		return s == null ? "" : s;
	}
	
	
	static BuiltinExchangeType readEXchangeType() {
		int op;
		Scanner scan = new Scanner(System.in);
		BuiltinExchangeType[] types = {
				BuiltinExchangeType.DIRECT,
				BuiltinExchangeType.FANOUT,
				BuiltinExchangeType.TOPIC,
				BuiltinExchangeType.HEADERS
		};
		do {
			;
			logger.info("EXCHANGE TYPE");
			logger.info(" 0 - {}", BuiltinExchangeType.DIRECT);
			logger.info(" 1 - {}", BuiltinExchangeType.FANOUT);
			logger.info(" 2 - {}", BuiltinExchangeType.TOPIC);
			logger.info(" 3 - {}", BuiltinExchangeType.HEADERS);
			logger.info("Choose an Option?");
			op = scan.nextInt();
		} while (!(op >= 0 && op <= 3));
		return types[op];
	}
	
}
