package grpcclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import serverWithClient.ServerWithClientGrpc;
import serverWithClient.UploadRequest;
import serverWithClient.UploadResponse;
import shared.General;
import shared.General.ImageBlock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static grpcclient.Client.shutdown;
import static transfer.TransferHelper.convertBytesToImageBlocks;

public class UploadHandler {
	
	private static final InternalLogger uploadLogger = Log4J2LoggerFactory.getInstance("UploadHandler");
	
	public static void upload(General.ServerInfo svcServer, String imagePathString) throws IOException {
		Path image = Path.of(imagePathString);
		
		// Convert image to byte array
		// Calculate the number of blocks (e.g., chunks of 4MB)
		ManagedChannel svcChannel = ManagedChannelBuilder
				.forAddress(svcServer.getIp(), svcServer.getPort())
				.usePlaintext()
				.build();
		
		ServerWithClientGrpc.ServerWithClientStub stub = ServerWithClientGrpc.newStub(svcChannel);
		StreamObserver<UploadRequest> uploadRequestObserver = stub.upload(new StreamObserver<>() {
			@Override
			public void onNext(UploadResponse value) {
				uploadLogger.info("Upload Response: {}", value.getId());
			}
			
			@Override
			public void onError(Throwable t) {
				uploadLogger.error("Error during upload: {}", t.getLocalizedMessage());
				svcChannel.shutdownNow();
				shutdown.set(true);
			}
			
			@Override
			public void onCompleted() {
				uploadLogger.info("Upload completed.");
				svcChannel.shutdownNow();
				shutdown.set(true);
			}
		});
		
		
		
		// Send blocks with metadata
		List<ImageBlock> imageBlocks = convertBytesToImageBlocks(image);

		for (ImageBlock imageBlock: imageBlocks) {
			uploadRequestObserver.onNext(
					UploadRequest.newBuilder()
							.setBlock(imageBlock)
							.build()
			);
		}
		uploadRequestObserver.onCompleted();
	}
	
}
