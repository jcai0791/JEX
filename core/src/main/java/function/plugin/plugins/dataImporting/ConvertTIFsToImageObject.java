package function.plugin.plugins.dataImporting;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.scijava.plugin.Plugin;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;
import tables.DimensionMap;


@Plugin(
		type = JEXPlugin.class,
		name="Import TIF Images as Image Object",
		menuPath="Data Importing",
		visible=true,
		description="Import images from a NIS Elements ND Acquisition ("
				+ "multiple, colors, times, locations, large image arrays... no"
				+ " Z stacks yet) from tif stacks to individual images"
		)
/*
 * This is a conversion of the old JEX_CTC_ImportImagesFromNISFolder.java
 * function to the new JEXPlugin style. The majority of the code is copied from
 * that class with the exception of input/output/parameter handling.
 * 
 * @author converted by Tom Huibregtse
 */
public class ConvertTIFsToImageObject extends JEXPlugin {

	public ConvertTIFsToImageObject()
	{}

	/////////// Define Inputs ///////////

	/*
	 * None necessary; Input Directory is classified as a parameter but could
	 * still be considered an input.
	 */

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input Directory", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;

	/////////// Define Outputs ///////////

	@OutputMarker(name="Multicolor TIF Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The converted image object", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		// create file object for input directory
		File inFolder = new File(inDir);

		// gather and sort the input files
		JEXStatics.statusBar.setStatusText("Sorting files to convert. May take minutes...");
		List<File> images = FileUtility.getSortedFileList(inFolder.listFiles(new SimpleFileFilter(new String[] { "tif" })));

		/* Save the data and create the database object */
		JEXStatics.statusBar.setStatusText("Converting files.");
		// Run the function
		double count = 0;
		double total = images.size();
		int percentage = 0;
		JEXStatics.statusBar.setProgressPercentage(percentage);
		// TreeMap<Integer,TreeMap<DimensionMap,String>> importedImages = new TreeMap<Integer,TreeMap<DimensionMap,String>>();
		TreeMap<DimensionMap,String> compiledImages = new TreeMap<DimensionMap,String>();

		for (File f : images)
		{
			if(this.isCanceled())
			{
				return false;
			}
			ImagePlus im = new ImagePlus(f.getAbsolutePath());
			DimensionMap baseMap = this.getMapFromPath(f.getAbsolutePath());
			for (int s = 1; s < im.getStackSize() + 1; s++)
			{
				if(this.isCanceled())
				{
					return false;
				}
				// TreeMap<DimensionMap,String> colorSet = importedImages.get(s);
				// if(colorSet == null)
				// {
				// colorSet = new TreeMap<DimensionMap,String>();
				// importedImages.put(s, colorSet);
				// }
				im.setSlice(s);

				// Save image into map specific to this color
				// String imagePath = JEXWriter.saveImage(im.getProcessor());
				// colorSet.put(baseMap.copy(), imagePath);

				// Save image into map for a multicolor image object for easier viewing etc.
				String imagePath2 = JEXWriter.saveImage(im.getProcessor());
				DimensionMap mapWithColor = baseMap.copy();
				mapWithColor.put("Color", "" + s);
				compiledImages.put(mapWithColor, imagePath2);

			}
			count = count + 1;
			percentage = (int) (100 * ((count) / (total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		output = ImageWriter.makeImageStackFromPaths(output.name, compiledImages);
		
		return true;
	}
	
	public DimensionMap getMapFromPath(String filePath)
	{
		String name = FileUtility.getFileNameWithoutExtension(filePath);
		String[] names = name.split("_");
		String x = names[1].substring(1);
		String y = names[2].substring(1);
		x = Integer.valueOf(x).toString();
		y = Integer.valueOf(y).toString();
		return new DimensionMap("ImRow=" + y + ",ImCol=" + x);
	}
	
	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}
		ImagePlus im = new ImagePlus(imagePath);
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
		
		// Adjust the image
		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
		
		// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		im.flush();
		
		// return temp filePath
		return imPath;
	}
	
	public static TreeSet<DimensionMap> getSplitDims(int rows, int cols)
	{
		TreeSet<DimensionMap> ret = new TreeSet<DimensionMap>();
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				
				ret.add(new DimensionMap("ImRow=" + r + ",ImCol=" + c));
			}
		}
		return ret;
	}
	
	public static TreeMap<DimensionMap,ImageProcessor> splitRowsAndCols(ImageProcessor imp, int rows, int cols)
	{
		TreeMap<DimensionMap,ImageProcessor> ret = new TreeMap<DimensionMap,ImageProcessor>();
		
		int wAll = imp.getWidth();
		int hAll = imp.getHeight();
		int w = wAll / cols;
		int h = hAll / rows;
		
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				int x = c * w;
				int y = r * h;
				Rectangle rect = new Rectangle(x, y, w, h);
				imp.setRoi(rect);
				ImageProcessor toCopy = imp.crop();
				ImageProcessor toSave = imp.createProcessor(w, h);
				toSave.copyBits(toCopy, 0, 0, Blitter.COPY);
				ret.put(new DimensionMap("ImRow=" + r + ",ImCol=" + c), toSave);
			}
		}
		
		return ret;
	}
}
