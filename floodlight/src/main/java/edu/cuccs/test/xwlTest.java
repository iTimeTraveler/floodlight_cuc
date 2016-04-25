package edu.cuccs.test;

import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.management.timer.Timer;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.loadbalancer.LBVip;
import net.floodlightcontroller.loadbalancer.LoadBalancer;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.OFMessageDamper;


public class xwlTest implements IFloodlightModule, IOFMessageListener,ITopologyListener{
	protected static Logger log = LoggerFactory.getLogger(LoadBalancer.class);
	
	
	//ymx
	protected static int OFMESSAGE_DAMPER_CAPACITY = 10000; // ms. 
    protected static int OFMESSAGE_DAMPER_TIMEOUT = 250; // ms 
    protected OFMessageDamper messageDamper;
    protected IRoutingService routingEngine;
    static short  notconnect = -1;
	static int daijia = 100;
	static int maplength = 16;
	protected short[][] map = new short [maplength][maplength];
	protected ITopologyService topology;
	protected Dijkstra dij = new Dijkstra();
	protected AA ant = new AA();
	boolean flag = true;
	//ymx
    
	// service modules needed
    protected IFloodlightProviderService floodlightProvider;
    
    protected HashMap<String, LBVip> vips;
    protected HashMap<Integer, String> vipIpToId;
    protected Queue<IPacket> TCP_pktQueue = new LinkedList<IPacket>();  
    protected Queue<pushPktInfo> TCP_infoQueue = new LinkedList<pushPktInfo>();  
    protected Queue<IPacket> ICMP_pktQueue = new LinkedList<IPacket>();  
    protected Queue<pushPktInfo> ICMP_infoQueue = new LinkedList<pushPktInfo>();  
    protected Counter count = new Counter();
	/********************************IOFMessageListener********************************/
	@Override
	public String getName() {
		return "xwlTestname";
	}
	public void pushPacket(IPacket packet, 
            IOFSwitch sw,
            int bufferId,
            short inPort,
            short outPort, 
            FloodlightContext cntx,
            boolean flush) 
	{
		OFPacketOut po =
				(OFPacketOut) floodlightProvider.getOFMessageFactory()
                                 .getMessage(OFType.PACKET_OUT);
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(outPort));
		po.setActions(actions)
		.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);
		short poLength =
				(short) (po.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);
		po.setBufferId(bufferId);
		po.setInPort(inPort);
		if (po.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
			if (packet == null) {
				return;
			}
			byte[] packetData = packet.serialize();
			poLength += packetData.length;
			po.setPacketData(packetData);
		}

		po.setLength(poLength);

		try {
			//counterStore.updatePktOutFMCounterStoreLocal(sw, po);
			messageDamper.write(sw, po, cntx, flush);
		} catch (IOException e) {
			//log.error("Failure writing packet out", e);
		}
}
	
	private void arpreply(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx)
	{
		//arp.setTargetProtocolAddress(Ethernet.toMACAddress("00:00:00:00:00"));
		 Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                 IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		 // retrieve original arp to determine host configured gw IP address                                          
		 if (! (eth.getPayload() instanceof ARP))
			 return;
		 ARP arpRequest = (ARP) eth.getPayload();
		 // have to do proxy arp reply since at this point we cannot determine the requesting application type
		 byte[] vipProxyMacBytes = Ethernet.toMACAddress("00:00:00:00:00:aa");//vips.get(vipId).proxyMac.toBytes();
		 // generate proxy ARP reply
		 IPacket arpReply = new Ethernet()
		 	.setSourceMACAddress(vipProxyMacBytes)
		 	.setDestinationMACAddress(eth.getSourceMACAddress())
		 	.setEtherType(Ethernet.TYPE_ARP)
		 	.setVlanID(eth.getVlanID())
		 	.setPriorityCode(eth.getPriorityCode())
		 	.setPayload(
		 			new ARP()
		 			.setHardwareType(ARP.HW_TYPE_ETHERNET)
		 			.setProtocolType(ARP.PROTO_TYPE_IP)
		 			.setHardwareAddressLength((byte) 6)
		 			.setProtocolAddressLength((byte) 4)
		 			.setOpCode(ARP.OP_REPLY)
		 			.setSenderHardwareAddress(vipProxyMacBytes)
		 			.setSenderProtocolAddress(
		 					arpRequest.getTargetProtocolAddress())
		 					.setTargetHardwareAddress(
		 							eth.getSourceMACAddress())
		 							.setTargetProtocolAddress(
		 									arpRequest.getSenderProtocolAddress()));

		 // push ARP reply out
		 pushPacket(arpReply, sw,OFPacketOut.BUFFER_ID_NONE, OFPort.OFPP_NONE.getValue(),
				 	pi.getInPort(), cntx, true);
		 //log.debug("proxy ARP reply pushed as {}", IPv4.fromIPv4Address(vips.get(vipId).address));

		 return;
	}
	private net.floodlightcontroller.core.IListener.Command 
	processPacketIn3(IOFSwitch sw, OFPacketIn pi,
            FloodlightContext cntx) {
		//switch(msg.getType()){
		//case PACKET_IN:
			// System.out.print(IPv4.toIPv4AddressBytes("10.0.0.6"));
			//OFPacketIn pi = (OFPacketIn) msg;			 
	        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
	                                                              IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	        OFMatch match = new OFMatch();
	        match.loadFromPacket(pi.getPacketData(), pi.getInPort());
	        //pushsetflow(match , sw, pi, cntx, decision, "10.0.0.6");
	        //chooseflowtable(sw, pi, decision, cntx, pi);
				if(eth.getPayload() instanceof ARP)
				{
					ARP arp = (ARP) eth.getPayload();
					int targetProtocolAddress = IPv4.toIPv4Address(arp
                            .getTargetProtocolAddress());
					//System.out.print("arp");
					if(IPv4.fromIPv4Address(targetProtocolAddress).equals("10.0.0.200"))
					     {
					    	 System.out.print("arp reply");
					    	 arpreply(sw, pi, cntx);
					    	 return Command.STOP;
					     }
					else 
					   return Command.CONTINUE;
				}
				else if(eth.getPayload() instanceof IPv4)
				{
					System.out.print("pushtable");
					IPv4 ip = (IPv4)eth.getPayload();
					if(IPv4.fromIPv4Address(ip.getSourceAddress()).equals("10.0.0.1")||IPv4.fromIPv4Address(ip.getSourceAddress()).equals("10.0.0.2"))
					{
						pushchangesrc(match, sw, pi, cntx, (short)3, "10.0.0.200");
					}
					else if(IPv4.fromIPv4Address(ip.getDestinationAddress()).equals("10.0.0.200"))
					{
						if(flag)
							{
							//pushchangedst("8a:f8:13:34:34:72",match, sw, pi, cntx, (short)1, "10.0.0.1");
							}
						else
							{
							
							}
						pushchangedst("92:88:ca:02:f6:5c",match, sw, pi, cntx, (short)2, "10.0.0.2");
						flag = !flag;
						return Command.STOP;
					}
				}
				return Command.CONTINUE;
     }
	public void pushchangesrc(OFMatch match,IOFSwitch sw,OFPacketIn pi, FloodlightContext cntx ,short outport,String  setsorce)
	{

	   OFFlowMod fm =
                (OFFlowMod) floodlightProvider.getOFMessageFactory()
                                              .getMessage(OFType.FLOW_MOD);
       List<OFAction> actions = new ArrayList<OFAction>();
       OFActionNetworkLayerSource ofs = new OFActionNetworkLayerSource(IPv4.toIPv4Address(setsorce));
       OFActionDataLayerSource ofdls = new OFActionDataLayerSource(Ethernet.toMACAddress("00:00:00:00:00:aa"));
       actions.add(ofdls);
       actions.add(ofs);
       OFActionOutput act = new OFActionOutput(outport);
	   actions.add(act);
       long cookie = 0 << 52;
       fm.setCookie(cookie)
          .setHardTimeout((short) 0)
          .setIdleTimeout((short) 5)
          .setBufferId(OFPacketOut.BUFFER_ID_NONE)
          .setMatch(match)
          .setActions(actions)
          .setCommand(OFFlowMod.OFPFC_ADD)
          .setLengthU(OFFlowMod.MINIMUM_LENGTH+
        		  OFActionOutput.MINIMUM_LENGTH
        		  +OFActionNetworkLayerSource.MINIMUM_LENGTH
        		  +OFActionDataLayerSource.MINIMUM_LENGTH
        		 );
       fm.getMatch().setInputPort(pi.getInPort());    
      	try {
   			messageDamper.write(sw, fm, cntx);
   		} catch (IOException e) {
   			// TODO Auto-generated catch block
   			e.printStackTrace();
   		}
      	try {
              fm = fm.clone();
          } catch (CloneNotSupportedException e) {
              //log.error("Failure cloning flow mod", e);
          }
	}
	public void pushchangedst(String mac,OFMatch match,IOFSwitch sw,OFPacketIn pi, FloodlightContext cntx  ,short outport, String setip)
	{
	   OFFlowMod fm =
                (OFFlowMod) floodlightProvider.getOFMessageFactory()
                                              .getMessage(OFType.FLOW_MOD);
       List<OFAction> actions = new ArrayList<OFAction>(); // Set no action to
       OFActionDataLayerDestination ofdld = new  OFActionDataLayerDestination(Ethernet.toMACAddress(mac));  
       OFActionNetworkLayerDestination of =new OFActionNetworkLayerDestination(IPv4.toIPv4Address(setip));
       actions.add(of);
       actions.add(ofdld);
       OFActionOutput act = new OFActionOutput(outport);
	   actions.add(act);
       long cookie = 0 << 52;
       fm.setCookie(cookie)
          .setHardTimeout((short) 0)
          .setIdleTimeout((short) 5)
          .setBufferId(OFPacketOut.BUFFER_ID_NONE)
          .setMatch(match)
          .setActions(actions)
          .setCommand(OFFlowMod.OFPFC_ADD)
          .setLengthU(OFFlowMod.MINIMUM_LENGTH+
        		  OFActionNetworkLayerDestination.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH   
        		  +OFActionDataLayerDestination.MINIMUM_LENGTH
        		 );
       fm.getMatch().setInputPort(pi.getInPort());    
      	try {
   			messageDamper.write(sw, fm, cntx);
   		} catch (IOException e) {
   			// TODO Auto-generated catch block
   			e.printStackTrace();
   		}
      	try {
              fm = fm.clone();
          } catch (CloneNotSupportedException e) {
              //log.error("Failure cloning flow mod", e);
          }
	}
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		//return (!type.equals(OFType.PACKET_IN) && (!(name.equals("forwarding")
		//		|| name.equals("loadbalancer"))));
		//return (type.equals(OFType.PACKET_IN) && (name.equals("loadbalancer")
		//		));
		return false;
	}
	public class Counter
	{
		 public double Tcpbyt = 0;
		 public double Udpbyt = 0;
		 public double Icmpbyt = 0;
		 public long tcpcounter = 0;
		 public long udpcounter = 0;
		 public long icmpcounter = 0;
		 public int counter = 0;
		 public static final int Tcpflow = 0;
		 public static final int Udpflow = 2;
		 public static final int Icmpflow = 4;
		 private int sleep = 99999000;
		 //public int type 
		 public Counter()
		 {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(true)
					{
						Icmpbyt = 0;
						Udpbyt = 0;
						tcpcounter = 0;
						icmpcounter = 0;
						udpcounter = 0;
						Icmpbyt = 0;
						System.out.print("重新计数");
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					}					
				}
			}).start();
		 }
		 public void print()
		 {
			 System.out.print("udp包："+udpcounter +"  udp流量：" +Udpbyt + "KB\n");
			 System.out.print("tcp包："+tcpcounter +"  tcp流量：" +Tcpbyt + "KB\n");
			 System.out.print("icmp包："+icmpcounter +"  icmp流量：" +Icmpbyt + "KB\n");
			 
		 }
		 public void add(OFPacketIn pi,int type)
		 {
			 if(type == Tcpflow)
				 {Tcpbyt = Tcpbyt+((double)pi.getPacketData().length);counter++;tcpcounter++;}
			 else if(type == Udpflow)
				 {Udpbyt = Udpbyt + ((double)pi.getPacketData().length);counter++;udpcounter++;}
			 else if(type == Icmpflow)
				 {Icmpbyt = Icmpbyt +((double)pi.getPacketData().length);counter++;icmpcounter++;}
		 }
		 public double getallbyte(){
			 return (Tcpbyt+Udpbyt+Icmpbyt);
		 }
		 public double getTCPbyte(){
			 return Tcpbyt;
		 }
		 public double getICMPbyte(){
			 return Icmpbyt;
		 }
		 
	}
	public void pushdelete()
	{
		
	}
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {	//加载完毕以后在执行 forwarding
		return (type.equals(OFType.PACKET_IN) && ((name.equals("forwarding"))||(name.equals("loadbalancer"))));
		//return true;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
        case PACKET_IN:
        	//System.out.println("Packet-in!!");
        	return processPacketIn(sw, (OFPacketIn)msg, cntx);
        case FLOW_MOD:
        	//System.out.print("flow mod");
        	return Command.CONTINUE;
        case PACKET_OUT:
        	//System.out.print("packet out");
        	return Command.CONTINUE;
        default:
            break;
        }

        return Command.CONTINUE;
	}
	 protected void doFlood(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
	        if (topology.isIncomingBroadcastAllowed(sw.getId(),
	                                                pi.getInPort()) == false) {
	            if (log.isTraceEnabled()) {
	                log.trace("doFlood, drop broadcast packet, pi={}, " +
	                          "from a blocked port, srcSwitch=[{},{}], linkInfo={}",
	                          new Object[] {pi, sw.getId(),pi.getInPort()});
	            }
	            return;
	        }

	        // Set Action to flood
	        OFPacketOut po =
	            (OFPacketOut) floodlightProvider.getOFMessageFactory().getMessage(OFType.PACKET_OUT);
	        List<OFAction> actions = new ArrayList<OFAction>();
	        if (sw.hasAttribute(IOFSwitch.PROP_SUPPORTS_OFPP_FLOOD)) {
	            actions.add(new OFActionOutput(OFPort.OFPP_FLOOD.getValue(),
	                                           (short)0xFFFF));
	        } else {
	            actions.add(new OFActionOutput(OFPort.OFPP_ALL.getValue(),
	                                           (short)0xFFFF));
	        }
	        po.setActions(actions);
	        po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

	        // set buffer-id, in-port and packet-data based on packet-in
	        short poLength = (short)(po.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);
	        po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	        po.setInPort(pi.getInPort());
	        byte[] packetData = pi.getPacketData();
	        poLength += packetData.length;
	        po.setPacketData(packetData);
	        po.setLength(poLength);

	        try {
	            if (log.isTraceEnabled()) {
	                log.trace("Writing flood PacketOut switch={} packet-in={} packet-out={}",
	                          new Object[] {sw, pi, po});
	            }
	            messageDamper.write(sw, po, cntx);
	        } catch (IOException e) {
	            log.error("Failure writing PacketOut switch={} packet-in={} packet-out={}",
	                    new Object[] {sw, pi, po}, e);
	        }

	        return;
	    }
	private net.floodlightcontroller.core.IListener.Command 
	processPacketIn2(IOFSwitch sw, OFPacketIn pi,
            FloodlightContext cntx) {
     Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                                              IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
     IPacket pkt = eth.getPayload();
     if(pkt instanceof ARP)
    	 return Command.CONTINUE;
     if(pkt instanceof IPv4)
     {
     // System.out.println("pushtable----------------");
      IDevice dstDevice =
             IDeviceService.fcStore.
                 get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
      IDevice srcDevice =
              IDeviceService.fcStore.
                  get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);
      SwitchPort[] dstDap = null;
      if(dstDevice != null)
      {    
    	  dstDap = dstDevice.getAttachmentPoints();
    	  if(dstDap != null)
    	  {
    		 // System.out.println("*****************\n");
    		  pushroute((int)sw.getId(), (int)dstDap[0].getSwitchDPID(), (short)dstDap[0].getPort(), pi, cntx);
    		  //System.out.println("*****************\n");
    		  //pushroute((int)dstDap[0].getSwitchDPID(), (int)sw.getId(), pi.getInPort(), pi, cntx);
    	  }
      }
     return Command.STOP;
     }
     return Command.STOP;
     }
	
	public void pushroute2(OFMatch match,int srcswitch,int dstswitch,short outport, OFPacketIn pi, FloodlightContext cntx)
    {
    	
    	/*int [] swi = dij.getroute(srcswitch, dstswitch);*/
		int [] swi = new int[3];
		swi [0]= 3;
		swi [1] =1;
		swi [2] = 2;
    	//if(swi == null)
    	//	return;
    	int swlink = swi.length - 1;
    	for(int i = 0;i<swlink;i++)
    	{
    		IOFSwitch sw = floodlightProvider.getSwitch(swi[i]);
    		//short port = map[swi[swlink]][swi[swlink+1]];
    		int left = swi[i];
    		int right = swi[i+1];
    		short port = map[left][right];
    		
    		//System.out.print("\n"+sw.getId()+"\n");
    		//short port = map[swi[swlink]][swi[swlink+1]];;
    		pushtable(sw,pi,pi.getInPort(),port,cntx);
    	}
    	IOFSwitch sw = floodlightProvider.getSwitch(swi[swlink]);
    	pushtable(sw,pi,pi.getInPort(),outport,cntx);
    	
    }
	public void pushroute(int srcswitch,int dstswitch,short outport, OFPacketIn pi, FloodlightContext cntx)
    {
		//System.out.println("*****************");
    	//System.out.print("push start");
    	//System.out.print("src:"+srcswitch+" dst "+dstswitch);
    	if(srcswitch == dstswitch&&outport!=pi.getInPort())
    	{
    		IOFSwitch sw = floodlightProvider.getSwitch(srcswitch);  
    		pushtable(sw,pi,(short)pi.getInPort(),outport,cntx);
    		return;
    	}
    	if(map[srcswitch][dstswitch] != notconnect)
    	{
    		
    		IOFSwitch sw = floodlightProvider.getSwitch(srcswitch);   		
    		pushtable(sw,pi,(short)pi.getInPort(),(short)map[srcswitch][dstswitch],cntx);
    		IOFSwitch sw1 = floodlightProvider.getSwitch(dstswitch);
    		if (sw1.getId() == 4)
    		{
    			//System.out.print(outport+" "+pi.getInPort());
    		}
    		pushtable(sw1,pi,pi.getInPort(),outport,cntx);
    		return;
    	}
    	//int []swi = dij.getroute(srcswitch, dstswitch);
    	int []swi = ant.getRoute(srcswitch, dstswitch, daijia);
    	int swlink = swi.length - 1;
    	
    	//pushtable(floodlightProvider.getSwitch(4),pi,pi.getInPort(),(short)4,cntx);
    	//pushtable(floodlightProvider.getSwitch(5),pi,pi.getInPort(),(short)4,cntx);
    	//pushtable(floodlightProvider.getSwitch(7),pi,pi.getInPort(),(short)2,cntx);
    	//OFMatch match = new OFMatch();
    	for(int i = 0;i< swlink ;i++)
    	{
    		
    		System.out.print(swi[i]+"  ");
    		IOFSwitch sw = floodlightProvider.getSwitch(swi[i]);
    		int left = swi[i];
    		int right = swi[i+1];
    		short port = map[left][right];
    		short inport = map[right][left];
    		pushtable(sw,pi,inport,port,cntx);
    	}
    	IOFSwitch sw1 = floodlightProvider.getSwitch(dstswitch);
    	pushtable(sw1,pi,pi.getInPort(),outport,cntx);
    	//System.out.println("*****************");
    	//System.out.print(sw1.getId());
    }
	public void pushtable(IOFSwitch sw, OFPacketIn pi,short inport, short outport, FloodlightContext cntx) {
        // initialize match structure and populate it using the packet
		
        OFMatch match = new OFMatch();          
        match.loadFromPacket(pi.getPacketData(), pi.getInPort());     
        // Create flow-mod based on packet-in and src-switch
        OFFlowMod fm =
                (OFFlowMod) floodlightProvider.getOFMessageFactory()
                                              .getMessage(OFType.FLOW_MOD);  
        List<OFAction> actions = new ArrayList<OFAction>(); // Set no action to
        OFActionOutput output = new OFActionOutput();    
        actions.add(output);
        // drop
        //long cookie = AppCookie.makeCookie(3, 0);
        long cookie = 007;
        fm.setCookie(cookie)
          .setHardTimeout((short) 0)
          .setIdleTimeout((short) 5)
          .setBufferId(OFPacketOut.BUFFER_ID_NONE)
          .setMatch(match)
          .setActions(actions)
          .setCommand(OFFlowMod.OFPFC_ADD)
          .setLengthU(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);       
        ((OFActionOutput)(actions.get(0))).setPort(outport);
        //fm.getMatch().setInputPort(inport);
     	try 
     	{
     		
			//System.out.print(sw.getId());
			messageDamper.write(sw, fm, cntx);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
   	   try {
           fm = fm.clone();
       } catch (CloneNotSupportedException e) {
           //log.error("Failure cloning flow mod", e);
       }
        
	}
	/********************************IFloodlightModule********************************/
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
    }

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IDeviceService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		vips = new HashMap<String, LBVip>();
		vipIpToId = new HashMap<Integer, String>();
		//
		messageDamper = new OFMessageDamper(OFMESSAGE_DAMPER_CAPACITY, 
                 EnumSet.of(OFType.FLOW_MOD),
                 OFMESSAGE_DAMPER_TIMEOUT);		
		routingEngine = context.getServiceImpl(IRoutingService.class);
		topology = context.getServiceImpl(ITopologyService.class);
		//topology.
		topology.addListener(this);
		for(int i = 0; i<maplength; i ++)
		{
			for(int j = 0;j<maplength; j++)
				map[i][j] = notconnect;
		}
		//
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// always place firewall in pipeline at bootup
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.PACKET_OUT, this);
        
