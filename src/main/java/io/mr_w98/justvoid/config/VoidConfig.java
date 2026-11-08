package io.mr_w98.justvoid.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class VoidConfig {

    private static final String FILE = "config.toml";
    private static final String DEFAULT_WORLD_KEY = "default-world";
    private static final String PLATFORM_SIZE_KEY = "platform-size";
    private static final String WORLDS_KEY = "worlds";
    private static final String NAME_KEY = "name";
    private static final String DIMENSION_KEY = "dimension";
    private static final String NAMESPACE = "minecraft:";
    private static final List<String> DIMENSIONS = List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
    private static final int DEFAULT_PLATFORM_SIZE = 16;
    private static final String DEFAULT_CONTENT = DEFAULT_WORLD_KEY + " = true\n"
            + PLATFORM_SIZE_KEY + " = " + DEFAULT_PLATFORM_SIZE + "\n"
            + WORLDS_KEY + " = []\n";

    private final boolean defaultWorld;
    private final int platformSize;
    private final List<WorldRule> worlds;

    private VoidConfig(boolean defaultWorld, int platformSize, List<WorldRule> worlds) {
        this.defaultWorld = defaultWorld;
        this.platformSize = platformSize;
        this.worlds = worlds;
    }

    public boolean defaultWorld() {
        return defaultWorld;
    }

    public int platformSize() {
        return platformSize;
    }

    public boolean matches(String worldName, String dimension) {
        for (WorldRule rule : worlds) {
            if (rule.matches(worldName, dimension)) return true;
        }
        return false;
    }

    public static VoidConfig load(String dataFolder) throws ConfigException {
        Path path = Paths.get(dataFolder, FILE);
        String text;
        try {
            if (!Files.exists(path)) {
                Files.write(path, DEFAULT_CONTENT.getBytes(StandardCharsets.UTF_8));
            }
            text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException | SecurityException e) {
            throw new ConfigException("Could not read config.toml: " + e.getMessage());
        }
        return parse(text);
    }

    private static VoidConfig parse(String text) throws ConfigException {
        Map<String, Object> values = new TomlParser(text).parseDocument();

        boolean defaultWorld = true;
        int platformSize = DEFAULT_PLATFORM_SIZE;
        List<WorldRule> worlds = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case DEFAULT_WORLD_KEY:
                    if (!(value instanceof Boolean flag)) throw new ConfigException("'" + key + "' must be true or false.");
                    defaultWorld = flag;
                    break;
                case PLATFORM_SIZE_KEY:
                    platformSize = parseSize(key, value);
                    break;
                case WORLDS_KEY:
                    worlds = parseWorlds(value);
                    break;
                default:
                    throw new ConfigException("Unknown setting '" + key + "' in config.toml.");
            }
        }
        return new VoidConfig(defaultWorld, platformSize, worlds);
    }

    private static int parseSize(String key, Object value) throws ConfigException {
        if (!(value instanceof Long size) || size < 0 || size > Integer.MAX_VALUE) {
            throw new ConfigException("'" + key + "' must be a whole number of 0 or more.");
        }
        return size.intValue();
    }

    private static List<WorldRule> parseWorlds(Object value) throws ConfigException {
        String invalid = "'" + WORLDS_KEY + "' must be a list of world name patterns or { name = '...', dimension = '...' } entries.";
        if (!(value instanceof List<?> list)) throw new ConfigException(invalid);

        List<WorldRule> rules = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String source) {
                rules.add(new WorldRule(compilePattern(source), null));
            } else if (item instanceof Map<?, ?> table) {
                rules.add(parseWorldTable(table));
            } else {
                throw new ConfigException(invalid);
            }
        }
        return rules;
    }

    private static WorldRule parseWorldTable(Map<?, ?> table) throws ConfigException {
        Pattern name = null;
        String dimension = null;
        for (Map.Entry<?, ?> entry : table.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (NAME_KEY.equals(key)) {
                if (!(value instanceof String source)) throw new ConfigException("'" + NAME_KEY + "' in '" + WORLDS_KEY + "' must be a string.");
                name = compilePattern(source);
            } else if (DIMENSION_KEY.equals(key)) {
                if (!(value instanceof String id)) throw new ConfigException("'" + DIMENSION_KEY + "' in '" + WORLDS_KEY + "' must be a string.");
                dimension = normalizeDimension(id);
            } else {
                throw new ConfigException("Unknown setting '" + key + "' in '" + WORLDS_KEY + "'.");
            }
        }

        if (name == null && dimension == null) throw new ConfigException("Each entry in '" + WORLDS_KEY + "' needs a '" + NAME_KEY + "' or '" + DIMENSION_KEY + "'.");
        return new WorldRule(name, dimension);
    }

    private static Pattern compilePattern(String source) throws ConfigException {
        try {
            return Pattern.compile(source);
        } catch (PatternSyntaxException e) {
            throw new ConfigException("Invalid pattern '" + source + "' in '" + WORLDS_KEY + "': " + e.getDescription());
        }
    }

    private static String normalizeDimension(String id) throws ConfigException {
        String dimension = id.contains(":") ? id : NAMESPACE + id;
        if (!DIMENSIONS.contains(dimension)) {
            throw new ConfigException("Unknown dimension '" + id + "'. Use overworld, the_nether or the_end.");
        }
        return dimension;
    }
}
