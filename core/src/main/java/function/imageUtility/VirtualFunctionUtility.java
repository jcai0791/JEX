package function.imageUtility;

import Database.DBObjects.JEXData;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.VirtualFunctionCruncher;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import logs.Logs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.scijava.util.Types;

public class VirtualFunctionUtility{

	public VirtualFunctionCruncher function;

	public VirtualFunctionUtility(String vfcPath) throws InstantiationException, IllegalAccessException, IOException
	{
		Logs.log("Reading from: "+vfcPath, this);
		BufferedReader reader = new BufferedReader(new FileReader(JEXWriter.getDatabaseFolder() + File.separator + vfcPath));
		String functionName = reader.readLine().split(",")[0];
		
		//Put new functions here
		if(functionName.equals("WeightedMeanFilter")) function = new WeightedMeanFilterUtility();
		else if(functionName.equals("ImageFilter")) function = new ImageFilterUtility();
		
		
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
		long start = System.currentTimeMillis();
		Boolean b = function.run();
		Long end = System.currentTimeMillis();
		Logs.log("Running "+function.getName()+" has returned "+b+". Took "+(end-start)+" milliseconds.", this);
		return function.getOutput();
		
	}
	


}
