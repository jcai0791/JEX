package cruncher;

import java.io.IOException;
import java.util.TreeMap;

import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import function.imageUtility.VirtualFunctionUtility;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import logs.Logs;

public abstract class VirtualFunctionCruncher {
	public String functionName;

	public ParameterSet parameters;
	public ImageProcessor output;

	//input image path
	public TreeMap<String,ImageProcessor> inputs = new TreeMap<String,ImageProcessor>();
	
	public abstract void initializeParameters();
	
	public void setParameters(ParameterSet params) {
		this.parameters = params;
	}

	public abstract boolean run();

	public void setParameters(TreeMap<String,String> params) {
		for(Parameter p : parameters.getParameters()) {
			p.setValue(params.get(p.title));
		}
	}
	
	public abstract String getName();
	
	public ImageProcessor getOutput() {
		return this.output;
	}
	
	public void save() {

	}
	
	public void setOutput(ImageProcessor ip) {
		this.output = ip;
	}
	
	public void setInputs(TreeMap<String,String> inputPaths) {
		for(String inputName : inputPaths.keySet()) {
			String inputPath = inputPaths.get(inputName);
			
			if(inputPath.endsWith("vfn")) {
				try {
					VirtualFunctionUtility vfc = new VirtualFunctionUtility(inputPath);
					inputs.put(inputName,vfc.call());
					
				} catch (InstantiationException | IllegalAccessException | IOException e) {
					e.printStackTrace();
				}
			}
			else {
				Logs.log("Path is: "+inputPath, this);
				inputs.put(inputName,new ImagePlus(inputPath).getProcessor());
			}
		}
	}
	

}
