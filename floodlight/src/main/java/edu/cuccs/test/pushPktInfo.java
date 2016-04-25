package edu.cuccs.test;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;

public class pushPktInfo {
	public FloodlightContext cntx;
	public IOFSwitch sw;
	public double length;
	public int bufferId;
	public short inPort;
	public short outPort;
	
//	public void setSwitch(IOFSwitch sw){
//		this.sw = sw;
//	}
//	public void setBufferId(int bufferId){
//		this.bufferId = bufferId;
//	}
//	public void setInPort(short inPort){
//		this.inPort = inPort;
//	}
//	public void setOutPort(short outPort){
//		this.outPort = outPort;
//	}
	
	public IOFSwitch getSwitch(){
		return sw;
	}
	public int getBufferId(){
		return bufferId;
	}
	public short getInPort(){
		return inPort;
	}
	public short getOutPort(){
		return outPort;
	}
}
