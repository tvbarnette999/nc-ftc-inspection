
package nc.ftc.inspection;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import nc.ftc.inspection.util.RedirectingPrintStream;
import nc.ftc.inspection.util.Resources;
import nc.ftc.inspection.util.URLMap;

public class Server {
	
	public static boolean DEBUG = false;
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat ("[hh:mm:ss] ");
	
	public static final String RED="\"#FF0000\"";
	public static final String GREEN="\"#00FF00\"";
	public static final String CYAN="\"#00FFFF\"";
	public static final String WHITE="\"#FFFFFF\"";
	
	public static final String TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";

	public static final int NO_DATA=0;
	public static final int PASS=3;
	public static final int FAIL=1;
	public static final int PROGRESS=2;	
	
//	public static final int LOGIN = 1;
	public static final int HARDWARE=2;
	public static final int SOFTWARE=3;
	public static final int FIELD=4;
	public static final int CUBE=5;
	public static final int CHECKIN=6;
	public static final int LOG_ERROR = 11;
	public static final int LOG_OUT = 26;
	public static final int LOG_COMM = 27;
	/*
	
	UHM PROBLEM! CMD HW DOESNT OVERRIDE FULL INSPECTION!!!!
	*/
	
	//These parameters are set to determine whether a given event will show status for that stage and do paperless inspection.
	
	//These are public because getters/setters for this with GUI would be way too much code.
	public static boolean trackCheckIn = false;
	public static boolean trackCube = false;
	public static boolean separateCube = false; //only relevant if trackCube
	public static boolean trackHardware = true;
	public static boolean fullHardware = true;
	public static boolean multiHardware = false; //default no
	
	public static boolean trackSoftware=true;
	public static boolean fullSoftware=true;
	public static boolean multiSoftware = false; //default no
	
	public static boolean trackField=true;
	public static boolean fullField=true;
	public static boolean multiField = true;
	
	private boolean done=false;
	
	//TODO add sections to the form data to handle new one, accommodate for dual columned entries like new SW
//	public static Vector<String> HWForm=new Vector<String>();
//	public static Vector<String> SWForm=new Vector<String>();
//	public static Vector<String> FDForm=new Vector<String>();
	
	public static InspectionForm hardwareForm = new InspectionForm(HARDWARE);
	public static InspectionForm softwareForm = new InspectionForm(SOFTWARE);
	public static InspectionForm fieldForm = new InspectionForm(FIELD);
	
	
	

	public static long seed = System.currentTimeMillis();
	public static final String password = "hello123";//"NCftc2016";

	public static String event = "KMS_17";
	public static String fullEventName;

	/**Thread pool for HTTP server*/
	private static ExecutorService threadPool;

	public Vector<Team> teams=new Vector<Team>();
	
	static Vector<String> statusLog=new Vector<String>();

	public static String cookieHeader="FTC_COOKIE=";

	public static Server theServer = new Server();
	
	public static Vector<InetAddress> whiteList = new Vector<InetAddress>();
	
	public URLMap urlMap;
	
	private PrintStream commStream;
	private String commFile;
	private Server(){		//Singleton
		
		try {
			commFile = "/log/" + Main.DATE_TIME_FORMAT.format(new Date()) + "_COMM.log";
			commStream = new PrintStream(Resources.getPrintStream(commFile));
		} catch (Exception e) {
			System.err.println("There was an error setting up the comm stream:");
			e.printStackTrace();
		}
		
		urlMap = new URLMap(this);
	}
	
	private byte[] hashedPass;
	private String hashedPassString;
	private String currentPasswordPlaintext;
	
	private int cookieCount;
	private int traffic;
	
