package edu.cuccs.test;

import java.util.Calendar;
import java.util.Queue;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.IPacket;

public class Consumer_test implements Runnable{
	protected Queue<IPacket> TCP_pktQueue = null; 
    protected Queue<IPacket> ICMP_pktQueue = null;
    protected Queue<pushPktInfo> TCP_infoQueue = null;  
    protected Queue<pushPktInfo> ICMP_infoQueue = null; 
    protected Counter count = new Counter();
    protected xwlTest xwltest;
    
    public Consumer_test(xwlTest xwltest) { 
        this.xwltest = xwltest;
        this.TCP_pktQueue = xwltest.getTCPQueue();
        this.ICMP_pktQueue = xwltest.getICMPQueue();
        this.TCP_infoQueue = xwltest.getTCPinfoQueue();
        this.ICMP_infoQueue = xwltest.getICMPinfoQueue();
    }
    
	@Override
	public void run() {
		Calendar calendar = Calendar.getInstance();	//获取当前时间
		long start = calendar.getTimeInMillis();
		boolean flag = false;
		
		while(true){
			Calendar calendar3 = Calendar.getInstance();	//获取当前时间
			long mark = calendar3.getTimeInMillis();
			if((mark - start) >= 20000){
				
				System.out.println("-------------------------------------------------------------");
				System.out.println("\t  HTTP \t Ping");
				System.out.println("In\t"+xwltest.count.getTCPbyte() + "\t" +xwltest.count.getICMPbyte());
				System.out.println("Out\t"+count.getTCPbyte() + "\t" +count.getICMPbyte());
				System.out.println("-------------------------------------------------------------");
				
				//System.out.println("----------------out.getallbyte():"+count.getallbyte());
				//System.out.println("----------------in.getallbyte():"+xwltest.count.getallbyte());
				start = mark;
			}
			
			try { 
                Thread.sleep(500); 
            } catch (InterruptedException e) { 
                return; 
            } 
			TCP_pktQueue = xwltest.getTCPQueue();
	        ICMP_pktQueue = xwltest.getICMPQueue();
	        TCP_infoQueue = xwltest.getTCPinfoQueue();
	        ICMP_infoQueue = xwltest.getICMPinfoQueue();
	        
	        if(flag == true){
	        	if(TCP_pktQueue == null || TCP_pktQueue.isEmpty()){
	        		flag = !flag;
					continue;
				}
				//System.out.println("TCP_output>>"+TCP_pktQueue.peek());
				IPacket packet = TCP_pktQueue.peek();
				pushPktInfo info = TCP_infoQueue.peek();
                IOFSwitch sw = info.sw;
                int bufferId = info.bufferId;
                short inPort = info.inPort;
                short outPort = info.outPort;
                FloodlightContext cntx = info.cntx;
                
				xwltest.pushPacket(packet, sw, bufferId, inPort, outPort, cntx, true); 	//发包
				
				count.add(info.length, Counter.Tcpflow);
				TCP_pktQueue.poll();
                TCP_infoQueue.poll();
	        }
	        if(flag == false){
	        	if(ICMP_pktQueue == null || ICMP_pktQueue.isEmpty()){
	        		flag = !flag;
					continue;
				}
				//System.out.println("ICMP_output>>"+ICMP_pktQueue.peek());
				IPacket packet = ICMP_pktQueue.peek();
				pushPktInfo info = ICMP_infoQueue.peek();
                IOFSwitch sw = info.sw;
                int bufferId = info.bufferId;
                short inPort = info.inPort;
                short outPort = info.outPort;
                FloodlightContext cntx = info.cntx;
                
				xwltest.pushPacket(packet, sw, bufferId, inPort, outPort, cntx, true); 	//发包
				
				count.add(info.length, Counter.Icmpflow);
				ICMP_pktQueue.poll();
				ICMP_infoQueue.poll();
	        }
	        flag = !flag;
			
		}
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
							Thread.sleep(9000000);
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
			 System.out.println("icmp包："+icmpcounter +"  icmp流量：" +Icmpbyt + "KB\n");
			 
		 }
		 public void add(double length,int type)
		 {
			 if(type == Tcpflow)
				 {Tcpbyt = Tcpbyt+length;counter++;tcpcounter++;}
			 else if(type == Udpflow)
				 {Udpbyt = Udpbyt + length;counter++;udpcounter++;}
			 else if(type == Icmpflow)
				 {Icmpbyt = Icmpbyt +length;counter++;icmpcounter++;}
		 }
		 public double getallbyte()
		 {
			 return (Tcpbyt+Udpbyt+Icmpbyt);
		 }
		 public double getTCPbyte(){
			 return Tcpbyt;
		 }
		 public double getICMPbyte(){
			 return Icmpbyt;
		 }
		 
	}
	
}
