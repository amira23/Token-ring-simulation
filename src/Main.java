/*
 * Shane Fitzpatrick 09487581
 * CS3031 Project 1 - Token Ring Simulation
 * Main class for project.  GUI command centre is generated where user initializes
 * the ring, starts the nodes and then can kill monitor node or dynamically add nodes
 * at their leisure
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

public class Main {
	@SuppressWarnings("deprecation")
	public static void main(String args[]) throws Exception{
		
		/*
		 * GUI elements
		 */
		JFrame frm;
		JPanel pnl;
		JButton b1,b2,b3,b4;
		
		/*
		 * Number of initial nodes and first nodes port number
		 */
		final int numNodes = 2;
		final int initialP = 4444; // first port number
		
		final ArrayList<Node> nodes = new ArrayList<Node>();
		
		/*
		 * GUI Logic
		 */
		frm = new JFrame("Command centre");
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setUndecorated(true);
		frm.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
		pnl = new JPanel();
		pnl.setLayout(null);
		
		b1 = new JButton("Init");
		b1.setBounds(10, 10, 75, 30);
		b1.addActionListener(new ActionListener()
		{

			/*
			 * Initialize initial nodes
			 */
			public void actionPerformed(ActionEvent e) {
				for(int i=0; i<numNodes; i++){
					try {
						Node n = new Node(i, initialP+i);
						nodes.add(n);
						nodes.get(i).frm.move(i*400, 100);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
				for(int i=0; i<nodes.size()-1; i++){
					nodes.get(i).setDownNeigh(nodes.get(i+1));
				}
				nodes.get(nodes.size()-1).setDownNeigh(nodes.get(0));
				nodes.get(0).makeMonitor();
			}
		});
		pnl.add(b1);
		
		b2 = new JButton("Start");
		b2.setBounds(85, 10, 75, 30);
		b2.addActionListener(new ActionListener() {
			
			/*
			 * Start nodes 
			 */
			public void actionPerformed(ActionEvent e) {
				try {
					nodes.get(0).injectToken();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				for(int i=0; i<nodes.size(); i++){
					nodes.get(i).start();
				}
			}
		});
		pnl.add(b2);
		
		b3 = new JButton("Kill M");
		b3.setBounds(160, 10, 75, 30);
		b3.addActionListener(new ActionListener() {
			/*
			 * Kill the monitor node
			 */
			public void actionPerformed(ActionEvent e) {
				for(int i=0; i<nodes.size(); i++){
					if(nodes.get(i).isMonitor) nodes.get(i).disconnect();	
				}
			}
		});
		pnl.add(b3);
		
		b4 = new JButton("Add");
		b4.setBounds(235, 10, 75, 30);
		b4.addActionListener(new ActionListener() {
			
			/*
			 * Dynamically add a new node and reconfigure system
			 */
			public void actionPerformed(ActionEvent e) {
				try {
					Node n = new Node(nodes.size(), initialP+nodes.size());
					n.frm.move(nodes.size()*400, 100);
					n.start();
					nodes.add(n);
					for(int i=0; i<nodes.size()-1; i++){
						nodes.get(i).setDownNeigh(nodes.get(i+1));
					}
					nodes.get(nodes.size()-1).setDownNeigh(nodes.get(0));
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		pnl.add(b4);
		
		frm.add(pnl);
		frm.setSize(350, 100);
		frm.setVisible(true);	
	}

}
