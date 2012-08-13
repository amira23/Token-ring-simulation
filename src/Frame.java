/*
 * Shane Fitzpatrick 09487581
 * CS3031 Project 1 - Token Ring Simulation
 * Frame class for frames that can be sent during this project
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;


@SuppressWarnings("serial")
public class Frame implements Serializable {
	
	// Different types of frame, perhaps enums would be better?
	public static final int TOKEN_TYPE = 0, REGULAR_TYPE = 1, RESPONSE_TYPE = 2, CLAIM_TOKEN = 3, AMP_TYPE = 4;
	
	@SuppressWarnings("unused")
	private static final byte SD=0x0, ED=0x0;
	private static final int ADDRESS_SIZE = 3; 
	private static final int DATA_SIZE = 4500;
	private byte AC, FC, FCS, FS; // should gen FCS itself
	private byte[] DA, SA, data;
	
	/*
	 * Constructor to a return a blank frame
	 */
	public Frame(){
		DA = new byte[ADDRESS_SIZE];
		SA = new byte[ADDRESS_SIZE];
		data = new byte[DATA_SIZE];
		AC = 0x0;
		FC = 0x0;
		FCS = 0x0;
		FS = 0x0;
	}
	
	/*
	 * Constructor to return a frame with passed values
	 */
	public Frame(byte ac, byte[] da, byte[] sa, byte[] d){
		AC = ac;
		DA = da;
		SA = sa;
		data = d;
	}
	
	/*
	 * Convert a frame into a byte array
	 * f - frame to convert
	 */
	public static byte[] frameToBytes(Frame f) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);   
		out.writeObject(f);
		byte[] result = bos.toByteArray(); 
		out.close();
		bos.close();
		return result;
	}
	
	/*
	 * Convert a byte array into a frame
	 * bs - byte array to convert
	 */
	public static Frame bytesToFrame(byte[] bs) throws IOException, ClassNotFoundException{
		ByteArrayInputStream bis = new ByteArrayInputStream(bs);
		ObjectInput in = new ObjectInputStream(bis);
		Frame f = (Frame)in.readObject();
		bis.close();
		in.close();
		return f;
	}
	
	/* 
	 * Set the destination address to the port given
	 */
	public void setDA(int d){
		DA = intToByteArray(d);
	}
	
	/* 
	 * Set the source address to the port given
	 */
	public void setSA(int s){
		SA = intToByteArray(s);
	}
	
	/*
	 * Set FS to the appropriate value
	 */
	public void setFS(){
		FS = (byte)0xCC;
	}
	
	/*
	 * Set the m bit in the access control byte
	 */
	public void setM(){
		AC = 4;
	}
	
	/*
	 * Return true if the m bit in the acces control byte is set
	 */
	public boolean mSet(){
		return AC==4;
	}
	
	/*
	 * Returns an integer representing the type of this frame
	 * @to-do: see if Enum would be better
	 */
	public int getType(){
		if(AC == 0x10){  // What if m bit is set by monitor?
			return TOKEN_TYPE;
		}else if(FS == (byte)0xCC){
			return RESPONSE_TYPE;
		}else if(AC == 0x18){
			return CLAIM_TOKEN;
		}else if(AC == 0x22){
			return AMP_TYPE;
		}else {
			return REGULAR_TYPE;
		}
	}
	
	/*
	 * return data as a byte array
	 */
	public byte[] getData(){
		return data;
	}
	
	/*
	 * DA getter
	 */
	public int getDA(){
		return byteArrayToInt(DA);
	}
	
	/*
	 * SA getter
	 */
	public int getSA(){
		return byteArrayToInt(SA);
	}
	
	/*
	 * Return a string description of this frame
	 */
	public String getDetailsS() throws UnsupportedEncodingException{
		String result = "\n=======================\nData is = " + new String(data, "UTF-8");
		result += "\nDA = " + byteArrayToInt(DA);
		result += "\nSA = " + byteArrayToInt(SA);
		result += "\nAC = " + Byte.toString(AC);
		result += "\nTESTING " + AC;
		result += "\nFC = " + Byte.toString(FC);
		result += "\nFCS = " + Byte.toString(FCS);
		result += "\nFS = " + Byte.toString(FS);
		result += "\nType = " + getType() + "\n=======================\n";
		return result;
	}
	
	/*
	 * Return the data bytes as a UTF-8 string
	 */
	public String getDataS() throws UnsupportedEncodingException{
		return new String(data, "UTF-8");
	}
	
	/*
	 * Converts an integer into a byte array
	 */
	public static byte[] intToByteArray(int value) {
	    return new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
	}
	
	/*
	 * convert a byte array back to an integer
	 * Byte array must be of size 4
	 */
	public static int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
	}
}
