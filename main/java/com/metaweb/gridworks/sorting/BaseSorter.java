package com.metaweb.gridworks.sorting;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.sorting.Criterion.KeyMaker;

abstract public class BaseSorter {
	protected Criterion[] 			_criteria;
	protected KeyMaker[] 			_keyMakers;
	protected ComparatorWrapper[]	_comparatorWrappers;
	protected List<Object[]>		_keys;
	
	public class ComparatorWrapper {
		final public int 		criterionIndex;
		final protected int 	multiplier;
		
		public ComparatorWrapper(int criterionIndex) {
			this.criterionIndex = criterionIndex;
			this.multiplier = _criteria[criterionIndex].reverse ? -1 : 1;
		}
		
		public Object getKey(Project project, Object o, int index) {
			while (index >= _keys.size()) {
				_keys.add(null);
			}
			
			Object[] keys = _keys.get(index);
			if (keys == null) {
				keys = makeKeys(project, o, index);
				_keys.set(index, keys);
			}
			return keys[criterionIndex];
		}
		
		public int compare(Project project, Object o1, int i1, Object o2, int i2) {
			Criterion c = _criteria[criterionIndex];
			Object key1 = getKey(project, o1, i1);
			Object key2 = getKey(project, o2, i2);
			
			if (key1 == null) {
				if (key2 == null) {
					return 0;
				} else if (key2 instanceof EvalError) {
					return c.blankPosition - c.errorPosition;
				} else {
					return c.blankPosition;
				}
			} else if (key1 instanceof EvalError) {
				if (key2 == null) {
					return c.errorPosition - c.blankPosition;
				} else if (key2 instanceof EvalError) {
					return 0;
				} else {
					return c.errorPosition;
				}
			} else {
				if (key2 == null) {
					return -c.blankPosition;
				} else if (key2 instanceof EvalError) {
					return -c.errorPosition;
				} else {
					return _keyMakers[criterionIndex].compareKeys(key1, key2) * multiplier;
				}
			}
		}
	}
	
	public void initializeFromJSON(Project project, JSONObject obj) throws JSONException {
		if (obj.has("criteria") && !obj.isNull("criteria")) {
			JSONArray a = obj.getJSONArray("criteria");
			int count = a.length();
			
			_criteria = new Criterion[count];
			_keyMakers = new KeyMaker[count];
			_comparatorWrappers = new ComparatorWrapper[count];
			
			for (int i = 0; i < count; i++) {
				JSONObject obj2 = a.getJSONObject(i);
				
				_criteria[i] = createCriterionFromJSON(project, obj2);
				_keyMakers[i] = _criteria[i].createKeyMaker();
				_comparatorWrappers[i] = new ComparatorWrapper(i);
			}
		} else {
			_criteria = new Criterion[0];
			_keyMakers = new KeyMaker[0];
			_comparatorWrappers = new ComparatorWrapper[0];
		}
	}
	
	public boolean hasCriteria() {
		return _criteria != null && _criteria.length > 0;
	}
	
	protected Criterion createCriterionFromJSON(Project project, JSONObject obj) throws JSONException {
		String valueType = "string";
		if (obj.has("valueType") && !obj.isNull("valueType")) {
			valueType = obj.getString("valueType");
		}
		
		Criterion c = null;
		if ("boolean".equals(valueType)) {
			c = new BooleanCriterion();
		} else if ("date".equals(valueType)) {
			c = new DateCriterion();
		} else if ("number".equals(valueType)) {
			c = new NumberCriterion();
		} else {
			c = new StringCriterion();
		}
		
		c.initializeFromJSON(project, obj);
		return c;
	}
	
	abstract protected Object makeKey(
			Project project, KeyMaker keyMaker, Criterion c, Object o, int index);
	
	protected Object[] makeKeys(Project project, Object o, int index) {
		Object[] keys = new Object[_keyMakers.length];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = makeKey(project, _keyMakers[i], _criteria[i], o, index);
		}
		return keys;
	}
	
	protected int compare(Project project, Object o1, int i1, Object o2, int i2) {
		int c = 0;
		for (int i = 0; c == 0 && i < _comparatorWrappers.length; i++) {
			c = _comparatorWrappers[i].compare(project, o1, i1, o2, i2);
		}
		return c;
	}
}