	public void setPassword(String password) {
		currentPasswordPlaintext = password;
		cookieCount = 0;
		SecureRandom rand = new SecureRandom();
		ByteBuffer buff = ByteBuffer.allocate(Long.BYTES + password.getBytes().length);
		buff.putLong(seed);
		buff.put(password.getBytes());
		rand.setSeed(buff.array());
		hashedPass = new byte[32];
		rand.nextBytes(hashedPass);
		hashedPassString = "";
		for (Byte b : hashedPass)
			hashedPassString += (char) (((int)'a') + Math.abs(b) / 12);
		whiteList.clear();
	}
	public void refreshPassword() {
		seed = System.currentTimeMillis();
		setPassword(currentPasswordPlaintext);
	}
	public boolean checkPassword(String password) {
//		System.out.println("Check Password: " + password);
		SecureRandom rand = new SecureRandom();
		ByteBuffer buff = ByteBuffer.allocate(Long.BYTES + password.getBytes().length);
		buff.putLong(seed);
		buff.put(password.getBytes());
		rand.setSeed(buff.array());
		byte[] checkPass = new byte[hashedPass.length];
		rand.nextBytes(checkPass);
//		System.out.println(Arrays.toString(hashedPass));
//		System.out.println(Arrays.toString(checkPass));
//		System.out.println(Arrays.toString(hashedPass));
//		System.out.println(Arrays.toString(checkPass));
		for (int i = 0; i < checkPass.length; i++)
			if (checkPass[i] != hashedPass[i])
				return false;
		return true;
	}
	public int getTraffic() {
		int temp = traffic;
		traffic = 0;
		return temp;
	}
	public int getCookieCount() {
		return cookieCount;
	}
	public boolean checkHash(String checkPass) {
		System.out.println("CHECKING HASH");
		System.out.println();
		System.out.println(checkPass);
		System.out.println();
		System.out.println(hashedPassString);
		return hashedPassString.equals(checkPass);
	}
	public void sendNormalHeader(PrintWriter pw) {
		System.out.println("sending normal header");
//		pw = new PrintWriter(new OutputStreamWriter(out, "utf-8")); //TODO check me here: this is not a memory leak cuz closing a pw closes the underlying stream right?
		pw.print("HTTP/1.1 200 OK\nContent-Type: text/html; charset=utf-8\nCache-Control:no-store\n");
		pw.println();
		pw.println();
	}
	public void sendImageHeader(PrintWriter pw) {
		System.out.println("sending image header");
		pw.println("HTTP/1.1 200 OK");
		pw.println("Content-Type: image/x-icon");
	}
	public void sendPDFHeader(PrintWriter pw) {
		System.out.println("sending pdf header");
		pw.println("HTTP/1.1 200 OK");
		pw.println("Content-Type: application/pdf");
		pw.println("Content-Disposition: inline; filename=manual1.pdf");
	}
	public void send204Header(PrintWriter pw) {
		System.out.println("sending 204 header");
		pw.println("HTTP/1.1 204 No Content\n");
	}
	public void sendIPPage(Handler handler) {
		sendNormalHeader(handler.pw);
		handler.pw.println("<html><h2>Your IP is: " + handler.sock.getInetAddress().getHostAddress() + "</h2></html>");
	}
	public void sendInspectionTeamPage(Handler handler, String url) {
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		if (url.equals("hardware") || url.equals("hw")) {
			if (fullHardware) {
				sendInspectionTeamSelect(handler.pw, HARDWARE);
			} else if(trackHardware){
				sendInspectionEditPage(handler.pw, HARDWARE);
			}
		} else if (url.equals("software") || url.equals("sw")) {
			if (fullSoftware) {
				sendInspectionTeamSelect(handler.pw, SOFTWARE);
			} else if(trackSoftware){
				sendInspectionEditPage(handler.pw, SOFTWARE);
			}
		} else if (url.equals("field") || url.equals("fd")) {
			if (fullField) {
				sendInspectionTeamSelect(handler.pw, FIELD);
			} else if(trackField){
				sendInspectionEditPage(handler.pw, FIELD);
			}
		} else if(url.equals("checkin") || url.equals("ci")){
			if(trackCheckIn)sendInspectionEditPage(handler.pw, CHECKIN);
		} else if(url.equals("cube") || url.equals("sc")){
			if(trackCube)sendInspectionEditPage(handler.pw, CUBE);
		}
	}
	/**
	 * Sends the admin page to the given printWriter
	 * @param pw The PrintWriter to send to it
	 */
	public void sendAdminPage(Handler handler) {
		PrintWriter pw = handler.pw;
		sendNormalHeader(pw);
		pw.println("<html><body bgcolor=#000><div style=\" overflow-y: auto; overflow-x:auto;\">");
		pw.println(Main.me.consoleTextArea.getPlainText());
		pw.println("</div><br>");
		pw.println("<script>"
				+ "\nfunction test() {"
				+ "\nif (window.event.keyCode == 13) {"
				+ "\nsendAdmin();"
				+ "\nreturn false;"
				+ "\n} else return true;"
				+ "\n}"
				+ "\nfunction sendAdmin() {"
				+ "\nvar xhttp = new XMLHttpRequest();"
				+ "\nxhttp.onreadystatechange = function() {"
				+ "\n    if (xhttp.readyState == XMLHttpRequest.DONE) {"
				+ "\n        //alert(xhttp.responseText);    "
				+ "\n		window.location.reload(true); //this means we have gotten a response back and processing is done"
				+ "\n}"
				+ "\n}"
				+ "\nxhttp.open(\"POST\",\"./admin?cmd=&&&\" + document.getElementById(\"admin\").value + \"&&&\" + document.cookie, true);"
				+ "\nxhttp.send();" // instead of sending everything as part of the URL, we could use the send for the data like the other text areas do. But this works for now
				+ "\n//window.location.reload(true);"
				+ "\n}"
				+ "\n</script>");
		pw.println("<textarea onkeypress=\"test();\" cols=\"100\" rows=\"1\" id=\"admin\" autofocus></textarea>");
		pw.println("<button onclick=\"sendAdmin();\">Send</button>");
		pw.println("</body></html>");
	}
	/**
	 * Handles GET requests.
	 * @param req
	 * @param sock
	 * @param fullReq 
	 * @throws IOException
	 */
	public void get(String req,Handler handler, String fullReq) throws IOException{
//		String other=null;
		String check = fullReq;
		boolean verified = false;
		try {
			check = check.substring(check.indexOf(cookieHeader) + cookieHeader.length() + 1);
			check = check.substring(check.indexOf("&&&") + 3, check.indexOf('\"')); // also take off [ ]
			verified = checkHash(check) || DEBUG;
		} catch (Exception e) {
			verified = DEBUG;
			//e.printStackTrace();
			//we dont have the password
		}
		if(whiteList.contains(handler.sock.getInetAddress()))verified = true;
		req=req.substring(1,req.indexOf(" "));
		System.out.println("req: " + req);
		if (req.endsWith("/")) {
			req = req.substring(0, req.length() - 1);
		}
		if (Resources.exists(req)) {
			sendResource(handler.pw, handler.sock.getOutputStream(), req);
		} else if (Resources.exists(urlMap.getResource(req))) {
			sendResource(handler.pw, handler.sock.getOutputStream(), urlMap.getResource(req));
		} else if (!urlMap.sendPage(handler, req, verified) ) {
			send404Page(handler, verified);
		}
	}
	Pattern imagePattern = Pattern.compile(".+\\.(ico|png|jpg|jpeg|bmp)");
	/**
	 * Sends the proper HTTP header and then the resource
	 * @param pw
	 * @param req
	 */
	private void sendResource(PrintWriter pw, OutputStream out,  String req) {
		System.out.println("sending resource: " + req);
		if (imagePattern.matcher(req).matches()) {
			sendImageHeader(pw);
			sendDocument(pw, out, req);
		} else if (req.endsWith(".pdf")) {
			sendPDFHeader(pw);
			sendDocument(pw, out, req);
		} else {
			sendNormalHeader(pw);
			sendPage(pw, req);
		}
		
	}
	public void sendCubeIndexPage(Handler handler) {
		sendNormalHeader(handler.pw);
		handler.pw.println((separateCube && fullHardware )?Team.CUBE_INDEX:-1);
	}
	public void send404Page(Handler handler, boolean verified) {
		sendNormalHeader(handler.pw);
		handler.pw.write("Error 404: Showing default<br><br>\n\n");
		if (verified) {
			sendHomePageNoHeader(handler);
		} else {
			sendStatusPageNoHeader(handler);
		}
	}
	/**
	 * This method handles POST requests. It should be passes the request, the data line, and the Socket.
	 * 
	 * @param req
	 * @param data
	 * @param sock
	 * @throws IOException
	 */
	public void post(String req, String data,Handler handler) throws IOException{
//		int pageID = 0;
//		boolean valid = false;
		String response = "";
		Socket sock = handler.sock;
		System.out.println("POST: "+req+"\nData: ("+data.length() + ")\n" +data);
		for(char c : data.toCharArray()){
			System.out.print(((int) c) + " ");
		}
		System.out.println();
		/*
		 * if the data contains a password, its from the login page.
		 * That means we can send it a secured page.
		 */
		if(req.contains("password")){
			String pass=req.substring(req.indexOf("password")+9);
			pass=pass.substring(0, pass.indexOf("&"));
//			OutputStream out=sock.getOutputStream();
//			PrintWriter pw=new PrintWriter(out);
			if(checkPassword(pass)){
//				extras = "Set-Cookie: " + cookieHeader + hashedPassString + "\"\n";
//				extras  = "\n\n<script>document.cookie = \"" + cookieHeader  + "\\\"" + sock.getInetAddress().getHostAddress() /*cookieCount++*/ + "&&&" + hashedPassString + "\\\";path=/\";</script>";
				cookieCount++;
//				pw.print("HTTP/1.1 200 OK\nContent-Type: text/html\nSet-Cookie: " + cookieHeader + hashedPassString + "\"\n\n    \n");
//				pw.flush();
//				valid=true;
//				System.out.println("VERIFIED PASSWORD");
				response = "document.cookie = \"" + cookieHeader  + "\\\"" + sock.getInetAddress().getHostAddress() /*cookieCount++*/ + "&&&" + hashedPassString + "\\\";path=/\";";
//				System.out.println(req +"  "+req.indexOf("/")+"  "+req.indexOf(" "));
//				req=req.substring(req.indexOf("/")+1, req.indexOf(" "));
//				System.out.println("REQ:"+req);
//				pageID = SEND_RESPONSE;
//				if(req.equals("hardware"))pageID=HARDWARE;
//				if(req.equals("field"))pageID=FIELD;
//				if(req.equals("home"))pageID=HOME;
//				if(req.equals("software"))pageID=SOFTWARE;
//				if(req.equals("cube"))pageID=CUBE;
//				if(req.equals("checkin"))pageID=CHECKIN;
				
				//FOR COMPLICATED SOCKET REASONS, YOU CANNOT AUTO-REDIRECT TO THE ADMIN PAGE
				whiteList.add(handler.sock.getInetAddress());
				sendResponse(handler, response);
			} else {
//				pageID = SEND_RESPONSE;
				response = "window.alert('Incorrect Password');";
//				pw.println("window.alert('Incorrect Password');");
//				pw.flush();
				sendResponse(handler, response);
				return;
//				extras = generateExtrasPopup("Incorrect Password");
			}
			//else, no password, pageID stays 0 (the status page)
		}
		/*If there is no password, then the POST's source is a page that already required authentication,
		 * therefore, we can handle it appropriately
		 */
		else{
			//HANDLE POST FROM VERIFIED INSPECTOR
			req=req.substring(1);
//			System.out.println("VERIFIED "+req);
			if(req.startsWith("cubeindex?")){//js is asking what the cube index is before passing the team
//				pageID = CUBE_INDEX_PAGE;
				sendCubeIndexPage(handler);
			}
			else if(req.startsWith("update?")){//These are requests that contain a state change for a team for a level of inspection.
				System.out.println("LINE 389: " + req);
				String s = req.substring(req.indexOf("=") + 1);
				int t = Integer.parseInt(s.substring(0, s.indexOf("_")));
				String type = s.substring(s.indexOf("_") + 1, s.indexOf("&"));
				String v = s.substring(s.indexOf("=")+1);
				v = v.substring(0, v.indexOf(" "));
				getTeam(t).setStatus(type, Integer.parseInt(v));
//				pageID=H204;
				send204Header(handler.pw);
			}
			else if(req.startsWith("fullupdate?")){//full inspection state change
				String s = req.substring(req.indexOf("=")+1);
				int t = Integer.parseInt(s.substring(0, s.indexOf("_")));
				String type = s.substring(s.indexOf("_")+1,s.indexOf("&"));
				int index = Integer.parseInt(type.substring(2));//type is 2 characters
				type = type.substring(0, 2);
				String v = s.substring(s.indexOf("=")+1);
				v = v.substring(0, v.indexOf(" "));
				getTeam(t).setInspectionIndex(type,index,Boolean.parseBoolean(v));
				//send conf wth id of td containing the checkbox and the data we received(v)
				response = t + "_" + type + index + "=" + v;
//				pageID = SEND_RESPONSE;
				sendResponse(handler, response);
			}
			else if(req.startsWith("note?")){
				String s=req.substring(req.indexOf("=")+1);
				int t=Integer.parseInt(s.substring(0, s.indexOf("_")));
				String type=s.substring(s.indexOf("_")+1,s.indexOf(" "));
				System.out.println("NOTE DATA:"+data);
				String note=data.substring(0, data.indexOf("&&&"));
				getTeam(t).setNote(type,note);
//				pageID=H204;
				send204Header(handler.pw);
			}
			else if(req.startsWith("sig?")){
				String s=req.substring(req.indexOf("=")+1);
				int t=Integer.parseInt(s.substring(0,s.indexOf("_")));
				String type=s.substring(s.indexOf("_")+1,s.indexOf(" "));
				System.out.println("SIG DATA:"+data);
				String teamSig=data.substring(data.indexOf("team=")+5,data.indexOf("&"));
				String inpSig=data.substring(data.indexOf("inspector=")+10,data.indexOf("&&&"));
				System.out.println(t+" "+type+": "+teamSig+", "+inpSig);
				getTeam(t).setSignature(type, teamSig, inpSig);
//				pageID=H204;
				send204Header(handler.pw);
			}
			else if (req.startsWith("admin?")) {
				req = fixURI(req);
				String cmd = req.substring(req.indexOf("&&&") + 3);
				cmd = cmd.substring(0, cmd.indexOf("&&&"));
				System.out.println("CMD: " + cmd);
				String who = getWho(sock);
				System.out.println("WHO: " + who);
				Main.me.handleCommand(cmd, who);
				response = "HELLO THIS IS A RESPONSE";
//				pageID = SEND_RESPONSE;
				sendResponse(handler, response);
			}
			else if (req.startsWith("fancySig")) {
				int x = 0;
				String img = req.substring((x = req.indexOf('?')) + 1, req.indexOf(" HTTP"));
				req = req.substring(0, x);
				System.out.println("Img: " + img);
				req = req.substring(req.indexOf('_') + 1);
				Team t = Team.getTeam(Integer.parseInt(req.substring(0, req.indexOf('_'))));
				System.out.println("Team: " + t.number);
				req = req.substring(req.indexOf('_'));
				System.out.println("Type: " + req);
				t.setSigURL(req, img);
			}
			else if (req.startsWith("test")) {
				
			}
			else{
				System.out.println("NOTHIN!");
//				pageID = H204;
				send204Header(handler.pw);
			}
		}
//		sendPage(sock,pageID, valid, response);	
	}
	private String getWho(Socket sock) {
		return sock.getInetAddress().getHostName();
	}
	public void sendResponse(Handler handler, String response) {
		sendNormalHeader(handler.pw);
		handler.pw.println(response);
	}
//	/**
//	 * Returns the number assigned to this user stored in the cookie
//	 * @param req the cookie header (or more, it will filter)
//	 * @return the number assigned to this user
//	 */
//	public String getWho(String req) {
//		req = fixURI(req);
//		req = req.substring(req.indexOf(cookieHeader) + cookieHeader.length());
//		req = req.substring(1, req.indexOf("&&&"));
//		return req;
//	}
	/**
	 * This replaces any %dd with the character that corresponds to the hex value of dd. This is the standard escape sequence for URI.
	 * NOTE: While this does check for %d and will not replace it, if there is a %dd it will be replaced. Apparently chrome will convert
	 * special characters to the %dd sequence, but it will not escape a %dd that is already there, so if your message is "hi%20" then it
	 * will be rendered as "hi " even if you type out the %20. If yo want to get around this, you could use %25, which is the hex code for
	 * the percent sign. So to get a return value of "hi%20" you would need to pass in "hi%2520"
	 * @param cmd The string to process
	 * @return A string with %dd replaced with the hex character dd.
	 */
	public String fixURI(String cmd) {
		String[] ss = cmd.split("%");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ss.length - 1; i++) {
			sb.append(ss[i]);
			if (ss[i + 1].length() > 1 && Character.isDigit(ss[i + 1].charAt(0)) && Character.isDigit(ss[i + 1].charAt(1))) {
				sb.append((char) Integer.parseInt(ss[i + 1].substring(0, 2), 16));
				ss[i + 1] = ss[i + 1].substring(2);
			} else {
				ss[i + 1] = '%' + ss[i + 1]; // reconstruct the original with the percent
			}
		}
		sb.append(ss[ss.length - 1]);
		return sb.toString();
	}
	/**
	 * Use extras = generateExtrasPopup(popup) to render a javascript pop up on the page
	 * @param popup The string to render
	 * @return The extra to show the pop up
	 */
	public String generateExtrasPopup(String popup) {
		return "\n\n<script> window.alert(\"" + popup + "\") </script>";
	}
	public void sendLoginPage(Handler handler) {
		PrintWriter pw = handler.pw;
		sendNormalHeader(pw);
		sendPage(pw, "inspectorLogin.php");
	}
	/**
	 * This is for sending an html webpage that is completely contained within the file. (No real-time generation by this server). 
	 * NOTE: this does not replace newlines with <br> or other such niceties, to do that see sendPageAsHTML
	 * @param pw
	 * @param f
	 * @throws IOException
	 */
	public void sendPage(PrintWriter pw,String f) {
		Scanner s;
		try {
			s = Resources.getScanner(f);
			while(s.hasNextLine()){
//				String ss = s.nextLine();
//				pw.println(ss);
//				System.out.println(ss);
				pw.println(s.nextLine());
			}
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This sends the file specified to the printwriter, replacing all instances of \n with <br> and \t with four spaces
	 * @param pw
	 * @param f
	 * @throws IOException
	 */
	public void sendPageAsHTML(PrintWriter pw, String f) throws IOException {
		Scanner s=Resources.getScanner(f);
		while(s.hasNextLine())pw.println(s.nextLine().replaceAll("\t", TAB) + "<br>");
		s.close();
	}

	/**
	 * This is for sending more complex pages that are contained within a file. These are pdfs,images,etc.
	 * 
	 * @param pw
	 * @param out
	 * @param f
	 */
	public void sendDocument(PrintWriter pw,OutputStream out,String f) {
		//if(f.substring(f.lastIndexOf(".")+1).equals("pdf")){
		try{

			System.out.println("Sending: "+f);
			InputStream fin=Resources.getInputStream(f);//new InputStream(f);

			int q=0;
			ByteArrayOutputStream bout=new ByteArrayOutputStream();
			while((q=fin.read())!=-1){
				bout.write(q);
			}
			fin.close();
			if (f.endsWith(".pdf")) {
				pw.println("Content-Length:"+bout.size());
				pw.println("\n");;
				pw.flush();
			}
			bout.writeTo(out);
			bout.flush();
			bout.close();
		}catch(Exception e){
			e.printStackTrace();
			addErrorEntry(e);
		}
		//}

	}
	/**
	 * returns the color used for the given integer representation of progress(PASS,FAIL,etc)
	 * This defaults to black
	 * @param i The int to check for color
	 * @return The color as a String
	 */
	public String getColor(int i){
		switch(i){
			case 0:return WHITE;
			case 1:return RED;
			case 2:return CYAN;
			case 3:return GREEN;
		}
		return "black";
	}
	/**
	 * returns white for false, green for true; 
	 * @param b
	 * @return
	 */
	public String getColor(boolean b){
		return getColor(b?3:0);
	}
	/**
	 * Sends the status page, which is a table with colors to indicate how far a team is through inspection
	 * @param pw
	 */
	public void sendStatusPage(Handler handler){
		PrintWriter pw = handler.pw;
		sendNormalHeader(pw);
		sendStatusPageNoHeader(handler);
	}
	public void sendStatusPageNoHeader(Handler handler) {
		PrintWriter pw = handler.pw;
//		pw.println("<html><meta http-equiv=\"refresh\" content=\"15\">");
		pw.println("<html>"
				+ "\n<script>"
				+ "\nsetInterval(function(){"
				+ "\n	var xhttp = new XMLHttpRequest()"
				+ "\n	xhttp.onreadystatechange = function(){"
				+ "\n		if (this.readyState == 4 && this.status == 200) {"
				+ "\n			document.getElementsByTagName(\"html\")[0].innerHTML=this.responseText;"
				+ "\n		}"
				+ "\n	}"
				+ "\n	xhttp.open(\"GET\", window.location.href, true);"
				+ "\n	xhttp.send();"
				+ "\n},15000);</script>");
		
		pw.println("<table align=center><tr><td>");
		pw.println("<table border=\"3\"><tr>");
		if(trackCheckIn)pw.println("<th>CI</th>");
		if(trackCube)pw.println("<th>SC</th>");
		if(trackHardware)pw.println("<th>HW</th>");
		if(trackSoftware)pw.println("<th>SW</th>");
		if(trackField)pw.println("<th>FD</th>");
		pw.println("<th>Team #</th><th>Team name</th></tr>");
		
		
		for(Team t:teams){
			pw.println("<tr>");
			if(trackCheckIn)pw.println("<td bgcolor="+getColor(t.checkedIn)+">&nbsp;</td>");
			if(trackCube)pw.println("<td bgcolor="+getColor(t.cube)+">&nbsp;</td>");
			if(trackHardware)pw.println("<td bgcolor="+getColor(t.hardware)+">&nbsp;</td>");
			if(trackSoftware)pw.println("<td bgcolor="+getColor(t.software)+">&nbsp;</td>");
			if(trackField)pw.println("<td bgcolor="+getColor(t.field)+">&nbsp;</td>");
			pw.println("<td bgcolor="+getColor(t.ready)+">"+t.number+"</td>");
			pw.println("<td bgcolor="+getColor(t.ready)+">"+t.name+"</td>");
			pw.println("</tr>");
		}
		pw.println("</table></td><td>");
		pw.println("<table border=\"3\" style=\"floating:left;\">");
		pw.println("<tr><th>Symbol</th><th>Meaning</th></tr>");
		pw.println("<tr><td bgcolor=\"#FFFFFF\">&nbsp;</td><td>Uninspected</td></tr>");
		pw.println("<tr><td bgcolor=\"#00FFFF\">&nbsp;</td><td>In Progress</td></tr>");
		pw.println("<tr><td bgcolor=\"#FF0000\">&nbsp;</td><td>Failed</td></tr>");
		pw.println("<tr><td bgcolor=\"#00FF00\">&nbsp;</td><td>Passed</td></tr>");
		if(trackCheckIn)pw.println("<tr><td>CI</td><td>Check In</td></tr>");
		if(trackCube)pw.println("<tr><td>SC</td><td>Sizing Cube</td></tr>");
		if(trackHardware)pw.println("<tr><td>HW</td><td>Hardware</td></tr>");
		if(trackSoftware)pw.println("<tr><td>SW</td><td>Software</td></tr>");
		if(trackField)pw.println("<tr><td>FD</td><td>Field</td></tr>");
		pw.println("</table></td></tr></table>");
		pw.println("<script>");
			sendPage(pw, "konami.js");
		pw.println("</script>");
//		pw.flush();
//		pw.println("<img src=\"firstfavicon.png\"></html>");
	}
	/**
	 * Send page to edit the status of a team's inspection
	 * @param pw
	 * @param i
	 * @throws IOException
	 */
	public void sendInspectionEditPage(PrintWriter pw, int i) {
		sendNormalHeader(pw);
		/*
		 * Check if doing a full inspection for that type
		 * 				-if so, send list of teams and button to inspect them, which loads /hardware/#####
		 *              -if not, check if tracking
		 *              		-if so, send old page
		 *              		-if not, return to home?
		 *              
		 *              
		 */
		
		String type="";
		switch(i){
			case CHECKIN:type="_CI";break;
			case CUBE:type="_SC";break;
			case HARDWARE:type="_HW";break;
			case SOFTWARE:type="_SW";break;
			case FIELD:type="_FD";break;
		}
		pw.println("<html><body><h1>");
		switch(i){
			case HARDWARE:pw.println("Hardware Inspection");break;
			case SOFTWARE:pw.println("Software Inspection");break;
			case FIELD:pw.println("Field Inspection");break;
			case CUBE:pw.println("Sizing Cube");break;
			case CHECKIN:pw.println("Checkin");break;
		}
		pw.println("</h1><table cellspacing=\"10\">");
		for(Team t:teams){
			pw.println("<tr><td id=\"R"+t.number+"\" bgcolor="+getColor(t.getStatus(i))+">"+t.number+"</td><td>");
			//ComboBox code:
			pw.println("<select onchange=\"update()\" name=\""+ t.number + type + "\""+">");
			pw.println("<option value=\"" + PASS     + "\"" + (t.getStatus(i) == PASS?"selected":"")     + ">PASS</option>");
			pw.println("<option value=\"" + FAIL     + "\"" + (t.getStatus(i) == FAIL?"selected":"")     + ">FAIL</option>");
			pw.println("<option value=\"" + PROGRESS + "\"" + (t.getStatus(i) == PROGRESS?"selected":"") + ">IN PROGRESS</option>");
			pw.println("<option value=\"" + NO_DATA  + "\"" + (t.getStatus(i) == NO_DATA?"selected":"")  + ">NONE</option>");
			pw.print("</select>");
			pw.println("</tr>");
		}
		pw.println("</table><script>");
			//OLD protocol
//			switch(i){
//				case 0:type="_CI";break;
//				case 1:type="_SC";break;
//				case 2:sendPage(pw,"Resources/inspectionUpdate.js");;break;
//				case 3:type="_SW";break;
//				case 4:sendPage(pw,"Resources/fieldUpdate.js");break;
//			}
			sendPage(pw,"update.js");

		pw.println("</script></body></html>");
	}
	/**
	 * returns the Team object associated with the team with the given number
	 * @param num
	 * @return
	 */
	public Team getTeam(int num){
		for(int i=0;i<teams.size();i++){
			if(teams.get(i).number == num)return teams.get(i);
		}
		return null;
	}
	
	/**
	 * Sends the page to select the team to inspect. Displays current status behind their numbers to prevent multiple inspections
	 * @param pw The printwriter to send the page through
	 * @param i The inspection type
	 */
	public void sendInspectionTeamSelect(PrintWriter pw, int i){
		
		String type="";
		switch(i){
			case HARDWARE: 
				if(multiHardware){
					System.out.println("multi h: " + multiHardware);
					sendMultiTeamSelect(pw, i);
					return;
				}
				type="hardware";break;
			case SOFTWARE:
				if(multiSoftware){
					sendMultiTeamSelect(pw, i);
					return;
				}
				type="software";break;
			case FIELD:  
				if(multiField){
					sendMultiTeamSelect(pw, i);
					return;
				}
				type="field";break;
			default:return;//TODO something else here?
		}
		pw.println("<html><meta http-equiv=\"refresh\" content=\"5\"><body><h1>");//TODO test if refresh is noticeable
		switch(i){
			case HARDWARE:pw.println("Hardware Inspection");break;
			case SOFTWARE:pw.println("Software Inspection");break;
			case FIELD:pw.println("Field Inspection");break;
		}
		pw.println("</h1><a href=\"/home\">Home</a><br><table cellspacing=\"10\"><tr><th>Team #</th><th>Link</th></tr>");
		for(Team t:teams){
			pw.println("<tr><td bgcolor="+getColor(t.getStatus(i))+">"+t.number+"</td><td><a href=\"/"+type+"/"+t.number+"\">Inspect</a></td></tr>");
		}
		pw.println("</table></body></html>");
		pw.flush();
	}
	
	/**
	 * Sends the page to select multiple teams to inspect
	 * @param pw
	 * @param i
	 * @throws IOException 
	 */
	public void sendMultiTeamSelect(PrintWriter pw, int i) {
		System.out.println("HI?");
		sendNormalHeader(pw);
		pw.println("<html><style>");
			sendPage(pw, "multi_select.css");
		pw.println("</style><h1>Select Teams to Inspect</h1><body><table>");
		pw.println("<tr><th>Available Teams</th><th>Selected Teams</th></tr>");
		pw.println("<tr><td><ul id=\"out\">");
		for(Team t : teams){
			pw.println("<li id=\"" + t.number + "\"><button onclick=add()>" + t.number + "</button></li>");
		}
		pw.println("</ul></td><td align = center><table id=\"in\" class=\"t2\"><tr><th>Index</th><th>Team Number</th><th/><th/><th/></tr></table><br><button onclick=inspect()>Inspect</button></td>");
		pw.println("<tr></table>"+"<a href=\"/home\">Home</a>"+"<script>");
			sendPage(pw, "multi_select.js");
		pw.println("</script></body></html>");
		pw.flush();
	}
	
	public void sendFullInspectionPage(Handler handler, String url) {
		PrintWriter pw = handler.pw;
		sendNormalHeader(pw);
		//FIXME move this into the body of the other method.
		int kind = -1;
		String type = url.substring(0, url.indexOf('/'));
		String team = url.substring(url.indexOf('/') + 1);
		if (type.equals("hardware") || type.equals("hw")) {
			kind = HARDWARE;
		} else if (type.equals("software") || type.equals("sw")) {
			kind = SOFTWARE;
		} else if (type.equals("field") || type.equals("fd")) {
			kind = FIELD;
		}
		sendFullInspectionPage(pw, kind, team);
	}
	/**
	 * Sends the full inspection page for the given team.
	 * @param pw
	 * @param i The inspection type (FD, HW, SW)
	 * @param extras The team number being inspected
	 */
	public void sendFullInspectionPage(PrintWriter pw, int i, String extras){
		
		//when submit button clicked, send note and thats how you know IP->fail (or pass)
		//note beng reason for failure as prescribed 
		//TODO do we want to be able to print an inspection sheet for a team if they ask? IE print job? -Nah scoring pc can just connect and print webpage
		Team team = null;
		try{
			team = getTeam(Integer.parseInt(extras));
			if(team == null) throw new IllegalArgumentException("Invalid team #: "+extras);
		}catch(Exception e){ //this is actually a multi-inspection page!
			System.out.println("EXTRAS = " + extras);
			String[] list = extras.substring(extras.indexOf("=") + 1).split(",");
			Team[] teams = new Team[list.length];
			for(int x = 0; x < list.length; x++){
				try{
					teams[x] = getTeam(Integer.parseInt(list[x]));
					if(teams[x] == null) throw new IllegalArgumentException("Invalid team #: "+extras);
				}catch(Exception e1){
					//TODO send error page. 404? some better way to do this?
					return;
				}
			}
			sendMultiInspectionPage(pw, i, teams);
			return;
		}
		
		InspectionForm form;
//		System.out.println("full: "+i);
		String type = "";
		String note = "";
		String head = "Appendix ";
		String back = "";
		switch(i){
			case HARDWARE: form = hardwareForm; type = "_HW"; note = team.hwNote; back = "/hardware"; break;
			case SOFTWARE: form = softwareForm; type = "_SW"; note = team.swNote; back = "/software"; break;
			case FIELD:    form = fieldForm; type = "_FD"; note = team.fdNote; back = "/field";    break;
			default: throw new IllegalArgumentException("Full inspection not supported");
		}
		if(form.header != null){
			head = form.header;
		} else{
			if(type.contains("HW")) head += "B - Robot Inspection Checklist";
			else head += "C - Field Inspection Checklist";
		}
		pw.println("<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><head><h2>" + head + "</h2><hr style=\"border: 3px solid #943634\" /><h3>Team Number: " + extras + "</h3></head>");
		//TODO adjust table size so it is useable on phone.
		pw.println("<body>");
		
		pw.println(form.getFormTable(team));
		
		
		
		pw.println("<br><b>General Comments or Reasons for Failure:</b><br><textarea name="+extras+type+" id=\"note\" rows=\"4\" co"
				+ "ls=\"100\">"+note+"</textarea>");
		
		String[] sigs=team.getSigs(type.substring(1));
	//	if(sigs.length>0){
			//TODO tidy this up a lot!
			pw.println("<br><br><b>I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of"+
									"the FIRST Tech Challenge have been abided by.</b><br><br>");
			pw.println("<table width=\"100%\" cellspacing=\"20\"><tr><td><input id=sig_1 value=\""+(sigs.length > 1? sigs[1]:"")+"\"></input><hr></td><td><input id=sig_0 value=\""+ (sigs.length > 0 ?sigs[0]:"")+"\"></input><hr></td></tr>");
			pw.println("<tr><td>FTC Inspector</td><td>Team Student Representative</td></tr></table>");
		//}
		
		
		
		pw.println("<br><br><button type=\"button\" name=\""+extras+type+"\" onclick=\"fullpass()\">Pass</button>&nbsp;&nbsp;&nbsp;");
		pw.println("<button type=\"button\" name=\""+extras+type+"\" onclick=\"fullfail()\">Fail</button>");
		pw.println("<br><br><a href=\"" + back + "\">Back</a>");
		

		pw.println("<script>");
			sendPage(pw,"fullUpdate.js");
		pw.println("</script></body></html>");
		pw.flush();
	}
	
	public void sendMultiInspectionPage(PrintWriter pw, int i, Team[] teams){
		
		InspectionForm form = null;
		String type = "";
//		String note = "";
		String head = "Appendix ";
		String back = ""; //TODO update note ad pas fail part
		String[] notes = new String[teams.length];
		System.out.print("MULTI PAGE: ");
		int ind = 0;
		for(Team t : teams){
			switch(i){
				case HARDWARE: form = hardwareForm; type = "_HW"; notes[ind] = t.hwNote; back = "/hardware"; break;
				case SOFTWARE: form = softwareForm; type = "_SW"; notes[ind] = t.swNote; back = "/software"; break;
				case FIELD:    form = fieldForm; type = "_FD"; notes[ind] = t.fdNote; back = "/field";    break;
				default: throw new IllegalArgumentException("Full inspection not supported");
			}
			ind++;
			System.out.print(t.number + ",");
		}
		System.out.println();
		if(form.header != null){
			head = form.header;
		} else{
			if(type.contains("HW")) head += "B - Robot Inspection Checklist";
			else head += "C - Field Inspection Checklist";
		}
		pw.println("<html><head><h2>" + head + "</h2><hr style=\"border: 3px solid #943634\" /><h3>Team Numbers: Multiple" + "</h3></head>"); //TODO make this better?
		//TODO adjust table size so it is useable on phone.
		pw.println("<body>");
		
		pw.println(form.getFormTable(teams));
		
//		String extras = "TEMP"; //FIXME get rid of this
		for(ind = 0; ind < teams.length; ind++){
			
			Team t = teams[ind];
			String[] sigs = t.getSigs(type.substring(1));
			pw.println("<h3>Team <font color=");
			if(t.getStatus(i) == PASS)pw.println("green");
			else if(t.getStatus(i) == FAIL)pw.println("red");
			pw.println(">" + t.number + "</font>:</h3>");
			pw.println("<br><b>General Comments or Reasons for Failure:</b><br><textarea name="+t.number+type+" id=\"note\" rows=\"4\" co"
					+ "ls=\"100\">"+notes[ind]+"</textarea>");
			pw.println("<br><br><b>I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of"+
					"the FIRST Tech Challenge have been abided by.</b><br><br>");
			pw.println("<table width=\"100%\" cellspacing=\"20\"><tr><td><input id=sig_1 value=\""+(sigs.length > 1? sigs[1]:"")+"\"></input><hr></td><td><input id=sig_0 value=\""+ (sigs.length > 0 ?sigs[0]:"")+"\"></input><hr></td></tr>");
			pw.println("<tr><td>FTC Inspector</td><td>Team Student Representative</td></tr></table>");
			pw.println("<br><br><button type=\"button\" name=\""+t.number+type+"\" onclick=\"fullpass()\">Pass</button>&nbsp;&nbsp;&nbsp;");
			pw.println("<button type=\"button\" name=\""+t.number+type+"\" onclick=\"fullfail()\">Fail</button>");
			
//			if(sigs.length>0){
//				pw.println("<br><br><b>I hereby state that all of the above is true, and to the best of my knowledge all rules and regulations of"+
//										"the FIRST Tech Challenge have been abided by.</b><br><br>");
//				pw.println("<table width=\"100%\" cellspacing=\"20\"><tr><td>"+sigs[1]+"<hr></td><td>"+sigs[0]+"<hr></td></tr>");
//				pw.println("<tr><td>FTC Inspector</td><td>Team Student Representative</td></tr></table>");
//			}
			pw.println("<hr style=\"border: 1px solid #000\" />");
		}
		pw.println("<br><br><a href=\"" + back + "\">Back</a>");
		
		
		
		pw.println("<script>");
			sendPage(pw,"fullUpdate.js");
		pw.println("</script></body></html>");
		pw.flush();
	}
	public void sendHomePageNoHeader(Handler handler) {
		PrintWriter pw = handler.pw;
		//TODO make this page better
		pw.println("<html>"
				+ "\n<style>"
				+ "a {"
				+ "font-size: 4.5vh;"
				+ "}"
				+ "\n</style>"
				+ "\n<body>");

		if(trackCheckIn)pw.println("<a href=\"/checkin\">Checkin</a>");
		pw.println("<br><br>");
		if(trackCube)pw.println("<a href=\"/cube\">Sizing Cube</a>");
		pw.println("<br><br>");
		if(trackHardware || fullHardware)pw.println("<a href=\"/hardware\">Hardware</a>");
		pw.println("<br><br>");
		if(trackSoftware || fullSoftware)pw.println("<a href=\"/software\">Software</a>");
		pw.println("<br><br>");
		if(trackField || fullField)pw.println("<a href=\"/field\">Field</a>");
		pw.println("<br><br>");
		pw.println("<a href=\"/\">Status</a>");//TODO do we want to have a /status page that has the links at the top? (diff from /)
		pw.println("<br><br>");
		pw.println("<a href=\"reference\">Manuals and Forums");
		pw.println("</body></html>");
		pw.flush();
	}
	/**
	 * Sends the inspection home page, which has a menu to choose inspection
	 * @param pw The writer to send to
	 */
	public void sendHomePage(Handler handler){
		sendNormalHeader(handler.pw);
		sendHomePageNoHeader(handler);
	}
	/**
	 * Sends the log page
	 * @param pw The writer to send to
	 */
	public void sendLogPage(Handler handler, int which) {
		PrintWriter pw = handler.pw;
		sendNormalHeader(pw);
		pw.println("<html><body bgcolor=\"#000000\">");
		try {
			if (which == LOG_ERROR) {
				pw.println("<font color=\"#FF0000\">");
				sendPageAsHTML(pw, ((RedirectingPrintStream) System.err).getRedirFile());
			} else if (which == LOG_OUT) {
				pw.println("<font color=\"#FFFFFF\">");
				sendPageAsHTML(pw, ((RedirectingPrintStream) System.out).getRedirFile());
			} else if (which == LOG_COMM) {
				pw.println("<font color=\"#FFFFFF\">");
				sendPage(pw, "commFilterHeader.html");
				sendPageAsHTML(pw, commFile);
			}
		} catch (Exception e) {
			pw.println("There was an error rendering the log page:");
			e.printStackTrace(pw);
		}
		pw.println("</font></body></html>");
		pw.flush();
	}
	/**
	 * Starts the server.
	 * @param port
	 * @throws FileNotFoundException
	 */
	public void startServer(final int port) throws FileNotFoundException{
		this.setPassword(password); 
		//loadEvent(event); //loads the default event if 
		addLogEntry("Starting server...");
		
//		try {
//			whiteList.add(InetAddress.getLocalHost());
//			whiteList.add(InetAddress.getByName("localhost"));
//			whiteList.add(InetAddress.getByName("0:0:0:0:0:0:0:1"));
			//FIXME WhiteList localhost!? This doesnt work?
//		} catch (UnknownHostException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		if(Resources.backup == null){
			addErrorEntry("No backup location set!");
		}else if(Resources.backupExists()){
			addLogEntry("Backup location set to: " + Resources.backup);
		} else{
			addErrorEntry("Backup Location Error! (" + Resources.backup + ")");
		}
		
		
		
		Thread serverThread=new Thread("Server"){
			public void run(){
				threadPool=Executors.newCachedThreadPool();
				try {
					ServerSocket server = new ServerSocket(port);
					server.setSoTimeout(1000);
					addLogEntry("Server ready at " + InetAddress.getLocalHost().getHostAddress());
					while(!done){
						try{
							threadPool.execute(new Handler(server.accept()));
						}catch(SocketTimeoutException e){
							//this is so we can safely shutdown
						}
					}
					server.close();
					addLogEntry("Server Socket closed");
				} catch (IOException e) {
					e.printStackTrace();
					addErrorEntry(e);
				}
				addLogEntry("Attempting to shutdown client threads...");
				threadPool.shutdown();
				try {
					threadPool.awaitTermination(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(threadPool.isShutdown()){
					addLogEntry("Client threads terminated");
				}else{
					addLogEntry("Client threads failed to terminate!");
				}
				threadPool.shutdownNow();
			}
		};
		serverThread.start();
		
	}

	/**
	 * Loads event data- name, code, and teams.
	 * file name: EVENTCODE ex:BCRI2017, NCCMP2016, CGHS
	 * Format:
	 * line 1:Event Full name
	 * line 2:Comma separated list of team #s
	 * @param event The string of the event to load. Must correspond to a .event file
	 * @throws FileNotFoundException
	 */
	public void loadEvent(String event) throws FileNotFoundException{
		if(event == null)return;//Do not attempt to load null event.
		Scanner scan = Resources.getScanner(event + ".event");//new Scanner(new File("Resources/"+event));
		Server.event = event;//if finds file, set Server event to the new one.
		fullEventName = scan.nextLine();
		Vector<Integer> nums=new Vector<Integer>();
		scan.useDelimiter(",| |\\n");
		while(scan.hasNext()){
			try{
				nums.add(scan.nextInt());
			}catch(Exception e){e.printStackTrace();}
		}
		scan.close();
		for(int i:nums){
			System.out.println("Loading team "+ i);
			Team t = Team.getTeam(i);
			if(t == null){
				System.err.println("WARNING! NO TEAM " + i);
				Server.addErrorEntry("WARNING! NO TEAM " + i +" CREATING DUMMY TEAM!");
				Team.registerTeam(i, null);
				t = Team.getTeam(i);
			}
			teams.add(t);
		}
		addLogEntry("Loaded event: "+fullEventName);
		Collections.sort(teams);
		
		//load status data if exists
		scan = Resources.getStatusScanner();
		if(scan == null)return;//were done here- no data
//		String[] line;
		while(scan.hasNextLine()){
			Team.loadDataFromString(scan.nextLine());		
		}
		scan.close();
		
		for(Team t:teams){
			scan = Resources.getHardwareScanner(t.number);
			try{
				for(int i = 0; i < t.hwData.length; i++){
					t.hwData[i] = scan.nextBoolean();
				}
				if(scan.hasNextBoolean()){
					addErrorEntry("Team file has more entries: " + t.number + " HW");
					while(scan.hasNextBoolean()) scan.nextBoolean();
				}
				scan.nextLine();
				t.hwTeamSig = scan.nextLine();
				t.hwInspSig = scan.nextLine();
				while(scan.hasNextLine()){
					t.hwNote += scan.nextLine()+"\n";
				}
			}catch(Exception e){
				//This means that the size of the form did not match the number of entries
				//in the team's .ins file
				addErrorEntry("Inspection File Mismatch: " + t.number + " HW");
			}
			scan.close();
			
			scan= Resources.getSoftwareScanner(t.number);
			try{
				for(int i = 0; i < t.swData.length; i++){
					t.swData[i] = scan.nextBoolean();
				}
				if(scan.hasNextBoolean()){
					addErrorEntry("Team file has more entries: " + t.number + " SW");
					while(scan.hasNextBoolean()) scan.nextBoolean();
				}
				scan.nextLine();
				t.swTeamSig = scan.nextLine();
				t.swInspSig = scan.nextLine();
				while(scan.hasNextLine()){
					t.swNote += scan.nextLine() + "\n";
				}
			}catch(Exception e){
				addErrorEntry("Inspection File Mismatch: " + t.number + "HW");
			}
			scan.close();
			
			scan= Resources.getFieldScanner(t.number);
			try{
				for(int i = 0; i < t.fdData.length; i++){
					t.fdData[i] = scan.nextBoolean();
				}
				if(scan.hasNextBoolean()){
					addErrorEntry("Team file has more entries: " + t.number + " FD");
					while(scan.hasNextBoolean()) scan.nextBoolean();
				}
				scan.nextLine();
				t.fdTeamSig = scan.nextLine();
				t.fdInspSig = scan.nextLine();
				while(scan.hasNextLine()){
					t.fdNote += scan.nextLine()+"\n";
				}
			}catch(Exception e){
				addErrorEntry("Inspection File Mismatch: " + t.number + "HW");
			}
			scan.close();
		}
		
		
	}
	

	/**
	 * Handles the HTTP requests and directs them to appropriate methods.	 * 
	 *
	 */
	public class Handler implements Runnable{
		Socket sock;
		PrintWriter pw;
		public Handler(Socket s){
			sock=s;
			try {
				pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(), "utf-8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void run(){
			traffic++;
			try{
				StringBuilder full = new StringBuilder();
				StringBuilder data = new StringBuilder();
				BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				String type = in.readLine();
				System.out.println(type);
				full.append(type);
				full.append('\n');
				String line = null;
				int len = -1;
				sock.setSoTimeout(50);
				try{
					while((line = in.readLine()) != null && !line.isEmpty()){
						full.append(line );
						full.append('\n');
						if(line.startsWith("Content-Length:")){
							Scanner scan = new Scanner(line.substring(line.indexOf(":") + 1));
							len = scan.nextInt();
							scan.close();
							break;
						}
					}
					
					//we have recieved cntent length!
					while((line = in.readLine()) != null && line.contains(":")){
						//still receiveing header
						full.append(line);
						full.append('\n');
					}
				}catch(SocketTimeoutException e){}//there has got to be a better way
				
				if(type == null)return;
				if(type.startsWith("GET")){
					commStream.println("<div name=\"" + sock.getInetAddress().getHostAddress() + "\" class=\"GET\">" + sock.getInetAddress().getHostAddress() + "<br>" + full + "<br><hr><br></div>");
					get(type.substring(4),this, full.toString());
				} else{
					for(int i = 0; i < len; i++){
						char c = (char)in.read();
						data.append(c);
						full.append(c);
					}
					commStream.println("<div name=\"" + sock.getInetAddress().getHostAddress() + "\" class=\"POST\">" + sock.getInetAddress().getHostAddress() + "<br>" + full + "<br><hr><br></div>");
				}
				
//				System.out.println("FULL REQUEST: " + full.toString() +"\n END FULL REQ");
				
				if(type.startsWith("POST")){			
					
//					System.out.println("POST: " + type + '\n' + "DATA:" + data.toString());
					post(type.substring(5), data.toString(), this);
				}
				pw.flush();
				pw.close();
				sock.close();
			}catch(Exception e){
				e.printStackTrace();
				addErrorEntry(e);
			}
		}
	}
	
	/**
	 * Adds a String to the Server's status log
	 * @param s The string to add to the log
	 */
	public static void addLogEntry(String s){ //TODO add who
		String time=DATE_FORMAT.format(Calendar.getInstance().getTime());
		statusLog.add(time+s);
		Main.me.append(s, null);//make this the who too
		
	}
	
	/**
	 * Adds a String to the Server's Error log
	 * @param s The string to add to the error log
	 */
	public static void addErrorEntry(String s) {
		String time=DATE_FORMAT.format(Calendar.getInstance().getTime());
		statusLog.add(time+s);
		Main.me.error(s, null);
	}
	
	/**
	 * Adds an exception to the Server's Error log
	 * @param e The exception to log
	 */ 
	public static void addErrorEntry(Exception e) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(b));
		addErrorEntry(b.toString());
	}
	
	/**
	 * Stops the server
	 */
	public static void stopServer(){
		Server.save();
		addLogEntry("Stopping server!");
		theServer.done=true;
	}
	
	/**
	 * Saves the current event data and changes active event, loading the data for the new event.
	 * @param name the code/abbreviation for the event to change to.
	 * @return true if successful
	 */
	public static boolean changeEvent(String name){
		Server.save();
		addLogEntry("Saved old event data");
		System.out.println("New event:"+name);
		event = name;
		theServer.teams.clear();
		try {
			theServer.loadEvent(name);
			return true;
		} catch (FileNotFoundException e) {
			System.err.println("Error opening "+name+".event");
			e.printStackTrace();
			
			addErrorEntry(e);
		}		
		return false;
	}

	public void unloadEvent() {
		save();
		event = null;
		teams.clear();
		addLogEntry("No Event Loaded");
		
	}
	
	/**
	 * Saves the current team status data to the .status file, and the full inspection data for each team
	 * to the teams respective .ins file. Also saves server configuration.
	 * @return
	 */
	public static boolean save(){
		Resources.saveEventFile();
		theServer.saveConfig();
		PrintWriter pw = Resources.getStatusWriter();
		if(pw == null)return false;
		for(Team t : theServer.teams){
			pw.println(t.getStatusString());
		}
		pw.flush();
		pw.close();
		
		for(Team t:theServer.teams){
			//hardware
			pw = Resources.getHardwareWriter(t.number);
			for(boolean b:t.hwData){
				pw.println(b);
			}
			pw.println(t.hwTeamSig);
			pw.println(t.hwInspSig);
			pw.print(t.hwNote);
			pw.flush();
			pw.close();
			
			//software
			pw=Resources.getSoftwareWriter(t.number);
			for(boolean b:t.swData){
				pw.println(b);
			}
			pw.println(t.swTeamSig);
			pw.println(t.swInspSig);
			pw.print(t.swNote);
			pw.flush();
			pw.close();
			
			//field
			pw=Resources.getFieldWriter(t.number);
			for(boolean b:t.fdData){
				pw.println(b);
			}
			pw.println(t.fdTeamSig);
			pw.println(t.fdInspSig);
			pw.print(t.fdNote);
			pw.flush();
			pw.close();
		}
		
		return true;
	}
	
	/**
	 * Saves the current Server configuration to the .config file. Should be called any time configuration changes
	 * @return true if successful
	 */ 
	public boolean saveConfig(){		
		PrintWriter pw = Resources.getConfigWriter();
		if(pw == null)return false;
		pw.println(event);
		pw.println(currentPasswordPlaintext);
		pw.println(trackCheckIn);
		pw.println(trackCube);
		pw.println(separateCube);
		pw.println(trackHardware);
		pw.println(fullHardware);
		pw.println(trackSoftware);
		pw.println(fullSoftware);
		pw.println(trackField);
		pw.println(fullField);
		pw.println(multiHardware);
		pw.println(multiSoftware);
		pw.println(multiField);
		pw.println(Resources.backup);
		pw.flush();
		pw.close();
		return true;
	}
	
	/**
	 * Loads the server configuration from the file.
	 */
	public void loadConfig(){
		Scanner scan = Resources.getConfigScanner();
		if(scan == null) return;
		try{
			String event = scan.nextLine();
			if(event != "null" && Main.events.contains(event)){
				Server.event = event;
			} else{
				Server.event = null;
				Server.addLogEntry("No Event Loaded");
			}
			this.setPassword(scan.nextLine());
			trackCheckIn = scan.nextBoolean();
			scan.nextLine();
			trackCube = scan.nextBoolean();
			scan.nextLine();
			separateCube = scan.nextBoolean();
			scan.nextLine();
			trackHardware = scan.nextBoolean();
			scan.nextLine();
			fullHardware = scan.nextBoolean();
			scan.nextLine();
			trackSoftware = scan.nextBoolean();
			scan.nextLine();
			fullSoftware = scan.nextBoolean();
			scan.nextLine();
			trackField = scan.nextBoolean();
			scan.nextLine();
			fullField = scan.nextBoolean();
			scan.nextLine();
		
			multiHardware = scan.nextBoolean();
			scan.nextLine();
			multiSoftware = scan.nextBoolean();
			scan.hasNextLine();
			multiField = scan.nextBoolean();
			scan.nextLine();
			
			Resources.backup = scan.nextLine();
			
		}catch(NoSuchElementException e){
			System.err.println("Config File Error! Check Event Settings Tab");
			Server.addErrorEntry("Config File Error! Check Event Settings Tab");
		}
		if(Resources.backup == null || Resources.backup.equals("null")){
			Resources.backup = null;
			Server.addErrorEntry("WARNING! No backup drive selected!");
		}
		scan.close();
		
	}
	
	/**
	 * Clears the Server data
	 * @return
	 */
	public static boolean clearData(){
		//TODO implement
		return false;
	}
	
	public static boolean addTeam(Team t){
		boolean b = theServer.teams.add(t);
		Collections.sort(theServer.teams);
		return b;
	}
	public static boolean track(int i) {
		switch(i){
			case CHECKIN: return trackCheckIn;
			case FIELD: return trackField;
			case HARDWARE: return trackHardware;
			case SOFTWARE: return trackSoftware;
			case CUBE: return trackCube;
		}
		return false;
	}
	
}
