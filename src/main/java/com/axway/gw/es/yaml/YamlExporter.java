package com.axway.gw.es.yaml;

import com.axway.gw.es.yaml.converters.EntityStoreESPKMapper;
import com.axway.gw.es.yaml.dto.entity.EntityDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static com.axway.gw.es.yaml.util.NameUtils.sanitize;

public class YamlExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlExporter.class);

    public static final String YAML_EXTENSION = ".yaml";
    public static final String METADATA_FILENAME = "metadata.yaml";

    private final List<EntityDTO> mappedEntities;
    private final boolean saveKeyMapping;
    private EntityStoreESPKMapper<String, String> entityStoreESPKMapper;

    public YamlExporter(List<EntityDTO> entityDTOList) {
        this(entityDTOList, false);
    }

    public YamlExporter(List<EntityDTO> entityDTOList, boolean saveKeyMapping) {
        this.mappedEntities = entityDTOList;
        this.saveKeyMapping = saveKeyMapping;
        if (saveKeyMapping) {
            entityStoreESPKMapper = new EntityStoreESPKMapper<>();
        }
    }

    public void writeEntities(File rootDir) throws IOException {

        createDirectoryIfNeeded(rootDir);

        // must provide rootDir (this happens when a file already exists)
        if (!rootDir.isDirectory())
            throw new IOException("Must provide a directory for YAML output");

        for (EntityDTO entityDTO : mappedEntities) {
            // deal with pk for parent  entity
            dumpAsYaml(rootDir, entityDTO.getKey(), entityDTO);
        }

        entityStoreESPKMapper.writeFederatedToYamlPkMapping(rootDir);

    }

    private void dumpAsYaml(File rootDir, String path, EntityDTO entityDTO) throws IOException {

        final File output = new File(rootDir, path);

        if (entityDTO.isAllowsChildren()) { // handle as directory with metadata
            createDirectoryIfNeeded(output);
            YamlEntityStore.YAML_MAPPER.writeValue(new File(output, METADATA_FILENAME), entityDTO);
        } else { // handle as file
            File f = new File(output.getPath() + YAML_EXTENSION);
            createDirectoryIfNeeded(f.getParentFile());

            extractContent(entityDTO, f);

            YamlEntityStore.YAML_MAPPER.writeValue(f, entityDTO);
        }

        if (saveKeyMapping) {
            entityStoreESPKMapper.addKeyPair(entityDTO.getSourceKey(), entityDTO.getKey());
        }

    }

    private void extractContent(EntityDTO entityDTO, File file) throws IOException {
        if (entityDTO.getChildren() != null) {
            for (EntityDTO yChild : entityDTO.getChildren().values()) {
                extractContent(yChild, file);
            }
        }

        File dir = file.getParentFile();
        final String metaType = entityDTO.getMeta().getType();

        switch (metaType) {
            case "JavaScriptFilter":
                String fileName = file.getName().replace(YAML_EXTENSION, "-Scripts/") + sanitize(entityDTO.buildKeyValue()) + "." + entityDTO.getFieldValue("engineName");
                writeContentToFile(entityDTO, dir, fileName, "script", false);
                break;
            case "Script":
                writeContentToFile(entityDTO, dir, entityDTO.buildKeyValue() + "." + entityDTO.getFields().get("engineName"), "script", false);
                break;
            case "Stylesheet":
                writeContentToFile(entityDTO, dir, entityDTO.getFields().get("URL") + ".xsl", "contents", true);
                break;
            case "Certificate":
                writeContentToFile(entityDTO, dir, file.getName().replace(YAML_EXTENSION, ".pem"), "key", true);
                writeContentToFile(entityDTO, dir, file.getName().replace(YAML_EXTENSION, ".crt"), "content", true);
                break;
            case "ResourceBlob":
                String type = entityDTO.getFields().get("type");
                if (Objects.equals(type, "schema")) {
                    type = "xsd";
                }
                writeContentToFile(entityDTO, dir, entityDTO.getFields().get("ID") + "." + type, "content", true);
                break;
            default:
                LOGGER.debug("Nothing to extract from type {}", metaType);
        }
    }

    private void writeContentToFile(EntityDTO entityDTO, File dir, String fileName, String field, boolean base64Decode) throws IOException {
        String content = entityDTO.getFields().remove(field);
        if (content == null) {
            return;
        }

        byte[] data;
        if (base64Decode) {
            data = Base64.getDecoder().decode(content.replaceAll("[\r?\n]", ""));
        } else {
            data = content.getBytes();
        }

        Path path = dir.toPath().resolve(fileName);
        File parentDir = path.getParent().toFile();
        createDirectoryIfNeeded(parentDir);
        Files.write(path, data);

        entityDTO.getFields().put(field + "#ref" + (base64Decode ? "base64" : ""), fileName);
    }

    private static void createDirectoryIfNeeded(File directory) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create directory:" + directory);
        }
    }

}
