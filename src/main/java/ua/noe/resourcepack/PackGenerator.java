package ua.noe.resourcepack;

import org.bukkit.Material;
import ua.noe.config.NoESettings;
import ua.noe.item.NoEItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the resource pack zip: copies the user's resourcepack folder and generates the model
 * definitions NoE items need. File IO only, safe to run asynchronously.
 *
 * <p>Conventions: a texture at {@code assets/noe/textures/item/<id>.png} produces a generated model
 * {@code noe:item/<id>}. A hand-written model at {@code assets/noe/models/item/<id>.json} is used as is.
 */
public final class PackGenerator {

    private static final long FIXED_TIME = 0L;

    private final Predicate<Material> isBlock;

    public PackGenerator(Predicate<Material> isBlock) {
        this.isBlock = isBlock;
    }

    /** Result of a generation run. */
    public record PackResult(Path file, String sha1, long size, int generatedFiles) {
    }

    public PackResult generate(Collection<NoEItem> items, Path sourceDir, Path target, NoESettings.PackSettings settings,
                               List<String> warnings) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        readSource(sourceDir, files);
        int generated = 0;

        if (!files.containsKey("pack.mcmeta")) {
            files.put("pack.mcmeta", packMeta(settings).getBytes(StandardCharsets.UTF_8));
            generated++;
        }

        Map<Material, List<NoEItem>> byMaterial = new TreeMap<>();
        for (NoEItem item : items) {
            String id = item.getId();
            String modelPath = "assets/noe/models/item/" + id + ".json";
            String texturePath = "assets/noe/textures/item/" + id + ".png";
            boolean hasModel = files.containsKey(modelPath);
            if (!hasModel && files.containsKey(texturePath)) {
                files.put(modelPath, generatedModel(id).getBytes(StandardCharsets.UTF_8));
                hasModel = true;
                generated++;
            }
            if (!hasModel) {
                warnings.add("Item '" + id + "' has no model or texture (expected " + texturePath + "); skipped in the pack");
                continue;
            }
            if (item.data().itemModel() != null) {
                String[] parts = item.data().itemModel().split(":", 2);
                String defPath = "assets/" + parts[0] + "/items/" + parts[1] + ".json";
                if (!files.containsKey(defPath)) {
                    files.put(defPath, itemDefinition("noe:item/" + id).getBytes(StandardCharsets.UTF_8));
                    generated++;
                }
            }
            if (item.getModelData() != null) {
                byMaterial.computeIfAbsent(item.getMaterial(), m -> new ArrayList<>()).add(item);
            }
        }

        for (Map.Entry<Material, List<NoEItem>> entry : byMaterial.entrySet()) {
            String name = entry.getKey().name().toLowerCase(Locale.ROOT);
            String path = "assets/minecraft/items/" + name + ".json";
            if (files.containsKey(path)) {
                warnings.add("Custom 'assets/minecraft/items/" + name + ".json' exists; NoE did not overwrite it, "
                        + "custom model data for " + name + " must be handled there");
                continue;
            }
            files.put(path, rangeDispatch(entry.getKey(), entry.getValue()).getBytes(StandardCharsets.UTF_8));
            generated++;
        }

        Files.createDirectories(target.getParent());
        byte[] zip = zip(files);
        Files.write(target, zip);
        return new PackResult(target, sha1(zip), zip.length, generated);
    }

    // ------------------------------------------------------------------ JSON

    public String packMeta(NoESettings.PackSettings s) {
        return "{\n  \"pack\": {\n    \"description\": " + quote(s.description())
                + ",\n    \"min_format\": " + s.minFormat()
                + ",\n    \"max_format\": " + s.maxFormat() + "\n  }\n}\n";
    }

    public String generatedModel(String id) {
        return "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\n    \"layer0\": \"noe:item/" + id + "\"\n  }\n}\n";
    }

    public String itemDefinition(String model) {
        return "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n    \"model\": " + quote(model) + "\n  }\n}\n";
    }

    /** Item definition of a vanilla material that switches model by custom_model_data. */
    public String rangeDispatch(Material material, List<NoEItem> items) {
        String name = material.name().toLowerCase(Locale.ROOT);
        String fallback = (isBlock.test(material) ? "minecraft:block/" : "minecraft:item/") + name;
        List<NoEItem> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> Integer.compare(a.getModelData(), b.getModelData()));

        StringBuilder entries = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            int value = sorted.get(i).getModelData();
            if (i > 0) {
                entries.append(",\n");
            }
            entries.append("        { \"threshold\": ").append(value)
                    .append(", \"model\": { \"type\": \"minecraft:model\", \"model\": \"noe:item/")
                    .append(sorted.get(i).getId()).append("\" } }");
            boolean nextIsConsecutive = i + 1 < sorted.size() && sorted.get(i + 1).getModelData() == value + 1;
            if (!nextIsConsecutive) {
                // range_dispatch matches "value >= threshold", so close the range with the vanilla model
                entries.append(",\n        { \"threshold\": ").append(value + 1)
                        .append(", \"model\": { \"type\": \"minecraft:model\", \"model\": ")
                        .append(quote(fallback)).append(" } }");
            }
        }
        return "{\n  \"model\": {\n    \"type\": \"minecraft:range_dispatch\",\n"
                + "    \"property\": \"minecraft:custom_model_data\",\n    \"index\": 0,\n"
                + "    \"fallback\": { \"type\": \"minecraft:model\", \"model\": " + quote(fallback) + " },\n"
                + "    \"entries\": [\n" + entries + "\n    ]\n  }\n}\n";
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    // ------------------------------------------------------------------ files

    private static void readSource(Path sourceDir, Map<String, byte[]> files) throws IOException {
        if (!Files.isDirectory(sourceDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(sourceDir)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                String rel = sourceDir.relativize(p).toString().replace('\\', '/');
                if (rel.startsWith(".") || rel.endsWith(".DS_Store") || rel.endsWith("Thumbs.db")) {
                    continue;
                }
                files.put(rel, Files.readAllBytes(p));
            }
        }
    }

    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                ZipEntry entry = new ZipEntry(e.getKey());
                entry.setTime(FIXED_TIME); // deterministic zip: identical content gives an identical hash
                out.putNextEntry(entry);
                out.write(e.getValue());
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    public static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 is not available", ex);
        }
    }
}