//        new Thread(new Runnable() {
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				while(true)
//				{
//					pushdelete();
//					try {
//						Thread.sleep(9000000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
//		}).start();
        
        Consumer comsumer = new Consumer(this);
        Consumer_test comsumer_test = new Consumer_test (this);
        Thread t1 = new Thread(comsumer); 
        Thread t2 = new Thread(comsumer_test); 
        //t1.start(); 
        t2.start();
	}
	/******************************IFloodlightModule OVER******************************/
	
	private void arpreply2(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx,String mac)
	{
		//arp.setTargetProtocolAddress(Ethernet.toMACAddress("00:00:00:00:00"));
		 Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                 IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		 // retrieve original arp to determine host configured gw IP address                                          
		 if (! (eth.getPayload() instanceof ARP))
			 return;
		 ARP arpRequest = (ARP) eth.getPayload();
		 // have to do proxy arp reply since at this point we cannot determine the requesting application type
		 byte[] vipProxyMacBytes = Ethernet.toMACAddress(mac);//vips.get(vipId).proxyMac.toBytes();
		 // generate proxy ARP reply
		 IPacket arpReply = new Ethernet()
		 	.setSourceMACAddress(vipProxyMacBytes)
		 	.setDestinationMACAddress(eth.getSourceMACAddress())
		 	.setEtherType(Ethernet.TYPE_ARP)
		 	.setVlanID(eth.getVlanID())
		 	.setPriorityCode(eth.getPriorityCode())
		 	.setPayload(
		 			new ARP()
		 			.setHardwareType(ARP.HW_TYPE_ETHERNET)
		 			.setProtocolType(ARP.PROTO_TYPE_IP)
		 			.setHardwareAddressLength((byte) 6)
		 			.setProtocolAddressLength((byte) 4)
		 			.setOpCode(ARP.OP_REPLY)
		 			.setSenderHardwareAddress(vipProxyMacBytes)
		 			.setSenderProtocolAddress(
		 					arpRequest.getTargetProtocolAddress())
		 					.setTargetHardwareAddress(
		 							eth.getSourceMACAddress())
		 							.setTargetProtocolAddress(
		 									arpRequest.getSenderProtocolAddress()));

		 // push ARP reply out
		 pushPacket(arpReply, sw,OFPacketOut.BUFFER_ID_NONE, OFPort.OFPP_NONE.getValue(),
				 	pi.getInPort(), cntx, true);
		 //log.debug("proxy ARP reply pushed as {}", IPv4.fromIPv4Address(vips.get(vipId).address));

		 return;
	}
	private Command processPacketIn(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                                                      IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		IPacket pkt = eth.getPayload();
		
		//if (eth.isBroadcast() || eth.isMulticast()) 
		{
			// handle ARP for VIP
			if (pkt instanceof ARP) {
			
				// retrieve arp to determine target IP address                                                       
				ARP arpRequest = (ARP) eth.getPayload();

				int targetProtocolAddress = IPv4.toIPv4Address(arpRequest.getTargetProtocolAddress());
				if(IPv4.fromIPv4Address(targetProtocolAddress).equals("10.0.0.1"))
				{
					arpreply2(sw, pi, cntx, "72:a3:cd:2f:ff:c8");
					return Command.STOP;
				}
				if(IPv4.fromIPv4Address(targetProtocolAddress).equals("10.0.0.2"))
				{
					arpreply2(sw, pi, cntx, "4e:b3:45:42:21:ed");
					return Command.STOP;
				}
				System.out.print("arp");
//				if (vipIpToId.containsKey(targetProtocolAddress)) {
//					String vipId = vipIpToId.get(targetProtocolAddress);
//					vipProxyArpReply(sw, pi, cntx, vipId);	 // 代理回复ARP消息
//					return Command.STOP;
//				}
				
				return Command.CONTINUE;
			}
		} 
		
		//else 
		
			// currently only load balance IPv4 packets - no-op for other traffic 
			if (pkt instanceof IPv4) {
				System.out.print("ipv4");
				IPv4 ip_pkt = (IPv4) pkt;
				//byt = byt + ((double)pi.getLength())/1024;
				//System.out.print(byt+"KB"+"\n");
				
				
				/******************************ymx******************************/
				IDevice dstDevice =
        		                IDeviceService.fcStore.
        		                   get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
				if(dstDevice == null)
					return Command.CONTINUE;
				SwitchPort[] dsp = dstDevice.getAttachmentPoints();
				for (int i=0;i<dsp.length;i++)
				{
					System.out.println(dsp[i]);
					System.out.println(dsp[i].getSwitchDPID());
				}
				IOFSwitch msw = floodlightProvider.getSwitch(dsp[0].getSwitchDPID());
				//short port = (short) dsp[0].getPort();
        		int bufferId  = pi.getBufferId();///pi.getBufferId();
                short inPort  = pi.getInPort();
        		short outPort = (short) dsp[0].getPort();
        		pushPktInfo info = new pushPktInfo();
        		info.sw = msw;
        		info.bufferId = bufferId;
        		info.inPort = inPort;
        		info.outPort = outPort;
        		info.cntx = cntx;
        		info.length = pi.getPacketData().length;
        		//pushPacket(pkt, msw, bufferId, inPort, outPort, cntx,true);
        		System.out.println("******************************ymx******************************");
        		
        		/******************************ymx******************************/
				// If match Vip and port, check pool and choose member
				int destIpAddress = ip_pkt.getDestinationAddress();
				
				if (ip_pkt.getPayload() instanceof TCP) {
					System.out.println("TCP-------------------TCP");
            		TCP tcp_pkt = (TCP) ip_pkt.getPayload();
            		count.add(pi, Counter.Tcpflow);
            		TCP_pktQueue.offer(pkt);	//插入一个元素 
            		TCP_infoQueue.offer(info);
        		}
        		if (ip_pkt.getPayload() instanceof UDP) {
            		UDP udp_pkt = (UDP) ip_pkt.getPayload();
            		//System.out.println("-UDP-");
            		count.add(pi, Counter.Udpflow);
        		}
        		if (ip_pkt.getPayload() instanceof ICMP) {
        			System.out.println("ICMP......ICMP");
        			ICMP_pktQueue.offer(pkt);	//插入到ICMP队列中
        			ICMP_infoQueue.offer(info);
        			count.add(pi, Counter.Icmpflow);
        		}
        		count.print();
        		return Command.STOP;
			
			
		}
		// bypass non-load-balanced traffic for normal processing (forwarding)
		return Command.CONTINUE;
	}
	
    
    /**
     * used to push any packet - borrowed routine from Forwarding
     * 
     * @param OFPacketIn pi
     * @param IOFSwitch sw
     * @param int bufferId
     * @param short inPort
     * @param short outPort
     * @param FloodlightContext cntx
     * @param boolean flush
     */    
   
    
    public Queue<IPacket> getTCPQueue(){
    	return this.TCP_pktQueue;
    }
    public Queue<IPacket> getICMPQueue(){
    	return this.ICMP_pktQueue;
    }
    public Queue<pushPktInfo> getTCPinfoQueue(){
    	return this.TCP_infoQueue;
    }
    public Queue<pushPktInfo> getICMPinfoQueue(){
    	return this.ICMP_infoQueue;
    }

	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		// TODO Auto-generated method stub
		for(int i = 0;i < linkUpdates.size(); i++)
		{
			//if(linkUpdates.get(i).getOperation() == ILinkDiscovery.UpdateOperation.PORT_UP)
			{   
				if(linkUpdates.get(i).getSrc()==0||linkUpdates.get(i).getDst()==0)
					continue;
				if(map[(int) linkUpdates.get(i).getSrc()][(int) linkUpdates.get(i).getDst()] == notconnect)
					{
					ant.addlink((int) linkUpdates.get(i).getSrc(), (int) linkUpdates.get(i).getDst(), daijia);
					dij.addlink((int) linkUpdates.get(i).getSrc(), (int) linkUpdates.get(i).getDst(), daijia);
					}
				map[(int) linkUpdates.get(i).getSrc()][(int) linkUpdates.get(i).getDst()] = linkUpdates.get(i).getSrcPort();
				map[(int) linkUpdates.get(i).getDst()][(int) linkUpdates.get(i).getSrc()] = linkUpdates.get(i).getDstPort();
				
				
			}
			if(linkUpdates.get(i).getOperation() == ILinkDiscovery.UpdateOperation.LINK_REMOVED)
			{
				//map[(int) linkUpdates.get(i).getSrc()][(int) linkUpdates.get(i).getDst()] = notconnect;
				//map[(int) linkUpdates.get(i).getDst()][(int) linkUpdates.get(i).getSrc()] = notconnect;
			}
		}
	}
}
