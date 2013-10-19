import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.security.*;


public class Slideshow extends JFrame implements Runnable, KeyListener {

// constants
	// menu labels
	private static final String	MENU_FILE	= "File";
	private static final String	ITEM_OPEN_DIR	= "Open Dir...";
	private static final String	ITEM_CHOOSE_TRASH_DIR	= "Choolse Trash Dir...";
	private static final String	ITEM_CHOOSE_FAVORITES_DIR	= "Choolse Favorites Dir...";
	private static final String	ITEM_QUIT	= "Quit";
	private static final String	MENU_SLIDESHOW	= "Slideshow";
	private static final String	ITEM_FULLSCREEN	= "Fullscreen";
	private static final String	ITEM_START	= "Start";
	private static final String	ITEM_STOP	= "Stop";


	// menu objects
	private JMenuBar	menubar;
	private JMenu		menuFile;
	private JMenuItem	itemOpenDir;
	private JMenuItem	itemChooseTrashDir;
	private JMenuItem	itemChooseFavoritesDir;
	private JMenuItem	itemQuit;
	private JMenu		menuSlideshow;
	private JMenuItem	itemFullscreen;
	private JMenuItem	itemStart;
	private JMenuItem	itemStop;


	private PhotoCanvas	photoCanvas;		
	private Toolkit		toolkit;
	private FilenameFilter	filenameFilter;

	private java.util.List<File>	imageFiles;
	private int		currentID;
	private Image		currentImage;
	private File		currentFile;
	private Image		nextImage;  // prefetch
	private File		nextFile;
	private Image		prevImage;  // prefetch
	private File		prevFile;

	private File		trashDir;
	private static final String	DEFAULT_TRASH_DIR_NAME = "_trash";
	private File		favoritesDir;
	private static final String	DEFAULT_FAVORITES_DIR_NAME = "_fav";
	private MessageDigest	md;

	private boolean		isFullscreen = false;

	// for animation
	private Thread		thread;
	private int		duration = 8000;  // ms

	static Slideshow _slideshow = new Slideshow();


	private Slideshow() {
		super();

		// setUndecorated(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		createMenu();

		addKeyListener(this);

		toolkit = Toolkit.getDefaultToolkit();
		imageFiles = new LinkedList<File>();
		filenameFilter = new ImageSuffixFilter();

		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		photoCanvas = new PhotoCanvas();
		add(photoCanvas); 
		photoCanvas.requestFocus();
	}

