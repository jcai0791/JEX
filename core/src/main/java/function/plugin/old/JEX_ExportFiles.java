package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import java.io.BufferedReader;
import java.io.FileWriter;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
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
public class JEX_ExportFiles extends JEXCrunchable {
	
	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName()
	{
		String result = "Export Table Files";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo()
	{
		String result = "Export table files to a folder";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox()
	{
		String toolbox = "Table Tools";
		return toolbox;
	}
	
	/**
	 * This method defines if the function appears in the list in JEX It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList()
	{
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return false;
	}
	
	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------
	
	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(FILE, "Files to Export");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[0];
		
		if(this.outputNames == null)
		{
			return this.defaultOutputNames;
		}
		return this.outputNames;
	}
	
	/**
	 * Returns a list of parameters necessary for this function to run... Every parameter is defined as a line in a form that provides the ability to set how it will be displayed to the user and what options are available to choose from The simplest
	 * FormLine can be written as: FormLine p = new FormLine(parameterName); This will provide a text field for the user to input the value of the parameter named parameterName More complex displaying options can be set by consulting the FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters()
	{
		Parameter p1 = new Parameter("Folder Path", "Location to which the files will be copied", Parameter.FILECHOOSER, "");
		Parameter p2 = new Parameter("File Extension", "Extension to put on the file", Parameter.DROPDOWN, new String[] { "same", "csv", "arff", "txt" }, 0);
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-rided by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return true;
	}
	
	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData data = inputs.get("Files to Export");
		
		if(!(data.getTypeName().getType().matches(JEXData.FILE) || data.getTypeName().getType().matches(JEXData.ROI) || data.getTypeName().getType().matches(JEXData.IMAGE) || data.getTypeName().getType().matches(JEXData.MOVIE) || data.getTypeName().getType().matches(JEXData.WORKFLOW) || data.getTypeName().getType().matches(JEXData.SOUND)))
		{
			return false;
		}
		
		// //// Get params
		// int depth =
		// Integer.parseInt(parameters.getValueOfParameter("Output Bit Depth"));
		String folderPath = this.parameters.getValueOfParameter("Folder Path");
		String ext = this.parameters.getValueOfParameter("File Extension");
		String ext2 = ext;
		
		File folder = new File(folderPath);
		// Run the function
		TreeMap<DimensionMap,String> filePaths = readObjectToFilePathTable(data);
		
		if(!folder.exists())
		{
			folder.mkdirs();
		}
		
		int count = 0;
		int total = filePaths.size();
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap dim : filePaths.keySet())
		{
			String path = filePaths.get(dim);
			Logs.log("Path is: "+path, this);
			File f = new File(path);
			String fileName = f.getName();
			if(ext2.equals("same"))
			{
				ext = FileUtility.getFileNameExtension(fileName);
			}
			String newFilePath = folder.getAbsolutePath() + File.separator + entry.getEntryExperiment() + File.separator + data.getDataObjectType() + " - " + data.getDataObjectName() + File.separator + FileUtility.getFileNameWithoutExtension(fileName) + "." + ext;
			File dst = new File(newFilePath);
			File parent = dst.getParentFile();
			if(!parent.exists())
			{
				parent.mkdirs();
			}
			
			try
			{
				//JEXWriter.copy(f, dst);
				
				
				//Experimental way to not have the @ stuff at the beginning of every csv
				Logs.log("Copying from " + f.getPath() + " to " + dst.getPath(), 0, this);
				FileWriter writer = new FileWriter(newFilePath);
				java.io.FileReader fileReader = new java.io.FileReader(f);
			    BufferedReader bufferedReader = new BufferedReader(fileReader);
			    
			    //writer.write("Time,Mean,Position,Value\r\n");
			    String line = "";
			    boolean enterData = false;
			    while ((line = bufferedReader.readLine()) != null) {
			        if(line.startsWith("@attribute")) {
			        	String[] lineArray = line.split(" ");
			        	writer.write(lineArray[1]+",");
			        }
			        // Write new line to new file
			        if(line.matches("[!-?A-~](.*)")) {
			        	if(!enterData) {
			        		writer.write("\r\n");
			        		enterData = true;
			        	}
			        	writer.write(line + "\r\n");
			        }
			    }

			    // Close reader and writer
			    bufferedReader.close();
			    writer.close();
			    

			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			
			// Status bar
			count++;
			int percentage = (int) (100 * ((double) count / (double) filePaths.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Return status
		return true;
	}
	
	/**
	 * Read all the images in the value object into a hashable table of image paths
	 * 
	 * @param data
	 * @return
	 */
	private static TreeMap<DimensionMap,String> readObjectToFilePathTable(JEXData data)
	{
		TreeMap<DimensionMap,String> result = new TreeMap<DimensionMap,String>();
		if(data.getTypeName().getType().matches(JEXData.ROI))
		{
			String dataFolder = data.getDetachedRelativePath();
			dataFolder = JEXWriter.getDatabaseFolder() + File.separator + dataFolder;
			result.put(new DimensionMap("File=1"), dataFolder);
		}
		else
		{
			JEXDataSingle ds = data.getFirstSingle();
			String dataFolder = (new File(FileReader.readToPath(ds))).getParent(); 			
			for (DimensionMap map : data.getDataMap().keySet())
			{
				ds = data.getData(map);
				String path = readToPath(dataFolder, ds);
				result.put(map, path);
			}
		}
		
		return result;
	}
	
	private static String readToPath(String dataFolder, JEXDataSingle ds)
	{
		String fileName = FileUtility.getFileNameWithExtension(ds.get(JEXDataSingle.RELATIVEPATH));
		String result = dataFolder + File.separator + fileName;
		return result;
	}
}
