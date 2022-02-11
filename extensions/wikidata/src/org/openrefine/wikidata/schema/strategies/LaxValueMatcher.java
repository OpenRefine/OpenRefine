package org.openrefine.wikidata.schema.strategies;

import java.net.URI;
import java.net.URISyntaxException;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * Generic matcher which attempts to equate values
 * which should generally be considered equivalent
 * in most data import contexts.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LaxValueMatcher implements ValueMatcher {

	@Override
	public boolean match(Value existing, Value added) {
		if (existing instanceof EntityIdValue && added instanceof EntityIdValue) {
			// only compare the string ids, not the siteIRIs, to avoid
			// federation-related issues. We expect that in a given context,
			// only entities from a given Wikibase can appear.
			// TODO revisit this when (if?) federation support makes it possible
			// to mix up entities from different Wikibases in the same data slot
			return ((EntityIdValue)existing).getId().equals(((EntityIdValue) added).getId());
			
		} else if (existing instanceof StringValue && added instanceof StringValue) {
			// disregard trailing whitespace differences
			String existingStr = ((StringValue) existing).getString().trim();
			String addedStr = ((StringValue) added).getString().trim();
			// if they look like URLs, then http(s) and trailing slashes do not matter
			try {
				URI existingUrl = extraURINormalize(new URI(existingStr).normalize());
				URI addedUrl = extraURINormalize(new URI(addedStr).normalize());
				return existingUrl.equals(addedUrl);
			} catch(URISyntaxException e) {
				; // fall back on basic comparison
			}
			return existingStr.equals(addedStr);
			
		} else if (existing instanceof MonolingualTextValue && added instanceof MonolingualTextValue) {
			// ignore differences of trailing whitespace
			MonolingualTextValue existingMTV = (MonolingualTextValue) existing;
			MonolingualTextValue addedMTV = (MonolingualTextValue) added;
			return (existingMTV.getLanguageCode().equals(addedMTV.getLanguageCode()) && 
					existingMTV.getText().trim().equals(addedMTV.getText().trim()));
		}
		// fall back to exact comparison for other datatypes
		return existing.equals(added);
	}
	
	// utility function to remove some more differences from URLs
	protected URI extraURINormalize(URI uri) throws URISyntaxException {
		String scheme = uri.getScheme();
		String userInfo = uri.getUserInfo();
		String host = uri.getHost();
		int port = uri.getPort();
		String path = uri.getPath();
		String query = uri.getQuery();
		String fragment = uri.getFragment();
		if ("https".equals(scheme)) {
			scheme = "http";
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length()-1);
		}
		return new URI(scheme, userInfo, host, port, path, query, fragment);
	}

	@Override
	public String toString() {
		return "LaxValueMatcher";
	}

	@Override
	public int hashCode() {
		// constant because this object has no fields
		return 2127;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return getClass() == obj.getClass();
	}
}
