package jex;

import Database.SingleUserDatabase.JEXDB;
import Database.SingleUserDatabase.JEXDBInfo;
import Database.SingleUserDatabase.Repository;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;

public class DatabaseCloningPane extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JTextField nameField = new JTextField();
	private JTextField infoField = new JTextField();
	private JTextField passField = new JTextField();
	private JCheckBox checkBox = new JCheckBox();
	private Repository rep;
	private JEXDatabaseChooser parent;
	private JButton doItButton;
	private JEXDBInfo db;
	private JButton cancelButton;
	
	public DatabaseCloningPane(JEXDatabaseChooser parent, Repository rep, JEXDBInfo db)
	{
		this.rep = rep;
		this.parent = parent;
		this.db = db;
		
		// initialize
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize()
	{
		// Make the button
		doItButton = new JButton("Done");
		doItButton.addActionListener(this);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		// Make the form layout
		ColumnSpec column1 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(70), FormSpec.NO_GROW);
		ColumnSpec column2 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(100), FormSpec.DEFAULT_GROW);
		ColumnSpec column3 = new ColumnSpec(ColumnSpec.FILL, Sizes.dluX(50), FormSpec.NO_GROW);
		ColumnSpec[] cspecs = new ColumnSpec[] { column1, column2, column3 };
		
		RowSpec row1 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row2 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row3 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row4 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row5 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row6 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec row7 = new RowSpec(RowSpec.CENTER, Sizes.dluX(14), FormSpec.NO_GROW);
		RowSpec[] rspecs = new RowSpec[] { row1, row2, row3, row4, row5, row6, row7 };
		
		FormLayout layout = new FormLayout(cspecs, rspecs);
		
		CellConstraints cc = new CellConstraints();
		this.setLayout(layout);
		this.setBackground(DisplayStatics.lightBackground);
		
		// Fill the layout
		JLabel nameLabel = new JLabel("Name");
		this.add(nameLabel, cc.xy(1, 1));
		this.add(nameField, cc.xywh(2, 1, 2, 1));
		nameField.setText("New Database");
		
		JLabel infoLabel = new JLabel("Info");
		this.add(infoLabel, cc.xy(1, 2));
		this.add(infoField, cc.xywh(2, 2, 2, 1));
		infoField.setText("No info yet");
		
		JLabel passLabel = new JLabel("Password");
		this.add(passLabel, cc.xy(1, 3));
		this.add(passField, cc.xywh(2, 3, 2, 1));
		
		JLabel checkLabel = new JLabel("Clone data?");
		this.add(checkLabel, cc.xy(1,4));
		this.add(checkBox, cc.xy(2,4));
		
		this.add(doItButton, cc.xy(2, 5));
		this.add(cancelButton,cc.xy(2,6));
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == doItButton)
		{
			Logs.log("Database creation validated", 1, this);
			String name = nameField.getText();
			String info = infoField.getText();
			String pass = passField.getText();
			boolean keepData = checkBox.isSelected();
			boolean done = JEXStatics.jexManager.cloneDatabase(db, JEXDB.LOCAL_DATABASE, rep, name, info, pass, keepData);
			Logs.log("Database creation returned " + done, 1, this);
			
			// Reset the datrabase chooser
			parent.setAlternatePanel(null);
		}
		if(e.getSource() == cancelButton) {
			Logs.log("Database cloning canceled", this);
			parent.setAlternatePanel(null);
		}
	}
}
