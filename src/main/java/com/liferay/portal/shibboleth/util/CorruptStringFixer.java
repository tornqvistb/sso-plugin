package com.liferay.portal.shibboleth.util;
import java.nio.ByteBuffer;

public class CorruptStringFixer {
	
	public String fixString(String corruptString) {
		
		if (corruptString == null) {
			return "";
		}
		
		String result = corruptString;		
		
		byte []bytes = corruptString.getBytes();
		int i = 0;
		StringBuffer sb = new StringBuffer();
		for (byte b:bytes) {
			if (i > 0)
				sb.append(":");
			sb.append(b);
			i++;
		}

		String s = replaceSubString(sb.toString());
		
		String[] array = s.split(":");
		
		// Create empty string to instantiate String Buffer
		StringBuffer emptyBuffer = new StringBuffer();
		for (String str : array) {
			emptyBuffer.append(" ");
		}
		
		ByteBuffer bb = ByteBuffer.wrap(emptyBuffer.toString().getBytes());
		
		for (String str : array) {
			bb.put(Byte.valueOf(str));
		}
		
		result = new String(bb.array());

		return result;
	}
	
	private String replaceSubString(String s) {
		s = s.replaceAll("-61:-125:-62:-91", "-61:-91"); 		// å
		s = s.replaceAll("-61:-125:-62:-92", "-61:-92");		// ä
		s = s.replaceAll("-61:-125:-62:-74", "-61:-74");		// ö
		s = s.replaceAll("-61:-125:-62:-87", "-61:-87");		// é
		s = s.replaceAll("-61:-125:-62:-123", "-61:-123");		// Å
		s = s.replaceAll("-61:-125:-62:-124", "-61:-124");		// Ä
		s = s.replaceAll("-61:-125:-62:-106", "-61:-106");		// Ö
		s = s.replaceAll("-61:-125:-62:-119", "-61:-119");		// É
		return s;
	}

}
