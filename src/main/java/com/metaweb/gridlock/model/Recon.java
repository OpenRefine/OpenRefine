package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Recon implements Serializable {
	private static final long serialVersionUID = 8906257833709315762L;
	
	static public enum Judgment {
		None,
		Approve,
		New
	}
	
	public Map<String, Object> 	features = new HashMap<String, Object>();
	public List<ReconCandidate> candidates = new LinkedList<ReconCandidate>();
}
