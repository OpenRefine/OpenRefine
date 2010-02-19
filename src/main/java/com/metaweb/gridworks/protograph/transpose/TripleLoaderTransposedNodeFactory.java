package com.metaweb.gridworks.protograph.transpose;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.protograph.AnonymousNode;
import com.metaweb.gridworks.protograph.CellKeyNode;
import com.metaweb.gridworks.protograph.CellNode;
import com.metaweb.gridworks.protograph.CellTopicNode;
import com.metaweb.gridworks.protograph.CellValueNode;
import com.metaweb.gridworks.protograph.FreebaseProperty;
import com.metaweb.gridworks.protograph.FreebaseTopicNode;
import com.metaweb.gridworks.protograph.ValueNode;

public class TripleLoaderTransposedNodeFactory implements TransposedNodeFactory {
	protected List<WritingTransposedNode> rootNodes = new LinkedList<WritingTransposedNode>();
	protected StringBuffer stringBuffer;
	protected Map<String, Long> varPool = new HashMap<String, Long>();
	
	public String getLoad() {
		stringBuffer = new StringBuffer();
		for (WritingTransposedNode node : rootNodes) {
			node.write(null, null);
		}
		return stringBuffer.toString();
	}
	
	protected void writeLine(String line) {
		if (stringBuffer.length() > 0) {
			stringBuffer.append('\n');
		}
		stringBuffer.append(line);
	}
	protected void writeLine(String subject, String predicate, String object) {
		if (subject != null) {
			writeLine("{ 's' : '" + subject + "', 'p' : '" + predicate + "', 'o' : " + object + " }");
		}
	}
	
	protected interface WritingTransposedNode extends TransposedNode {
		public String write(String subject, String predicate);
	}
	
	abstract protected class TransposedNodeWithChildren implements WritingTransposedNode {
		public List<FreebaseProperty> properties = new LinkedList<FreebaseProperty>();
		public List<WritingTransposedNode> children = new LinkedList<WritingTransposedNode>();
		
		protected void writeChildren(String subject) {
			for (int i = 0; i < children.size(); i++) {
				WritingTransposedNode child = children.get(i);
				String predicate = properties.get(i).id;
				
				child.write(subject, predicate);
			}
		}
	}
	
	protected class AnonymousTransposedNode extends TransposedNodeWithChildren {
		AnonymousNode node;
		
		protected AnonymousTransposedNode(
			AnonymousNode node
		) {
			this.node = node;
		}
		
		public String write(String subject, String predicate) {
			if (children.size() == 0 || subject == null) {
				return null;
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("{ ");
			
			boolean first = true;
			for (int i = 0; i < children.size(); i++) {
				String s = children.get(i).write(null, null);
				if (s != null) {
					if (first) {
						first = false;
					} else {
						sb.append(", ");
					}
					sb.append("\"" + properties.get(i).id + "\": ");
					sb.append(s);
				}
			}
			sb.append(" }");
			
			writeLine(subject, predicate, sb.toString());
			
			return null;
		}
	}
	
	protected class CellTopicTransposedNode extends TransposedNodeWithChildren {
		protected CellTopicNode node;
		protected Cell cell;
		
		public CellTopicTransposedNode(CellTopicNode node, Cell cell) {
			this.node = node;
			this.cell = cell;
		}
		
		public String write(String subject, String predicate) {
			String id = null;
			if (cell.recon != null && 
				cell.recon.judgment == Recon.Judgment.Matched &&
				cell.recon.match != null) {
				id = cell.recon.match.topicID;
			} else {
			    long var = 0;
			    if (varPool.containsKey(node.columnName)) {
			        var = varPool.get(node.columnName);
			    }
			    varPool.put(node.columnName, var + 1);
			    
				id = "$" + node.columnName.replaceAll("\\W+", "_") + "_" + var;
				
				writeLine("{ 's' : '" + id + "', 'p' : 'type', 'o' : '" + node.type.id + "' }");
				writeLine("{ 's' : '" + id + "', 'p' : 'name', 'o' : " + JSONObject.quote(cell.value.toString()) + " }");
			}
			
			if (subject != null) {
			    writeLine(subject, predicate, id);
			}
			
			writeChildren(id);
			
			return JSONObject.quote(id);
		}
	}
	
	protected class CellValueTransposedNode implements WritingTransposedNode {
		protected JSONObject obj;
		protected CellValueNode node;
		protected Cell cell;
		
		public CellValueTransposedNode(CellValueNode node, Cell cell) {
			this.node = node;
			this.cell = cell;
		}
		
