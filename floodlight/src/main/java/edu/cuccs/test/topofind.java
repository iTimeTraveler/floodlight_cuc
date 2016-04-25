package edu.cuccs.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
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





import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.OFMessageDamper;


public class topofind implements IFloodlightModule,ITopologyListener,ILinkDiscoveryListener{
//@Override    
	protected IFloodlightProviderService floodlightProvider;
	protected ITopologyService topology;
	protected ILinkDiscoveryService linkdiscover;
	protected ITopologyService itopo;
	Map<Link, LinkInfo> link;
	static short  notconnect = -1;
	static int daijia = 100;
	static int maplength = 16;
	protected OFMessageDamper messageDamper;
	short[][] map = new short [maplength][maplength];
	Dijkstra dij = new Dijkstra();
	boolean flag = true;
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}
	public void pushsetnewtable()
	{
		
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
	private void pushsetflow(OFMatch match,IOFSwitch sw,OFPacketIn pi, FloodlightContext cntx , IRoutingDecision decision, String setip ,short outport,String  setsorce)//( , FloodlightContext cntx , OFPacketIn inmsg)
	{
	
		OFFlowMod fm =
                (OFFlowMod) floodlightProvider.getOFMessageFactory()
                                              .getMessage(OFType.FLOW_MOD);
       List<OFAction> actions = new ArrayList<OFAction>(); // Set no action to
         
       OFActionNetworkLayerDestination of =new OFActionNetworkLayerDestination(IPv4.toIPv4Address(setip));
       actions.add(of);
       OFActionNetworkLayerSource ofs = new OFActionNetworkLayerSource(IPv4.toIPv4Address(setsorce));
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
        		  OFActionNetworkLayerDestination.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH
        		  +OFActionNetworkLayerSource.MINIMUM_LENGTH
        		 );
        		//  OFActionDataLayerDestination.MINIMUM_LENGTH+
        		 // OFActionOutput.MINIMUM_LENGTH);
        		  //+OFActionOutput.MINIMUM_LENGTH);
       //int wildcard_hints;
      // if(decision != null)
       //{
       //	 wildcard_hints = decision.getWildcards();
      // }
       //else
       //{
      // wildcard_hints = ((Integer) sw
       //        .getAttribute(IOFSwitch.PROP_FASTWILDCARDS))
       //        .intValue()
      //         & ~OFMatch.OFPFW_IN_PORT
       //        & ~OFMatch.OFPFW_DL_VLAN  
       //        & ~OFMatch.OFPFW_DL_SRC
       //        & ~OFMatch.OFPFW_DL_DST
       //        & ~OFMatch.OFPFW_NW_SRC_MASK
       //        & ~OFMatch.OFPFW_NW_DST_MASK;
       
       //}
       //match.setWildcards(wildcard_hints);
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
	private net.floodlightcontroller.core.IListener.Command 
	processPacketIn3(IOFSwitch sw, OFPacketIn pi,
            FloodlightContext cntx, IRoutingDecision decision) {
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
					if(IPv4.fromIPv4Address(targetProtocolAddress).equals("10.0.0.200"));
					     {
					    	 System.out.print("arp reply");
					    	 arpreply(sw, pi, cntx);
					    	 return Command.STOP;
					     }
				}
				else if(eth.getPayload() instanceof IPv4)
				{
					IPv4 ip = (IPv4)eth.getPayload();
					if(IPv4.fromIPv4Address(ip.getSourceAddress()).equals("10.0.0.1")||IPv4.fromIPv4Address(ip.getSourceAddress()).equals("10.0.0.2"))
					{
						pushchangesrc(match, sw, pi, cntx, (short)3, "10.0.0.200");
					}
					else if(IPv4.fromIPv4Address(ip.getDestinationAddress()).equals("10.0.0.200"))
					{
						if(flag)
							{
							pushchangedst("00:00:00:00:00:01",match, sw, pi, cntx, (short)1, "10.0.0.1");
							}
						else
							{
							pushchangedst("00:00:00:00:00:02",match, sw, pi, cntx, (short)2, "10.0.0.2");
							}
						flag = !flag;
						return Command.STOP;
					}
				}
				return Command.CONTINUE;
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
		 pushPacket(arpReply, sw, OFPacketOut.BUFFER_ID_NONE, OFPort.OFPP_NONE.getValue(),
				 	pi.getInPort(), cntx, true);
		 //log.debug("proxy ARP reply pushed as {}", IPv4.fromIPv4Address(vips.get(vipId).address));

		 return;
	}
	public void pushPacket(IPacket packet, 
            IOFSwitch sw,
            int bufferId,
            short inPort,
            short outPort, 
            FloodlightContext cntx,
            boolean flush) 
	{
		OFPort a;
		OFPacketOut po =
				(OFPacketOut) floodlightProvider.getOFMessageFactory()
                                 .getMessage(OFType.PACKET_OUT);
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(outPort, (short) 0xffff));
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
	private net.floodlightcontroller.core.IListener.Command 
	processPacketIn2(IOFSwitch sw, OFPacketIn pi,
            FloodlightContext cntx, IRoutingDecision decision) {
     Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                                              IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
     IPacket pkt = eth.getPayload();
     IDevice dstDevice =
             IDeviceService.fcStore.
                 get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
     SwitchPort[] dstDap = dstDevice.getAttachmentPoints();
     IOFSwitch sw1 = floodlightProvider.getSwitch(dstDap[0].getSwitchDPID());
     short outport = (short)dstDap[0].getPort();
     return Command.CONTINUE;
     }
	private net.floodlightcontroller.core.IListener.Command 
	processPacketIn(IOFSwitch sw, OFPacketIn pi,
            FloodlightContext cntx, IRoutingDecision decision) {
     Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
                                              IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
     IPacket pkt = eth.getPayload();
     IDevice dstDevice =
             IDeviceService.fcStore.
                 get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
     SwitchPort[] dstDap = dstDevice.getAttachmentPoints();
     if(pkt.getPayload() instanceof IPv4)
     {
     pushroute((int)sw.getId(),(int)dstDap[1].getSwitchDPID(), (short) dstDap[0].getPort() ,pi, cntx);	  
     return Command.STOP;
     }
     
     return Command.CONTINUE;
     }
    public void pushroute(int srcswitch,int dstswitch,short outport, OFPacketIn pi, FloodlightContext cntx)
    {
    	
    	int [] swi = dij.getroute(srcswitch, dstswitch);
    	if(swi == null)
    		return;
    	int swlink = swi.length - 1;
    	for(int i = 0;i<swlink;i++)
    	{
    		IOFSwitch sw = floodlightProvider.getSwitch(swi[i]);
    		short port = map[swi[swlink]][swi[swlink+1]];
    		pushtable(sw,pi,port,cntx);
    	}
    	IOFSwitch sw = floodlightProvider.getSwitch(swi[swi.length]);
    	pushtable(sw,pi,outport,cntx);
    	
    }
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l = 
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);	        
       // l.add(ICounterStoreService.class);	
       // l.add(IStaticFlowEntryPusherService.class);
        l.add(ITopologyService.class);
        l.add(ILinkDiscoveryService.class);
        //l.add(ITopologyListener.class);
       // l.add(IDeviceService.class);
        return l;
	}
    private void pushtables()
    {
    	//pushtables
    }
	@Override
	public void init(FloodlightModuleContext cntx)
			throws FloodlightModuleException {
		floodlightProvider = cntx.getServiceImpl(IFloodlightProviderService.class);
		topology = cntx.getServiceImpl(ITopologyService.class);
		//topology.
		topology.addListener(this);
		for(int i = 0; i<maplength; i ++)
		{
			for(int j = 0;j<maplength; j++)
				map[i][j] = notconnect;
		}
		messageDamper = new OFMessageDamper(10000,
                EnumSet.of(OFType.FLOW_MOD),
                250);
		
	}
	public void pushtable(IOFSwitch sw, OFPacketIn pi, short outport, FloodlightContext cntx) {
        // initialize match structure and populate it using the packet
        OFMatch match = new OFMatch();
        match.loadFromPacket(pi.getPacketData(), pi.getInPort());
        
        // Create flow-mod based on packet-in and src-switch
        OFFlowMod fm =
                (OFFlowMod) floodlightProvider.getOFMessageFactory()
                                              .getMessage(OFType.FLOW_MOD);
        
        List<OFAction> actions = new ArrayList<OFAction>(); // Set no action to
        OFActionOutput output = new OFActionOutput();    
        actions.add(output);// drop
        //long cookie = AppCookie.makeCookie(3, 0);
        long cookie = 007;
        fm.setCookie(cookie)
          .setHardTimeout((short) 0)
          .setIdleTimeout((short) 5)
          .setBufferId(OFPacketOut.BUFFER_ID_NONE)
          .setMatch(match)
          .setActions(actions)
          .setLengthU(OFFlowMod.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);       
        ((OFActionOutput)(actions.get(0))).setPort(outport);
     	try 
     	{
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
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		
		//link = linkdiscover.getLinks();
		//link.
		System.out.print("succece   ");
	} 
	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		// TODO Auto-generated method stub
		for(int i = 0;i < linkUpdates.size(); i++)
		{
			if(linkUpdates.get(i).getOperation() == ILinkDiscovery.UpdateOperation.PORT_UP)
			{
				map[(int) linkUpdates.get(i).getSrc()][(int) linkUpdates.get(i).getDst()] = linkUpdates.get(i).getSrcPort();
				map[(int) linkUpdates.get(i).getDst()][(int) linkUpdates.get(i).getSrc()] = linkUpdates.get(i).getDstPort();
				dij.addlink((int) linkUpdates.get(i).getSrc(), (int) linkUpdates.get(i).getDst(), daijia);
			}
			if(linkUpdates.get(i).getOperation() == ILinkDiscovery.UpdateOperation.PORT_DOWN)
			{
				map[(int) linkUpdates.get(i).getSrc()][(int) linkUpdates.get(i).getDst()] = notconnect;
				map[(int) linkUpdates.get(i).getDst()][(int) linkUpdates.get(i).getSrc()] = notconnect;
			}
		}
	}
    
	@Override
	public void linkDiscoveryUpdate(LDUpdate update) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
		// TODO Auto-generated method stub
		for(int i = 0;i < updateList.size(); i++)
		{
			if(updateList.get(i).getType() == ILinkDiscovery.LinkType.DIRECT_LINK)
			{
				
			}
		}
	}



	

}
