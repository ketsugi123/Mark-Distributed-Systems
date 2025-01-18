package transfer;

import com.google.protobuf.ByteString;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.Log4J2LoggerFactory;
import shared.General;
import shared.General.ImageBlock;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class TransferHelper {
	private final String TEMP_DIR;
	private final String SHARED_DIR;
	private static final InternalLogger transferLogger = Log4J2LoggerFactory.getInstance("TransferHelper");
	private static final Long BLOCK_SIZE = 4000000L;
	public static void main(String[] args) {
	}
	
	public static final ConcurrentHashMap<String, Set<Integer>> receivedBlocks = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<String, Integer> totalBlocksMap = new ConcurrentHashMap<>();
	
	private TransferHelper(String tempDir, String sharedDir) {
		this.TEMP_DIR = tempDir;
		this.SHARED_DIR = sharedDir;
	}
	
	public static TransferHelper initTransfer(String tempDir, String sharedDir) {
		return new TransferHelper(tempDir, sharedDir);
	}
	
	public void insertBlock(String imageId, ImageBlock block) {
		receivedBlocks.putIfAbsent(imageId, ConcurrentHashMap.newKeySet());
	}
	
	public void insertTotalBlocks(String imageId, Integer totalBlocks) {
		totalBlocksMap.putIfAbsent(imageId, totalBlocks);
	}
	
	public Optional<Path> storeTempFile(BlockInfo blockInfo) {
		String imageId = blockInfo.getImageId();
		ImageBlock imageBlock = blockInfo.getBlock();
		int currentSequenceNumber = blockInfo.getSequenceNumber();
		int totalBlocks = blockInfo.getTotalBlocks();
		
		Path tempFile = Path.of(TEMP_DIR, imageId + ".temp");
		receivedBlocks.get(imageId).add(currentSequenceNumber);
		try {
			// Create temp directory

			if (currentSequenceNumber == 1) {
				Files.createDirectories(tempFile.getParent());
			}

			// Write block data to temp file in append mode
			try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile(), true)) {
				fileOutputStream.write(imageBlock.getPixelData().toByteArray());
			}
			
			return currentSequenceNumber == totalBlocks ? Optional.of(tempFile) : Optional.empty();
		} catch (IOException e) {
			transferLogger.error("Error writing block {} for image {}: {}", currentSequenceNumber, imageId, e.getMessage());
		}
		return Optional.empty();
	}
	
	public void storeFinalFile(BlockInfo blockInfo, Path tempFile) throws IOException, InterruptedException {
		String imageId = blockInfo.getImageId();
		int totalBlocks = blockInfo.getTotalBlocks();
		
		if (receivedBlocks.get(imageId).size() == totalBlocks) {
			transferLogger.info("All blocks from this image have been received");
			moveTempToFinalDir(imageId, tempFile);
			Thread.sleep(1000);
			clearTempImageFile(tempFile, imageId);
		}
	}
	
	public Optional<Path> getMarkedFileById(String imageId) {
		Path filePath = Path.of(SHARED_DIR, imageId + "-" + "marks.png");
		if (Files.exists(filePath)) {
			transferLogger.info("File with ID {} found at {}", imageId, filePath.toString());
			return Optional.of(filePath);
		} else {
			transferLogger.warn("File with ID {} not found.", imageId);
			return Optional.empty();
		}
	}
	
	public void moveTempToFinalDir(String imageId, Path tempFile) throws IOException {
		
		Path finalPath = Path.of(SHARED_DIR, imageId + ".png");
		try {
			Files.createDirectories(finalPath.getParent());
			Files.copy(tempFile, finalPath);
			transferLogger.info("Image {} successfully saved to {}.", imageId, finalPath);
		} catch (IOException e) {
			transferLogger.error("Error trying to move temp file to final directory: {}", e.getMessage());
			throw e;
		}
	}
	
	public void clearTempImageFile(Path tempFile, String imageId) throws IOException {
		try {
			Files.delete(tempFile);
			totalBlocksMap.remove(imageId);
			receivedBlocks.remove(imageId);
		} catch (IOException e) {
			transferLogger.error(e.getMessage());
			throw e;
		}
	}
	
	
	
	public static List<ImageBlock> convertBytesToImageBlocks(String imageId, Path filePath) throws IOException {
		List<ImageBlock> imageBlocks = new ArrayList<>();
		long fileSize = Files.size(filePath);
		long blockSize = Math.min(BLOCK_SIZE, fileSize); // Cap block size at Integer.MAX_VALUE
		int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);
		
		try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
			extract(imageBlocks, fileSize, blockSize, totalBlocks, fileChannel);
		} catch (IOException e) {
			transferLogger.error("Error converting file {} to ImageBlocks: {}", imageId, e.getMessage(), e);
			throw e;
		}
		
		return imageBlocks;
	}
	
	private static void extract(List<ImageBlock> imageBlocks, long fileSize, long blockSize, int totalBlocks, FileChannel fileChannel) throws IOException {
		long position = 0;
		int sequenceNumber = 1;
		
		while (position < fileSize) {
			long remainingBytes = fileSize - position;
			int bufferSize = (int) Math.min(blockSize, remainingBytes);
			ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
			
			fileChannel.read(buffer, position);
			buffer.flip();
			
			ByteString pixelData = ByteString.copyFrom(buffer);
			General.ImageBlockMetadata metadata = General.ImageBlockMetadata.newBuilder()
					.setSequenceNumber(sequenceNumber)
					.setTotalBlocks(totalBlocks)
					.build();
			
			ImageBlock block = ImageBlock.newBuilder()
					.setMetadata(metadata)
					.setPixelData(pixelData)
					.build();
			
			imageBlocks.add(block);
			sequenceNumber++;
			position += bufferSize;
		}
	}
	
	public static List<ImageBlock> convertBytesToImageBlocks(Path filePath) throws IOException {
		List<ImageBlock> imageBlocks = new ArrayList<>();
		long fileSize = Files.size(filePath);
		long blockSize = Math.min(BLOCK_SIZE, Integer.MAX_VALUE); // Cap block size at Integer.MAX_VALUE
		int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);
		
		try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
			extract(imageBlocks, fileSize, blockSize, totalBlocks, fileChannel);
		} catch (IOException e) {
			transferLogger.error("Error image bytes to ImageBlocks: {}", e.getMessage());
			throw e;
		}
		
		return imageBlocks;
	}
}
