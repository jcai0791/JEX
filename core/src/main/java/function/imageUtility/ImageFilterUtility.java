package function.imageUtility;

import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.VirtualFunctionCruncher;
import function.plugin.plugins.imageProcessing.GaussianBlur2;
import function.plugin.plugins.imageProcessing.GaussianBlurForcedRadius;
import function.plugin.plugins.imageProcessing.RankFilters2;
import function.plugin.plugins.imageProcessing.TIECalculator;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import jex.utilities.ImageUtility;
import miscellaneous.CSVList;
import miscellaneous.Pair;
import miscellaneous.StringUtility;

public class ImageFilterUtility extends VirtualFunctionCruncher{

	public static String MEAN = "mean", MIN = "min", MAX = "max", MEDIAN = "median", VARIANCE = "variance", STDEV = "std. dev.", OUTLIERS="outliers", DESPECKLE="despeckle", REMOVE_NAN="remove NaN", OPEN="open", CLOSE="close", OPEN_TOPHAT="open top-hat", CLOSE_TOPHAT="close top-hat", SUM="sum", SNR="Signal:Noise Ratio", NSR="Noise:Signal Ratio", GAUSSIAN="Gaussian", DOG="DoG",  GAUSSIAN_FORCED_RADIUS="Gaussian (forced radius)", VARIANCE_WEIGHTS="Variance Weights", DECAY="Power Decay 1/(1+(r/radius)^power)";
	public static String OP_NONE = "None", OP_LOG = "Natural Log", OP_EXP="Exp", OP_SQRT="Square Root", OP_SQR="Square", OP_INVERT="Invert";

