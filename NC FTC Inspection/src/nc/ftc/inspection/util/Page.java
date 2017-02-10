package nc.ftc.inspection.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import nc.ftc.inspection.Server.Handler;

public class Page {
	public int requiredLevel = -1;
	public BiConsumer<Handler, String> patternMethod;
	public Consumer<Handler> method;
	public Page(int requiredLevel, Consumer<Handler> method) {
		this.requiredLevel = requiredLevel;
		this.method = method;
	}
	public Page(int requiredLevel, BiConsumer<Handler, String> patternMethod) {
		this.requiredLevel = requiredLevel;
		this.patternMethod = patternMethod;
	}
	public Page(Consumer<Handler> method) {
		this(User.GENERAL, method);
	}
	public Page(BiConsumer<Handler, String> patternMethod) {
		this(User.GENERAL, patternMethod);
	}
	public void send(Handler handler) {
		method.accept(handler);
	}
	public void send(Handler handler, String url) {
		patternMethod.accept(handler, url);
	}
}
