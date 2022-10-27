
package org.openrefine.wikibase.manifests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManifestParser {

    private static final Logger logger = LoggerFactory.getLogger(ManifestParser.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Manifest parse(String manifestJson) throws ManifestException {
        JsonNode root;
        try {
            root = mapper.readTree(manifestJson);
        } catch (JsonProcessingException e) {
            throw new ManifestException("invalid manifest format", e);
        }
        return parse(root);
    }

    public static Manifest parse(JsonNode manifestJson) throws ManifestException {
        String version = manifestJson.path("version").textValue();
        if (StringUtils.isBlank(version)) {
            throw new ManifestException("invalid manifest format, version is missing");
        }
        if (!version.matches("[0-9]+\\.[0-9]+")) {
            throw new ManifestException("invalid version: " + version);
        }

        String majorVersion = version.split("\\.")[0];
        // support only v1.x for now
        if ("1".equals(majorVersion)) {
            return new ManifestV1(manifestJson);
        } else if ("2".equals(majorVersion)) {
            try {
                return new ManifestV2(manifestJson);
            } catch (IOException e) {
                throw new ManifestException("invalid manifest format: " + e.getMessage());
            }
        } else {
            throw new ManifestException("unsupported manifest version: " + version);
        }
    }
}
