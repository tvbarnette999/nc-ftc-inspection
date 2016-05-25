import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DNS {
	private static DNS dns=new DNS();
	private boolean stopped=false;
	private DNS(){
		
	}
	public static void startDNS(){
		dns.start();
	}
	public static void stopDNS(){
		dns.stopped=true;
	}
	private void start(){
		Thread dnsThread=new Thread("DNS"){
			public void run(){
				DatagramSocket serv = null;
				try {
					serv = new DatagramSocket(53);
				} catch (SocketException e1) {
					e1.printStackTrace();
				}
				byte[] data=new byte[512];
				DatagramPacket dgp=new DatagramPacket(data,data.length);
				while(!stopped){
						
					try {
						serv.receive(dgp);
						handleRequest(data);
						serv.send(new DatagramPacket(data,data.length,dgp.getAddress(),dgp.getPort()));
						data=new byte[512];
						dgp=new DatagramPacket(data,data.length);
					} catch (Exception e) {
						//e.printStackTrace();
					}					
				}
			}
		};

		//dnsThread.setDaemon(true);
		dnsThread.start();
	}
	private void handleRequest(byte[] data) {
		//0,1 are random identifiers-dont touch
		if((data[2] & 0x80) >0){
			System.out.println("Got response?");
			return;
		}
		data[2] |= 0x80; //set type to response
		//if error set data[3][5:8]
		int QC=(Byte.toUnsignedInt(data[4])<<8)+ Byte.toUnsignedInt(data[5]);
		System.out.println(QC+" Questions");
		//data[6:11] should be 0 in request 6:7 is ANSWER COUNT 
		//set data[7] to 1
		data[7]=1;
		
		//parse question
		String add="";
		int i=12;
		for(int length=0;(length=Byte.toUnsignedInt(data[i]))!=0;i+=length+1){
			System.out.println(length);
			byte[] temp=new byte[length];
			System.arraycopy(data,i+1, temp, 0, length);
			add+=new String(temp)+".";
		}
		add=add.substring(0,add.length()-1);//remove last .
		System.out.println(add);
		int QType=Byte.toUnsignedInt(data[i])+Byte.toUnsignedInt(data[i+1]);
		int QClass=Byte.toUnsignedInt(data[i+2])+Byte.toUnsignedInt(data[i+3]);
		System.out.println(QType+" "+QClass);
				//put answer in:
		System.arraycopy(data, 12, data, i+5, i-12+5);//+1 for end +4 for types
		i=(i+i-12+5+5);//set i to index of TTL
		i+=2;

		data[i]=0x07;
		data[i+1]=(byte) 0x08;//0x708=1800 seconds=1 hour
		
		data[i+2]=0;
		data[i+3]=4;
		
		try {
			i+=4;
			String host=InetAddress.getLocalHost().getHostAddress();
			System.out.println(host);
			for(String s:host.split("\\.")){
				int part=Integer.parseInt(s);				
				data[i]=(byte)part;
				i++;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		//for(int x=i-8;x<i+10;x++)System.out.println(data[x]);
	}
}
