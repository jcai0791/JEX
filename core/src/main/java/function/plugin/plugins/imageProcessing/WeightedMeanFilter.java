package function.plugin.plugins.imageProcessing;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;
import org.scijava.util.ClassUtils;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.SingleUserDatabase.JEXWriter;
import function.imageUtility.VirtualFunctionUtility;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.ImageUtility;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Weighted Mean Filtering",
		menuPath="Image Processing",
		visible=true,
		description="Calculate a weighted mean using the variance of the image as the inverse of the weight."
		)
public class WeightedMeanFilter extends JEXPlugin {

	public WeightedMeanFilter()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	@InputMarker(uiOrder=2, name="Mask (optional)", type=MarkerConstants.TYPE_IMAGE, description="Mask which defines objects (white) amidst background (black)", optional=true)
	JEXData maskData;
	
	@InputMarker(uiOrder=3, name="IF (optional)", type=MarkerConstants.TYPE_IMAGE, description="Illumination correction image (typically has a mean of 1)", optional=true)
	JEXData IFData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Gaussian Filter Radius", description="Radius of the weighted gaussian mean filter.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50")
	double meanRadius;
	
	@ParameterMarker(uiOrder=1, name="Gaussian Filter Kernel Radius", description="Radius of the weighted gaussian mean filter kernel. This should be big enough to encompass the objects of interest..", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50")
	double meanOuterRadius;
	
//	@ParameterMarker(uiOrder=2, name="Kernal Outer Weighting Factor", description="How much weight should the outer portion of the kernel be given relative to the inner portion (Kernel = Gaussian * (1-Gaussian)^factor). Typically 0 (standard Gaussian) to 5 (weighted to outer portion), but can go higher with diminishing impact.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
//	double outerWeighting;

	@ParameterMarker(uiOrder=3, name="Std. Dev. Filter Radius", description="Radius of the std. dev. filter used to generate the pixel weights. Keep small to preserve edges.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2.0")
	double varRadius;

	@ParameterMarker(uiOrder=4, name="Subtraction Weight Scaling Factor", description="Typically 0.5-3 but can increase or decrease to make the weighting more drastic or less drastic, respectively. A little higher seems to work better for subtraction relative to thresholding.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2.0")
	double subScale;

	@ParameterMarker(uiOrder=5, name="Threshold Weight Scaling Factor", description="Typically 0.5-3 but can increase or decrease to make the weighting more drastic or less drastic, respectively. A little lower seems to work better for thresholding relative to subtraction.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.75")
	double threshScale;

	@ParameterMarker(uiOrder=6, name="Operation", description="What should be done?", ui=MarkerConstants.UI_DROPDOWN, choices={"Get Background", "Subtract Background", "Threshold Image", "Subtract and Threshold", "Divide by Background", "Divide and Threshold"}, defaultChoice = 3)
	String operation;

	@ParameterMarker(uiOrder=7, name="Nominal Value to Add/Mult Back", description="Nominal value to add (or multiply by if doing division with background) to all pixels after background subtraction/division. (Use following notation to specify different parameters for differen dimension values, '<Dim Name>'=<val1>,<val2>,<val3>' e.g., 'Channel=0,100,100'. The values will be applied in that order for the ordered dim values.) ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	String nominalsString;

	@ParameterMarker(uiOrder=8, name="Threshold Sigmas", description="Sigma (i.e., Std. Dev.) for thresholding after background subtraction etc. 0 outputs signal-to-noise ratio instead of thresholded image (Use following notation to specify different parameters for differen dimension values, '<Dim Name>'=<val1>,<val2>,<val3>' e.g., 'Channel=0,100,100'. The values will be applied in that order for the ordered dim values.) ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	String sigmasString;
	
	@ParameterMarker(uiOrder=9, name="Mask Output Bit Depth", description="What bit depth should the mask be saved as.", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=0)
	int maskBitDepth;

	double nominalVal;

	@ParameterMarker(uiOrder=10, name="Image Output Bit Depth", description="What bit depth should the main image output be saved as.", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=1)
	int outputBitDepth;

	@ParameterMarker(uiOrder=11, name="Exclusion Filter DimTable", description="Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String exclusionFilterString;
	
	@ParameterMarker(uiOrder=12, name="Tiles: Rows", description="If desired, the images can be split into tiles before processing by setting the number of tile rows here to > 1.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int rows;
	
	@ParameterMarker(uiOrder=13, name="Tiles: Cols", description="If desired, the images can be split into tiles before processing by setting the number of tile cols here to > 1.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int cols;
	
	@ParameterMarker(uiOrder=14, name="Tiles: Overlap", description="Set the percent overlap of the tiles", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double overlap;

	@ParameterMarker(uiOrder=15, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	@ParameterMarker(uiOrder=16, name="Dark Field Intensity", description="What is the intensity of an image without any illumination (i.e., the dark field). This is subtracted prior to division for division options.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100.0")
	double darkfield;
	
	@ParameterMarker(uiOrder=17, name="IF is One Tile?", description="Is the (optional) IF image provided a single tile (vs one large stitched IF image)?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean oneTile;
	
	@ParameterMarker(uiOrder=99, name="Virtual Output", description="Check to make the output virtual (does not save output image)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean virtualOutput;

	//	@ParameterMarker(uiOrder=12, name="Output Std. Dev. Images?", description="Should the locally weighted standard deviation images be output?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	//	boolean outputStdDev;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Image (BG)", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;

	@OutputMarker(uiOrder=2, name="SNR Mask", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant thresholded mask image", enabled=true)
	JEXData maskOutput;

	//	@OutputMarker(uiOrder=4, name="Local Std. Dev.", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant weighting image used for bg subtraction and thresholding", enabled=true)
	//	JEXData stdevOutput;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		DimTable filterTable = new DimTable(this.exclusionFilterString);

		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}

		// Get the nominal add back values (typically different for fluorescence and brightfield
		TreeMap<DimensionMap,Double> nominals = getMultipleValuesFromParameter(this.nominalsString);
		if(nominals == null)
		{
			return false;
		}

		// Get the nominal add back values (typically different for fluorescence and brightfield
		TreeMap<DimensionMap,Double> sigmas = getMultipleValuesFromParameter(this.sigmasString);
		if(sigmas == null)
		{
			return false;
		}

		if(this.meanOuterRadius < this.meanRadius)
		{
			this.meanOuterRadius = 5*this.meanRadius;
		}
		boolean doBackgroundOnly = false;
		boolean doSubtraction = false;
		boolean doThreshold = false;
		boolean doDivision = false;
		if(operation.equals("Get Background"))
		{
			doBackgroundOnly = true;
		}
		if(operation.equals("Subtract Background") || operation.equals("Subtract and Threshold"))
		{
			doSubtraction = true;
		}
		if(operation.equals("Threshold Image") || operation.equals("Subtract and Threshold") || operation.equals("Divide and Threshold"))
		{
			doThreshold = true;
		}
		if(operation.equals("Divide by Background") || operation.equals("Divide and Threshold"))
		{
			doDivision = true;
		}
		
		
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> maskMap = null;
		if(maskData != null)
		{
			maskMap = ImageReader.readObjectToImagePathTable(maskData);
		}
		TreeMap<DimensionMap,String> IFMap = null;
		if(IFData != null)
		{
			IFMap = ImageReader.readObjectToImagePathTable(IFData);
		}
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> outputMaskMap = new TreeMap<DimensionMap,String>();
		//TreeMap<DimensionMap,String> outputStDevMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				Logs.log("Function canceled.", this);
				return false;
			}
			
			//This is for handling virtual outputs
			if(this.virtualOutput) {
				List<Field> parameterList = ClassUtils.getAnnotatedFields(this.getClass(), ParameterMarker.class);
				TreeMap<String,Parameter> parameters = new TreeMap<String,Parameter>();
				for(Field f : parameterList) {
					f.setAccessible(true); // expose private fields					
					// Get the marker
					final ParameterMarker parameter = f.getAnnotation(ParameterMarker.class);
					
					// add items to the relevant lists
					String name = parameter.name();
					String description = parameter.description();
					int ui = parameter.ui();
					String defaultText = parameter.defaultText();
					Boolean defaultBoolean = parameter.defaultBoolean();
					int defaultChoice = parameter.defaultChoice();
					String[] choices = parameter.choices();
					
					Parameter p = null;
					if(ui == Parameter.CHECKBOX)
					{
						p = new Parameter(name, description, ui, defaultBoolean);
					}
					else if(ui == Parameter.DROPDOWN)
					{
						p = new Parameter(name, description, ui, choices, defaultChoice);
					}
					else
					{ // Filechooser, Password, or Textfield
						p = new Parameter(name, description, ui, defaultText);
					}
					
					parameters.put(name,p);
				}
				TreeMap<String,String> inputs = new TreeMap<String,String>();
				inputs.put("Image",imageMap.get(map));
				if(maskData!=null)inputs.put("Mask (optional)",maskMap.get(map));
				if(IFData!=null) inputs.put("IF (optional)",IFMap.get(map));
				String virtualImagePath = JEXWriter.saveVirtualImage(inputs,parameters,"WeightedMeanFilter");
				outputImageMap.put(map.copy(),virtualImagePath);
				continue;
			}
			
			
			// Skip if told to skip.
			if(filterTable.testMapAsExclusionFilter(map))
			{
				if(this.keepExcluded && this.rows == 1 && this.cols == 1)
				{
					Logs.log("Skipping the processing of " + map.toString(), this);
					ImagePlus out = new ImagePlus(imageMap.get(map));
					tempPath = JEXWriter.saveImage(out);
					if(tempPath != null)
					{
						outputImageMap.put(map, tempPath);
					}
				}
				else
				{
					Logs.log("Skipping the processing and saving of " + map.toString(), this);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size() * this.rows * this.cols)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}

			// Skip if no image available.
			String tempImPath = imageMap.get(map);
			if(tempImPath == null)
			{
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size() * this.rows * this.cols)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}
			
			FloatProcessor fp = null;
			if(imageData.hasVirtualFunctionFlavor()) {
				try {
					VirtualFunctionUtility vfu = new VirtualFunctionUtility(imageMap.get(map));
					fp = vfu.call().convertToFloatProcessor();
				} catch (InstantiationException | IllegalAccessException | IOException e1) {
					e1.printStackTrace();
				}
			}
			else fp = (new ImagePlus(imageMap.get(map)).getProcessor().convertToFloatProcessor());
			FloatProcessor mp = null;
			if(maskData != null && maskMap != null)
			{
				mp = (new ImagePlus(maskMap.get(map)).getProcessor().convertToFloatProcessor());
			}
			FloatProcessor IFp = null;
			if(IFData != null && IFMap != null)
			{
				IFp = (new ImagePlus(IFMap.get(map)).getProcessor().convertToFloatProcessor());
			}
			TreeMap<DimensionMap,ImageProcessor> temp = new TreeMap<>();
			TreeMap<DimensionMap,ImageProcessor> maskTemp = new TreeMap<>();
			TreeMap<DimensionMap,ImageProcessor> tiles = new TreeMap<>();
			TreeMap<DimensionMap,ImageProcessor> maskTiles = new TreeMap<>();
			TreeMap<DimensionMap,ImageProcessor> IFTemp = new TreeMap<>();
			TreeMap<DimensionMap,ImageProcessor> IFTiles = new TreeMap<>();
			if(this.rows > 1 || this.cols > 1)
			{
				Logs.log("Splitting image into tiles", this);
				temp.put(map.copy(), fp);
				tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols, this.getCanceler()));
				if(mp != null)
				{
					maskTemp.put(map.copy(), mp);
					maskTiles.putAll(ImageWriter.separateTilesToProcessors(maskTemp, overlap, rows, cols, this.getCanceler()));
				}
				if(IFp != null & !this.oneTile)
				{
					IFTemp.put(map.copy(), IFp);
					maskTiles.putAll(ImageWriter.separateTilesToProcessors(IFTemp, overlap, rows, cols, this.getCanceler()));
				}
				else
				{
					if(IFp != null)
					{
						IFTiles.put(map.copy(), IFp);
					}
				}
			}
			else
			{
				tiles.put(map.copy(), fp);
				if(mp != null)
				{
					maskTiles.put(map.copy(), mp);
				}
				if(IFp != null)
				{
					IFTiles.put(map.copy(), IFp);
				}
			}

			// Get images and parameters
			Double sigma = sigmas.get(map);
			Double nominal = nominals.get(map);
			
			for(Entry<DimensionMap, ImageProcessor> e : tiles.entrySet())
			{
				if(maskTiles.size() != 0)
				{
					FloatProcessor fpToSave = ImageUtility.getWeightedMeanFilterImage((FloatProcessor) e.getValue(), (FloatProcessor) maskTiles.get(e.getKey()), doSubtraction, doBackgroundOnly, doDivision, 0.4*this.meanRadius, this.varRadius, this.subScale, this.threshScale, 0d, sigma, this.darkfield, this.meanOuterRadius);
					if(IFTiles.get(e.getKey()) != null)
					{
						FloatBlitter blit = new FloatBlitter(fpToSave);
						blit.copyBits(IFTiles.get(e.getKey()), 0, 0, Blitter.DIVIDE);
					}
					fpToSave.add(nominal);
					String filteredImagePath = JEXWriter.saveImage(fpToSave, this.outputBitDepth);
					outputImageMap.put(e.getKey().copy(), filteredImagePath);
				}
				else
				{
					long start = System.currentTimeMillis();
					Pair<FloatProcessor, ImageProcessor> images = ImageUtility.getWeightedMeanFilterImage((FloatProcessor) e.getValue(), doThreshold, doSubtraction, doBackgroundOnly, doDivision, 0.4*this.meanRadius, this.varRadius, this.subScale, this.threshScale, 0d, sigma, this.darkfield, this.meanOuterRadius);
					long mid2 = System.currentTimeMillis();
					if(images.p1 != null)
					{
						if(IFTiles.get(e.getKey()) != null)
						{
							FloatBlitter blit = new FloatBlitter(images.p1);
							blit.copyBits(IFTiles.get(e.getKey()), 0, 0, Blitter.DIVIDE);
						}
						images.p1.add(nominal);
						long mid = System.currentTimeMillis();
						String filteredImagePath = JEXWriter.saveImage(images.p1, this.outputBitDepth);
						long end = System.currentTimeMillis();
						Logs.log("Saving took "+(end-mid)+" milliseconds", this);
						Logs.log("Processing took "+(mid2-start)+" milliseconds", this);
						outputImageMap.put(e.getKey().copy(), filteredImagePath);
					}
					if(images.p2 != null)
					{
						String maskPath = JEXWriter.saveImage(images.p2, this.maskBitDepth);
						outputMaskMap.put(e.getKey().copy(), maskPath);
					}
				}

				// Update the progress bar.
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size() * this.rows * this.cols)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			
		}

		// Save the background/subtraction image
		if(outputImageMap.size() > 0)
		{
			this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);
			if(this.virtualOutput) this.output.setDataObjectFlavor("Virtual Function");
		}

		// Save a threshold image if necessary
		if(outputMaskMap.size() > 0)
		{
			this.maskOutput = ImageWriter.makeImageStackFromPaths("temp", outputMaskMap);
			if(this.virtualOutput) this.maskOutput.setDataObjectFlavor("Virtual Function");
		}

		// Return status
		return true;
	}

	public TreeMap<DimensionMap,Double> getMultipleValuesFromParameter(String s)
	{
		TreeMap<DimensionMap, Double> ret = new TreeMap<>();
		Dim nominalDim = null;
		Double nominalD = 0.0;
		try
		{
			nominalD = Double.parseDouble(s);
			ret.put(new DimensionMap(), nominalD);
			return ret;
		}
		catch(Exception e)
		{
			String[] temp = s.split("=");
			if(temp.length != 2)
			{
				JEXDialog.messageDialog("Couldn't parse the parameter '" + s + "' into a number or into format '<Dim Name>=<val1>,<val2>...'. Aborting.", this);
				return null;
			}
			nominalDim = imageData.getDimTable().getDimWithName(temp[0]);
			CSVList temp2 = new CSVList(temp[1]);
			if(nominalDim.size() != 1 && nominalDim.size() != temp2.size())
			{
				JEXDialog.messageDialog("The number of parameters values did not match the number of values in the dimension '" + nominalDim.dimName + "'. Aborting.", this);
				return null;
			}
			for(int i=0; i < temp2.size(); i++)
			{
				try
				{
					Double toGet = Double.parseDouble(temp2.get(i));
					ret.put(new DimensionMap(nominalDim.dimName + "=" + nominalDim.dimValues.get(i)), toGet);
				}
				catch(Exception e2)
				{
					JEXDialog.messageDialog("Couldn't parse the value '" + temp2.get(i) + "' to type double. Aborting.", this);
					return null;
				}

			}
			return ret;
		}
	}
}
