
package org.openrefine.wikibase.schema.validation;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A part of a path to a faulty/missing element in a schema.
 * 
 * @author Antonin Delpeuch
 *
 */
public class PathElement {

    /**
     * The type of data field to follow to reach the faulty/missing part of the schema
     */
    public enum Type {
        @JsonProperty("entity")
        ENTITY, @JsonProperty("subject")
        SUBJECT, @JsonProperty("label")
        LABEL, @JsonProperty("description")
        DESCRIPTION, @JsonProperty("alias")
        ALIAS, @JsonProperty("sitelink")
        SITELINK, @JsonProperty("badge")
        BADGE, @JsonProperty("statement")
        STATEMENT, @JsonProperty("property")
        PROPERTY, @JsonProperty("rank")
        RANK, @JsonProperty("value")
        VALUE, @JsonProperty("unit")
        UNIT, @JsonProperty("language")
        LANGUAGE, @JsonProperty("qualifier")
        QUALIFIER, @JsonProperty("reference")
        REFERENCE, @JsonProperty("filename")
        FILENAME, @JsonProperty("filepath")
        FILEPATH, @JsonProperty("wikitext")
        WIKITEXT
    };

    private final Type type;
    private final int position;
    private final String name;

    /**
     * Constructs a PathElement given by the type of element in which to recurse, and an ordinal number indicating which
     * of the children of that type to follow.
     */
    public PathElement(Type type, int position) {
        this.type = type;
        this.position = position;
        this.name = null;
    }

    /**
     * Constructs a PathElement given by the type of the element in which to recurse, as well as a string to identify
     * which one (in case there are multiple ones). For instance, indicating which property to follow, to select the
     * right statement in an item.
     */
    public PathElement(Type type, String name) {
        this.type = type;
        this.position = -1;
        this.name = name;
    }

    /**
     * A PathElement in which there is a single element of the given type, so the ordinal number is not needed.
     */
    public PathElement(Type type) {
        this.type = type;
        this.position = -1;
        this.name = null;
    }

    @JsonProperty("type")
    public Type getType() {
        return type;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("position")
    public int getPosition() {
        return position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PathElement other = (PathElement) obj;
        return position == other.position && type == other.type && Objects.equals(name, other.getName());
    }

    @Override
    public String toString() {
        return "PathElement [type=" + type + ", position=" + position + ", name=" + Objects.toString(name) + "]";
    }

}
