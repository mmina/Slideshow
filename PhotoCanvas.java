import java.lang.*;
import java.awt.*;
import java.awt.event.*;

public class PhotoCanvas extends Canvas {
	private Dimension prefsize = new Dimension(640, 640);

	private Image image;

	public PhotoCanvas() {
		super();
		setBackground(Color.BLACK);
		setFocusable(false);
	}

	public Dimension getPreferredSize() {
		return prefsize;
	}
	public Dimension getMinimumSize() {
		return prefsize;
	}

	void storeCurrentSize() {
		prefsize = getSize();
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		if (image == null)  return;

		int imageHeight = image.getHeight(this);
		int imageWidth = image.getWidth(this);
		if (imageHeight == 0 || imageWidth == 0)  return;
		Dimension size = getSize();

		double sx = (double)(size.width) / (double)imageWidth;
		double sy = (double)(size.height) / (double)imageHeight;
		double scale = sx < sy ? sx : sy;
		int width = (int)(imageWidth * scale);
		int height = (int)(imageHeight * scale);
		int x = (size.width - width) / 2;
		int y = (size.height - height) / 2;
		g.drawImage(image, x, y, width, height, this);
	}
	
	public void setImage(Image image) {
		this.image = image;
		repaint();
	}
}
