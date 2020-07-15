package org.openrefine.wikidata.manifests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openrefine.wikidata.manifests.v1_0.ManifestV1_0;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ManifestManager {

    private Manifest manifest;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final ManifestManager instance = new ManifestManager();

    public static ManifestManager getInstance() {
        return instance;
    }

    private ManifestManager() {
        try {
            InputStream manifestFile = this.getClass().getClassLoader().getResourceAsStream("wikidata-manifest-v1.0.json");
            String manifestJson = IOUtils.toString(manifestFile, StandardCharsets.UTF_8);
            updateManifest(manifestJson);
        } catch (IOException | ManifestException e) {
            e.printStackTrace();
        }
    }

    public void updateManifest(String manifestJson) throws ManifestException {
        JsonNode root;
        try {
            root = mapper.readTree(manifestJson);
        } catch (JsonProcessingException e) {
            throw new ManifestException("invalid json format");
        }

        String version = root.path("version").textValue();
        if (StringUtils.isBlank(version)) throw new ManifestException("");

        if ("1.0".equals(version)) {
            manifest = new ManifestV1_0(root);
        } else {
            throw new ManifestException("unsupported manifest version: " + version);
        }
    }

    public Manifest getManifest() {
        return manifest;
    }
}
