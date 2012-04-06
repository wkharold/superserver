package com.dell.dea.superserver.dispatcher.impl;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SuperServerHeader {
	public static class Option {
		public final String key;
		public final String value;
		
		public Option(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public Option(String key) {
			this.key = key;
			this.value = "_present_";
		}
	}
	
	public static SuperServerHeader BOGUS_HEADER = new SuperServerHeader();
	
	public static SuperServerHeader parse(String hdr) {
		SuperServerHeader result = BOGUS_HEADER;
		
		String protocolVersion;
		String service;
		String serviceVersion;
		int optionCount = 0;
		List<Option> options = new ArrayList<Option>();
		
		String[] tokens = hdr.split("\\x20");
		
		if (tokens.length <= 0 || tokens[0].startsWith("backtalk") == false) {
			result.parseError = "Missing Protocol";
			return result;
		}

		Pattern p = Pattern.compile("backtalk/(\\d+\\.\\d+)");
		Matcher m = p.matcher(tokens[0]);
		
		if (m.matches()) {
			protocolVersion = m.group(1);
		} else {
			result.parseError = "Bad Protocol Version";
			return result;
		}
		
		if (tokens.length <= 1) {
			result.parseError = "Incomplete Header";
			return result;
		} else {
			p = Pattern.compile("\\w+");
			m = p.matcher(tokens[1]);

			if (m.matches()) {
				service = m.group(0);
			} else {
				result.parseError = "Missing Service";
				return result;
			}
		}
		
		if (tokens.length < 3) {
			result.parseError = "Missing Service Version";
			return result;
		} else {
			p = Pattern.compile("(\\d+\\.\\d+(\\.\\d+)?(-(RELEASE|SNAPSHOT))?)");
			m = p.matcher(tokens[2]);
			
			if (m.matches()) {
				serviceVersion = m.group(0);
			} else {
				result.parseError = "Bad Service Version";
				return result;
			}
		}
		
		if (tokens.length >= 4) {
			p = Pattern.compile("\\((\\p{Graph}+?)\\)");
			m = p.matcher(tokens[3]);
			
			if (m.matches()) {
				try {
					options = parseOptions(m.group(1));
					optionCount = options.size();
				} catch (ParseException e) {
					result.parseError = "Bad Option Specification";
					return result;
				}
			} else {
				result.parseError = "Bad Option Specification";
				return result;
			}
		}
		
		result = new SuperServerHeader(protocolVersion, service, serviceVersion, optionCount, options);
		return result;
	}
	
	private static List<Option> parseOptions(String ops) throws ParseException {
		List<Option> result = new ArrayList<Option>();
		
		String[] options = ops.split(";");
		
		Pattern keyvalue = Pattern.compile("([\\p{Graph}&&[^\\p{Punct}]]+?)=(\\p{Graph}+?)");
		Pattern tag = Pattern.compile("[\\p{Graph}&&[^\\p{Punct}]]+?");
		
		for (String option : options) {
			Matcher kv = keyvalue.matcher(option);
			Matcher t = tag.matcher(option);
			if (kv.matches()) {
				result.add(new Option(kv.group(1), kv.group(2)));
			} else if (t.matches()){
				result.add(new Option(t.group(0)));
			} else {
				throw new ParseException("Bad Option Specification", 0);
			}
		}
		
		return result;
	}
	
	public final String protocolVersion;
	public final String service;
	public final String serviceVersion;
	public final int optionCount;
	public final List<Option> options;
	
	private String parseError;
	
	private SuperServerHeader(String protocolVersion, String service, String serviceVersion, int optionCount, List options) {
		this.protocolVersion = protocolVersion;
		this.service = service;
		this.serviceVersion = serviceVersion;
		this.optionCount = optionCount;
		this.options = options;
	}
	
	private SuperServerHeader() {
		protocolVersion = null;
		service = null;
		serviceVersion = null;
		optionCount = 0;
		options = null;
	}
	
	public String getParseError() {
		return parseError;
	}
}
