package transfer;

import shared.General.ImageBlock;

public class BlockInfo {
	private final String imageId;
	private final ImageBlock block;
	private final int sequenceNumber;
	private final int totalBlocks;
	
	private BlockInfo(String imageId, ImageBlock block, int sequenceNumber, int totalBlocks) {
		this.imageId = imageId;
		this.block = block;
		this.sequenceNumber = sequenceNumber;
		this.totalBlocks = totalBlocks;
	}
	
	public String getImageId() {
		return imageId;
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	public ImageBlock getBlock() {
		return block;
	}
	
	public int getTotalBlocks() {
		return totalBlocks;
	}
	
	public static BlockInfo createImageInfo(String imageId, ImageBlock image, int sequenceNumber, int totalBlocks) {
		return new BlockInfo(imageId, image, sequenceNumber, totalBlocks);
	}
}
