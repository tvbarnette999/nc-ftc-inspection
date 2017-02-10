package nc.ftc.inspection.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

public class HTTPPrintWriter {
	private PrintWriter pw;
	public HTTPPrintWriter(Socket sock) throws UnsupportedEncodingException, IOException {
		pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(), "utf-8"));
	}

	private boolean hasSentHeader = false;
	public void sendUnauthorizedHeader() {
		if (hasSentHeader) return;
		hasSentHeader = true;
		System.out.println("Sending unauthorized header");
		print("HTTP/1.1 401 Access Denied\nWWW-Authenticate: Basic realm=\"NC FTC Inspection\"\nContent-Length: 0");
		println();
		println();
	}
	public void sendNormalHeader() {
		if (hasSentHeader) return;
		hasSentHeader = true;
		System.out.println("sending normal header");
		print("HTTP/1.1 200 OK\nContent-Type: text/html; charset=utf-8\nCache-Control:no-store\n");
		println();
		println();
	}
	public void sendImageHeader() {
		if (hasSentHeader) return;
		hasSentHeader = true;
		System.out.println("sending image header");
		println("HTTP/1.1 200 OK");
		println("Content-Type: image/x-icon");
	}
	public void sendPDFHeader() {
		if (hasSentHeader) return;
		hasSentHeader = true;
		System.out.println("sending pdf header");
		println("HTTP/1.1 200 OK");
		println("Content-Type: application/pdf");
		println("Content-Disposition: inline; filename=manual1.pdf");
	}
	public void send204Header() {
		if (hasSentHeader) return;
		hasSentHeader = true;
		System.out.println("sending 204 header");
		println("HTTP/1.1 204 No Content\n");
	}
	
	public void println(String s) {
		print(s);
		println();
	}
	
	public void println() {
		print("\n");
	}
	
	public void print(String s) {
		if (!hasSentHeader) {
			System.out.println("NO HEADER WAS SPECIFIED, SENDING NORMAL HEADER");
			sendNormalHeader();
		}
		pw.print(s);
	}
	public void println(int i) {
		println(Integer.toString(i));
	}
	public void flush() {
		pw.flush();
	}
	public void println(boolean b) {
		println(Boolean.toString(b));
	}
	public void close() {
		pw.close();
	}
	public void printStackTrace(Exception e) {
		e.printStackTrace(pw);
	}
}
