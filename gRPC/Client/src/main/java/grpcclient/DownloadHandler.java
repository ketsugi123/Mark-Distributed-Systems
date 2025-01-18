package grpcclient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import serverWithClient.DownloadRequest;
import serverWithClient.DownloadResponse;
import serverWithClient.ServerWithClientGrpc;
import shared.General;
import transfer.BlockInfo;
import transfer.TransferHelper;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static grpcclient.Client.*;

public class DownloadHandler {
	private static final InternalLogger logger = Log4J2LoggerFactory.getInstance("DownloadHelper");
	
	public static void download(General.ServerInfo svcInfo, String imageId, String downloadDir) {

		ManagedChannel svcChannel = ManagedChannelBuilder
				.forAddress(svcInfo.getIp(), svcInfo.getPort())
				.usePlaintext()
				.build();
		
		ServerWithClientGrpc.ServerWithClientStub stub = ServerWithClientGrpc.newStub(svcChannel);
		TransferHelper transferHelper = TransferHelper.initTransfer(downloadDir + "/temp/", downloadDir);
		StreamObserver<DownloadRequest> downloadRequestStream = stub.download(
				new StreamObserver<>() {
					@Override
					public void onNext(DownloadResponse value) {
						General.ImageBlock image = value.getBlocks();
						General.ImageBlockMetadata metadata = image.getMetadata();
						
						String imageId = UUID.randomUUID().toString();
						int currentSequenceNumber = metadata.getSequenceNumber();
						int totalBlocks = metadata.getTotalBlocks();
						
						BlockInfo currentBlockInfo = BlockInfo.createImageInfo(imageId, image, currentSequenceNumber, totalBlocks);
						
						// Initialize tracking for image
						transferHelper.insertBlock(imageId, image);
						transferHelper.insertTotalBlocks(imageId, totalBlocks);
						
						logger.info("Received Image block with sequence number {}", currentSequenceNumber);
						
						Optional<Path> tempFile = transferHelper.storeTempFile(currentBlockInfo);
						tempFile.ifPresent(path -> {
							try {
								transferHelper.storeFinalFile(currentBlockInfo, path);
							} catch (Exception e) {
								logger.error("Error while storing final file {}", e.getMessage());
							}
						});
					}
					
					@Override
					public void onError(Throwable t) {
						logger.error("Error while downloading image :{}", t.getMessage());
						svcChannel.shutdownNow();
						shutdown.set(true);
					}
					
					@Override
					public void onCompleted() {
						logger.info("Download Completed");
						svcChannel.shutdownNow();
						shutdown.set(true);
					}
				}
		);
		
		downloadRequestStream.onNext(
				DownloadRequest.newBuilder()
				.setId(imageId)
				.build()
		);
		downloadRequestStream.onCompleted();
		
	}
}
