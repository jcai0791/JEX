package jex.arrayView;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;

public abstract class ArrayCellPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	protected Color background = DisplayStatics.lightBackground;
	protected Color temporaryGround = DisplayStatics.lightBackground;
	
	public ArrayCellController controller;
	
	public ArrayCellPanel(ArrayCellController controller)
	{
		this.controller = controller;
		this.setMaximumSize(new Dimension(Math.max(JEXStatics.main.centerPane.getWidth()/this.controller().parent.width(),DisplayStatics.arrayWidth),Math.max(JEXStatics.main.centerPane.getHeight()/this.controller().parent.height(),DisplayStatics.arrayHeight)));
	}
	
	public ArrayCellController controller()
	{
		return controller;
	}
	
	public void setController(ArrayCellController controller)
	{
		this.controller = controller;
	}
	
	public abstract void rebuild();
}
