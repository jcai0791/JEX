package function.imageUtility;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.scijava.util.ClassUtils;

import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.VirtualFunctionCruncher;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.utilities.ImageUtility;
import logs.Logs;
import miscellaneous.Pair;
import tables.DimensionMap;

public class WeightedMeanFilterUtility extends VirtualFunctionCruncher{

	public WeightedMeanFilterUtility() {
		// TODO Auto-generated constructor stub
	}

	/////////// Define Inputs ///////////

	//	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	//	JEXData imageData;
	//
	//	@InputMarker(uiOrder=2, name="Mask (optional)", type=MarkerConstants.TYPE_IMAGE, description="Mask which defines objects (white) amidst background (black)", optional=true)
	//	JEXData maskData;
	//
	//	@InputMarker(uiOrder=3, name="IF (optional)", type=MarkerConstants.TYPE_IMAGE, description="Illumination correction image (typically has a mean of 1)", optional=true)
	//	JEXData IFData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Gaussian Filter Radius", description="Radius of the weighted gaussian mean filter.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50")
	double meanRadius;

	@ParameterMarker(uiOrder=1, name="Gaussian Filter Kernel Radius", description="Radius of the weighted gaussian mean filter kernel. This should be big enough to encompass the objects of interest..", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50")
	double meanOuterRadius;

	//@ParameterMarker(uiOrder=2, name="Kernal Outer Weighting Factor", description="How much weight should the outer portion of the kernel be given relative to the inner portion (Kernel = Gaussian * (1-Gaussian)^factor). Typically 0 (standard Gaussian) to 5 (weighted to outer portion), but can go higher with diminishing impact.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	//double outerWeighting;

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

	//	@ParameterMarker(uiOrder=12, name="Output Std. Dev. Images?", description="Should the locally weighted standard deviation images be output?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	//	boolean outputStdDev;

	/////////// Define Outputs ///////////

	//	@OutputMarker(uiOrder=1, name="Image (BG)", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	//	JEXData output;
	//
	//	@OutputMarker(uiOrder=2, name="SNR Mask", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant thresholded mask image", enabled=true)
	//	JEXData maskOutput;

	@Override
	public boolean run() {
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
		String image = inputs.get("Image");
		String mask = null;
		if(inputs.containsKey("Mask (optional)")) mask = inputs.get("Mask (optional)");
		String IF = null;
		if(inputs.containsKey("IF (optional)")) IF = inputs.get("IF (optional)");
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> outputMaskMap = new TreeMap<DimensionMap,String>();
		//TreeMap<DimensionMap,String> outputStDevMap = new TreeMap<DimensionMap,String>();


		//This is for handling virtual outputs


		FloatProcessor fp = (new ImagePlus(image).getProcessor().convertToFloatProcessor());
		FloatProcessor mp = null;
		if(mask != null)
		{
			mp = (new ImagePlus(mask).getProcessor().convertToFloatProcessor());
		}
		FloatProcessor IFp = null;
		if(IF != null)
		{
			IFp = (new ImagePlus(IF).getProcessor().convertToFloatProcessor());
		}
		
		DimensionMap map = new DimensionMap();
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
			tiles.putAll(ImageWriter.separateTiles(temp, overlap, rows, cols));
			if(mp != null)
			{
				maskTemp.put(map.copy(), mp);
				maskTiles.putAll(ImageWriter.separateTiles(maskTemp, overlap, rows, cols));
			}
			if(IFp != null & !this.oneTile)
			{
				IFTemp.put(map.copy(), IFp);
				maskTiles.putAll(ImageWriter.separateTiles(IFTemp, overlap, rows, cols));
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
		Double sigma = Double.parseDouble(this.sigmasString);
		Double nominal = Double.parseDouble(this.nominalsString);

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
				this.setOutput(fpToSave);
			}
			else
			{
				Pair<FloatProcessor, ImageProcessor> images = ImageUtility.getWeightedMeanFilterImage((FloatProcessor) e.getValue(), doThreshold, doSubtraction, doBackgroundOnly, doDivision, 0.4*this.meanRadius, this.varRadius, this.subScale, this.threshScale, 0d, sigma, this.darkfield, this.meanOuterRadius);
				if(images.p1 != null)
				{
					if(IFTiles.get(e.getKey()) != null)
					{
						FloatBlitter blit = new FloatBlitter(images.p1);
						blit.copyBits(IFTiles.get(e.getKey()), 0, 0, Blitter.DIVIDE);
					}
					images.p1.add(nominal);
					this.setOutput(images.p1);
				}
				if(images.p2 != null)
				{
//					String maskPath = JEXWriter.saveImage(images.p2, this.maskBitDepth);
//					outputMaskMap.put(e.getKey().copy(), maskPath);
				}
			}

		}


		// Return status
		return true;
	}

	


	@Override
	public void initializeParameters() {
		this.parameters = new ParameterSet();
		List<Field> parameterFields =	ClassUtils.getAnnotatedFields(this.getClass(), ParameterMarker.class);
		for (Field f : parameterFields)
		{
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

			this.parameters.addParameter(p);
		}
		


	}
	
	@Override
	public void setParameters(TreeMap<String,String> params) {
		this.meanRadius = Double.parseDouble(params.get("Gaussian Filter Radius"));
		this.meanOuterRadius = Double.parseDouble(params.get("Gaussian Filter Kernel Radius"));
		this.varRadius = Double.parseDouble(params.get("Std. Dev. Filter Radius"));
		this.subScale = Double.parseDouble(params.get("Subtraction Weight Scaling Factor"));
		this.threshScale = Double.parseDouble(params.get("Threshold Weight Scaling Factor"));
		this.operation = params.get("Operation");
		this.nominalsString = params.get("Nominal Value to Add/Mult Back");
		this.sigmasString = params.get("Threshold Sigmas");
		this.maskBitDepth = Integer.parseInt(params.get("Mask Output Bit Depth"));
		this.outputBitDepth = Integer.parseInt(params.get("Image Output Bit Depth"));
		this.exclusionFilterString = params.get("Exclusion Filter DimTable");
		this.rows = Integer.parseInt(params.get("Tiles: Rows"));
		this.cols = Integer.parseInt(params.get("Tiles: Cols"));
		this.overlap = Double.parseDouble(params.get("Tiles: Overlap"));
		this.keepExcluded = Boolean.parseBoolean(params.get("Keep Excluded Images?"));
		this.darkfield = Double.parseDouble(params.get("Dark Field Intensity"));
		this.oneTile = Boolean.parseBoolean(params.get("IF is One Tile?"));
	}

	@Override
	public String getName() {
		return "Weighted Mean Filter Virtual";
	}

}
