package function.plugin.plugins.R;

import java.util.TreeMap;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import logs.Logs;
import rtools.R;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Call R Script",
		menuPath="R",
		visible=true,
		description="Pass JEXData's to an R script with each JEXData as a data.frame table.\nUse 'jexTempRFolder' and 'jexDBFolder' variable for directory in which to create files and read database files from respectively."
		)
public class CallRScript extends JEXPlugin {

	public CallRScript()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="data1", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data1 for use in the provided R command.", optional=true)
	JEXData data1;

	@InputMarker(uiOrder=2, name="data2", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data2 for use in the provided R command.", optional=true)
	JEXData data2;

	@InputMarker(uiOrder=3, name="data3", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data3 for use in the provided R command.", optional=true)
	JEXData data3;

	@InputMarker(uiOrder=4, name="data4", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data4 for use in the provided R command.", optional=true)
	JEXData data4;

	@InputMarker(uiOrder=5, name="data5", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data5 for use in the provided R command.", optional=true)
	JEXData data5;

	/////////// Define Parameters ///////////
	public final String header = "library(foreign)\n" + 
			"library(data.table)\n" + 
			"library(bit64)\n" + 
			"sourceGitHubFile <- function(user, repo, branch, file){\n" + 
			"	require(curl)\n" + 
			"	destfile <- tempfile()\n" + 
			"	fileToGet <- paste0('https://raw.githubusercontent.com/', user, '/', repo, '/', branch, '/', file)\n" + 
			"	curl_download(url=fileToGet, destfile)\n" + 
			"	source(destfile)\n" + 
			"}\n" + 
			"sourceGitHubFile('jaywarrick','R-General','master','.Rprofile')\n" + 
			"sourceGitHubFile('jaywarrick','R-Cytoprofiling','master','PreProcessingHelpers.R')"+
			"my.roll.median <- function(x, win.width=2, na.rm=T, ...)\n" + 
			"{\n" + 
			"	# Also check out the \"fill\" argument to rollapply (?rollapply)\n" + 
			"	library(zoo)\n" + 
			"	# This will return a vector of the same size as original and will deal with NAs and optimize for mean.\n" + 
			"	return(rollapply(x, width=win.width, FUN=median, na.rm=na.rm, ..., partial=F, align='center'))\n" + 
			"}";

	public final String scriptDefaultValue = "### Some pointers... ###\n\n" + 

		"# jexTempRFolder is a temp folder in which you can save files freely using your\n" +
		"# own naming convetions (e.g.,\n" +
		"# copy.file(from='/Users/myName/myFolder/myFile.pdf',\n" +
		"# 	to=paste(jexTempRFolder, '/myFile.pdf'))\n\n" +

        "# Use single quotes for all strings in these scripts to avoid parsing issues\n\n" +

        "# jexDBFolder is the base folder from with all paths in JEXData objects are relative\n" +
        "# to. Thus, if you read a file path from a data object such as\n" +
        "# '/Cell_x0_y1/File-Results.txt' from within 'data1', the actual\n" +
        "# full file path is '<jexDBFolder>/Cell_x0_y1/File-Results.txt'\n\n" +

        "# data1... data5 are special variable names that can be used to pass data from\n" +
        "# JEX to R. These objects are the same as the .jxd objects within the database\n" +
        "# and typically refer to files or contain data such as point information within\n" +
        "# an ROI. These data are provided as a three part list. The first element\n" +
        "# (e.g., data1$type) is a string indicating the type of the object. The second \n" +
        "# element (e.g., data1$name) is a string indicating the name of the object. The\n" +
        "# third element (e.g., data1$value) contains the information/value/content of the\n" +
        "# data object from the database. This value element is the .jxd table associated\n" +
        "# with the object and links to files (e.g., image files) or contains data directly\n" +
        "# (e.g., the points of an ROI). This table is given as a data.frame using the 'foreign'\n" +
        "# and its 'read.arff' function. If one of these objects  'data1',\n" +
        "# contains a list of files, you can access them by calling data1$value$Value[i] where i\n" +
        "# is you index of interest. Use the 'Array' tab (tab number 2 of JEX) and select and object\n" +
        "# you wish to view. Double click the .jxd file shown within the array view to open this txt\n" +
        "# file to see the contents that you would see as a table in R. To do this you must set\n" +
        "# your default viewer for .jxd files as a text editor.\n\n" +

        "# You can also save data back to JEX by providing a list of file names using\n" +
        "# the reserved variable names fileList1, fileList2, imageList1, and imageList2.\n" +
        "# The file lists are for any type of file while the image lists are interpreted as \n" +
        "# image files. For example, 'imageList1 <- c(filepath1, filepath2)' will result\n" +
        "# in an image object in JEX that has 1 dimension named 'i' for index with\n" +
        "# filepath1 at i=0 and filepath2 at i=1. These images can then be viewed in\n" +
        "# JEX's built in image viewer.\n\n" +

        "# It is also good to make sure that all attempts to plot are 'shut off' before trying \n" +
        "# to do any plotting. Plotting commands can get stranded if the script errors\n" +
        "# before calling 'dev.off()' to close the plot file (e.g. pdf, jpeg, tif) before ending.\n" +
        "# This is accomplished with a single call to graphics.off()\n\n" +

        "# library(foreign) is required for passing in the JEX data objects and is part of the\n" +
        "# normal R distribution (at least with RStudio).\n\n" +

        "# Happy scripting...\n\n" +

		"# Here is an example of reading in a data object...\n" +
		"# (currently commented out so you can run this without passing an object)\n" +
		"# print(data1$Type)\n" +
		"# print(data1$Name)\n" +
		"# The following is typically a data.frame with the contents of the JEXData object\n\n" +
		"# print(data1$Value)\n\n" +
		
		"graphics.off()\n\n" +

        "x <- (0:1000)/100\n" +
        "y <- cos(x)\n" +
        "temp1 <- paste(jexTempRFolder, '/RPlot.pdf', sep='')\n" +
        "pdf(file=temp1)\n" +
        "plot(x,y)\n" +
        "dev.off()\n\n" +

        "x <- (0:1000)/100\n" +
        "y <- sin(x)\n" +
        "temp2 <- paste(jexTempRFolder, '/RPlot2.pdf', sep='')\n" +
        "pdf(file=temp2)\n" +
        "plot(x,y)\n" +
        "dev.off()\n\n" +

        "fileList1 <- c(temp1, temp2)\n";

	@ParameterMarker(uiOrder=1, name="Script", description="Script of commands to run in R environment using data1... data5, jexTempRFolder, and jexDBFolder as potential inputs and fileList1, fileList2, imageList1, and imageList2 as specially interpreted objects that can be translated back into JEXData outputs.", ui=MarkerConstants.UI_SCRIPT, defaultText=scriptDefaultValue)
	String script;
	
	@ParameterMarker(uiOrder=2, name="Output to console?", description="Output the script (or the output of each line if line-by-line checked) to the console. Sometimes helpful for debuggin but slows computation to transfer console output.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean console;
	
	@ParameterMarker(uiOrder=3, name="Eval line-by-line?", description="Evaluate script line by line (not good for loops or function defs, i.e., multiline statements, but can help debug).", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean lineByLine;
	
	@ParameterMarker(uiOrder = 4, name = "Input Variables", description="Define extra variables that are available to the script (e.g. fileName=\"C:\\temp\")", ui=MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String variables;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="fileList1", type=MarkerConstants.TYPE_FILE, flavor="", description="File object output populated by collecting the variable named 'fileList1' from the R workspace after the script.", enabled=true)
	JEXData fileList1;

	@OutputMarker(uiOrder=2, name="fileList2", type=MarkerConstants.TYPE_FILE, flavor="", description="File object output populated by collecting the variable named 'fileList2' from the R workspace after the script.", enabled=true)
	JEXData fileList2;

	@OutputMarker(uiOrder=3, name="imageList1", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Image object output populated by collecting the variable named 'imageList1' from the R workspace after the script.", enabled=true)
	JEXData imageList1;

	@OutputMarker(uiOrder=4, name="imageList2", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Image object output populated by collecting the variable named 'imageList2' from the R workspace after the script.", enabled=true)
	JEXData imageList2;

	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		JEXEntry firstEntry = JEXStatics.jexManager.getSelectedEntries().first();
		if(optionalEntry.getTrayX()!=firstEntry.getTrayX()||optionalEntry.getTrayY()!=firstEntry.getTrayY()) return true;
		R.initializeWorkspace();
		R.eval(header);
		parseVariables();
		R.initializeData2(data1, "data1");
		R.initializeData2(data2, "data2");
		R.initializeData2(data3, "data3");
		R.initializeData2(data4, "data4");
		R.initializeData2(data5, "data5");
		//R.eval(RScripter.getRScript_FileTable(JEXStatics.jexManager.getSelectedEntries(), data1.getTypeName()));

		if(console)
		{
			if(lineByLine)
			{
				R.evalToConsoleLineByLine(script);
			}
			else
			{
				R.evalToConsole(script);
			}
		}
		else
		{
			if(lineByLine)
			{
				R.evalLineByLine(script);
			}
			else
			{
				R.eval(script);
			}
		}

		fileList1 = getOutput("fileList1");
		fileList2 = getOutput("fileList2");
		imageList1 = getOutput("imageList1");
		imageList2 = getOutput("imageList2");

		// Return status
		return true;
	}

	public static JEXData getOutput(String name)
	{
		TreeMap<DimensionMap,String> files = new TreeMap<DimensionMap,String>();

		boolean image = false;
		if(name.substring(0, 1).equals("i"))
		{
			image = true;
		}

		REXP workspaceVariables = R.eval("ls()");
		boolean found = false;
		try
		{
			String[] vars = workspaceVariables.asStrings();
			for(String var : vars)
			{
				if(var.equals(name))
				{
					found = true;
					break;
				}
			}
			if(!found)
			{
				return null;
			}
		}
		catch (REXPMismatchException e1)
		{
			Logs.log("Couldn't get workspace variables as list of names.", CallRScript.class);
			e1.printStackTrace();
			return null;
		}
		REXP fileObject = R.eval(name);
		String[] fileStrings = null;
		try
		{
			fileStrings = fileObject.asStrings();
			int i = 0;
			for(String s : fileStrings)
			{
				String fixedString = s;//s.replaceAll("/", File.separator); // Might have to figure out Pattern.quote(File.separator) stuff for windows.
				files.put(new DimensionMap("i=" + i), fixedString);
				i++;
			}
			JEXData ret = null;
			if(image)
			{
				ret = ImageWriter.makeImageStackFromPaths("dummy", files);
			}
			else
			{
				ret = FileWriter.makeFileObject("dummy", null, files);
			}

			return ret;
		}
		catch (REXPMismatchException e)
		{
			Logs.log("Couldn't convert " + name + " to String[]", CallRScript.class);
			e.printStackTrace();
		}
		return null;
	}
	
	public void parseVariables() {
		String[] assignments = variables.split(",");
		for(String s : assignments) R.eval(s);
	}
}
