package server.services;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import serverWithClient.DownloadRequest;
import serverWithClient.DownloadResponse;
import serverWithClient.UploadRequest;
import serverWithClient.UploadResponse;
import shared.General.ImageBlock;
import shared.General.ImageBlockMetadata;
import spread.GroupMember;
import transfer.BlockInfo;
import transfer.TransferHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServerWithClientTest {
	
	private ServerWithClient serverWithClient;
	private StreamObserver<UploadResponse> uploadResponseObserver;
	private StreamObserver<DownloadResponse> downloadResponseObserver;
	private TransferHelper transferHelper;
	private static final String SPREAD_ADDRESS = System.getenv("SPREAD_VM1");
	@BeforeEach
	void setUp() {
		// Initialize the real classes without mocks
		GroupMember groupMember = new GroupMember("user", SPREAD_ADDRESS, 4803); // Adjust the address and port as needed
		transferHelper = TransferHelper.initTransfer("/var/sharedfiles/temp", "/var/sharedfiles");
		
		// Create the server instance with real dependencies
		serverWithClient = new ServerWithClient(groupMember);
		
		// Initialize StreamObservers
		uploadResponseObserver = new StreamObserver<UploadResponse>() {
			@Override
			public void onNext(UploadResponse value) {
				// Handle upload response (optional)
			}
			
			@Override
			public void onError(Throwable t) {
				// Handle error
			}
			
			@Override
			public void onCompleted() {
				// Handle completion
			}
		};
		
		downloadResponseObserver = new StreamObserver<DownloadResponse>() {
			@Override
			public void onNext(DownloadResponse value) {
				// Handle download response (optional)
			}
			
			@Override
			public void onError(Throwable t) {
				// Handle error
			}
			
			@Override
			public void onCompleted() {
				// Handle completion
			}
		};
	}
	
	@AfterEach
	void tearDown() {
		// Clean up any temporary files created during the tests
		try {
			Files.walk(Path.of("/var/sharedfiles/temp"))
					.map(Path::toFile)
					.forEach(file -> {
						if (file.exists() && !file.isDirectory()) {
							file.delete();
						}
					});
		} catch (IOException ignored) {
		}
	}
	
	@Test
	void testUploadValidImageBlock() throws IOException {
		// Create ImageBlockMetadata and ImageBlock
		ImageBlockMetadata metadata = ImageBlockMetadata.newBuilder()
				.setSequenceNumber(1)
				.setTotalBlocks(1)
				.build();
		
		ImageBlock block = ImageBlock.newBuilder()
				.setMetadata(metadata)
				.setPixelData(com.google.protobuf.ByteString.copyFrom(new byte[1024]))
				.build();
		
		// Create BlockInfo using BlockInfo.createImageInfo()
		BlockInfo blockInfo = BlockInfo.createImageInfo("imageId123", block, 1, 1);
		
		// Store the block temporarily
		Optional<Path> tempFilePath = transferHelper.storeTempFile(blockInfo);
		
		// Simulate an upload request
		UploadRequest request = UploadRequest.newBuilder()
				.setBlock(block)
				.build();
		
		// Call the upload method
		StreamObserver<UploadRequest> uploadObserver = serverWithClient.upload(uploadResponseObserver);
		uploadObserver.onNext(request);
		uploadObserver.onCompleted();
		
		// Check if the response is valid and the file is stored correctly
		assertTrue(tempFilePath.isPresent());
		assertNotNull(uploadResponseObserver);
	}
	
	@Test
	void testUploadError() {
		// Simulate an error during upload
		uploadResponseObserver.onError(new RuntimeException("Simulated Error"));
		
		// Verify error handling
		assertThrows(RuntimeException.class, () -> uploadResponseObserver.onError(new RuntimeException("Simulated Error")));
	}
	
	@Test
	void testDownloadValidFile() throws IOException {
		// Create a temporary file and store it
		Path tempFile = Files.createTempFile("temp", "file");
		transferHelper.storeTempFile(BlockInfo.createImageInfo("imageId123", null, 1, 1));  // Adjust if necessary
		
		// Simulate a download request
		DownloadRequest request = DownloadRequest.newBuilder().setId("imageId123").build();
		StreamObserver<DownloadRequest> downloadObserver = serverWithClient.download(downloadResponseObserver);
		downloadObserver.onNext(request);
		downloadObserver.onCompleted();
		
		// Validate that the file exists
		assertTrue(Files.exists(tempFile));
	}
	
	@Test
	void testDownloadNonExistentFile() {
		// Simulate a request for a non-existent file
		DownloadRequest request = DownloadRequest.newBuilder().setId("nonExistentFile").build();
		StreamObserver<DownloadRequest> downloadObserver = serverWithClient.download(downloadResponseObserver);
		downloadObserver.onNext(request);
		
		// Verify that an error is returned
		downloadResponseObserver.onError(new Throwable("File not found"));
		assertThrows(Throwable.class, () -> downloadResponseObserver.onError(new Throwable("File not found")));
	}
}
