package server.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.grpc.stub.StreamObserver;
import rabbitmq.Producer;
import server.Server;
import serverWithClient.*;
import shared.General.ImageBlock;
import shared.General.ImageBlockMetadata;
import spread.GroupMember;
import spread.SpreadException;
import transfer.BlockInfo;
import transfer.TransferHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static server.Server.clients;
import static server.Server.spreadUserName;
import static transfer.TransferHelper.convertBytesToImageBlocks;

public class ServerWithClient extends ServerWithClientGrpc.ServerWithClientImplBase {
	
	private static final InternalLogger logger = Log4J2LoggerFactory.getInstance(ServerWithClient.class);
	private static final String SHARED_DIR = "/mnt/sharedfs";
	private static final String TEMP_DIR = "/mnt/sharedfs/temp";
	private static final long BLOCK_SIZE = 4096L * 1024 * 1024;
	private static final String keyword = spreadUserName;
	
	private final GroupMember member;
	
	public ServerWithClient(GroupMember member) {
		this.member = member;
	}
	
	@Override
	public StreamObserver<UploadRequest> upload(StreamObserver<UploadResponse> responseObserver) {
		TransferHelper transferHelper = TransferHelper.initTransfer(TEMP_DIR, SHARED_DIR);
		final String imageId = UUID.randomUUID().toString();
		return new StreamObserver<UploadRequest>() {
			@Override
			public void onNext(UploadRequest value) {
				logger.info("Number of clients updated: " + clients.incrementAndGet());
				sendClientUpdateMessage();
				
				ImageBlock image = value.getBlock();
				ImageBlockMetadata metadata = image.getMetadata();

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
						logger.info("tempFile is present, path is: " + path);
						transferHelper.storeFinalFile(currentBlockInfo, path);
						responseObserver.onNext(UploadResponse.newBuilder().setId(imageId).build());
						responseObserver.onCompleted();

						// Send image id and keywords to rabbit
						JsonObject jsonObject = new JsonObject();
						jsonObject.addProperty("name", imageId);
						jsonObject.addProperty("keyword", keyword);
						String imageMessage = new Gson().toJson(jsonObject);
						Producer.produce(imageMessage);
					} catch (Exception e) {
						logger.error("Error while storing final file {}", e.getMessage());
						responseObserver.onError(e);
					}
				});
			}
			
			@Override
			public void onError(Throwable t) {
				handleError(t);
			}
			
			@Override
			public void onCompleted() {
				handleCompletion();
			}
		};
	}
	
	@Override
	public StreamObserver<DownloadRequest> download(StreamObserver<DownloadResponse> responseObserver) {
		TransferHelper transferHelper = TransferHelper.initTransfer(TEMP_DIR, SHARED_DIR);
		
		return new StreamObserver<DownloadRequest>() {
			@Override
			public void onNext(DownloadRequest value) {
				String imageId = value.getId();
				Optional<Path> file = transferHelper.getMarkedFileById(imageId);
				
				logger.info("Number of clients updated: " + clients.incrementAndGet());
				sendClientUpdateMessage();
				file.ifPresent(image -> {
					try {
						List<ImageBlock> imageBlocks = convertBytesToImageBlocks(imageId, image);
						for (ImageBlock block : imageBlocks) {
							responseObserver.onNext(DownloadResponse.newBuilder().setBlocks(block).build());
						}
						responseObserver.onCompleted();
						handleCompletion();
					} catch (IOException e) {
						responseObserver.onError(e);
						handleError(e);
					}
				});
			}
			
			@Override
			public void onError(Throwable t) {
				logger.error("Error while performing download request {}", t.getMessage());
			}
			
			@Override
			public void onCompleted() {
				logger.info("Download Complete");
			}
		};
	}
	
	
	private void sendClientUpdateMessage() {
		try {
			member.sendMessage(Server.spreadGroup, Server.buildJsonMessage());
		} catch (SpreadException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void handleError(Throwable t) {
		logger.error("Error during upload: {}", t.getMessage());
		logger.info("Number of clients updated: " + clients.decrementAndGet());
		sendClientUpdateMessage();
	}
	
	private void handleCompletion() {
		logger.info("Upload stream completed.");
		logger.info("Number of clients updated: " + clients.decrementAndGet());
		sendClientUpdateMessage();
	}
	
	

}
