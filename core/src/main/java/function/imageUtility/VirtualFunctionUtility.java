package function.imageUtility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import cruncher.VirtualFunctionCruncher;
import ij.process.ImageProcessor;
import logs.Logs;

public class VirtualFunctionUtility{

	public VirtualFunctionCruncher function;
	public static final String INPUT_STRING = "Input";
	public static final String PARAMETER_STRING = "Parameter";
	public static final String UTILITY_STRING = "Utility";
	
	public VirtualFunctionUtility(String vfcPath) throws InstantiationException, IllegalAccessException, IOException
	{
		//Logs.log("Reading from: "+vfcPath, this);
		BufferedReader reader = new BufferedReader(new FileReader(vfcPath));
		String functionName = reader.readLine().split(",")[0];

//		//Put new functions here
//		if(functionName.equals("WeightedMeanFilter")) function = new WeightedMeanFilterUtility();
//		else if(functionName.equals("ImageFilter")) function = new ImageFilterUtility();
//		else if(functionName.equals("AdjustImageIntensities")) function = new AdjustImageIntensitiesUtility();
		
		// Create new class based on name
		try {
			Class<?> className = Class.forName(this.getClass().getPackage().getName()+"."+functionName+UTILITY_STRING);
			Constructor<?> constructor = className.getConstructor();
			Object functionObject = constructor.newInstance();
			this.function = (VirtualFunctionCruncher)functionObject;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String line = "";
		List<List<String>> table = new ArrayList<List<String>>();
		while((line = reader.readLine())!=null) {
			table.add(Arrays.asList(line.split(",")));
		}

		reader.close();

		TreeMap<String, String> inputs = new TreeMap<String,String>();
		TreeMap<String, String> parameters = new TreeMap<String,String>();
		for(List<String> l : table) {
			if(l.get(0).equals(INPUT_STRING)) {
				inputs.put(l.get(1), l.get(2));
			}
			else if (l.get(0).equals(PARAMETER_STRING)) {
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
		Logs.log("Running "+function.getName()+" has returned "+b, this);
		return function.getOutput();

	}



}
