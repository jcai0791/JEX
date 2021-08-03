package function.plugin.plugins.FileIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.LabelReader;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import Database.SingleUserDatabase.tnvi;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import rtools.R;
import tables.DimensionMap;


@Plugin(
		type = JEXPlugin.class,
		name = "Export CSV",
		menuPath="File IO",
		visible=true,
		description = "Exports experiment data to CSV"
		)

public class ExportToOneCSV extends JEXPlugin {

	
	public ExportToOneCSV() {}
	
	/////////// Define Inputs ///////////
		
	@InputMarker(uiOrder=1, name="Data", type=MarkerConstants.TYPE_FILE, description="Data to be exported", optional=false)
	JEXData data;
	
	/////////// Define Parameters ///////////
	@ParameterMarker(uiOrder = 1, name = "Output Destination", description = "Warning: This function does not overwrite.", ui = MarkerConstants.UI_FILECHOOSER, defaultText = "~/")
	String filePath;
	
	@ParameterMarker(uiOrder = 1, name = "File Name", description = "Warning: Make sure the file is empty or does not exist.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "output")
	String userChosenName;
	
	/////////// Define Outputs ///////////
	
	
	@Override
	public int getMaxThreads()
	{
		return 1;
	}
	
	
	@Override
	public boolean run(JEXEntry entry) {
		// Collect the inputs

		// TreeMap<String,JEXData> DataTree = entry.getDataList().get(new Type("File"));

		String folderPath = this.filePath;
		String ext = "csv";
		String ext2 = ext;
		File folder = new File(folderPath);
		// Run the function
		TreeMap<DimensionMap, String> filePaths = readObjectToFilePathTable(this.data);



		int count = 0;
		int total = filePaths.size();
		JEXStatics.statusBar.setProgressPercentage(0);
		String newFilePath = folder.getAbsolutePath() + File.separator + entry.getEntryExperiment() + File.separator
				+ userChosenName + "." + ext;
		File dst = new File(newFilePath);
		File parent = dst.getParentFile();
		if (!folder.exists()) {
			folder.mkdirs();
		}
		if (!parent.exists()) {
			parent.mkdirs();
		}
		Vector<Pair<String, String>> labels = getLabelsInEntry(entry);

		String labelString = "";
		for (Pair<String, String> p : labels) {
			labelString = labelString + p.getB() + ",";
		}
		
		//Alternate way to get dimensions
		String dimensionString = "";
		String firstPath = filePaths.get(filePaths.firstKey());
		File firstFile = new File(firstPath);
		try {
			java.io.FileReader firstReader = new java.io.FileReader(firstFile);
			BufferedReader firstBufferedReader = new BufferedReader(firstReader);
			String firstLines = "";
			while((firstLines = firstBufferedReader.readLine())!=null) {
				if(firstLines.startsWith("@attribute")) {
		        	String[] firstLineArray = firstLines.split(" ");
		        	dimensionString+=(firstLineArray[1]+",");
		        }
				if (firstLines.matches("[!-?A-~](.*)")) break;
			}
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		

		// If dst is empty, write the opening things
		try 
		{
			java.io.FileReader emptyTest = new java.io.FileReader(dst);
		} 
		catch (FileNotFoundException e2) 
		{
			try 
			{
				Logs.log("Writing header for: "+dst.getPath(), this);
				
				java.io.FileWriter headWriter = new java.io.FileWriter(newFilePath, true);
				
				for (Pair<String, String> p : labels) {
					headWriter.write(p.getA() + ",");
				}
				headWriter.write("e.x,e.y,");
				//This doesn't work for some reason: It's null
				//headWriter.write(this.data.getDimensionCSV().toString());
				headWriter.write(dimensionString);
				headWriter.write("\r\n");
				headWriter.close();
			} 
			catch (IOException e1) 
			{

				e1.printStackTrace();
			}
		}
		

		

		//This works, for some reason.
		for (DimensionMap dim : filePaths.keySet()) {
			String path = filePaths.get(dim);
			Logs.log("Path is: " + path, this);
			File f = new File(path);
			String fileName = f.getName();
			if (ext2.equals("same")) {
				ext = FileUtility.getFileNameExtension(fileName);
			}

			try {
				// JEXWriter.copy(f, dst);

				// Experimental way to not have the @ stuff at the beginning of every csv
				Logs.log("Copying from " + f.getPath() + " to " + dst.getPath(), 0, this);

				java.io.FileReader fileReader = new java.io.FileReader(f);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				java.io.FileWriter writer = new java.io.FileWriter(newFilePath, true);
				// writer.write("Time,Mean,Position,Value\r\n");
				String line = "";
				while ((line = bufferedReader.readLine()) != null) {
					// Write new line to new file
					if (line.matches("[!-?A-~](.*)")) {
						writer.write(labelString + entry.getTrayX() + "," + entry.getTrayY() + "," + line + "\r\n");
					}
				}

				// Close reader and writer
				bufferedReader.close();
				writer.close();

			} catch (IOException e) {
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
	public static String getRScript_FileTable(JEXEntry e, TypeName tn)
	{
		
		//		TreeMap<DimensionMap,String> files = new TreeMap<DimensionMap,String>();
		//		DimTable totalDimTable = new DimTable();
		//		TreeSet<String> expts = new TreeSet<String>();
		//		// TreeSet<String> trays = new TreeSet<String>();
		//		TreeSet<String> xs = new TreeSet<String>();
		//		TreeSet<String> ys = new TreeSet<String>();
		
		
			
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, e);
			if(data != null)
			{
				String dataS = getJEXDataAsRString(e, tn);
				return ("jData[[" + R.sQuote(e.getEntryID()) + "]] <- " + dataS);
				
			}
			return null;
	}
	
	public static Vector<Pair<String,String>> getLabelsInEntry(JEXEntry e)
	{
		Vector<TypeName> tns = new Vector<>();
		Vector<Pair<String,String>> ret = new Vector<>();
		TreeSet<JEXEntry> set = new TreeSet<>();
		set.add(e);
		tnvi a = JEXStatics.jexManager.getTNVIforEntryList(set);
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> labels = a.get(JEXData.LABEL);
		for(String name : labels.keySet())
		{
			tns.add(new TypeName(JEXData.LABEL, name));
		}
		for(TypeName tn : tns)
		{
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, e);
			ret.add(new Pair<String,String>(LabelReader.readLabelName(data), LabelReader.readLabelValue(data)));
		}
		return ret;
	}
	
	private static String getListRCommand(Vector<Pair<String,String>> listItems)
	{
		String ret = "list(";
		boolean first = true;
		for(Pair<String,String> item : listItems)
		{
			if(first)
			{
				ret = ret + item.p1 + "=" + R.sQuote(item.p2);
			}
			else
			{
				ret = ret + "," + item.p1 + "=" + R.sQuote(item.p2);
			}
			first = false;
		}
		ret = ret + ")";
		return ret;
	}
	
	public static String getJEXDataAsRString(JEXEntry e, TypeName tn)
	{
		Vector<Pair<String,String>> labels = getLabelsInEntry(e);
		String listC = getListRCommand(labels);
		String ret = "readJEXData(dbPath=" + R.sQuote(JEXWriter.getDatabaseFolder()) + ", ds=" + R.sQuote(e.getEntryExperiment()) + ", e.x=" + e.getTrayX() + ", e.y=" + e.getTrayY() + ", type=" + R.sQuote(tn.getType().getType()) + ", name=" + R.sQuote(tn.getName()) + ", labels=" + listC + ")";
		ret = ret.replace("\\", "/"); // R likes forward slashes.
		return ret;
	}
	

}