		public String write(String subject, String predicate) {
			String object = cell.value instanceof String ? 
					JSONObject.quote((String) cell.value) : cell.value.toString();
					
			if (subject != null) {
				if ("/type/text".equals(node.lang)) {
					writeLine(
						"{ 's' : '" + subject + 
						"', 'p' : '" + predicate + 
						"', 'o' : " + object + 
						", 'lang' : '" + node.lang + 
						"' }"
					);
				} else {
					writeLine(
						"{ 's' : '" + subject + 
						"', 'p' : '" + predicate + 
						"', 'o' : " + object + " }"
					);
				}
			}
			
			return object;
		}
	}
	
	protected class CellKeyTransposedNode implements WritingTransposedNode {
		protected CellKeyNode node;
		protected Cell cell;
		
		public CellKeyTransposedNode(CellKeyNode node, Cell cell) {
			this.node = node;
			this.cell = cell;
		}
		
		public String write(String subject, String predicate) {
			writeLine(subject, "key", JSONObject.quote(node.namespace.id + "/" + cell.value));
			
			return null;
		}
	}
	
	protected class TopicTransposedNode extends TransposedNodeWithChildren {
		protected FreebaseTopicNode node;
		
		public TopicTransposedNode(FreebaseTopicNode node) {
			this.node = node;
		}

		public String write(String subject, String predicate) {
			String object = JSONObject.quote(node.topic.id);
			
			writeLine(subject, predicate, object);
			writeChildren(object);
			
			return object;
		}
	}

	protected class ValueTransposedNode implements WritingTransposedNode {
		protected ValueNode node;
		
		public ValueTransposedNode(ValueNode node) {
			this.node = node;
		}

		public String write(String subject, String predicate) {
			String object = node.value instanceof String ? 
					JSONObject.quote((String) node.value) : node.value.toString();
					
			if ("/type/text".equals(node.lang)) {
				writeLine(
					"{ 's' : '" + subject + 
					"', 'p' : '" + predicate + 
					"', 'o' : " + object + 
					", 'lang' : '" + node.lang + 
					"' }"
				);
			} else {
				writeLine(
					"{ 's' : '" + subject + 
					"', 'p' : '" + predicate + 
					"', 'o' : " + object + " }"
				);
			}
			
			return object;
		}
	}
	
	public TransposedNode transposeAnonymousNode(
			TransposedNode parentNode,
			FreebaseProperty property, 
			AnonymousNode node) {
		
		WritingTransposedNode tnode = new AnonymousTransposedNode(node);
		
		processTransposedNode(tnode, parentNode, property);
		
		return tnode;
	}

	public TransposedNode transposeCellNode(
			TransposedNode parentNode,
			FreebaseProperty property, 
			CellNode node, 
			Cell cell) {
		
		WritingTransposedNode tnode = null;
		if (node instanceof CellTopicNode) {
			tnode = new CellTopicTransposedNode((CellTopicNode) node, cell);
		} else if (node instanceof CellValueNode) {
			tnode = new CellValueTransposedNode((CellValueNode) node, cell);
		} else if (node instanceof CellKeyNode) {
			tnode = new CellKeyTransposedNode((CellKeyNode) node, cell);
		}
		
		if (tnode != null) {
			processTransposedNode(tnode, parentNode, property);
		}
		return tnode;
	}

	public TransposedNode transposeTopicNode(
			TransposedNode parentNode,
			FreebaseProperty property, 
			FreebaseTopicNode node) {
		
		WritingTransposedNode tnode = new TopicTransposedNode(node);
		
		processTransposedNode(tnode, parentNode, property);
		
		return tnode;
	}

	public TransposedNode transposeValueNode(
			TransposedNode parentNode,
			FreebaseProperty property, 
			ValueNode node) {
		
		WritingTransposedNode tnode = new ValueTransposedNode(node);
		
		processTransposedNode(tnode, parentNode, property);
		
		return tnode;
	}
	
	protected void processTransposedNode(
		WritingTransposedNode 	tnode, 
		TransposedNode 			parentNode,
		FreebaseProperty 		property 
	) {
		if (parentNode != null) {
			if (parentNode instanceof TransposedNodeWithChildren) {
				TransposedNodeWithChildren parentNode2 = (TransposedNodeWithChildren) parentNode;
				parentNode2.children.add(tnode);
				parentNode2.properties.add(property);
			}
		} else {
			rootNodes.add(tnode);
		}
	}
}
