package cruncher;

import java.util.TreeMap;

import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import ij.process.ImageProcessor;

public abstract class VirtualFunctionCruncher {
	public String functionName;

	public ParameterSet parameters;
	public ImageProcessor output;

	//input image path
	public TreeMap<String,String> inputs;
	
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
	
	public void setInputs(TreeMap<String,String> inputs) {
		this.inputs = inputs;
	}
	

}
