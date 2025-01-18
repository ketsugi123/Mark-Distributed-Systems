package markApp;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MarkApp {
	public static void markAndSaveImage(String inputPath, String outputPath, String keyword) {
		try {
			BufferedImage img = loadImage(inputPath);
			annotateImage(img, Collections.singletonList(keyword));
			saveImage(img, inputPath, outputPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private static List<String> extractKeywords(String[] args) {
		List<String> keywords = new ArrayList<>();
		for (int i = 2; i < args.length; i++) {
			keywords.add(args[i]);
		}
		return keywords;
	}
	
	private static BufferedImage loadImage(String inputPath) throws IOException {
		return ImageIO.read(Path.of(inputPath).toFile());
	}
	
	private static void annotateImage(BufferedImage img, List<String> keywords) {
		Graphics2D gfx = img.createGraphics();
		gfx.setFont(new Font("Arial", Font.PLAIN, 22));
		gfx.setColor(new Color(0x0000ff));
		
		String sentence = String.join(" ", keywords);
		gfx.drawString(sentence, 10, 20);
		
		Polygon poly = createHighlightPolygon(sentence.length());
		gfx.setColor(new Color(0xff0000));
		gfx.draw(poly);
		
		gfx.dispose();
	}
	
	private static Polygon createHighlightPolygon(int textLength) {
		Polygon poly = new Polygon();
		poly.addPoint(3, 3);
		poly.addPoint(10 * textLength, 3);
		poly.addPoint(10 * textLength, 25);
		poly.addPoint(3, 25);
		poly.addPoint(3, 3);
		return poly;
	}
	
	private static void saveImage(BufferedImage img, String inputPath, String outputPath) throws IOException {
		int lastIndex = inputPath.lastIndexOf('.');
		String format = inputPath.substring(lastIndex + 1);
		ImageIO.write(img, format, Path.of(outputPath).toFile());
	}
}
