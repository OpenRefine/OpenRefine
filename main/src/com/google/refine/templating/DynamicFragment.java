package com.google.refine.templating;

import com.google.refine.expr.Evaluable;

class DynamicFragment extends Fragment {
	final public Evaluable eval;
	
	public DynamicFragment(Evaluable eval) {
		this.eval = eval;
	}
}
