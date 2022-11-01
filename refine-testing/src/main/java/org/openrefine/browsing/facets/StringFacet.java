
package org.openrefine.browsing.facets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.openrefine.model.ColumnModel;
import org.openrefine.overlay.OverlayModel;

public class StringFacet implements Facet {

    public static class Config implements FacetConfig {

        public String columnName;
        public String selected;

        public Config(String columnName, String selected) {
            this.columnName = columnName;
            this.selected = selected;
        }

        @Override
        public Facet apply(ColumnModel columnModel, Map<String, OverlayModel> overlayModels) {
            return new StringFacet(columnModel.getColumnIndexByName(columnName), this);
        }

        @Override
        public Set<String> getColumnDependencies() {
            return Collections.singleton(columnName);
        }

        @Override
        public FacetConfig renameColumnDependencies(Map<String, String> substitutions) {
            return new Config(substitutions.getOrDefault(columnName, columnName), selected);
        }

        @Override
        public boolean isNeutral() {
            return selected == null;
        }

        @Override
        public String getJsonType() {
            return null;
        }
    }

    private int columnIndex;
    private Config config;

    public StringFacet(int columnIndex, Config config) {
        this.columnIndex = columnIndex;
        this.config = config;
    }

    @Override
    public FacetConfig getConfig() {
        return config;
    }

    @Override
    public FacetState getInitialFacetState() {
        return new StringFacetState();
    }

    @Override
    public FacetAggregator<?> getAggregator() {
        return new StringFacetAggregator(columnIndex, config.selected);
    }

    @Override
    public FacetResult getFacetResult(FacetState state) {
        return (StringFacetState) state;
    }

}