	public ImageFilterUtility() {

	}

	
	@Override
	public boolean run() {
		CSVList radii = new CSVList(StringUtility.removeAllWhitespace(parameters.getValueOfParameter("Radius")));
		double radius = Double.parseDouble(radii.get(0));
		String method = parameters.getValueOfParameter("Filter Type");
		int bitDepth = Integer.parseInt(parameters.getValueOfParameter("Output Bit-Depth"));
		String mathOp = parameters.getValueOfParameter("Post-math Operation");
		double mult = Double.parseDouble(parameters.getValueOfParameter("Post-multiplier"));


		ImageProcessor ip = inputs.get("Image");
		ImageProcessor orig = null;

		if(method.equals(OPEN_TOPHAT) || method.equals(CLOSE_TOPHAT) || method.equals(SNR) || method.equals(NSR) || method.equals(DOG))
		{
			orig = ip.duplicate();
		}

		// //// Begin Actual Function
		RankFilters2 rF = new RankFilters2();
		GaussianBlur2 gb = new GaussianBlur2();
		GaussianBlurForcedRadius gbfr = new GaussianBlurForcedRadius();
		if(!(method.equals(GAUSSIAN) || method.equals(DOG) || method.equals(VARIANCE_WEIGHTS) || method.equals(GAUSSIAN_FORCED_RADIUS) || method.equals(DECAY)))
		{
			rF.rank(ip, radius, getMethodInt(method));
		}

		if(method.equals(OPEN_TOPHAT) || method.equals(CLOSE_TOPHAT))
		{
			orig.copyBits(ip, 0, 0, Blitter.SUBTRACT);
			ip = orig;
			orig = null;
		}
		else if(method.equals(GAUSSIAN))
		{
			gb.blurGaussian(ip, radius, radius, 0.0002); // Default accuracy = 0.0002
		}
		else if(method.equals(DOG))
		{
			if(radii.size() > 1)
			{
				//					FileUtility.showImg(orig, true);
				double radius2 = Double.parseDouble(radii.get(1));
				gb.blurGaussian(orig, radius, radius, 0.0002); // Default accuracy = 0.0002
				gb.blurGaussian(ip, radius2, radius2, 0.0002); // Default accuracy = 0.0002
				orig.copyBits(ip, 0, 0, Blitter.SUBTRACT);
				//					FileUtility.showImg(orig, true);
				ip = orig;
				orig = null;
			}
			else
			{
				JEXDialog.messageDialog("The DoG filter requires two radii as a CSV list. Supply as a simple CSV list, 'radius1, radius2'", this);
				return false;
			}
		}
		else if(method.equals(NSR))
		{
			rF.rank(orig, radius, RankFilters2.STDEV);
			orig.copyBits(ip, 0, 0, Blitter.DIVIDE);
			ip = orig;
			orig = null;
		}
		else if(method.equals(SNR))
		{
			rF.rank(orig, radius, RankFilters2.STDEV);
			ip.copyBits(orig, 0, 0, Blitter.DIVIDE);
			orig = null;
		}
		else if(method.equals(VARIANCE_WEIGHTS))
		{
			if(radii.size() > 1)
			{
				Pair<FloatProcessor[], FloatProcessor> ret = ImageUtility.getImageVarianceWeights(ip, radius, false, false, Double.parseDouble(radii.get(1)));
				ip = ret.p1[0];
				//					float[] pixels = (float[]) ip.getPixels();
				//					for(int i = 0; i < pixels.length; i++)
				//					{
				//						if(pixels[i] <= 0.5)
				//						{
				//							pixels[i] = 0;
				//						}
				//						else
				//						{
				//							pixels[i] = 1;
				//						}
				//					}
				//					ip.setPixels(pixels);
				//					ip.resetMinAndMax();
				//					rF.rank(ip, 4.0, RankFilters2.MAX);
				//					rF.rank(ip, 4.0, RankFilters2.MIN);
				//					rF.rank(ip, 4.0, RankFilters2.MIN);
				//					rF.rank(ip, 4.0, RankFilters2.MAX);
				//					ip.invert();
			}
			else
			{
				JEXDialog.messageDialog("The Variance Weight filter requires a radius followed by a scaling factor provided as a CSV list (see Weighted Mean Filter). Supply as a simple CSV list, 'radius, scale'", this);
				return false;
			}

		}
		else if(method.equals(GAUSSIAN_FORCED_RADIUS))
		{
			gbfr.blurGaussian(ip, radius, radius, Double.parseDouble(radii.get(1)));
		}
		else if(method.equals(DECAY))
		{
			//				FileUtility.showImg(temp, true);
			FHT kernel=null;
			if(radii.size() > 1)
			{
				double radius2 = Double.parseDouble(radii.get(1));
				int FHTSize = TIECalculator.nearestSuperiorPowerOf2((int) Math.max(ip.getWidth(), ip.getHeight()));
				kernel = getDecayKernel((int) Math.pow(2, FHTSize), radius, radius2);
			}
			else
			{
				JEXDialog.messageDialog("The Power Decay filter requires two values as a CSV list. Supply as a simple CSV list, 'v1, v2'", this);
				return false;
			}
			convolve(ip, kernel);
			//				FileUtility.showImg(temp, true);
		}

		if(!mathOp.equals(OP_NONE))
		{
			if(mathOp.equals(OP_EXP))
			{
				ip.exp();
			}
			else if(mathOp.equals(OP_LOG))
			{
				ip.ln();
			}
			else if(mathOp.equals(OP_SQR))
			{
				ip.sqr();
			}
			else if(mathOp.equals(OP_SQRT))
			{
				ip.sqrt();
			}
			else if(mathOp.equals(OP_INVERT))
			{
				ip.resetMinAndMax();
				ip.invert();
			}
		}
		if(mult != 1.0)
		{
			ip.multiply(mult);
		}

		ip = JEXWriter.convertToBitDepthIfNecessary(ip, bitDepth);

		setOutput(ip);
		return true;

	}

