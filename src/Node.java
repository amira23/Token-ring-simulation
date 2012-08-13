/*
 * Shane Fitzpatrick 09487581
 * CS3031 Project 1 - Token Ring Simulation
 * Node class for this project
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.TimerTask;
import java.util.Timer;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class Node extends Thread{
 
	/*
	 * GUI elements
	 */
	public JFrame frm;
	private JPanel pnl;
	private JTextField gui_da, gui_mesg;
	private JTextArea gui_log;
	private JScrollPane pane;
	private JLabel l1;
	private JButton b1,b2;
	
	private Timer timer;
	
	private static int BUF_SIZE = 256;
	private static InetAddress ADDRESS; // IP address of this node
	public boolean isMonitor, isAlive, waitingToSend, waitingResponse;
	private DatagramSocket socket;
	private int port; // This nodes port number
	private Node down_Neigh; // The node that is this nodes down stream neighbour
	
	private String dataToSend, desAddrToSend;
	
	private byte[] temp;
	Frame current_frame = new Frame();
	
	/*
	 * Constructor for a node
	 * n - used for thread name
	 * p - port number of this node
	 */
	public Node(int n, int p) throws Exception{
	//	super(Integer.toString(n));
		// Assign all variables appropriate initial values
		isMonitor = false;
		isAlive = true;
		waitingToSend = false;
		waitingResponse = false;
		port = p;
		down_Neigh = null;
		ADDRESS = InetAddress.getLocalHost();
		socket = new DatagramSocket(port);
		
		this.timer = new Timer();
		
		/*
		 * GUI logic
		 */
		//frm = new JFrame(NODE_NUM + " Addr = "+port);
		frm = new JFrame("Addr = "+port);
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setUndecorated(true);
		frm.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
		pnl = new JPanel();
		pnl.setLayout(null);
		l1 = new JLabel("Desination Addr:");
		l1.setBounds(10, 20, 100, 30);
		pnl.add(l1);
		gui_da = new JTextField(20);
		gui_da.setBounds(125, 25, 150, 20);
		pnl.add(gui_da);
		l1 = new JLabel("Message:");
		l1.setBounds(10, 50, 100, 30);
		pnl.add(l1);
		gui_mesg = new JTextField(10);
		gui_mesg.setBounds(125, 55, 100, 20);
		pnl.add(gui_mesg);
		gui_log = new JTextArea();
		pane = new JScrollPane(gui_log);
		
		DefaultCaret caret = (DefaultCaret)gui_log.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		pane.setBounds(10, 100, 300, 200);
		pnl.add(pane);
		b1 = new JButton("Send");
		b1.setBounds(235, 310, 75, 30);
		b1.addActionListener(new ActionListener()
		{

			/*
			 * Sends the message that is in the messge field to the address in the 
			 * address field
			 */
			public void actionPerformed(ActionEvent e) {
				if(gui_mesg.getText().equals("") || gui_da.getText().equals("")) {
					System.out.println("Error, must have address and message");
				}else{
					waitingToSend = true;
					dataToSend = gui_mesg.getText();
					desAddrToSend = gui_da.getText();
				}
				
			}
		});
		pnl.add(b1);
		b2 = new JButton("On");
		b2.setBounds(310, 310, 75, 30);
		b2.addActionListener(new ActionListener() {
			
			/*
			 * On - Off toggle button
			 */
			public void actionPerformed(ActionEvent e) {
				if(b2.getText().equals("On")){
					b2.setText("Off");
					disconnect();
					gui_log.append("Node is dead.  Bypass relay kicking in\n");
				}else{
					b2.setText("On");
					connect();
					gui_log.append("Node is online. Functioning normally\n");
				}
				
			}
		});
		pnl.add(b2);
		frm.add(pnl);
		frm.setSize(400, 400);
		frm.setVisible(true);
	}	
	
	/*
	 * Method that starts a timer based on weither the node is the monitor or not
	 */
	public void startTimer(){
		if(isMonitor) timer.schedule(new TimedTasks(this), 4000);
		else timer.schedule(new TimedTasks(this), 7000);
	}
	
	/*
	 * Sets the down stream neighbour of this node to d
	 */
	public void setDownNeigh(Node d){
		down_Neigh = d;
	}
	
	/*
	 * Threads main run fucntion
	 */
 	public void run(){
 		try{
 			
 			if(isMonitor){
 				gui_log.append("Monitor\n");
 				startTimer();
 			}else startTimer();
 			
 			// Continue until the system is destroyed
	 		while(true){
	 			
	 			// Make sure the on-off toggle button has the correct text
	 			if(isAlive)b2.setText("On");
	 			else b2.setText("Off");
	 			
	 			// If the node is dead then it will act as a bypass relay
	 			if(!isAlive){
	 				gui_log.append("Bypass relay - relaying frame\n");
	 				timer.cancel();
	 				relay(); 
				}else{
					
					if(waitingToSend) gui_log.append("Waiting to send\n");
					if(waitingResponse) gui_log.append("Waiting response\n");
					
					receive();
					
					Thread.sleep(2000); // Introduce a wait so it is easier for the human eye
					
					/*
					 * Switch statement that will determine the course of action for this turn
					 * Uses current frames type and the nodes attributes to determine the correct
					 * course of action
					 */
					switch (current_frame.getType()){
					
					// Frame received is a token
					case Frame.TOKEN_TYPE:
						if(isMonitor) {
							gui_log.append("Token received\n");
						}
						
						if(waitingResponse){
							waitingResponse= false;
							gui_log.append("Sending failed\n");
						}else if(waitingToSend){
							waitingToSend = false;
							waitingResponse = true;
							byte AC = 0x0;
							int d = Integer.parseInt(desAddrToSend);
							byte[] DA = Frame.intToByteArray(d);
							byte[] SA = Frame.intToByteArray(port);
							byte[] data = dataToSend.getBytes();
							Frame f = new Frame(AC, DA, SA, data);
							send(f);	
							gui_log.append("Sending frame to "+desAddrToSend+" "+dataToSend+"\n");
						}else{
							if(isMonitor) gui_log.append("Token passed on\n");
							send(current_frame);
						}
						
						break;
					
					// Frame is a message
					case Frame.REGULAR_TYPE:
						
						if(current_frame.getDA()==port){
							// Frame is meant for this node
							gui_log.append("Received frame from "+current_frame.getSA()+" - "+current_frame.getDataS()+"\n");
							current_frame.setDA(current_frame.getSA());
							current_frame.setSA(port);
							current_frame.setFS();	// Set the A and C bits of FS
							send(current_frame);  // send the frame on
							gui_log.append("Sending response\n");
						}else{
							if(isMonitor){
								if(!current_frame.mSet()){
									gui_log.append("Received a data frame for node "+current_frame.getDA()+ " setting m bit\n");
									current_frame.setM();
									send(current_frame);
								}else {
									gui_log.append("ERROR - ORPHAN FRAME - DRAINING FRAME\nRELEASING NEW TOKEN\n");
									injectToken();
								}
							}else send(current_frame);
						}
						
						break;
						
					// Frame is a response	
					case Frame.RESPONSE_TYPE:
						if(current_frame.getDA()==port){
							// Frame is meant for this node
							if(waitingResponse){
								waitingResponse = false;
								gui_log.append("Got response from "+current_frame.getSA()+"\n");
								gui_log.append("Passing token\n");
								injectToken();
							}else{
								send(current_frame);
							}
						}else{
							if(isMonitor){
								if(!current_frame.mSet()){
									gui_log.append("Received a response frame for node "+current_frame.getDA()+ " setting m bit\n");
									current_frame.setM();
									send(current_frame);
								}else{
									gui_log.append("ERROR - ORPHAN FRAME - DRAINING FRAME\nRELEASING NEW TOKEN\n");
									injectToken();
								}	
							}else send(current_frame);
							
						}
						
						break;
					
					// Frame is a claim monitor frame	
					case Frame.CLAIM_TOKEN:
						if(current_frame.getSA()==port){
							gui_log.append("Elected monitor\n");
							isMonitor = true;
							timer.cancel();
							timer = new Timer();
							startTimer();
							injectToken();
						}else if(current_frame.getSA() > port){
							send(current_frame);
						}else{
							claimMonitor();
						}
						
						break;
					
					// Frame is a amp frame
					case Frame.AMP_TYPE:
						if(isMonitor){
							gui_log.append("Draining amp frame \n");
						}else{
							timer.cancel();
							timer = new Timer();
							startTimer();
							send(current_frame);
						}
					
						break;
					
					default:
						System.out.println("DEFAULTED IN SWITCH");
						send(current_frame);
						break;
					}
				}
	 		}
 		}catch(IOException e){
 			System.out.println("*** IO_ERROR "+e);
 		} catch (ClassNotFoundException e) {
 			System.out.println("*** CNF_ERROR"+e);
		} catch (InterruptedException e) {
			System.out.println("*** Interrupted Exception error"+e);
		}
	}	
 	
 	/*
 	 * Method to relay traffic - makes node act as a bypass
 	 */
 	private void relay() throws IOException{
 		byte[] buf = new byte[BUF_SIZE];
 		DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
 		socket.receive(packet);
 		packet.setPort(down_Neigh.getPort());
 		socket.send(packet);
 	}
 	
 	
 	/*
 	 * Method that sends the frame f
 	 */
 	private void send(Frame f) throws IOException{
 		byte[] buf = Frame.frameToBytes(f);
 		DatagramPacket packet = new DatagramPacket(buf, buf.length, ADDRESS, down_Neigh.getPort());
		socket.send(packet);
 	}
 	
 	/*
 	 * Method to send a amp frame
 	 */
 	private void sendAMP() throws IOException{
		gui_log.append("Sending AMP frame\n");
		byte AC = 0x22;
		byte[] DA = Frame.intToByteArray(down_Neigh.getPort());
		byte[] SA = Frame.intToByteArray(port);
		byte[] data = "AMP frame".getBytes();
		Frame f = new Frame(AC, DA, SA, data);
		send(f);
		send(current_frame);
 	}
 	
 	/*
 	 * Method that receives a frame and stores it in current frame
 	 */
 	private void receive() throws IOException, ClassNotFoundException{
 		byte[] buf = new byte[BUF_SIZE];
 		DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
 		socket.receive(packet);
 		temp = packet.getData();
 		current_frame = Frame.bytesToFrame(temp);
 	}
 	
 	/*
 	 * Monitor should inject a token into the ring
 	 * CARE MUST BE TAKEN IF THERE ARE ORPHAN FRAMES IN THE RING
 	 */
 	public void injectToken() throws IOException{
		byte AC = 0x10; // T bit set - M, PPP, RRR all 0
		byte[] DA = Frame.intToByteArray(down_Neigh.getPort());
		byte[] SA = Frame.intToByteArray(port);
		byte[] data = "This is a token".getBytes();
		send(new Frame(AC,DA,SA,data));
 	}
 	
 	/*
 	 * Disconnect the current node - turns it into a bypass relay
 	 */
	public void disconnect(){
		gui_log.append("Disconnected\n");
		isAlive = false;
		isMonitor = false;
	}

	/*
	 * Reconnect the current node
	 */
	public void connect(){
		gui_log.append("Reconnected\n");
		isAlive = true;
		isMonitor = false;
	}
	
	/*
	 * Reset the timer
	 */
	private void resetT(){
		timer.cancel();
		timer = new Timer();
		startTimer();
	}
	
	/*
	 * Claim to  be the monitor
	 */
	public void claimMonitor() throws IOException{
		if(isAlive){
			byte AC = 0x18; // T bit set - M, PPP, RRR all 0
			byte[] DA = Frame.intToByteArray(down_Neigh.getPort());
			byte[] SA = Frame.intToByteArray(port);
			byte[] data = "This is a claim token".getBytes();
			gui_log.append("Sending claim frame\n");
			send(new Frame(AC, DA, SA, data));
		}
	}
	
	/*
	 * getter - this.port
	 */
	public int getPort(){
		return port;
	}
	
	/*
	 * Method to set this node as the monitor
	 */
	public void makeMonitor(){
		isMonitor = true;
	}

	/*
	 * Class used for the timer tasks
	 */
	class TimedTasks extends TimerTask{
		Node node;
		
		public TimedTasks(Node n){
			node=n;
		}
		
		public void run(){
			/*
			 * If monitor then send an AMP frame when the timer expires
			 */
			if(node.isMonitor){
				try {
					node.sendAMP();
					node.injectToken();
					node.resetT();
				} catch (IOException e) {
					System.out.println("Error with sending amp frame "+e);
				}
			}
			else {
				/*
				 * Not monitor - so send a claim frame when the timer expires
				 */
				try {
					if(node.isAlive) node.claimMonitor();
				} catch (IOException e) {
					System.out.println("Error with claiming monitor "+ e);
				}
			}
		}
	}
}
