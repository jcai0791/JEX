package function.plugin.plugins.imageProcessing;

import java.lang.reflect.Field;
import java.util.List;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;
import org.scijava.util.ClassUtils;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.process.ImageProcessor;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ImageUtility;
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
		name="Adjust Image Intensities",
		menuPath="Image Processing",
		visible=true,
		description="Adjust defined intensities in the original image to be new defined intensities, scaling all other intensities accordingly."
		)
public class AdjustImageIntensities extends JEXPlugin {

	public AdjustImageIntensities()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Old Min", description="Current 'min' intensity to be mapped to new min value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double oldMin;

	@ParameterMarker(uiOrder=2, name="Old Max", description="Current 'max' intensity to be mapped to new max value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4095.0")
	double oldMax;

	@ParameterMarker(uiOrder=3, name="New Min", description="New intensity value for current 'min' to be mapped to new min value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double newMin;

	@ParameterMarker(uiOrder=4, name="New Max", description="New intensity value for current 'max' to be mapped to new min value.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="65535.0")
	double newMax;

	@ParameterMarker(uiOrder=5, name="Gamma", description="0.1-5.0, value of 1 results in no change", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double gamma;

	@ParameterMarker(uiOrder=6, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;

	@ParameterMarker(uiOrder=99, name="Virtual Output", description="Check to make the output virtual (does not save output image)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean virtualOutput;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Adjusted Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
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
				String virtualImagePath = JEXWriter.saveVirtualImage(inputs,parameters,"AdjustImageIntensities");
				outputImageMap.put(map.copy(),virtualImagePath);
				this.output.setDataObjectFlavor(JEXData.FLAVOR_VIRTUAL_FUNCTION);
				continue;
			}

			//Virtual Function support

			if(imageData.hasVirtualFunctionFlavor()) {
				ImageProcessor ip = ImageUtility.getImageProcessor(imageData, imageMap, map);
				// Adjust the image
				FunctionUtility.imAdjust(ip, oldMin, oldMax, newMin, newMax, gamma);
				
				// Save the results
				String imPath = JEXWriter.saveImage(ip, bitDepth);
				if(imPath!=null) outputImageMap.put(map,imPath);

				// return temp filePath
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;

			}

			// Call helper method
			tempPath = saveAdjustedImage(imageMap.get(map), oldMin, oldMax, newMin, newMax, gamma, bitDepth);
			if(tempPath != null)
			{
				outputImageMap.put(map, tempPath);
			}
			
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);

		// Return status
		return true;
	}

	//	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	//	{
	//		// Get image data
	//		File f = new File(imagePath);
	//		if(!f.exists())
	//		{
	//			return null;
	//		}
	//		ImagePlus im = new ImagePlus(imagePath);
	//		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
	//		
	//		// Adjust the image
	//		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//		
	//		// Save the results
	//		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
	//		String imPath = JEXWriter.saveImage(toSave);
	//		im.flush();
	//		
	//		// return the filepath
	//		return imPath;
	//	}
}
