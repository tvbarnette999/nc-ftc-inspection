package nc.ftc.inspection.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import nc.ftc.inspection.Server.Handler;

public class Page {
	public boolean requiresLogin;
	public BiConsumer<Handler, String> patternMethod;
	public Consumer<Handler> method;
	public Page(boolean requiresLogin, Consumer<Handler> method) {
		this.requiresLogin = requiresLogin;
		this.method = method;
	}
	public Page(boolean requiresLogin, BiConsumer<Handler, String> patternMethod) {
		this.requiresLogin = requiresLogin;
		this.patternMethod = patternMethod;
	}
	public Page(Consumer<Handler> method) {
		this(false, method);
	}
	public Page(BiConsumer<Handler, String> patternMethod) {
		this(false, patternMethod);
	}
	public void send(Handler handler) {
		method.accept(handler);
	}
	public void send(Handler handler, String url) {
		patternMethod.accept(handler, url);
	}
}
