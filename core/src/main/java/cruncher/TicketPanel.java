package cruncher;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class TicketPanel {
	
	public JPanel panel;
	public Ticket ticket;
	private JLabel funcName = new JLabel(" ");
	private JLabel funcStart = new JLabel(" ");
	private JLabel funcEnd = new JLabel(" ");
	private JLabel funcEst = new JLabel(" ");
	
	public TicketPanel(Ticket ticket)
	{
		this.ticket = ticket;
		initialize();
	}
	
	private void initialize()
	{
		// this.funcEnd.setDoubleBuffered(true);
		this.panel = new JPanel();
		this.panel.setLayout(new MigLayout("flowy, ins 0", "[grow, 100]3", "[]"));
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.funcName.setText(this.ticket.cr.getName());
		this.funcName.setFont(FontUtility.boldFont);
		this.panel.add(funcName, "left");
		this.panel.add(funcStart, "right");
		this.panel.add(funcEnd, "right");
		this.panel.add(funcEst, "right");
		this.panel.revalidate();
		this.panel.repaint();
		SSCenter.defaultCenter().connect(this.ticket, Ticket.SIG_TicketStarted_NULL, this, "ticketStarted", (Class[]) null);
		SSCenter.defaultCenter().connect(this.ticket, Ticket.SIG_TicketUpdated_NULL, this, "ticketUpdated", (Class[]) null);
		SSCenter.defaultCenter().connect(this.ticket, Ticket.SIG_TicketFinished_NULL, this, "ticketFinished", (Class[]) null);
	}
	
	public synchronized void ticketStarted()
	{
		funcStart.setText("Start Time: " + ticket.startTime);
		funcEst.setText("Estimated Time: ");
		funcEst.repaint();
		funcStart.repaint();
	}
	
	public synchronized void ticketFinished()
	{
		funcEnd.setText("End Time: " + ticket.endTime);
		Logs.log("Start Time: "+ticket.startTime, this);
		Logs.log("End Time: "+ticket.endTime, this);
		funcEnd.repaint();
	}
	
	public synchronized void ticketUpdated()
	{
		funcEnd.setText("Completed: " + ticket.functionsFinished + " of " + ticket.size() + ", Threads: " + (ticket.functionsStarted - ticket.functionsFinished));
		funcEnd.repaint();
		int secondsLeft = (int)Math.round((System.currentTimeMillis()-ticket.startTimeMilli)/1000*(double)(ticket.size()-ticket.functionsFinished)/(double)(ticket.functionsFinished));
		String time = "";
		if(secondsLeft<=0) time = "";
		else if(secondsLeft<60) time = "<1 minute";
		else if(secondsLeft <3600) time = secondsLeft/60 + " minutes";
		else time = secondsLeft/3600 + " hours, "+(secondsLeft%3600)/60+" minutes";
		funcEst.setText("Estimated Time Left : "+time);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
}
