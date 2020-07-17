package org.openrefine.wikidata.manifests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openrefine.wikidata.manifests.v1_0.ManifestV1_0;
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
            throw new ManifestException("invalid manifest format");
        }

        String version = root.path("version").textValue();
        if (StringUtils.isBlank(version)) {
            version = "1.0";
            logger.warn("version is missing in the manifest, assumed to be \"1.0\"");
        }

        if ("1.0".equals(version)) {
            return new ManifestV1_0(root);
        } else {
            throw new ManifestException("unsupported manifest version: " + version);
        }
    }
}
