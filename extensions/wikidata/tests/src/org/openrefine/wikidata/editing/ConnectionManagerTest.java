package org.openrefine.wikidata.editing;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.ParsingUtilities;
import org.openrefine.wikidata.testing.WikidataRefineTest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wikidata.wdtk.wikibaseapi.BasicApiConnection;
import org.wikidata.wdtk.wikibaseapi.OAuthApiConnection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openrefine.wikidata.editing.ConnectionManager.PREFERENCE_STORE_KEY;
import static org.testng.Assert.*;

@PrepareForTest({ConnectionManager.class, BasicApiConnection.class})
public class ConnectionManagerTest extends WikidataRefineTest {

    Constructor<ConnectionManager> constructor;

    @BeforeClass
    public void initConstructor() throws NoSuchMethodException {
        constructor = ConnectionManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

    @Test
    public void testCantRestoreConnection() throws IllegalAccessException, InvocationTargetException, InstantiationException {
        PreferenceStore prefStore = new PreferenceStore();
        ProjectManager.singleton = mock(ProjectManager.class);
        when(ProjectManager.singleton.getPreferenceStore()).thenReturn(prefStore);
        ConnectionManager manager = constructor.newInstance();
        assertFalse(manager.isLoggedIn());
    }

    @Test
    public void testRestoreConnectionByPassword() throws Exception {
        PreferenceStore prefStore = new PreferenceStore();
        ArrayNode array = ParsingUtilities.mapper.createArrayNode();
        ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
        obj.put("username", "foo");
        obj.put("password", "123456");
        array.add(obj);
        prefStore.put(PREFERENCE_STORE_KEY, array);
        ProjectManager.singleton = mock(ProjectManager.class);
        when(ProjectManager.singleton.getPreferenceStore()).thenReturn(prefStore);

        BasicApiConnection connection = mock(BasicApiConnection.class);
        PowerMockito.whenNew(BasicApiConnection.class).withAnyArguments().thenReturn(connection);

        ConnectionManager manager = constructor.newInstance();
        assertTrue(manager.isLoggedIn());
    }


    @Test
    public void testRestoreConnectionByOAuth() throws Exception {
        PreferenceStore prefStore = new PreferenceStore();
        ArrayNode array = ParsingUtilities.mapper.createArrayNode();
        ObjectNode obj = ParsingUtilities.mapper.createObjectNode();
        obj.put("access_token", "access_token123");
        obj.put("access_secret", "access_secret456");
        array.add(obj);
        prefStore.put(PREFERENCE_STORE_KEY, array);
        ProjectManager.singleton = mock(ProjectManager.class);
        when(ProjectManager.singleton.getPreferenceStore()).thenReturn(prefStore);

        OAuthApiConnection connection = mock(OAuthApiConnection.class);
        when(connection.getCurrentUser()).thenReturn("foo");
        PowerMockito.whenNew(OAuthApiConnection.class).withAnyArguments().thenReturn(connection);

        ConnectionManager manager = constructor.newInstance();
        assertTrue(manager.isLoggedIn());
        assertEquals("foo", manager.getUsername());
    }

}
