package grpc.services;

import grpc.RegistryServerMain;
import grpc.utils.HashMapUtils;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import registryWithClient.GetSvcRequest;
import registryWithClient.GetSvcResponse;
import registryWithClient.RegistryWithClientGrpc;
import shared.General.ServerInfo;

public class RegistryServerWithClient extends RegistryWithClientGrpc.RegistryWithClientImplBase {
	private final InternalLogger logger = Log4J2LoggerFactory.getInstance(this.getClass());

	public RegistryServerWithClient() {
	}

	@Override
	public void getSvc(GetSvcRequest request, StreamObserver<GetSvcResponse> responseObserver) {
		ServerInfo svcInfo = HashMapUtils.findLowestOrZeroKey(RegistryServerMain.serversContext);
		if (svcInfo == null) {
			logger.error("No servers available");
			responseObserver.onError(new RuntimeException("No servers available"));
		} else {
			logger.info("Sending svc to client: " + svcInfo.getIp() + ":" + svcInfo.getPort());
			responseObserver.onNext(GetSvcResponse.newBuilder().setSvc(svcInfo).build());
			responseObserver.onCompleted();
		}

	}
}	
