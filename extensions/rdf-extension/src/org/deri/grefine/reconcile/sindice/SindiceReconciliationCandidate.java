package org.deri.grefine.reconcile.sindice;

import org.deri.grefine.reconcile.model.ReconciliationCandidate;

public class SindiceReconciliationCandidate extends ReconciliationCandidate{

		private final String domain;
		
		public SindiceReconciliationCandidate(String id, String name, String domain, String[] types, double score, boolean match){
			super(id, name, types,score, match);
			this.domain = domain;
		}

		public String getDomain() {
			return domain;
		}
		
}
