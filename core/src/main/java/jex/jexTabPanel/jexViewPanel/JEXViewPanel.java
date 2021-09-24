package jex.jexTabPanel.jexViewPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.JEXManager;
import jex.arrayView.ArrayViewController;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.Definition.Experiment;
import Database.Definition.HierarchyLevel;

public class JEXViewPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private ArrayViewController arrayPaneController;
	
	JEXViewPanel()
	{
		initialize();
		
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(arrayPaneController);
		SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		// make a controller
		Logs.log("Initializing new controller", 1, this);
		
		navigationChanged();
	}
	
	public void navigationChanged()
	{
		Logs.log("Navigation changed, displaying update", 1, this);
		
		// Get the viewed experiment
		String expViewed = JEXStatics.jexManager.getViewedExperiment();
		if(expViewed == null)
		{
			openArray(null);
			return;
		}
		// String arrayViewed = JEXStatics.jexManager.getArrayViewed();
		TreeMap<String,Experiment> tree = JEXStatics.jexManager.getExperimentTree();
		if(tree == null)
		{
			openArray(null);
			return;
		}
		Experiment exp = tree.get(expViewed);
		openArray(exp);
	}
	
	public void openArray(HierarchyLevel tray)
	{
		if(tray == null)
		{
			displayArray(null);
		}
		if(tray instanceof Experiment)
		{
			displayArray((Experiment) tray);
		}
	}
	
	private void displayArray(Experiment tray)
	{
		Logs.log("Displaying experimental array", 1, this);
		
		// Remove all
		this.removeAll();
		
		// Set graphics
		this.setLayout(new MigLayout("flowx, ins 0","[grow 100]","[grow 100]"));
		this.setBackground(DisplayStatics.background);
		
		if(tray != null)
		{
			// Make the array controller
			arrayPaneController = new ArrayViewController();
			arrayPaneController.setArray(tray);
			JScrollPane scrollPane = new JScrollPane(arrayPaneController.panel());
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			// Place the components in this panel
			this.add(scrollPane, "grow");
		}
		else
		{
			JLabel label = new JLabel("No array selected... Use the left panel to browse until an array is selected");
			label.setForeground(Color.white);
			JPanel pane = new JPanel();
			pane.setBackground(DisplayStatics.background);
			pane.setLayout(new MigLayout("flowy, ins 5, center, center", "[grow]", "20[grow]10"));
			pane.add(label, "width 100%, grow");
			this.add(pane, "grow");
		}
		
		// REvalidate
		this.revalidate();
		this.repaint();
	}
	
}