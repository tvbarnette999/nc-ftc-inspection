package nc.ftc.inspection.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Date;

import nc.ftc.inspection.Main;

public class RedirectingPrintStream extends PrintStream {
	public static final PrintStream DUMMY_STREAM = new PrintStream(new OutputStream(){
	    public void write(int b) {
	        //NO-OP
	    }
	});
	private PrintStream old;
	private PrintStream cur;
	private String file;
	private boolean onlyOld;
	public RedirectingPrintStream(PrintStream old, String file) throws IOException {
		super((old != null) ? old : DUMMY_STREAM );
		this.old = old;
		this.cur = Resources.getPrintStream(file);
		this.file = file;
	}
	
	public RedirectingPrintStream(PrintStream old, String file, boolean onlyOld) throws IOException {
		this(old, file);
		this.onlyOld = onlyOld;
	}
	
	@Override
	public void print(String x) {
		println(x);
	}
	
	@Override
	public void println(String x) {
		if (x == null) {
			x = "null\n";
		}
		if (!x.endsWith("\n"))
			x += "\n";
		String s = "[" + Main.TIME_FORMAT.format(new Date()) + "] " + x;
		if (old != null) old.print(s);
		if (!onlyOld) cur.print(s);
	}
	
	public void onlySendOld(boolean onlyOld) {
		this.onlyOld = onlyOld;
	}
	
	public boolean isOnlyOld() {
		return onlyOld;
	}
	
	public String getRedirFile() {
		return file;
	}
}
