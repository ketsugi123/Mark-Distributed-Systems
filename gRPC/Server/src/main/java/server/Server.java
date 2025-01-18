package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import server.services.ServerWithClient;
import spread.GroupMember;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static server.utils.IpUtils.getExternalIp;

public class Server {
    private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(Server.class.getName());

    private static final String externalAddress = getExternalIp();
    private static int svcPort;

    // Spread
    private static final int daemonPort = 4803;
    public static String spreadUserName;
    public static String spreadGroup;
    public static AtomicInteger clients = new AtomicInteger(0);
    public static AtomicBoolean leader = new AtomicBoolean(false);
    public static GroupMember member;
    public static StringBuffer groupLogs = new StringBuffer();
    public static CopyOnWriteArrayList<String> newServers = new CopyOnWriteArrayList<>();

    // RabbitMQ
    public static String IP_BROKER;
    public static String exchangeName = "tp2-ex";
    public static String routingKey = "ex-workers";

    public static String buildJsonMessage() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", spreadUserName);
        jsonObject.addProperty("ip", externalAddress);
        jsonObject.addProperty("port", svcPort);
        jsonObject.addProperty("clients", clients.get());
        jsonObject.addProperty("leader", leader.get());
        jsonObject.addProperty("new", false);
        return new Gson().toJson(jsonObject);
    }

    public static String buildJsonMessage(Boolean newServerInGroup) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", spreadUserName);
        jsonObject.addProperty("ip", externalAddress);
        jsonObject.addProperty("port", svcPort);
        jsonObject.addProperty("clients", clients.get());
        jsonObject.addProperty("leader", leader.get());
        jsonObject.addProperty("new", newServerInGroup);
        return new Gson().toJson(jsonObject);
    }

    public static void main(String[] args) {
        String daemonIP;

        if (args.length >= 5) {
            svcPort = Integer.parseInt(args[0]);
            daemonIP = args[1];
            spreadUserName = args[2];
            spreadGroup = args[3];
            IP_BROKER = args[4];
            if (args.length > 5) {
                exchangeName = args[5];
                routingKey = args[6];
            }
        } else {
            return;
        }



        try {

            logger.info("Connecting to daemon at {}", daemonIP);

            member = new GroupMember(spreadUserName, daemonIP, daemonPort);
            member.joinToGroup(spreadGroup);
            member.sendMessage(spreadGroup, buildJsonMessage(true));

            io.grpc.Server server = ServerBuilder.forPort(svcPort)
                    .addService(new ServerWithClient(member))
                    .build();

            server.start();

            logger.info("Svc started, listening on {}", svcPort);

            // Add shutdown hook to gracefully stop the server
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(server));

            // Block the main thread until the server terminates
            server.awaitTermination();
            member.close();
            server.shutdown();
        } catch(Exception ex) {
            logger.error(ex.getMessage());
        }
    }
    

}
