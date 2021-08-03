package jex.jexTabPanel.jexDistributionPanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import Database.Definition.Experiment;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import jex.statics.PrefsUtility;
import logs.Logs;
import miscellaneous.StringUtility;
import net.miginfocom.swing.MigLayout;

public class DimensionSelector extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private Color foregroundColor = DisplayStatics.lightBackground;
	public String name;
	public int num;
	public String[] possibilities;
	public int sizeOfDimension = 1;
	public String token = "";
	JLabel numLabel;
	JLabel nameLabel;
	JComboBox<String> values;
	JTextField sizeField;
	JTextField tokenField;
	
	DimensionSelector(int num, String name, String[] possibilities)
	{
		this.num = num;
		this.name = "" + num + ". " + name;
		this.possibilities = possibilities;
		initialize();
	}
	
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0", "[0:0,15]3[grow,fill]3[]3[0:0,40]", "[]"));
		this.setBackground(foregroundColor);
		
		nameLabel = new JLabel(name);
		nameLabel.setBackground(foregroundColor);
		values = new JComboBox<String>(possibilities);
		values.setEditable(true);
		values.setBackground(foregroundColor);
		values.addActionListener(this);
		values.setSelectedIndex(num - 1);
		sizeField = new JTextField(70);
		tokenField = new JTextField(70);
		// sizeField.setPreferredSize(new Dimension(15,
		// sizeField.getPreferredSize().height));
		// sizeField.setMaximumSize(new Dimension(15,
		// sizeField.getPreferredSize().height));
		Logs.log("Size of sizeField " + sizeField.getMaximumSize(), 0, this);
		//sizeField.setText("" + sizeOfDimension);
		//sizeField.setText(PrefsUtility.getFileDeal(getDimensionName()));
		TreeMap<String,Experiment> expTree = JEXStatics.jexManager.getExperimentTree();
		String viewedExp = JEXStatics.jexManager.getViewedExperiment();
		if(getDimensionName().equals("T")) sizeField.setText(PrefsUtility.getFileDealT());		
		else if(getDimensionName().equals("Array Row")) sizeField.setText(""+expTree.get(viewedExp).getArrayDimension().width);
		else if(getDimensionName().equals("Array Column")) sizeField.setText(""+expTree.get(viewedExp).getArrayDimension().height);
		else sizeField.setText("" + sizeOfDimension);
		sizeField.setBackground(foregroundColor);
		JLabel temp = new JLabel("size:");
		temp.setBackground(foregroundColor);
		
		tokenField.setText(token);
		tokenField.setBackground(foregroundColor);
		JLabel temp2 = new JLabel("token:");
		temp2.setBackground(foregroundColor);
		
		// this.setMaximumSize(new Dimension(250,20));
		// this.add(Box.createHorizontalGlue());
		this.add(nameLabel);
		this.add(values, "growx");
		this.add(temp);
		this.add(sizeField);
		this.add(temp2);
		this.add(tokenField);
		// this.add(Box.createHorizontalGlue());
	}
	
	public String getDimensionName()
	{
		String result = values.getSelectedItem().toString();
		return result;
	}
	
	public int getDimensionSize()
	{
		String s = sizeField.getText();
		Integer i = Integer.parseInt(s);
		return i;
	}
	
	public String getDimensionToken()
	{
		String s = tokenField.getText();
		return StringUtility.removeAllWhitespace(s);
	}
	public void setSizeField(String s) {
		this.sizeField.setText(s);
	}
	
	public void setRestrictedPossibilityList(List<String> removeThesePossibilities)
	{   
		
	}
	
	public void actionPerformed(ActionEvent e)
	{}
}