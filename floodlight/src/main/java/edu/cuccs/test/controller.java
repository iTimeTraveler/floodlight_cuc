package edu.cuccs.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;
import org.openflow.protocol.OFFlowRemoved.OFFlowRemovedReason;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerAddress;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionTransportLayerDestination;
import org.openflow.protocol.action.OFActionType;
import org.openflow.protocol.factory.OFActionFactory;
import org.python.apache.xerces.impl.xpath.regex.Match;

import com.google.common.hash.BloomFilter;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.counter.CounterStore;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.OFMessageDamper;

public class controller implements IFloodlightModule, IOFMessageListener {
	private IFloodlightProviderService floodlightProvider;
	private CounterStore packetinconter;
	// private OFFlowMod fm;
	protected IStaticFlowEntryPusherService sfp;
	protected OFMessageDamper messageDamper;
	protected boolean flag;
	// protected <String> ips;
	protected String ip;
	private ITopologyService topology;

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "flowcontroller";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub

		return (name.equals("loadbalancer") || name.equals("forwarding"));
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		IRoutingDecision decision = null;
		decision = IRoutingDecision.rtStore.get(cntx,
				IRoutingDecision.CONTEXT_DECISION);
		return mydealformsg(sw, msg, cntx, decision);
	}

	protected void doDropFlow(IOFSwitch sw, OFPacketIn pi,
			IRoutingDecision decision, FloodlightContext cntx) {

	}

	private void pushsetflow(OFMatch match, IOFSwitch sw, OFPacketIn pi,
			FloodlightContext cntx, IRoutingDecision decision, String setip,
			short outport, String setsorce)// ( , FloodlightContext cntx ,
											// OFPacketIn inmsg)
	{
		// OFMatch match = new OFMatch();
		// match.loadFromPacket(pi.getPacketData(), pi.getInPort());

		OFFlowMod fm = (OFFlowMod) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.FLOW_MOD);
		List<OFAction> actions = new ArrayList<OFAction>(); // Set no action to

		// OFActionDataLayerDestination actiond = new
		// OFActionDataLayerDestination(IPv4.toIPv4AddressBytes("10.0.0.6"));
		// actiond.setMaxLength((short)0xffff);
		// actions.add(actio
		// actiond.setDataLayerAddress(IPv4.toIPv4AddressBytes("10.0.0.2"));
		OFActionNetworkLayerDestination of = new OFActionNetworkLayerDestination(
				IPv4.toIPv4Address(setip));
		actions.add(of);
		OFActionNetworkLayerSource ofs = new OFActionNetworkLayerSource(
				IPv4.toIPv4Address(setsorce));
		actions.add(ofs);
		// OFActionTransportLayerDestination of = new ofa
		// OFActionOutput actionout = new OFActionOutput();
		// actionout.setMaxLength((short)0xffff);
		// actionout.setPort((short)2);
		// actions.add(actiond);
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
				.setLengthU(
						OFFlowMod.MINIMUM_LENGTH
								+ OFActionNetworkLayerDestination.MINIMUM_LENGTH
								+ OFActionOutput.MINIMUM_LENGTH
								+ OFActionNetworkLayerSource.MINIMUM_LENGTH);
		// OFActionDataLayerDestination.MINIMUM_LENGTH+
		// OFActionOutput.MINIMUM_LENGTH);
		// +OFActionOutput.MINIMUM_LENGTH);
		// int wildcard_hints;
		// if(decision != null)
		// {
		// wildcard_hints = decision.getWildcards();
		// }
		// else
		// {
		// wildcard_hints = ((Integer) sw
		// .getAttribute(IOFSwitch.PROP_FASTWILDCARDS))
		// .intValue()
		// & ~OFMatch.OFPFW_IN_PORT
		// & ~OFMatch.OFPFW_DL_VLAN
		// & ~OFMatch.OFPFW_DL_SRC
		// & ~OFMatch.OFPFW_DL_DST
		// & ~OFMatch.OFPFW_NW_SRC_MASK
		// & ~OFMatch.OFPFW_NW_DST_MASK;

		// }
		// match.setWildcards(wildcard_hints);
		fm.getMatch().setInputPort(pi.getInPort());

		// ((OFActionDataLayerDestination)fm.getActions().get(0)).setDataLayerAddress(IPv4.toIPv4AddressBytes("10.0.0.6"))
		// ;

		// ((OFActionOutput)fm.getActions().get(0)).setPort(outport);
		try {
			messageDamper.write(sw, fm, cntx);
			// if (doFlush) {
			// sw.flush();
			// counterStore.updateFlush();
			// }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fm = fm.clone();
		} catch (CloneNotSupportedException e) {
			// log.error("Failure cloning flow mod", e);
		}
	}

	private void arpreply(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		// arp.setTargetProtocolAddress(Ethernet.toMACAddress("00:00:00:00:00"));
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		// retrieve original arp to determine host configured gw IP address
		if (!(eth.getPayload() instanceof ARP))
			return;
		ARP arpRequest = (ARP) eth.getPayload();

		// have to do proxy arp reply since at this point we cannot determine
		// the requesting application type
		byte[] vipProxyMacBytes = Ethernet.toMACAddress("00:00:00:00:00:aa");// vips.get(vipId).proxyMac.toBytes();

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
		pushPacket(arpReply, sw, OFPacketOut.BUFFER_ID_NONE,
				OFPort.OFPP_NONE.getValue(), pi.getInPort(), cntx, true);
		// log.debug("proxy ARP reply pushed as {}",
		// IPv4.fromIPv4Address(vips.get(vipId).address));

		return;
	}

	public void pushPacket(IPacket packet, IOFSwitch sw, int bufferId,
			short inPort, short outPort, FloodlightContext cntx, boolean flush) {
		// if (log.isTraceEnabled()) {
		// log.trace("PacketOut srcSwitch={} inPort={} outPort={}",
		// new Object[] {sw, inPort, outPort});
		// }

		OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.PACKET_OUT);

		// set actions
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(outPort, (short) 0xffff));

		po.setActions(actions).setActionsLength(
				(short) OFActionOutput.MINIMUM_LENGTH);
		short poLength = (short) (po.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);

		// set buffer_id, in_port
		po.setBufferId(bufferId);
		po.setInPort(inPort);

		// set data - only if buffer_id == -1
		if (po.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
			if (packet == null) {
				// log.error("BufferId is not set and packet data is null. " +
				// "Cannot send packetOut. " +
				// "srcSwitch={} inPort={} outPort={}",
				// new Object[] {sw, inPort, outPort});
				return;
			}
			byte[] packetData = packet.serialize();
			poLength += packetData.length;
			po.setPacketData(packetData);
		}

		po.setLength(poLength);

		try {
			// counterStore.updatePktOutFMCounterStoreLocal(sw, po);
			messageDamper.write(sw, po, cntx, flush);
		} catch (IOException e) {
			// log.error("Failure writing packet out", e);
		}
	}

	private Command mydealformsg(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx, IRoutingDecision decision) {
		switch (msg.getType()) {
		case PACKET_IN:
			// System.out.print(IPv4.toIPv4AddressBytes("10.0.0.6"));
			OFPacketIn pi = (OFPacketIn) msg;

			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
					IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			OFMatch match = new OFMatch();
			match.loadFromPacket(pi.getPacketData(), pi.getInPort());
			// pushsetflow(match , sw, pi, cntx, decision, "10.0.0.6");
			// chooseflowtable(sw, pi, decision, cntx, pi);
			if (eth.getPayload() instanceof ARP) {
				ARP arp = (ARP) eth.getPayload();
				int targetProtocolAddress = IPv4.toIPv4Address(arp
						.getTargetProtocolAddress());

				if (IPv4.fromIPv4Address(targetProtocolAddress).equals(
						"10.0.0.200"))
					;
				{
					System.out.print("arp reply");
					arpreply(sw, pi, cntx);
					return Command.STOP;
				}
			}

			if (eth.getPayload() instanceof IPv4) {

				IPv4 ip = (IPv4) eth.getPayload();
				System.out.print(ip.getDestinationAddress());
				if (IPv4.fromIPv4Address(ip.getDestinationAddress()).equals(
						"10.0.0.200")) {
					System.out.print("action set");
					flag = !flag;
					// if(flag)
					// int wildcard_hints;
					{
						// wildcard_hints = ((Integer) sw
						// .getAttribute(IOFSwitch.PROP_FASTWILDCARDS))
						// .intValue()
						// & ~OFMatch.OFPFW_IN_PORT
						// & ~OFMatch.OFPFW_DL_VLAN
						// & ~OFMatch.OFPFW_DL_SRC
						// & ~OFMatch.OFPFW_DL_DST
						// / & ~OFMatch.OFPFW_NW_SRC_MASK
						// & ~OFMatch.OFPFW_NW_DST_MASK
						// ;
						// match.setWildcards(wildcard_hints);
						// match.setNetworkDestination(IPv4.toIPv4Address("10.0.0.200"));
						// match.setNetworkSource(IPv4.toIPv4Address("10.0.0.3"));
						pushsetflow(match, sw, pi, cntx, decision, "10.0.0.1",
								(short) 1, "10.0.0.2");
						OFMatch ofma = new OFMatch();
						// ofma.loadFromPacket(pi, inputPort)

						ofma.setInputPort((short) 2);
						ofma.setNetworkDestination(IPv4
								.toIPv4Address("10.0.0.2"));
						pushsetflow(ofma, sw, pi, cntx, decision, "10.0.0.2",
								(short) 2, "10.0.0.200");

					}

					// else
					{
						// pushsetflow(sw, pi, cntx, decision, "10.0.0.2");
					}
					return Command.CONTINUE;
				}

			}
			/**/
			return Command.STOP;
			// return Command.CONTINUE;
		case FLOW_REMOVED: {
			OFFlowRemoved rmsg = (OFFlowRemoved) msg;
			if (rmsg.getReason() == OFFlowRemovedReason.OFPRR_HARD_TIMEOUT
					|| rmsg.getReason() == OFFlowRemovedReason.OFPRR_IDLE_TIMEOUT) {
				// rmsg.
				// fm.setCookie(rmsg.getCookie());
				// sfp.addFlow("flowcontroller", fm , sw.toString());
			}
			return Command.CONTINUE;
		}
		case FLOW_MOD: {
			break;
		}
		default:
			break;
		}
		return Command.CONTINUE;
	}

	private void gettopo() {

	}

	private void pushoutportflowtable(OFMatch match, IOFSwitch sw,
			OFPacketIn pi, short inport, short outport, FloodlightContext cntx,
			IRoutingDecision decision)// ( , FloodlightContext cntx , OFPacketIn
										// inmsg)
	{
		// OFMatch match = new OFMatch();

		// match.loadFromPacket(pi.getPacketData(), pi.getInPort());
		OFFlowMod fm = (OFFlowMod) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.FLOW_MOD);
		List<OFAction> actions = new ArrayList<OFAction>(); // Set no action to
		OFActionOutput action = new OFActionOutput();
		action.setMaxLength((short) 0xffff);
		actions.add(action);
		// OFActionDataLayerDestination action2 = new
		// OFActionDataLayerDestination();
		// actions.add(acion2);
		// long cookie = AppCookie.makeCookie(2, 0);
		long cookie = 0 << 52;
		fm.setCookie(cookie)
				.setHardTimeout((short) 0)
				.setIdleTimeout((short) 5)
				.setBufferId(OFPacketOut.BUFFER_ID_NONE)
				.setMatch(match)
				.setActions(actions)
				.setCommand(OFFlowMod.OFPFC_ADD)
				.setLengthU(
						OFFlowMod.MINIMUM_LENGTH
								+ OFActionOutput.MINIMUM_LENGTH);
		int wildcard_hints;
		if (decision != null) {
			wildcard_hints = decision.getWildcards();
		} else {
			wildcard_hints = ((Integer) sw
					.getAttribute(IOFSwitch.PROP_FASTWILDCARDS)).intValue()
					& ~OFMatch.OFPFW_IN_PORT
					& ~OFMatch.OFPFW_DL_VLAN
					& ~OFMatch.OFPFW_DL_SRC
					& ~OFMatch.OFPFW_DL_DST
					& ~OFMatch.OFPFW_NW_SRC_MASK & ~OFMatch.OFPFW_NW_DST_MASK;

		}
		match.setWildcards(wildcard_hints);
		fm.getMatch().setInputPort(inport);
		((OFActionOutput) fm.getActions().get(0)).setPort(outport);
		try {
			messageDamper.write(sw, fm, cntx);
			// if (doFlush) {
			// sw.flush();
			// counterStore.updateFlush();
			// }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fm = fm.clone();
		} catch (CloneNotSupportedException e) {
			// log.error("Failure cloning flow mod", e);
		}
	}

	protected void doFlood(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		if (topology.isIncomingBroadcastAllowed(sw.getId(), pi.getInPort()) == false) {
			// if (log.isTraceEnabled()) {
			// log.trace("doFlood, drop broadcast packet, pi={}, " +
			// "from a blocked port, srcSwitch=[{},{}], linkInfo={}",
			// new Object[] {pi, sw.getId(),pi.getInPort()});
			// }
			return;
		}

		// Set Action to flood
		OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.PACKET_OUT);
		List<OFAction> actions = new ArrayList<OFAction>();
		if (sw.hasAttribute(IOFSwitch.PROP_SUPPORTS_OFPP_FLOOD)) {
			actions.add(new OFActionOutput(OFPort.OFPP_FLOOD.getValue(),
					(short) 0xFFFF));
		} else {
			actions.add(new OFActionOutput(OFPort.OFPP_ALL.getValue(),
					(short) 0xFFFF));
		}
		po.setActions(actions);
		po.setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

		// set buffer-id, in-port and packet-data based on packet-in
		short poLength = (short) (po.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);
		po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		po.setInPort(pi.getInPort());
		byte[] packetData = pi.getPacketData();
		poLength += packetData.length;
		po.setPacketData(packetData);
		po.setLength(poLength);

		try {
			// if (log.isTraceEnabled()) {
			// log.trace("Writing flood PacketOut switch={} packet-in={} packet-out={}",
			// new Object[] {sw, pi, po});
			// }
			messageDamper.write(sw, po, cntx);
		} catch (IOException e) {
			// log.error("Failure writing PacketOut switch={} packet-in={} packet-out={}",
			// new Object[] {sw, pi, po}, e);
		}

		return;
	}

	private void chooseflowtable(IOFSwitch sw, OFPacketIn pi,
			IRoutingDecision decision, FloodlightContext cntx, OFPacketIn inmsg) {
		OFMatch match = new OFMatch();
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());
		IDevice dstDevice = IDeviceService.fcStore.get(cntx,
				IDeviceService.CONTEXT_DST_DEVICE);
		// pushoutportflowtable(sw, pi, (short)1, (short)2, cntx, decision);
		if (dstDevice != null) {
			IDevice srcDevice = IDeviceService.fcStore.get(cntx,
					IDeviceService.CONTEXT_SRC_DEVICE);
			Long srcIsland = topology.getL2DomainId(sw.getId());

			if (srcDevice == null) {
				// log.debug("No device entry found for source device");
				return;
			}
			if (srcIsland == null) {
				// log.debug("No openflow island found for source {}/{}",
				// sw.getStringId(), pi.getInPort());
				return;
			}

			for (SwitchPort dstDap : dstDevice.getAttachmentPoints()) {
				long dstSwDpid = dstDap.getSwitchDPID();
				Long dstIsland = topology.getL2DomainId(dstSwDpid);
				if (dstDap.getPort() == 3 || dstDap.getPort() == 2)
					pushoutportflowtable(match, sw, pi, pi.getInPort(),
							(short) 1, cntx, decision);
				else {
					if (flag)
						pushoutportflowtable(match, sw, pi, pi.getInPort(),
								(short) 2, cntx, decision);
					else
						pushoutportflowtable(match, sw, pi, pi.getInPort(),
								(short) 3, cntx, decision);
				}
				// pushoutportflowtable(, pi,
				// (short)dstDap.getPort(),pi.getInPort(), cntx);
				System.out.print(dstDap.getPort());
				System.out.print(pi.getInPort());
				// if ((dstIsland != null) && dstIsland.equals(srcIsland)) {
				// on_same_island = true;
				// if ((sw.getId() == dstSwDpid) &&
				// (pi.getInPort() == dstDap.getPort())) {
				// on_same_if = true;
				// }
				// break;
			}
		} else
			doFlood(sw, pi, cntx);
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(ICounterStoreService.class);
		l.add(IStaticFlowEntryPusherService.class);
		l.add(ITopologyService.class);
		l.add(IDeviceService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		flag = true;
		floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		messageDamper = new OFMessageDamper(10000, EnumSet.of(OFType.FLOW_MOD),
				250);
		this.topology = context.getServiceImpl(ITopologyService.class);
		sfp = context.getServiceImpl(IStaticFlowEntryPusherService.class);
		packetinconter = new CounterStore();
		packetinconter.createCounter("packetin",
				net.floodlightcontroller.counter.CounterValue.CounterType.LONG);
		flag = false;
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		floodlightProvider.addOFMessageListener(OFType.GET_CONFIG_REQUEST , this);
	}

}