	public void keyTyped(KeyEvent e) {
		// do nothing
	}
	public void keyPressed(KeyEvent e) {
		// do nothing
	}
	public void keyReleased(KeyEvent e) {
		switch(e.getKeyCode()) {
			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_BACK_SPACE:
			// move the current file to the trash
				if (currentFile == null || trashDir == null)  break;
				if (currentFile.renameTo(new File(trashDir, currentFile.getName()))) {
					System.out.println("trashed: " + currentFile.getName());
				}
				break;
			/*
			case KeyEvent.VK_SHIFT:
			// move the current file to the favorites
			{
				if (currentFile == null || favoritesDir == null)  break;
				File destFile = new File(favoritesDir, getHashedFilename(currentFile));
				if (destFile.exists()) {
					System.err.println("tried to copy but it has already exist: " + destFile.getName());
					break;
				}
				if (currentFile.renameTo(destFile)) {
					System.out.println("move to the favorites: " + destFile.getName());
				}
			}
				break;
			*/
			case KeyEvent.VK_ENTER:
			// copy the current file to the favorites
			{
				if (currentFile == null || favoritesDir == null)  break;
				/* old: keep the original filename
				File destFile = new File(favoritesDir, currentFile.getName());
				int id = 0;
				while (destFile.exists()) {
					id++;
					destFile = new File(favoritesDir, addNumberToFilename(currentFile.getName(), id));
				}
				*/
				File destFile = new File(favoritesDir, getHashedFilename(currentFile));
				if (destFile.exists()) {
					System.err.println("tried to copy but it has already exist: " + destFile.getName());
					break;
				}
				FileChannel srcChannel = null, destChannel = null;
				try {
					srcChannel = new FileInputStream(currentFile).getChannel();
					destChannel = new FileOutputStream(destFile).getChannel();
					srcChannel.transferTo(0, srcChannel.size(), destChannel);
					System.out.println("copy to the favorites: " + destFile.getName());
				} catch (Exception exception) {
					System.err.println("failed to copy to the favorites dir: " + currentFile.getName());
				} finally {
					try {
						srcChannel.close();
						destChannel.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}
				break;
			case KeyEvent.VK_SPACE:
			// toggle start/stop
				if (thread == null) {
					start();
				} else {
					stop();
				}
				break;
			case KeyEvent.VK_ESCAPE:
				setFullscreen(!isFullscreen);
				break;
			case KeyEvent.VK_MINUS:
				duration -= 500;
				if (duration < 500)  duration = 500;
				System.err.println("duration = " + duration);
				break;
			case KeyEvent.VK_PLUS:
			case KeyEvent.VK_EQUALS:
				duration += 500;
				System.err.println("duration = " + duration);
				break;
			case KeyEvent.VK_N:
			case KeyEvent.VK_RIGHT:
				if (!next())  stop();
				break;
			case KeyEvent.VK_B:
			case KeyEvent.VK_LEFT:
				if (!prev())  stop();
				break;
			default:
				System.err.println("other key");
		}
	}

	public static Slideshow getSlideshow()  {return _slideshow;}

	void createMenu() {
		menubar = new JMenuBar();

		// file menu
		menuFile = new JMenu(MENU_FILE);
		menubar.add(menuFile);
		itemOpenDir = new JMenuItem(ITEM_OPEN_DIR);
		itemOpenDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.setMultiSelectionEnabled(true);
				if (jfc.showOpenDialog(Slideshow.getSlideshow()) == JFileChooser.APPROVE_OPTION) {
					File[] dirs = jfc.getSelectedFiles();
					// trashDir = new File(dirs[0], DEFAULT_TRASH_DIR_NAME);
					trashDir = new File(dirs[0].getAbsolutePath() + DEFAULT_TRASH_DIR_NAME);
					if (!trashDir.exists()) {
						if (!trashDir.mkdir()) {
							System.err.println("making trash directory failed.");
							trashDir = null;
						}
						System.out.println("trash dir " + trashDir.getAbsolutePath() + " created.");
					}
					// favoritesDir = new File(dirs[0], DEFAULT_FAVORITES_DIR_NAME);
					favoritesDir = new File(dirs[0].getAbsolutePath() + DEFAULT_FAVORITES_DIR_NAME);
					if (!favoritesDir.exists()) {
						if (!favoritesDir.mkdir()) {
							System.err.println("making favorites directory failed.");
							favoritesDir = null;
						}
						System.out.println("favorites dir " + favoritesDir.getAbsolutePath() + " created.");
					}

					imageFiles.clear();
					readFiles(dirs);
					System.out.println(imageFiles.size() + "images");
					Collections.shuffle(imageFiles);

					currentID = 0;
					prevFile = null;
					if (prevImage != null)  prevImage.flush();
					prevImage = null;
					currentFile = null;
					if (currentImage != null)  currentImage.flush();
					currentImage = null;
					nextFile = null;
					if (nextImage != null)  nextImage.flush();
					nextImage = null;

					if (imageFiles.size() > 0) {
						currentFile = imageFiles.get(0);
						currentImage = toolkit.getImage(currentFile.getAbsolutePath());
					}
					if (imageFiles.size() > 1) {
						nextFile = imageFiles.get(1);
						nextImage = toolkit.getImage(nextFile.getAbsolutePath());
					}

					photoCanvas.setImage(currentImage);
				}
			}
		});
		menuFile.add(itemOpenDir);
		itemChooseTrashDir = new JMenuItem(ITEM_CHOOSE_TRASH_DIR);
		itemChooseTrashDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.setMultiSelectionEnabled(false);
				if (jfc.showOpenDialog(Slideshow.getSlideshow()) == JFileChooser.APPROVE_OPTION) {
					trashDir = jfc.getSelectedFile();
				}
			}
		});
		menuFile.add(itemChooseTrashDir);
		itemChooseFavoritesDir = new JMenuItem(ITEM_CHOOSE_FAVORITES_DIR);
		itemChooseFavoritesDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				jfc.setMultiSelectionEnabled(false);
				if (jfc.showOpenDialog(Slideshow.getSlideshow()) == JFileChooser.APPROVE_OPTION) {
					favoritesDir = jfc.getSelectedFile();
				}
			}
		});
		menuFile.add(itemChooseFavoritesDir);
		menuFile.addSeparator();
		itemQuit = new JMenuItem(ITEM_QUIT);
		itemQuit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		menuFile.add(itemQuit);

		// slideshow menu
		menuSlideshow = new JMenu(MENU_SLIDESHOW);
		menubar.add(menuSlideshow);
		itemFullscreen = new JMenuItem(ITEM_FULLSCREEN);
		itemFullscreen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setFullscreen(true);
			}
		});
		menuSlideshow.add(itemFullscreen);
		itemStart = new JMenuItem(ITEM_START);
		itemStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});
		menuSlideshow.add(itemStart);
		itemStop = new JMenuItem(ITEM_STOP);
		itemStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		menuSlideshow.add(itemStop);

		setJMenuBar(menubar);
	}

	public void start() {
		thread = new Thread(this);
		thread.start();
		System.out.println("started");
		setTitle("auto");
	}

	public void stop() {
		thread = null;
		System.out.println("stopped");
		setTitle("");
	}

	public void run() {
		while (thread == Thread.currentThread()) {
			try {
				/*
				if (nextImage == null) {
					stop();
					break;
				}
				photoCanvas.setImage(nextImage);
				if (currentImage != null)  currentImage.flush();
				currentImage = nextImage;
				currentFile = nextFile;
				if (imageIter.hasNext()) {
					nextFile = imageIter.next();
					nextImage = toolkit.getImage(nextFile.getAbsolutePath());
				} else {
					nextImage = null;
				}
				*/
				if (!next()) {
					stop();
					break;
				}
				Thread.sleep(duration);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	private boolean next() {
		if (nextImage == null)  return false;

		photoCanvas.setImage(nextImage);

		if (prevImage != null)  prevImage.flush();

		prevImage = currentImage;
		prevFile = currentFile;
		currentImage = nextImage;
		currentFile = nextFile;

		currentID++;
		if (currentID < imageFiles.size() - 1) {
			nextFile = imageFiles.get(currentID + 1);
			nextImage = toolkit.getImage(nextFile.getAbsolutePath());
		} else {
			nextFile = null;
			nextImage = null;
		}
		if (currentID >= imageFiles.size())  currentID = imageFiles.size() - 1;

		return true;
	}

	private boolean prev() {
		if (prevImage == null)  return false;

		photoCanvas.setImage(prevImage);

		if (nextImage != null)  nextImage.flush();

		nextImage = currentImage;
		nextFile = currentFile;
		currentImage = prevImage;
		currentFile = prevFile;

		currentID--;
		if (currentID > 0) {
			prevFile = imageFiles.get(currentID - 1);
			prevImage = toolkit.getImage(prevFile.getAbsolutePath());
		} else {
			prevFile = null;
			prevImage = null;
		}
		if (currentID < 0)  currentID = 0;

		return true;
	}


	private void readFiles(File[] dirs) {
		for (File d : dirs) {
			readFiles(d);
		}
	}

	private void readFiles(File dir) {
		File[] files = dir.listFiles(filenameFilter);
		for (File f : files) {
			if (!f.isDirectory())  imageFiles.add(f);
		}

		// search subdirectories
		files = dir.listFiles();
		for (File f : files) {
			if (f.isDirectory())  readFiles(f);
		}
	}

	private void setFullscreen(boolean mode) {
		isFullscreen = mode;

		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if (mode) {
		// fullscreen
			photoCanvas.storeCurrentSize();
			dispose();
			setUndecorated(true);
			menubar.setVisible(false);
			pack();
			setVisible(true);
			device.setFullScreenWindow(this);
		} else {
			dispose();
			setUndecorated(false);
			if (thread != null)  setTitle("auto");
			menubar.setVisible(true);
			pack();
			setVisible(true);
			device.setFullScreenWindow(null);
		}
	}

	private String addNumberToFilename(String filename, int num) {
		int suffixPos = filename.lastIndexOf('.');
		if (suffixPos == -1)  return filename + num;
		StringBuilder newFilename = new StringBuilder(filename.substring(0, suffixPos));
		newFilename.append("_" + num);
		newFilename.append(filename.substring(suffixPos));
		return newFilename.toString();
	}

	private String getSuffix(File file) {
		String filename = file.getName();
		int suffixPos = filename.lastIndexOf('.');
		return filename.substring(suffixPos);
	}

	private String getHashedFilename(File source) {
		DigestInputStream in = null;
		StringBuffer buf = new StringBuffer();
		try {
			in = new DigestInputStream(new FileInputStream(source), md);
			while (in.read() != -1) ;
			byte[] digest = md.digest();
			for (int i = 0; i < digest.length; i++) {
				String c = Integer.toHexString(digest[i] & 0xFF);
				if (c.length() == 1)  buf.append('0');
				buf.append(c);
			}
			buf.append(getSuffix(source));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
		return buf.toString();
	}

	public static void main (String args[]) {
		Slideshow app = Slideshow.getSlideshow();
		app.pack();
		app.setVisible(true);
	}
}
