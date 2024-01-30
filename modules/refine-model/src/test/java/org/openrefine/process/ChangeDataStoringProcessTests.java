
package org.openrefine.process;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.browsing.Engine.Mode;
import org.openrefine.history.History;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataId;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.ChangeDataStore;

public class ChangeDataStoringProcessTests {

    History history;
    ChangeDataStore changeDataStore;
    ChangeDataSerializer<String> serializer;
    ChangeDataId changeDataId;
    Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess;
    ChangeDataStoringProcess<String> SUT;

    @BeforeMethod
    public void setUp() {
        history = mock(History.class);
        changeDataStore = mock(ChangeDataStore.class);
        when(history.getChangeDataStore()).thenReturn(changeDataStore);
        changeDataId = new ChangeDataId(1234L, "subdir");
        completionProcess = incompleteChangeData -> null;
        serializer = new ChangeDataSerializer<String>() {

            @Override
            public String serialize(String changeDataItem) {
                return changeDataItem;
            }

            @Override
            public String deserialize(String serialized) throws IOException {
                return serialized;
            }
        };

        SUT = new ChangeDataStoringProcess<>(
                "description",
                Optional.empty(),
                changeDataId, changeDataStore, serializer, completionProcess,
                null, history, 3, Mode.RowBased);
    }

    @Test
    public void testRequirementsSatisfied() {
        when(history.isStreamableAtStep(3)).thenReturn(true);

        assertTrue(SUT.hasSatisfiedDependencies());
    }

    @Test
    public void testRequirementsNotSatisfied() {
        when(history.isStreamableAtStep(3)).thenReturn(false);

        assertFalse(SUT.hasSatisfiedDependencies());
    }
}
