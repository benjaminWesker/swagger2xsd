package com.bdef.bundle;

import java.io.UnsupportedEncodingException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CodeReturn {

    private final static ResourceBundle messageBundle = ResourceBundle.getBundle("properties.codeReturn");

	public static String getString(String key){			
		try {
			String value = messageBundle.getString(key); 
			return new String(value.getBytes("ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return key;
		}		
		catch (MissingResourceException e) {
			return key;
		}
	}

}
