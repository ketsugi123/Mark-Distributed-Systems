package grpc;

// generic ServerApp for hosting grpcService

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import grpc.services.RegistryServerWithClient;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import shared.General.ServerInfo;
import spread.GroupMember;

import java.util.concurrent.ConcurrentHashMap;

import static grpc.utils.IpUtils.getExternalIp;


public class RegistryServerMain {

    private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(RegistryServerMain.class);

    private static final String externalAddress = getExternalIp();
    private static int svcPort;
    private static final int daemonPort = 4803;
    public static final String spreadUserName = "registryServer";
    public static String spreadGroup;

    public static ConcurrentHashMap<ServerInfo, Integer> serversContext = new ConcurrentHashMap<>();

    public static String buildJsonMessageRegistry(Boolean newServerInGroup) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", spreadUserName);
        jsonObject.addProperty("ip", externalAddress);
        jsonObject.addProperty("port", svcPort);
        jsonObject.addProperty("new", newServerInGroup);
        return new Gson().toJson(jsonObject);
    }

    public static void main(String[] args) {
        String daemonIP;

        if (args.length >= 3) {
            svcPort = Integer.parseInt(args[0]);
            daemonIP = args[1];
            spreadGroup = args[2];
        } else {
            return;
        }

        try {
            logger.info("Connecting to daemon at {}", daemonIP);

            GroupMember member = new GroupMember(spreadUserName, daemonIP, daemonPort);
            member.joinToGroup(spreadGroup);
            member.sendMessage(spreadGroup, buildJsonMessageRegistry(true));

            io.grpc.Server svc = ServerBuilder
                    .forPort(svcPort)
                    .addService(new RegistryServerWithClient())
                    .build();
            svc.start();
            logger.info("Registry Server started, listening on " + svcPort);

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(svc));

            svc.awaitTermination();
            member.close();
            svc.shutdown();

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

}
