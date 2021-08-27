package function.imageUtility;

import java.lang.reflect.Field;
import java.util.List;
import java.util.TreeMap;

import org.scijava.util.ClassUtils;

import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import cruncher.VirtualFunctionCruncher;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import ij.process.ImageProcessor;
import jex.utilities.FunctionUtility;

public class AdjustImageIntensitiesUtility extends VirtualFunctionCruncher{

	public AdjustImageIntensitiesUtility() {
		// TODO Auto-generated constructor stub
	}

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


	@Override
	public boolean run() {
		ImageProcessor image = inputs.get("Image");
		FunctionUtility.imAdjust(image,oldMin,oldMax,newMin,newMax,gamma);
		this.setOutput(image);
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
		this.oldMin = Double.parseDouble(params.get("Old Min"));
		this.oldMax = Double.parseDouble(params.get("Old Max"));
		this.newMin = Double.parseDouble(params.get("New Min"));
		this.newMax = Double.parseDouble(params.get("New Max"));
		this.gamma = Double.parseDouble(params.get("Gamma"));

	}

	@Override
	public String getName() {
		return "Adjust Image Intensities Virtual";
	}

}
