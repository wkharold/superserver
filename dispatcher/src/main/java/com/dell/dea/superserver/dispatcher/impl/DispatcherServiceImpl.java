package com.dell.dea.superserver.dispatcher.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dell.dea.superserver.dispatcher.DispatcherService;
import com.dell.dea.superserver.handler.HandlerService;

public class DispatcherServiceImpl implements DispatcherService {
	public static final String SERVICE_NAME_KEY = "__SERVICE__";
	public static final String SERVICE_VERSION_KEY = "__VERSION__";
	
	private static final String HANDLER_SERVICE_KEY = "__HANDLER__";
	
	private List<Map> handlers = new ArrayList<Map>();
	
    public Runnable newHandler(final Socket s) {
    	return new Runnable() {
    		public void run() {
    			try {
					BufferedReader rdr = new BufferedReader(new InputStreamReader(s.getInputStream()));
					SuperServerHeader h = SuperServerHeader.parse(rdr.readLine());
					
					HandlerService hs = findHandler(h.service, h.serviceVersion);
					if (hs == null) {
						s.getOutputStream().write("no such service".getBytes());
						s.close();
					} else {
						s.getOutputStream().write("proceed".getBytes());
						hs.handle(s);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	};
    }
    
    public void bindHandler(HandlerService h, Map properties) {
    	if (isBound(h, properties) == false) {
    		HashMap hprops = new HashMap(properties);
	    	hprops.put(HANDLER_SERVICE_KEY, h);
	    	handlers.add(hprops);
    	}
    }
    
    public void unbindHandler(HandlerService h, Map properties) {
    	if (h != null && properties != null) {
    		String targetService = (String) properties.get(SERVICE_NAME_KEY);
    		String targetVersion = (String) properties.get(SERVICE_VERSION_KEY);
    		
    		if (targetService != null && targetVersion != null) {
	        	Iterator<Map> i = handlers.listIterator();
	        	while (i.hasNext()) {
	        		Map handler = i.next();
	        		
	        		HandlerService hs = (HandlerService) handler.get(HANDLER_SERVICE_KEY);
	        		String s = (String) handler.get(SERVICE_NAME_KEY);
	        		String v = (String) handler.get(SERVICE_VERSION_KEY);
	        		
	        		if ((s != null && s.equalsIgnoreCase(targetService)) &&
	        			(v != null && v.equalsIgnoreCase(targetVersion))) {
	        			i.remove();
	        			break;
	        		}
	        	}
    		}
    	}
    }
    
    public boolean isBound(HandlerService h, Map properties) {
    	String service = (String) properties.get(SERVICE_NAME_KEY);
    	String version = (String) properties.get(SERVICE_VERSION_KEY);
    	
    	boolean result = false;
    	
    	if (service != null && version != null) {
        	HandlerService hs = findHandler(service, version);
        	result = hs != null && hs == h;
    	}
    	
    	return result;
    }
    
    private HandlerService findHandler(String service, String version) {
    	HandlerService result = null;
    	
    	for (Map handler : handlers) {
    		String s = (String) handler.get(SERVICE_NAME_KEY);
    		String v = (String) handler.get(SERVICE_VERSION_KEY);
    		
    		if ((s != null && s.equalsIgnoreCase(service)) &&
    		    (v != null && v.equalsIgnoreCase(version))) {
    			result = (HandlerService) handler.get(HANDLER_SERVICE_KEY);
    			break;
    		}
    	}
    	
    	return result;
    }
}

