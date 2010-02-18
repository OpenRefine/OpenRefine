package com.metaweb.gridworks.browsing.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.DecoratedValue;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.filters.ExpressionEqualRowFilter;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.Parser;
import com.metaweb.gridworks.model.Project;

public class ListFacet implements Facet {
	protected List<NominalFacetChoice> _selection = new LinkedList<NominalFacetChoice>();
	protected List<NominalFacetChoice> _choices = new LinkedList<NominalFacetChoice>();
	
	protected String 	_name;
	protected String 	_expression;
	protected String	_columnName;
	protected int		_cellIndex;
	protected Evaluable _eval;
	
	public ListFacet() {
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("name"); writer.value(_name);
		writer.key("expression"); writer.value(_expression);
		writer.key("columnName"); writer.value(_columnName);
		
		writer.key("choices"); writer.array();
		for (NominalFacetChoice choice : _choices) {
			choice.write(writer, options);
		}
		writer.endArray();
		writer.endObject();
	}

	public void initializeFromJSON(Project project, JSONObject o) throws Exception {
		_name = o.getString("name");
		_expression = o.getString("expression");
		_columnName = o.getString("columnName");
		_cellIndex = project.columnModel.getColumnByName(_columnName).getCellIndex();
		
		_eval = new Parser(_expression).getExpression();
		_selection.clear();
		
		JSONArray a = o.getJSONArray("selection");
		int length = a.length();
		
		for (int i = 0; i < length; i++) {
			JSONObject oc = a.getJSONObject(i);
			JSONObject ocv = oc.getJSONObject("v");
			
			DecoratedValue decoratedValue = new DecoratedValue(
				ocv.get("v"), ocv.getString("l"));
			
			NominalFacetChoice nominalFacetChoice = new NominalFacetChoice(decoratedValue);
			nominalFacetChoice.selected = true;
			
			_selection.add(nominalFacetChoice);
		}
	}

	public RowFilter getRowFilter() {
		return _selection.size() == 0 ? null :
			new ExpressionEqualRowFilter(_eval, _cellIndex, createMatches());
	}

	public void computeChoices(Project project, FilteredRows filteredRows) {
		ExpressionNominalRowGrouper grouper = 
			new ExpressionNominalRowGrouper(_eval, _cellIndex);
		
		filteredRows.accept(project, grouper);
		
		_choices.clear();
		_choices.addAll(grouper.choices.values());
		
		for (NominalFacetChoice choice : _selection) {
			if (grouper.choices.containsKey(choice.decoratedValue.value)) {
				grouper.choices.get(choice.decoratedValue.value).selected = true;
			} else {
				choice.count = 0;
				_choices.add(choice);
			}
		}
	}
	
	protected Object[] createMatches() {
		Object[] a = new Object[_selection.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = _selection.get(i).decoratedValue.value;
		}
		return a;
	}
}