	@Override
	public void initializeParameters()
	{
		Parameter p1 = new Parameter("Filter Type", "Type of filter to apply.", Parameter.DROPDOWN, new String[] { MEAN, MIN, MAX, MEDIAN, VARIANCE, STDEV, OUTLIERS, DESPECKLE, REMOVE_NAN, OPEN, CLOSE, OPEN_TOPHAT, CLOSE_TOPHAT, SUM, SNR, NSR, GAUSSIAN, GAUSSIAN_FORCED_RADIUS, DOG, VARIANCE_WEIGHTS, DECAY}, 0);
		Parameter p7 = new Parameter("Exclusion Filter Table", "<DimName>=<Val1>,<Val2>,...<Valn>;<DimName2>=<Val1>,<Val2>,...<Valn>, Specify the dimension and dimension values to exclude. Leave blank to process all.", "");
		Parameter p8 = new Parameter("Keep Unprocessed Images?", "Should the images within the object that are exlcluded from analysis by the Dimension Filter be kept in the result?", Parameter.CHECKBOX, true);
		Parameter p2 = new Parameter("Radius", "Radius/Sigma of filter in pixels. (CSV list of 2 radii of increasing size for DoG <Val1>,<Val2>, whitespace ok)", "2.0");
		Parameter p3 = new Parameter("Output Bit-Depth", "Bit-Depth of the output image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 2);
		Parameter p5 = new Parameter("Post-math Operation", "Choose a post math operation to perform if desired. Otherwise, leave as 'None'.", Parameter.DROPDOWN, new String[] { OP_NONE, OP_LOG, OP_EXP, OP_SQRT, OP_SQR, OP_INVERT}, 0);
		Parameter p6 = new Parameter("Post-multiplier", "Value to multiply by after processing and any math operation and before saving.", "1.0");
		// Make an array of the parameters and return it
		
		
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		
		this.parameters = parameterArray;
		
		
	}

	public static int getMethodInt(String method)
	{
		int methodInt = RankFilters2.MEAN;
		if(method.equals(MIN))
		{
			methodInt = RankFilters2.MIN;
		}
		else if(method.equals(MAX))
		{
			methodInt = RankFilters2.MAX;
		}
		else if(method.equals(MEDIAN))
		{
			methodInt = RankFilters2.MEDIAN;
		}
		else if(method.equals(VARIANCE))
		{
			methodInt = RankFilters2.VARIANCE;
		}
		else if(method.equals(STDEV))
		{
			methodInt = RankFilters2.STDEV;
		}
		else if(method.equals(OUTLIERS))
		{
			methodInt = RankFilters2.OUTLIERS;
		}
		else if(method.equals(DESPECKLE))
		{
			methodInt = RankFilters2.DESPECKLE;
		}
		else if(method.equals(REMOVE_NAN))
		{
			methodInt = RankFilters2.REMOVE_NAN;
		}
		else if(method.equals(OPEN) || method.equals(OPEN_TOPHAT))
		{
			methodInt = RankFilters2.OPEN;
		}
		else if(method.equals(CLOSE) || method.equals(CLOSE_TOPHAT))
		{
			methodInt = RankFilters2.CLOSE;
		}
		else if(method.equals(SUM))
		{
			methodInt = RankFilters2.SUM;
		}
		else if(method.equals(SNR) || method.equals(NSR))
		{
			methodInt = RankFilters2.MEAN;
		}
		return methodInt;
	}
	public static FHT getDecayKernel(int width, double radius, double power)
	{
		FloatProcessor fp = new FloatProcessor(width, width);
		double tot = 0;
		int origin = (int) width/2;
		for(int x=0; x < width; x++)
		{
			for(int y=0; y < width; y++)
			{
				double r = Math.sqrt((x-origin)*(x-origin) + (y-origin)*(y-origin));
				double v = 0;
				if(r <= origin)
				{
					v = 1/(1+Math.pow(r/radius, power));// - 1/(1+Math.pow(width/radius, power));
					fp.putPixelValue(x, y, v);
					tot = tot + v;
				}
			}
		}
		fp.resetMinAndMax();
		for(int x=0; x < width; x++)
		{
			for(int y=0; y < width; y++)
			{
				fp.putPixelValue(x, y, Float.intBitsToFloat(fp.getPixel(x, y))/tot);
			}
		}
		fp.resetMinAndMax();
		//		FileUtility.showImg(fp, true);
		FHT ret = new FHT(fp);
		ret.transform();
		return ret;
	}
	public static void convolve(ImageProcessor ip1, FHT kernel)
	{
		ImageProcessor temp = TIECalculator.pad2Power2(ip1);
		FHT h1 = new FHT(temp);
		h1.transform();
		FHT ret = h1.multiply(kernel);
		ret.inverseTransform();
		ret.swapQuadrants();
		ip1.copyBits(ret, -((int) (temp.getWidth()/2 - ip1.getWidth()/2)), -((int) (temp.getHeight()/2 - ip1.getHeight()/2)), Blitter.COPY);
	}


	@Override
	public String getName() {
		return "Image Filter Virtual";
	}

}
