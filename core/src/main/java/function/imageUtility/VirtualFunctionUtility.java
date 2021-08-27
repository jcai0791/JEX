package function.imageUtility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import cruncher.VirtualFunctionCruncher;
import ij.process.ImageProcessor;
import logs.Logs;

public class VirtualFunctionUtility{

	public VirtualFunctionCruncher function;

	/**
	 * Format for adding virtual function input support:
	 * 
			//Virtual Function support
			ImageProcessor ip = null;
			if(imageData.hasVirtualFunctionFlavor()) {
				try {
					VirtualFunctionUtility vfu = new VirtualFunctionUtility(imageMap.get(map));
					ip = vfu.call();
				} catch (InstantiationException | IllegalAccessException | IOException e1) {
					e1.printStackTrace();
				}
			}
			else ip = (new ImagePlus(imageMap.get(map)).getProcessor());
	 */

	public VirtualFunctionUtility(String vfcPath) throws InstantiationException, IllegalAccessException, IOException
	{
		//Logs.log("Reading from: "+vfcPath, this);
		BufferedReader reader = new BufferedReader(new FileReader(vfcPath));
		String functionName = reader.readLine().split(",")[0];

		//Put new functions here
		if(functionName.equals("WeightedMeanFilter")) function = new WeightedMeanFilterUtility();
		else if(functionName.equals("ImageFilter")) function = new ImageFilterUtility();
		else if(functionName.equals("AdjustImageIntensities")) function = new AdjustImageIntensitiesUtility();


		String line = "";
		List<List<String>> table = new ArrayList<List<String>>();
		while((line = reader.readLine())!=null) {
			table.add(Arrays.asList(line.split(",")));
		}

		reader.close();

		TreeMap<String, String> inputs = new TreeMap<String,String>();
		TreeMap<String, String> parameters = new TreeMap<String,String>();
		for(List<String> l : table) {
			if(l.get(0).equals("Input")) {
				inputs.put(l.get(1), l.get(2));
			}
			else if (l.get(0).equals("Parameter")) {
				if(l.size()>2) parameters.put(l.get(1),l.get(2));
				else parameters.put(l.get(1),null);
			}
		}
		function.initializeParameters();
		function.setParameters(parameters);
		function.setInputs(inputs);


	}
	public ImageProcessor call() {
		//long start = System.currentTimeMillis();
		Boolean b = function.run();
		//Long end = System.currentTimeMillis();
		//Logs.log("Running "+function.getName()+" has returned "+b+". Took "+(end-start)+" milliseconds.", this);
		return function.getOutput();

	}



}
