package ic2_120.tools;

import appengx.client.guidebook.compiler.PageCompiler;
import appengx.client.guidebook.compiler.ParsedGuidePage;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class GuidebookValidationTool {
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");
    private static final Pattern BLOCKQUOTE_LINE = Pattern.compile("(?m)^>");
    private static final Pattern JSON_STRING_PROPERTY = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern BLOCK_IMAGE_TAG = Pattern.compile("<BlockImage\\s+id=\"([^\"]+)\"");
    private static final Pattern ITEM_IMAGE_TAG = Pattern.compile("<ItemImage\\s+id=\"([^\"]+)\"");

    private GuidebookValidationTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: GuidebookValidationTool <project-root> OR <guidebook-root> <namespace>");
        }

        List<String> errors = new ArrayList<>();
        List<GuideDefinition> definitions = args.length == 1
                ? discoverGuideDefinitions(Path.of(args[0]).toAbsolutePath().normalize(), errors)
                : List.of(new GuideDefinition(
                Path.of(args[0]).toAbsolutePath().normalize(),
                args[1],
                Path.of(args[0]).toAbsolutePath().normalize().resolve("index.md"),
                Path.of(args[0]).toAbsolutePath().normalize().resolve("index.md")));

        int pageCount = 0;
        for (GuideDefinition definition : definitions) {
            pageCount += validateGuide(definition, errors);
        }


        if (!errors.isEmpty()) {
            throw new IllegalStateException("Guidebook validation failed:\n - " + String.join("\n - ", errors));
        }

        System.out.println("Validated " + pageCount + " guidebook markdown pages from " + definitions.size()
                + " guide definition(s) with Fabric Guidebook PageCompiler.parse.");
    }

    private static List<GuideDefinition> discoverGuideDefinitions(Path projectRoot, List<String> errors) throws IOException {
        List<GuideDefinition> definitions = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(projectRoot)) {
            List<Path> configs = stream
                    .filter(path -> Files.isRegularFile(path)
                            && path.getFileName().toString().endsWith(".json")
                            && slash(path).contains("/src/main/resources/assets/")
                            && slash(path).contains("/guidebook_guides/"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            for (Path config : configs) {
                String normalized = slash(config);
                int assetsIndex = normalized.indexOf("/assets/");
                int guidesIndex = normalized.indexOf("/guidebook_guides/");
                if (assetsIndex < 0 || guidesIndex < 0 || guidesIndex <= assetsIndex + "/assets/".length()) {
                    errors.add(slash(projectRoot.relativize(config)) + ": cannot infer namespace from guide definition path");
                    continue;
                }

                String namespace = normalized.substring(assetsIndex + "/assets/".length(), guidesIndex);
                Path assetsNamespaceRoot = config;
                while (assetsNamespaceRoot != null && !assetsNamespaceRoot.getFileName().toString().equals(namespace)) {
                    assetsNamespaceRoot = assetsNamespaceRoot.getParent();
                }
                if (assetsNamespaceRoot == null) {
                    errors.add(slash(projectRoot.relativize(config)) + ": cannot locate namespace asset root");
                    continue;
                }

                String json = Files.readString(config, StandardCharsets.UTF_8);
                String folder = readStringProperty(json, "folder");
                String landingPage = readStringProperty(json, "landing_page");
                if (folder == null || folder.isBlank()) {
                    errors.add(slash(projectRoot.relativize(config)) + ": missing guidebook folder");
                    continue;
                }
                if (landingPage == null || landingPage.isBlank()) {
                    landingPage = "index.md";
                }

                Path guideRoot = assetsNamespaceRoot.resolve(folder).normalize();
                Path landing = guideRoot.resolve(landingPage).normalize();
                definitions.add(new GuideDefinition(guideRoot, namespace, config, landing));
            }
        }

        return definitions;
    }

    private static int validateGuide(GuideDefinition definition, List<String> errors) throws IOException {
        if (!Files.isDirectory(definition.root())) {
            errors.add(slash(definition.config()) + ": guidebook folder does not exist: " + definition.root());
            return 0;
        }
        if (!Files.isRegularFile(definition.landingPage())) {
            errors.add(slash(definition.config()) + ": landing_page does not exist: " + definition.landingPage());
        }

        List<Path> pages;
        try (Stream<Path> stream = Files.walk(definition.root())) {
            pages = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }

        Map<String, String> itemIdOwners = new LinkedHashMap<>();
        for (Path page : pages) {
            validateWithGuidebookParser(definition.root(), definition.namespace(), page, itemIdOwners, errors);
            validateRelativeLinks(definition.root(), page, errors);
            validateUnsupportedMarkdown(definition.root(), page, errors);
            validateItemsPageImageTag(definition.root(), page, errors);
        }

        return pages.size();
    }

    private static void validateWithGuidebookParser(
            Path root,
            String namespace,
            Path page,
            Map<String, String> itemIdOwners,
            List<String> errors
    ) {
        String rel = slash(root.relativize(page));
        String pageIdPath = pageIdPath(rel);

        try {
            String content = Files.readString(page, StandardCharsets.UTF_8);
            ParsedGuidePage parsed = PageCompiler.parse("ic2_120-dev", new Identifier(namespace, pageIdPath), content);

            if (content.startsWith("---") && parsed.getFrontmatter().navigationEntry() == null) {
                errors.add(rel + ": Guidebook parser did not produce a navigation entry from frontmatter");
            }

            validateItemIds(namespace, rel, parsed, itemIdOwners, errors);
        } catch (Exception e) {
            errors.add(rel + ": Guidebook parser failed: " + describe(e));
        }
    }

    private static void validateItemIds(
            String namespace,
            String rel,
            ParsedGuidePage parsed,
            Map<String, String> itemIdOwners,
            List<String> errors
    ) {
        if (rel.startsWith("i18n/")) {
            return;
        }

        Object itemIds = parsed.getFrontmatter().additionalProperties().get("item_ids");
        if (itemIds == null) {
            return;
        }
        if (!(itemIds instanceof List<?> list)) {
            errors.add(rel + ": item_ids frontmatter must be a list");
            return;
        }

        for (Object entry : list) {
            if (!(entry instanceof String itemIdText) || itemIdText.isBlank()) {
                errors.add(rel + ": item_ids entry must be a non-empty string: " + entry);
                continue;
            }

            String normalized = itemIdText.contains(":") ? itemIdText : namespace + ":" + itemIdText;
            try {
                Identifier identifier = new Identifier(normalized);
                normalized = identifier.toString();
            } catch (Exception e) {
                errors.add(rel + ": malformed item_ids entry: " + itemIdText);
                continue;
            }

            String previous = itemIdOwners.putIfAbsent(normalized, rel);
            if (previous != null) {
                errors.add(rel + ": duplicate item_ids entry " + normalized + " already owned by " + previous);
            }
        }
    }

    private static void validateRelativeLinks(Path root, Path page, List<String> errors) throws IOException {
        String rel = slash(root.relativize(page));
        String content = Files.readString(page, StandardCharsets.UTF_8);
        Matcher matcher = MARKDOWN_LINK.matcher(content);

        while (matcher.find()) {
            String link = matcher.group(1);
            if (link.startsWith("#")
                    || link.startsWith("item:")
                    || link.startsWith("block:")
                    || link.matches("(?i)^(https?:|mailto:).*")) {
                continue;
            }

            String target = link.split("#", 2)[0];
            if (target.isBlank()) {
                continue;
            }

            Path targetPath = page.getParent().resolve(target).normalize();
            if (!targetPath.startsWith(root) || !Files.isRegularFile(targetPath)) {
                errors.add(rel + ": markdown link target does not exist: " + link);
            }
        }
    }

    private static void validateUnsupportedMarkdown(Path root, Path page, List<String> errors) throws IOException {
        String rel = slash(root.relativize(page));
        String content = Files.readString(page, StandardCharsets.UTF_8);
        Matcher blockquote = BLOCKQUOTE_LINE.matcher(content);
        if (blockquote.find()) {
            int line = 1;
            for (int i = 0; i < blockquote.start(); i++) {
                if (content.charAt(i) == '\n') {
                    line++;
                }
            }
            errors.add(rel + ":" + line + ": blockquote syntax is not supported by Fabric Guidebook");
        }
    }

    /**
     * Pages under {@code items/} (or {@code i18n/<lang>/items/}) are the equipment reference pages.
     * They must render items as {@code <ItemImage>}; using {@code <BlockImage>} for an item id
     * will silently fail at runtime in the Fabric Guidebook viewer. We catch the common mistake
     * here so the error is reported at build time, where it is easy to fix.
     */
    private static void validateItemsPageImageTag(Path root, Path page, List<String> errors) throws IOException {
        String rel = slash(root.relativize(page));
        // match both the English tree and every i18n locale tree
        if (!rel.contains("/items/") && !rel.startsWith("items/")) {
            return;
        }

        String content = Files.readString(page, StandardCharsets.UTF_8);
        Matcher blockImage = BLOCK_IMAGE_TAG.matcher(content);
        while (blockImage.find()) {
            int line = lineOf(content, blockImage.start());
            errors.add(rel + ":" + line
                    + ": <BlockImage> used in items/ page; use <ItemImage> for item ids (id="
                    + blockImage.group(1) + ")");
        }
    }

    private static int lineOf(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String pageIdPath(String rel) {
        String path = rel.replace('\\', '/');
        if (path.startsWith("i18n/")) {
            int nextSlash = path.indexOf('/', "i18n/".length());
            if (nextSlash >= 0) {
                path = path.substring(nextSlash + 1);
            }
        }
        return path;
    }

    private static String readStringProperty(String json, String propertyName) {
        Matcher matcher = Pattern.compile(JSON_STRING_PROPERTY.pattern().formatted(Pattern.quote(propertyName))).matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getName();
        }
        return throwable.getClass().getName() + ": " + message;
    }

    private record GuideDefinition(Path root, String namespace, Path config, Path landingPage) {
    }
}
