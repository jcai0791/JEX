package Database.SingleUserDatabase;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.Parameter;
import Database.Definition.Type;
import function.plugin.IJ2.IJ2PluginUtility;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.Canceler;
import miscellaneous.DateUtility;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import preferences.XPreferences;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

public class JEXWriter {
	
	// File system statics
	public static String ATTACHEDFILES = "Attached Files";
	public static String NOTES = "Note.rtf";
	
	// ---------------------------------------------
	// Saving methods
	// ---------------------------------------------
	
	public static void saveBDInfo(JEXDBInfo dbInfo)
	{
		// Get the file location to save the db info file
		String path = dbInfo.getAbsolutePath();
		File f = new File(path);
		
		// Create an archive path
		String dbFileName = FileUtility.getNextName(f.getParent(), JEXDBInfo.LOCAL_DBINFO_CURRENTVERSION, "Archive");
		File newf = new File(f.getParent() + File.separator + dbFileName);
		
		// If an old one exists, create an archive
		if(f.exists())
		{
			try
			{
				FileUtils.copyFile(f, newf);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		// Save the info file
		dbInfo.set(JEXDBInfo.DB_MDATE, DateUtility.getDate());
		dbInfo.getXML().saveToPath(dbInfo.getAbsolutePath());
	}
	
	public static JEXData performVirtualMerge(JEXData[] dataArray, JEXEntry e, String mergedName, Canceler canceler)
	{
		Vector<JEXData> datalist = new Vector<>();
		for(JEXData d : dataArray)
		{
			datalist.add(d);
		}
		return performVirtualMerge(datalist, e, mergedName, canceler);
	}
	
	public static JEXData copyData(JEXData data)
	{
		JEXData ret = new JEXData(data);
		
		int count = 0;
		int percentage = 0;
		int tot = data.getDimTable().mapCount();
		for(DimensionMap map : data.getDimTable().getMapIterator())
		{
			JEXDataSingle toAdd = null;
			toAdd = data.getData(map).copy();
			if(toAdd == null)
			{
				continue;
			}
			
			if(data.getTypeName().getType().matches(JEXData.FILE) || data.getTypeName().getType().matches(JEXData.IMAGE) || data.getTypeName().getType().matches(JEXData.MOVIE) || data.getTypeName().getType().matches(JEXData.SOUND))
			{
				// Copy any additional file data if necessary, otherwise it is stored directly in the JEXDataSingle
				File src = new File(JEXWriter.getDatabaseFolder() + File.separator + toAdd.get(JEXDataSingle.RELATIVEPATH));
				String extension = FileUtility.getFileNameExtension(toAdd.get(JEXDataSingle.RELATIVEPATH));
				String relativePath = JEXWriter.getUniqueRelativeTempPath(extension);
				File dst = new File(JEXWriter.getDatabaseFolder() + File.separator + relativePath);
				try {
					if(!JEXWriter.copy(src, dst))
					{
						Logs.log("Failed to copy a file for the object! Aborting", JEXWriter.class);
						return null;
					}
				} catch (IOException e) {
					Logs.log("Failed to copy a file for the object! Aborting", JEXWriter.class);
					return null;
				}
				toAdd.put(JEXDataSingle.RELATIVEPATH, relativePath);
			}

			ret.addData(map.copy(), toAdd.copy());
			// Status bar
			count = count + 1;
			percentage = (int) (100 * ((double) count / (double) tot));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		ret.setDimTable(new DimTable(ret.getDataMap()));
		return ret;
	}
	
	/**
	 * Merge newData into old data, replacing where necessary.
	 * Data earlier in the list takes precedence.
	 * @param datalist
	 */
	public static JEXData performVirtualMerge(Collection<JEXData> datalist, JEXEntry e, String mergedName, Canceler canceler)
	{
		// Return null if no data provided
		if(datalist == null || datalist.size() == 0)
		{
			return null;
		}
		
		// Check that all the data in the datalist are of the same general Type
		Type t = datalist.iterator().next().getTypeName().getType();
		for(JEXData d : datalist)
		{
			if(!d.getTypeName().getType().matches(t))
			{
				JEXDialog.messageDialog("All data must be of the same type to merge. Aborting.", JEXWriter.class);
				return null;
			}
		}
		
		// Create a DimTable for the unioned object
		DimTable retDT = new DimTable();
		for(JEXData d : datalist)
		{
			retDT = DimTable.union(retDT, d.getDimTable());
		}
		
		// Make sure the DimTable for each object has all the same dimension names as the unioned object (i.e., dimensionality matches)
		for(JEXData d : datalist)
		{
			DimTable tempDT = d.getDimTable();
			for(Dim dim : retDT)
			{
				if(tempDT.getDimWithName(dim.name())==null)
				{
					JEXDialog.messageDialog("The dimension " + dim.name() + " does not exist in all objects. For example that dim doesn't exist in " + d.getTypeName().toString() + ". Aborting.", JEXWriter.class);
					return null;
				}
			}
		}
		
		// Create a new object of merged data.
		// Create the fresh new object
		JEXData ret = new JEXData(t, mergedName);

		// Initialize status parameters
		int tot = retDT.mapCount();
		int count = 0;
		int percentage = 0;

		Logs.log("" + retDT, JEXWriter.class);
		// For each map in the unioned DimTable
		for(DimensionMap map : retDT.getMapIterator())
		{
			// Try to grab and copy the JEXDataSingles from each object prioritizing objects higher in the list.
			JEXDataSingle toAdd = null;
			for(JEXData d : datalist)
			{
				if(d.getData(map) != null)
				{
					toAdd = d.getData(map);
					break; // This way, the first object with something at this dimension map takes precedent
				}
			}
			if(toAdd == null)
			{
				continue;
			}
			
			// Check to see if progress should be canceled.
			if(canceler != null && canceler.isCanceled())
			{
				return null;
			}
			
			//			// If there are attached files, copy over that information
			//			if(t.matches(JEXData.FILE) || t.matches(JEXData.IMAGE) || t.matches(JEXData.MOVIE) || t.matches(JEXData.SOUND))
			//			{
			//				toAdd.put(JEXDataSingle.RELATIVEPATH,  toAdd.get(JEXDataSingle.RELATIVEPATH));
			//			}

			ret.addData(map.copy(), toAdd.copy());
			// Status bar
			count = count + 1;
			percentage = (int) (100 * ((double) count / (double) tot));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		Logs.log("Printing Merged Data:", JEXWriter.class);
		for(DimensionMap map : ret.getDataMap().keySet())
		{
			Logs.log("" + map + " " + ret.getDataMap().get(map), JEXWriter.class);
		}
		
		ret.setDimTable(new DimTable(ret.getDataMap()));
		//	output.add(ret);

		return ret;
		// Return status
		// return true;
	}
	
	public static void editDBInfo(JEXDBInfo dbInfo, String name, String info, String password)
	{
//		// Can't reliably change the database name yet so this is disabled for
//		// the moment.
//		Logs.log("Can't reliably change the database name yet so this is disabled for the moment.", 0, JEXWriter.class.getSimpleName());
//		return;
		if(name == null || name.equals(""))
		{
			return;
		}

		// Archive the new info into the current DBInfo
		// Not saving the DB_NAME anymore. Just using the parent folder name instead.
		dbInfo.set(JEXDBInfo.DB_INFO, info);
		dbInfo.set(JEXDBInfo.DB_PWD, password);
		JEXWriter.saveBDInfo(dbInfo);

		File currentDBDirectory = new File(dbInfo.getDirectory());
		String currentDBDirectoryParent = currentDBDirectory.getParent();

		// Check the new name and change the directory name if necessary
		name = FileUtility.removeWhiteSpaceOnEnds(name);
		File newDBDirectory = new File(currentDBDirectoryParent + File.separator + name);
		if(!name.equals(dbInfo.getDBName()))
		{
			// If the name changed then the folder of the database requires a name change
			// This method renames the folder.
			// We can do this and not affect DB behavior as long as we change the database name and
			// classy path (jexpath) in the dbInfo object held by JEXManager
			// because all file paths are relative to dbInfo.getDirectory(), which references jexPath
			boolean successful = currentDBDirectory.renameTo(newDBDirectory);
			if(successful)
			{
				// Change the information in dbInfo
				dbInfo.setPath(newDBDirectory.getAbsolutePath() + File.separator + JEXDBInfo.LOCAL_DBINFO_CURRENTVERSION);
			}
			else
			{
				JEXDialog.messageDialog("Couldn't rename database for some reason. Check to see if a database of that name already exists?");
			}	
		}
	}
	
	public static void cleanDB(JEXDBInfo dbInfo)
	{   
		
	}
	
	// ---------------------------------------------
	// Temporary saving methods
	// ---------------------------------------------
	
	/**
	 * Save the image in the temporary database folder
	 */
	public static String saveImage(ImagePlus im)
	{
		// Create the file path
		String fullPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("tif");
		
		// If the image is null return null
		if(im == null)
		{
			return null;
		}
		
		// Save the image
		FileSaver imFS = new FileSaver(im);
		Logs.log("Saving image to: " + fullPath, 1, JEXWriter.class);
		boolean success = imFS.saveAsTiff(fullPath);
		if(success)
		{
			return fullPath;
		}
		else
		{
			Logs.log("Error saving image to: " + fullPath, 1, JEXWriter.class);
			return null;
		}
	}
	
	/**
	 * Save the image in the temporary database folder
	 */
	public static <T extends RealType<T>> String saveImage(RandomAccessibleInterval<T> img)
	{
		ImagePlus im = ImageJFunctions.wrap(img, "temp");
		im.setFileInfo(new FileInfo());
		return JEXWriter.saveImage(im);
	}
	
	/**
	 * Save the image in the temporary database folder
	 */
	public static <T extends RealType<T>> void debugImage(Img<T> img)
	{
		ImagePlus im = ImageJFunctions.wrap(img, "temp");
		String path = JEXWriter.saveImage(im);
		try {
			FileUtility.openFileDefaultApplication(path);
		} catch (Exception e) {
			Logs.log("Couldn't save, open, and show the supplied image.", FileUtility.class);
			e.printStackTrace();
		}
	}
	
	public static void debugImage(ImageProcessor imp)
	{
		imp.resetMinAndMax();
		try {
			FileUtility.openFileDefaultApplication(JEXWriter.saveImage(imp));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void debugImage(ImagePlus imp)
	{
		imp.getProcessor().resetMinAndMax();
		try {
			FileUtility.openFileDefaultApplication(JEXWriter.saveImage(imp));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void convertToBitDepthIfNecessary(ImagePlus im, int bitDepth)
	{
		if(bitDepth != im.getBitDepth())
		{
			if(bitDepth == 8)
			{
				im.setProcessor(im.getProcessor().convertToByte(false));
			}
			else if(bitDepth == 16)
			{
				im.setProcessor(im.getProcessor().convertToShort(false));
			}
			else if(bitDepth == 32)
			{
				im.setProcessor(im.getProcessor().convertToFloat());
			}
		}
	}
	
	/**
	 * If conversion is necessary, a new object is returned. If not, the same instance is returned.
	 * 
	 * @param imp
	 * @param bitDepth
	 * @return
	 */
	public static ImageProcessor convertToBitDepthIfNecessary(ImageProcessor imp, int bitDepth)
	{
		if(imp.getBitDepth() == 32 && bitDepth < 32)
		{
			imp.setMinAndMax(0, Math.pow(2, bitDepth)-1);
		}
		if(bitDepth != imp.getBitDepth())
		{
			if(bitDepth == 8)
			{
				return imp.convertToByteProcessor(false);
			}
			else if(bitDepth == 16)
			{
				return imp.convertToShortProcessor(false);
			}
			else
			{
				return imp.convertToFloatProcessor();
			}
		}
		else
		{
			return imp;
		}
	}
	
	/**
	 * Save the image in the temporary database folder
	 */
	public static String saveImage(Dataset im)
	{
		// Create the file path
		String fullPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("tif");
		
		// If the image is null return null
		if(im == null)
		{
			return null;
		}
		
		// Save the image
		Logs.log("Saving image to: " + fullPath, 1, JEXWriter.class);
		try
		{
			IJ2PluginUtility.ij().io().save(im, fullPath);
		}
		catch (IOException e)
		{
			Logs.log("Error saving image to: " + fullPath, 1, JEXWriter.class);
			e.printStackTrace();
			return null;
		}
		return fullPath;
	}
	
	/**
	 * Save the image in the temporary database folder
	 */
	public static String saveImage(ImageProcessor imp)
	{
		return saveImage(new ImagePlus("", imp));
	}
	
	/**
	 * Save the image in the temporary database folder
	 */
	public static String saveImage(ImageProcessor imp, int bitDepth)
	{
		imp = JEXWriter.convertToBitDepthIfNecessary(imp, bitDepth);
		return saveImage(new ImagePlus("", imp));
	}
	
	/**
	 * Save the image in the temporary database folder
	 * 
	 * WARNING: this converts the ImagePlus to the specified bitDepth internally if necessary.
	 */
	public static String saveImage(ImagePlus im, int bitDepth)
	{
		JEXWriter.convertToBitDepthIfNecessary(im, bitDepth);
		return saveImage(im);
	}
	
	/**
	 * Save figure in using type defined by extension (jpg, tif, or png)
	 * 
	 * @param bImage
	 * @param extension
	 */
	public static String saveFigure(BufferedImage bImage, String extension)
	{
		return saveFigure(new ImagePlus("figure", bImage), extension);
	}
	
	/**
	 * Save figure in using type defined by extension (jpg, tif, or png)
	 * 
	 * @param im
	 * @param extension
	 */
	public static String saveFigure(ImagePlus im, String extension)
	{
		String fullPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(extension);
		
		Logs.log("Saving figure to: " + fullPath, 1, null);
		
		FileSaver fs = new FileSaver(im);
		if(extension.equals("jpg"))
		{
			fs.saveAsJpeg(fullPath);
		}
		else if(extension.equals("tif"))
		{
			fs.saveAsTiff(fullPath);
		}
		else if(extension.equals("png"))
		{
			fs.saveAsPng(fullPath);
		}
		else
		{
			Logs.log("Extension not supported: " + extension + ", must be jpg, tif, or png.", 0, null);
			return null;
		}
		
		return fullPath;
	}
	
	/**
	 * Save the image in the temporary database folder
	 */
	public static String saveText(String text, String extension)
	{
		// Create the file path
		String fullPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(extension);
		
		// Save the text
		printToFile(text, fullPath);
		
		return fullPath;
	}
	
	/**
	 * Copy file SRC into DST, force overwrite if possible
	 * 
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static boolean copy(File src, File dst, boolean forceCopyWithinTempFolder) throws IOException
	{
		// test if destination or source files are null
		if(src == null || dst == null)
		{
			Logs.log("File copy impossible, source or destination is null", 1, "FileUtility");
			return false;
		}
		
		if(src.equals(dst))
		{
			Logs.log("No copying necessary (src == dst)", 1, "FileUtility");
			return true;
		}
		
		// Make destination folder if it doesn't exist
		if(!dst.getParentFile().exists())
		{
			Logs.log("Making destination folder " + dst.getParent(), 1, "FileUtility");
			dst.getParentFile().mkdirs();
		}
		
		boolean srcIsInTempFolder = FileUtility.isFileInDirectory(src, new File(JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName()));
		boolean srcInSameFolderAsDest = src.getParentFile().equals(dst.getParentFile());
		
		if(srcInSameFolderAsDest && !srcIsInTempFolder) // Just rename/move
		{
			Logs.log("(src folder == dst folder). Renaming file " + src.getPath() + " to " + dst.getPath(), 1, "FileUtility");
			return FileUtility.moveFileOrFolder(src, dst, true);
		}
		
		if(srcIsInTempFolder && !forceCopyWithinTempFolder) // Just rename/move
		{
			Logs.log("(src folder == dst folder). Renaming file " + src.getPath() + " to " + dst.getPath(), 1, "FileUtility");
			return FileUtility.moveFileOrFolder(src, dst, true);
		}
		else
			// else copy
		{
			return copyFileOrFolder(src, dst, true);
		}
	}
	
	/**
	 * Copies the file or folder from src to dst creating directories as needed and overwrites the file if overwrite is true. If src is a directory, this method copies the contents of the specified source directory to within the specified destination
	 * directory. The destination directory is created if it does not exist. If the destination directory did exist, then this method merges the source with the destination, with the source taking precedence.
	 * 
	 * @param src
	 * @param dst
	 * @param overwrite
	 * @return
	 */
	private static boolean copyFileOrFolder(File src, File dst, boolean overwrite)
	{
		Logs.log("Copying from " + src.getPath() + " to " + dst.getPath(), 0, JEXWriter.class);
		
		if(!overwrite)
		{
			if(dst.exists())
			{
				Logs.log("Destination file/folder exists. Copying canceled: from " + src.getPath() + " to " + dst.getPath(), 0, JEXWriter.class);
				return false;
			}
		}
		
		if(src.isDirectory())
		{
			try
			{
				FileUtils.copyDirectory(src, dst);
				return true;
			}
			catch (IOException e)
			{
				Logs.log("Error occurred during directory copy from " + src.getPath() + " to " + dst.getPath(), 0, JEXWriter.class);
				e.printStackTrace();
				return false;
			}
		}
		else
		{
			try
			{
				FileUtils.copyFile(src, dst);
				return true;
			}
			catch (IOException e)
			{
				Logs.log("Error occurred file copy from " + src.getPath() + " to " + dst.getPath(), 0, JEXWriter.class);
				e.printStackTrace();
				return false;
			}
		}
	}
	
	/**
	 * Copy file SRC into DST, force overwrite if possible
	 * 
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static boolean copy(File src, File dst) throws IOException
	{
		return copy(src, dst, false); // forceCopyWithinTempFolder = false; will
		// overwrite
	}
	
	/**
	 * Copy a file to the temp folder
	 */
	public static String saveFile(File f)
	{
		// Create the file path
		String extension;
		try
		{
			extension = miscellaneous.FileUtility.getFileNameExtension(f.getCanonicalPath());
			String fullPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(extension);
			
			// Save the image
			File dst = new File(fullPath);
			JEXWriter.copy(f, dst);
			return dst.getCanonicalPath();
		}
		catch (IOException e)
		{
			Logs.log("Couldn't copy file! Returning null.", 0, "JEXWriter");
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
	
	// ---------------------------------------------
	// DB creation methods
	// ---------------------------------------------
	
	/**
	 * Create a new local database
	 * 
	 * @param name
	 * @param info
	 * @param password
	 * @return
	 */
	public static JEXDBInfo createDBInfo(Repository rep, String name, String info, String password)
	{
		// Make database folder
		File folder = new File(rep.getPath() + File.separator + name);
		boolean madeFolders = false;
		if(folder.exists())
		{
			JEXStatics.statusBar.setStatusText("DB creation impossible, folder exists... " + folder.getPath());
			Logs.log("DB creation impossible, folder exists... " + folder.getPath(), JEXWriter.class);
			return null;
		}
		else
		{
			madeFolders = folder.mkdirs();
		}
		
		if(!madeFolders)
		{
			JEXStatics.statusBar.setStatusText("DB creation impossible, folder exists... " + folder.getPath());
			Logs.log("DB creation impossible, couldn't create necessary folder... " + folder.getPath(), JEXWriter.class);
			return null;
		}
		
		XPreferences dbInfo = new XPreferences();
		
		dbInfo.put(JEXDBInfo.DB_INFO, info);
		dbInfo.put(JEXDBInfo.DB_AUTHOR, JEXStatics.jexManager.getUserName());
		dbInfo.put(JEXDBInfo.DB_DATE, miscellaneous.DateUtility.getDate());
		dbInfo.put(JEXDBInfo.DB_MDATE, miscellaneous.DateUtility.getDate());
		dbInfo.put(JEXDBInfo.DB_VERSION, JEXDBIO.VERSION);
		dbInfo.put(JEXDBInfo.DB_PWD, password);
		dbInfo.put(JEXDBInfo.DB_PWDREQUIRED, "false");
		dbInfo.put(JEXDBInfo.DB_TYPE, JEXDB.LOCAL_DATABASE);
		
		String path = folder.getAbsolutePath() + File.separator + JEXDBInfo.LOCAL_DBINFO_CURRENTVERSION;
		boolean success = dbInfo.saveToPath(path);
		
		if(!success)
		{
			return null;
		}
		
		JEXDBInfo ret = new JEXDBInfo(path);
		return ret;
	}
	
	// ---------------------------------------------
	// Path methods
	// ---------------------------------------------
	
	/**
	 * Returns the database folder, ie the root of the file structure
	 * 
	 * @return
	 */
	public static String getDatabaseFolder()
	{
		return DirectoryManager.getHostDirectory().getAbsolutePath();
		// JEXDBInfo dbInfo = JEXStatics.jexManager.getDatabaseInfo();
		// if(dbInfo == null)
		// {
		// return null;
		// }
		// return dbInfo.getDirectory();
	}
	
	/**
	 * Returns the folder path relative to the database containing all the data and attached files to the experiment of entry
	 * 
	 * @param entry
	 * @return
	 */
	public static String getExperimentFolder(JEXEntry entry, boolean loadPath, boolean relative)
	{
		String expName;
		if(loadPath && entry.loadTimeExperimentName != null)
		{
			expName = FileUtility.removeWhiteSpaceOnEnds(entry.loadTimeExperimentName);
		}
		else
		{
			expName = FileUtility.removeWhiteSpaceOnEnds(entry.getEntryExperiment());
		}
		// Create it if the folder doesn't exist
		File f = new File(JEXWriter.getDatabaseFolder() + File.separator + expName);
		if(!f.exists())
		{
			f.mkdirs();
		}
		
		if(relative)
		{
			return expName;
		}
		else
		{
			return f.getAbsolutePath();
		}
	}
	
	/**
	 * Returns the folder path relative to the database containing all the data and attached files to the array of entry
	 * 
	 * @param entry
	 * @return
	 */
	public static String getOldArrayFolder(JEXEntry entry, boolean relative)
	{
		String arrayName;
		arrayName = FileUtility.removeWhiteSpaceOnEnds(entry.loadTimeTrayName);
		// Create it if the folder doesn't exist
		File f = new File(JEXWriter.getExperimentFolder(entry, true, false) + File.separator + arrayName);
		if(!f.exists())
		{
			f.mkdirs();
		}
		
		return JEXWriter.getExperimentFolder(entry, true, relative) + File.separator + arrayName;
	}
	
	/**
	 * Returns the folder path relative to the database containing all the data and attached files to the entry of entry
	 * 
	 * @param entry
	 * @return
	 */
	public static String getEntryFolder(JEXEntry entry, boolean loadPath, boolean relative)
	{
		String entryFolderName = "Cell_x" + entry.getTrayX() + "_y" + entry.getTrayY();
		
		// Create it if the folder doesn't exist
		File f = new File(JEXWriter.getExperimentFolder(entry, loadPath, false) + File.separator + entryFolderName);
		if(!f.exists())
		{
			f.mkdirs();
		}
		
		return JEXWriter.getExperimentFolder(entry, loadPath, relative) + File.separator + entryFolderName;
	}
	
	/**
	 * Returns the folder path relative to the database containing all the data and attached files to the entry of entry
	 * 
	 * @param entry
	 * @return
	 */
	public static String getOldEntryFolder(JEXEntry entry, boolean relative)
	{
		String entryFolderName = "Cell_x" + entry.getTrayX() + "_y" + entry.getTrayY();
		
		// Create it if the folder doesn't exist
		File f = new File(JEXWriter.getOldArrayFolder(entry, false) + File.separator + entryFolderName);
		if(!f.exists())
		{
			f.mkdirs();
		}
		
		return JEXWriter.getOldArrayFolder(entry, relative) + File.separator + entryFolderName;
	}
	
	/**
	 * Returns the folder path relative to the database containing all the data and attached files to the entry of entry
	 * 
	 * @param entry
	 * @return
	 */
	public static String getDataFolder(JEXData data, boolean relative)
	{
		String dataFolderName = FileUtility.removeWhiteSpaceOnEnds(data.getTypeName().toString());
		
		// Create it if the folder doesn't exist
		File f = new File(JEXWriter.getEntryFolder(data.getParent(), false, false) + File.separator + dataFolderName);
		if(!f.exists())
		{
			f.mkdirs();
		}
		
		return JEXWriter.getEntryFolder(data.getParent(), false, relative) + File.separator + dataFolderName;
	}
	
	// /**
	// * Return the path to the attached files for an entry
	// * The hierarchylevel sets the scope of the folder returned
	// * it can take the values of Experiment, Tray or nothing for hte whole DB
	// */
	// public static String getAttachedFolderPath(JEXEntry entry, String
	// hierarchyLevel, boolean loadPath, boolean relative)
	// {
	// String result = null;
	//
	// if (hierarchyLevel.equals(""))
	// {
	// if(relative)
	// {
	// result = ATTACHEDFILES;
	// }
	// else
	// {
	// result = getDatabaseFolder() + File.separator + ATTACHEDFILES;
	// }
	// }
	// else if (hierarchyLevel.equals(JEXEntry.EXPERIMENT)){
	// result = getExperimentFolder(entry, loadPath, relative) + File.separator
	// + ATTACHEDFILES;
	// }
	// else if (hierarchyLevel.equals(JEXEntry.TRAY)){
	// result = getArrayFolder(entry, loadPath, relative) + File.separator +
	// ATTACHEDFILES;
	// }
	//
	// // Create it if the folder doesn't exist
	// File f;
	// if(relative)
	// {
	// f = new File(JEXWriter.getDatabaseFolder() + File.separator + result);
	// }
	// else
	// {
	// f = new File(result);
	// }
	//
	// if (!f.exists()) f.mkdirs();
	//
	// return result;
	// }
	
	// /**
	// * Return the path to the attached notes file for a specific entry
	// * at a specific hierarchy level
	// */
	// public static String getAttachedNotesPath(JEXEntry entry, String
	// hierarchyLevel, boolean loadPath, boolean relative)
	// {
	// String result =
	// getAttachedFolderPath(entry,hierarchyLevel,loadPath,relative);
	//
	// if (result == null)
	// {
	// return null;
	// }
	//
	// result = result + File.separator + NOTES;
	//
	// // Create it if the folder doesn't exist
	// File f;
	// if(relative)
	// {
	// f = new File(JEXWriter.getDatabaseFolder() + File.separator + result);
	// }
	// else
	// {
	// f = new File(result);
	// }
	// if (!f.exists()) XMLUtility.XMLsave(result, "");
	//
	// return result;
	// }
	
	/**
	 * Get the path of the temporary folder to save data that is not yet attached to the database
	 * 
	 * @return
	 */
	public static String getTempFolderName()
	{
		// Create it if the folder doesn't exist
		File f = null;
		try
		{
			f = new File(DirectoryManager.getTempFolderPath());
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
		if(!f.exists())
		{
			f.mkdirs();
		}
		return DirectoryManager.TEMP_FOLDER_NAME;
	}
	
	/**
	 * Return a unique name in the temporary folder
	 * 
	 * @return
	 */
	public synchronized static String getUniqueRelativeTempPath(String extension)
	{
		// Create the file path
		String relativePath = null;
		try
		{
			relativePath = DirectoryManager.getUniqueRelativeTempPath(extension);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
		return relativePath;
	}
	
	// /**
	// * Get the next free file name based on the core name, a suffix and a
	// * selected extension
	// *
	// * @param path
	// * @param coreName
	// * @param suffix
	// * @param extension
	// * @return
	// */
	// private synchronized static String getAvailableTempName(String coreName,
	// int suffixNumberLength, String extension)
	// {
	// String fileName = coreName + StringUtility.fillLeft("" + fileCounter,
	// suffixNumberLength, "0") + "." + extension;
	// fileCounter = fileCounter + 1;
	// return fileName;
	// }
	
	/**
	 * Print string STR in file at path PATH
	 * 
	 * @param str
	 * @param path
	 */
	private static void printToFile(String str, String path)
	{
		try
		{
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
			writer.write(str);
			writer.close();
		}
		catch (IOException e)
		{
			System.out.println("ERROR creatingfile");
		}
	}

	/**
	 * Saves an arff file of this format:
	 * imagePath,
	 * functionName,
	 * Parameter1name, Parameter1value,
	 * Parameter2name, Parameter2value,
	 * 
	 * @param imagePath
	 * @param parameters
	 * @param functionName
	 * @return path
	 */
	public static String saveVirtualImage(TreeMap<String,String> inputs, TreeMap<String,Parameter> parameters, String functionName) {
		// Create the file path
		String fullPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("vfn");

		// If the image is null return null
		if(inputs==null||parameters==null) return null;

		// Save the image
		
		Logs.log("Saving virtual image to: " + fullPath, 1, JEXWriter.class);
		try
		{
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPath), "UTF-8"));
			writer.write(functionName+",\n");
			for(String s : inputs.keySet()) {
				writer.write("Input,"+s+","+inputs.get(s)+",\n");
			}
			for(String s : parameters.keySet()) {
				writer.write("Parameter,"+parameters.get(s).getTitle()+","+parameters.get(s).getValue()+",\n");
			}
			writer.close();
		}
		catch (IOException e)
		{
			System.out.println("ERROR creating file");
		}
		return fullPath;
	}
	
}
