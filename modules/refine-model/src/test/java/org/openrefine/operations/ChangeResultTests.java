
package org.openrefine.operations;

import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.testng.annotations.Test;

import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ChangeResultTests {

    Grid grid = mock(Grid.class);

    @Test
    public void testSerialize() {
        List<FacetConfig> facets = Collections.singletonList(new MyFacetConfig());
        ChangeResult SUT = new ChangeResult(grid, GridPreservation.PRESERVES_RECORDS, facets);

        String expectedSerialization = "{\n"
                + "  \"createdFacets\" : [ {\n"
                + "    \"type\" : \"core/myfacet\"\n"
                + "  } ]\n"
                + "}";
        TestUtils.isSerializedTo(SUT, expectedSerialization, ParsingUtilities.defaultWriter);
    }

    protected static class MyFacetConfig implements FacetConfig {

        @Override
        public Facet apply(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, long projectId) {
            throw new NotImplementedException();
        }

        @Override
        public Set<String> getColumnDependencies() {
            throw new NotImplementedException();
        }

        @Override
        public FacetConfig renameColumnDependencies(Map<String, String> substitutions) {
            throw new NotImplementedException();
        }

        @Override
        public boolean isNeutral() {
            return true;
        }

        @Override
        public String getJsonType() {
            return "core/myfacet";
        }
    }
}
