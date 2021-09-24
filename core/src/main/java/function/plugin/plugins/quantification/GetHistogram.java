package function.plugin.plugins.quantification;

import java.awt.Shape;
import java.text.DecimalFormat;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.RoiWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.singleCellAnalysis.SingleCellUtility;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.ROIPlus.PatternRoiIterator;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.ImageUtility;
import logs.Logs;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

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
		name="Create Histogram",
		menuPath="Quantification",
		visible=true,
		description="Creates a histogram of the intensities in the input image"
		)
public class GetHistogram extends JEXPlugin {

	public GetHistogram()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Number of Bins", description="Number of bins in output histogram", ui=MarkerConstants.UI_TEXTFIELD, defaultText = "256")
	String numBins;


	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Data Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant data table", enabled=true)
	JEXData fileOutput;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry)
	{
		// Check the inputs
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		//Initialize variables
		DimTable imageTable = imageData.getDimTable();
		DimTable dimTable = imageTable.copy();
		Dim dimension = new Dim("Bin", "");
		dimension.dimValues.clear();
		for(int i = 0; i<Integer.parseInt(this.numBins); i++) dimension.dimValues.add(Integer.toString(i));
		dimTable.add(dimension);
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		JEXTableWriter writer = new JEXTableWriter(fileOutput.getTypeName().getName(), "arff");
		writer.writeNumericTableHeader(dimTable);
		String fullPath = writer.getPath();
		int count = 0;
		int total = imageTable.mapCount();
		int percentage = 0;
		
		
		//Loop through images
		JEXStatics.statusBar.setStatusText("Making histograms");
		Logs.log("Making Histograms", 0, this);
		for (DimensionMap imMap : imageTable.getMapIterator()) {
			if(this.isCanceled()) return false;
			ImageProcessor ip = ImageUtility.getImageProcessor(imageData, paths, imMap);
			int[] histogram = ip.getHistogram(Integer.parseInt(this.numBins));
			for(int i = 0; i<histogram.length; i++) {
				DimensionMap mapToSave= imMap.copy();
				mapToSave.put("Bin", ""+i);
				writer.writeData(mapToSave, histogram[i]);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		writer.close();

		this.fileOutput = FileWriter.makeFileObject("dummy", null, fullPath);
		
		
		return true;
	}

}
