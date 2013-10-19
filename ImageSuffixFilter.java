import java.io.*;
import java.lang.*;

class ImageSuffixFilter implements FilenameFilter {

static final String JPG_SUFFIX = ".JPG";
static final String jpg_SUFFIX = ".jpg";
static final String GIF_SUFFIX = ".GIF";
static final String gif_SUFFIX = ".gif";

	ImageSuffixFilter() {
		super();
	}

	static public String getBody(String name) {
	int	lastIndex;
		if (!(name.endsWith(JPG_SUFFIX) ||
		      name.endsWith(jpg_SUFFIX) ||
		      name.endsWith(GIF_SUFFIX) ||
		      name.endsWith(gif_SUFFIX)))  return null;

		if ((lastIndex = name.lastIndexOf(JPG_SUFFIX)) != -1) {
			return name.substring(0, lastIndex);
		} else
		if ((lastIndex = name.lastIndexOf(jpg_SUFFIX)) != -1) {
			return name.substring(0, lastIndex);
		} else
		if ((lastIndex = name.lastIndexOf(GIF_SUFFIX)) != -1) {
			return name.substring(0, lastIndex);
		} else
		if ((lastIndex = name.lastIndexOf(gif_SUFFIX)) != -1) {
			return name.substring(0, lastIndex);
		}

		return null;
	}

	public boolean accept(File dir, String name) {
		return (name.endsWith(JPG_SUFFIX) ||
			name.endsWith(jpg_SUFFIX) ||
			name.endsWith(GIF_SUFFIX) ||
			name.endsWith(gif_SUFFIX));
	}
}

