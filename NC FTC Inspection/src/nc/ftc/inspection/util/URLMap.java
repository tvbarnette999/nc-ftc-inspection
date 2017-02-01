package nc.ftc.inspection.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import nc.ftc.inspection.Server;
import nc.ftc.inspection.Server.Handler;

public class URLMap {
	private HashMap<String, String> resourceMap = new HashMap<String, String>();
	private HashMap<String, Page> map = new HashMap<String, Page>();
	private HashMap<Pattern, Page> patternMap = new HashMap<Pattern, Page>();
	private Page def;
	public URLMap(Server server) {
		def = new Page(handler->server.sendLoginPage(handler));
		map.put("error", new Page(true, handler->server.sendLogPage(handler, Server.LOG_ERROR)));
		map.put("out", new Page(true, handler->server.sendLogPage(handler, Server.LOG_OUT)));
		map.put("comm", new Page(true, handler->server.sendLogPage(handler, Server.LOG_COMM)));
		map.put("home", new Page(true, handler->server.sendHomePage(handler)));
		map.put("admin", new Page(true, handler->server.sendAdminPage(handler)));
		map.put("ip", new Page(handler->server.sendIPPage(handler)));
		map.put("", new Page(handler->server.sendStatusPage(handler)));
		resourceMap.put("reference", "reference.html");
		resourceMap.put("forum", "reference.html");
		resourceMap.put("reference/game", "gameForum.html");
		resourceMap.put("reference/mechanical", "mechanicalForum.html");
		resourceMap.put("reference/electrical", "electricalForum.html");
		resourceMap.put("reference/software", "softwareForum.html");
		resourceMap.put("reference/tournament", "tournamentForum.html");
		resourceMap.put("reference/judge", "judgeForum.html");
		resourceMap.put("reference/manual1", "manual1.pdf");
		resourceMap.put("reference/manual2", "manual2.pdf");
		patternMap.put(Pattern.compile("(hardware|hw|software|sw|field|fd)/(\\S)+((/?)(\\S)*)"), new Page(true, (handler,url)->server.sendFullInspectionPage(handler, url)));
		patternMap.put(Pattern.compile("(hardware|hw|software|sw|field|fd|ci|checkin|cube|sc)(/)*"), new Page(true, (handler,url)->server.sendInspectionTeamPage(handler, url)));
	}

	public boolean sendPage(Handler handler, String url, boolean verified) {
		System.out.println(url);
		Page p = map.get(url);
		if (p == null && url.charAt(url.length() - 1) == '/') {
			p = map.get(url.substring(0, url.length() - 1)); // check to see if there is a trailing slash (this should be handled elsewhere)
		}
		if (p != null) {
			System.out.println("got page for (" + url.length() + "): " + url + " was: " + p);
			if (p.requiresLogin && !verified) { //we don't have permission to view this
				def.send(handler);
			} else {
				System.out.println("sending page");
				p.send(handler);
			}
			return true;
		} else {
			Iterator<Entry<Pattern, Page>> it = patternMap.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Pattern, Page> e = it.next();
				if (e.getKey().matcher(url).matches()) {
					if (e.getValue().requiresLogin && !verified) {
						def.send(handler);
					} else {
						e.getValue().send(handler, url);
					}
					return true;
				}
			}
		}
		return false;
	}
	
	public String getResource(String page) {
		return resourceMap.get(page);
	}
}
