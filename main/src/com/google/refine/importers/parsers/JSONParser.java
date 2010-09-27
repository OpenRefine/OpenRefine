package com.google.refine.importers.parsers;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;

public class JSONParser implements TreeParser{
	JsonFactory factory = new JsonFactory();
	JsonParser parser = null;

	public JSONParser(InputStream inputStream){
		try {
			parser = factory.createJsonParser(inputStream);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public int getAttributeCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getAttributeLocalName(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAttributePrefix(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAttributeValue(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getEventType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPrefix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasNext() throws ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int next() throws ServletException {
		// TODO Auto-generated method stub
		return 0;
	}

}
