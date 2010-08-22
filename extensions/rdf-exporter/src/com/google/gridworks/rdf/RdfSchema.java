package com.google.gridworks.rdf;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.gridworks.model.OverlayModel;
import com.google.gridworks.model.Project;
import com.google.gridworks.rdf.ResourceNode.RdfType;

public class RdfSchema implements OverlayModel {

    final protected List<Node> _rootNodes = new ArrayList<Node>();
    final protected List<ConstantBlankNode> _blanks = new ArrayList<ConstantBlankNode>();

    public List<ConstantBlankNode> get_blanks() {
        return _blanks;
    }
    
    @Override
    public void onBeforeSave() {
    }
    
    @Override
    public void onAfterSave() {
    }
    
    
    @Override
    public void dispose() {
    }

    protected URI baseUri;

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public void setBaseUri(String base) throws URISyntaxException {
        this.baseUri = new URI(base);
    }

    public RdfSchema() {
        // FIXME
        try {
            this.baseUri = new URI("http://localhost:3333/");
        } catch (URISyntaxException ue) {
        }
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public Node getRoot() {
        return _rootNodes.get(0);
    }

    static public RdfSchema reconstruct(JSONObject o) throws JSONException {
        RdfSchema s = new RdfSchema();
        // TODO
        try {
            s.baseUri = new URI(o.getString("baseUri"));
        } catch (URISyntaxException me) {
            me.printStackTrace();
        }

        JSONArray rootNodes = o.getJSONArray("rootNodes");
        int count = rootNodes.length();

        for (int i = 0; i < count; i++) {
            JSONObject o2 = rootNodes.getJSONObject(i);
            Node node = reconstructNode(o2, s);
            if (node != null) {
                s._rootNodes.add(node);
            }
        }

        return s;
    }

    static protected Node reconstructNode(JSONObject o, RdfSchema s)
            throws JSONException {
        Node node = null;
        int blanksCount = 0;
        String nodeType = o.getString("nodeType");
        if (nodeType.startsWith("cell-as-")) {
            int columnIndex = o.getInt("columnIndex");
            String columnName = null;
            if(columnIndex!=-1){
                columnName = o.getString("columnName");
            }

            if ("cell-as-resource".equals(nodeType)) {
                String exp = o.getString("uriExpression");
                node = new CellResourceNode(columnIndex, columnName, exp);
                if (o.has("rdfTypes")) {
                    List<RdfType> types = reconstructTypes(o
                            .getJSONArray("rdfTypes"));
                    ((CellResourceNode) node).setTypes(types);
                }
            } else if ("cell-as-literal".equals(nodeType)) {
                String valueType = o.has("valueType")?Util.getDataType(o.getString("valueType")):null;
                String lang = o.has("lang") ? o.getString("lang"):null;
                node = new CellLiteralNode(columnIndex, columnName, valueType, lang);
            } else if ("cell-as-blank".equals(nodeType)) {
                node = new CellBlankNode(columnIndex,columnName);
                if (o.has("rdfTypes")) {
                    List<RdfType> types = reconstructTypes(o
                            .getJSONArray("rdfTypes"));
                    ((CellBlankNode) node).setTypes(types);
                }
            }
        } else if ("resource".equals(nodeType)) {
            node = new ConstantResourceNode(o.getString("uri"));
            if (o.has("rdfTypes")) {
                List<RdfType> types = reconstructTypes(o
                        .getJSONArray("rdfTypes"));
                ((ConstantResourceNode) node).setTypes(types);
            }
        } else if ("literal".equals(nodeType)) {
            String valueType = o.has("valueType")?Util.getDataType(o.getString("valueType")):null;
            String lang = o.has("lang") ? o.getString("lang"):null;
            node = new ConstantLiteralNode(o.getString("value"), valueType,lang);
        } else if ("blank".equals(nodeType)) {
            node = new ConstantBlankNode(blanksCount);
            blanksCount += 1;
            s._blanks.add((ConstantBlankNode) node);
            if (o.has("rdfTypes")) {
                List<RdfType> types = reconstructTypes(o
                        .getJSONArray("rdfTypes"));
                ((ConstantBlankNode) node).setTypes(types);
            }
        }

        if (node != null && node instanceof ResourceNode && o.has("links")) {
            ResourceNode node2 = (ResourceNode) node;

            JSONArray links = o.getJSONArray("links");
            int linkCount = links.length();

            for (int j = 0; j < linkCount; j++) {
                JSONObject oLink = links.getJSONObject(j);

                node2.addLink(new Link(oLink.getString("uri"), oLink.getString("curie"),oLink
                        .has("target")
                        && !oLink.isNull("target") ? reconstructNode(oLink
                        .getJSONObject("target"), s) : null));
            }
        }

        return node;
    }

    static private List<RdfType> reconstructTypes(JSONArray arr)
            throws JSONException {
        List<RdfType> lst = new ArrayList<RdfType>();
        for (int i = 0; i < arr.length(); i++) {
            String uri = arr.getJSONObject(i).getString("uri");
            String curie = arr.getJSONObject(i).getString("curie");
            lst.add(new RdfType(uri, curie));
        }
        return lst;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("baseUri"); writer.value(baseUri);
        
        writer.key("rootNodes");
        writer.array();
        
        for (Node node : _rootNodes) {
            node.write(writer, options);
        }
        
        writer.endArray();
        writer.endObject();
    }
    
    static public RdfSchema load(Project project, JSONObject obj) throws Exception {
        return reconstruct(obj);
    }
}
