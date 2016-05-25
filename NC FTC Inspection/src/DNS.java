import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

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
					} catch (IOException e) {
						e.printStackTrace();
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
		//for(int i=13;i<40;i++)System.out.println(data[i]);
	}
}
