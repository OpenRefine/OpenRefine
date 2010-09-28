package com.google.refine.importers.parsers;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

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
	public TreeParserToken getEventType() throws ServletException {
		return this.convertToTreeParserToken(parser.getCurrentToken());
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
	public TreeParserToken next() throws ServletException {
		JsonToken next;
        try {
            next = parser.nextToken();
        } catch (JsonParseException e) {
            throw new ServletException(e.getMessage());
        } catch (IOException e) {
            throw new ServletException(e.getMessage());
        }
        
		if(next == null)
		    throw new ServletException("No more Json Tokens in stream");
		
		return convertToTreeParserToken(next);
	}
	
	protected TreeParserToken convertToTreeParserToken(JsonToken token) throws ServletException{
	    switch(token){
            case START_ARRAY: return TreeParserToken.StartEntity;
            case END_ARRAY: return TreeParserToken.EndEntity;
            case START_OBJECT: return TreeParserToken.StartEntity;
            case END_OBJECT: return TreeParserToken.EndEntity;
            case VALUE_STRING: return TreeParserToken.Value;
            //Json does not have START_DOCUMENT
            //Json does not have END_DOCUMENT
            
            //TODO finish the rest of the cases
            default: throw new ServletException("Not yet implemented");
        }
	}

}
