package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 全局配置加载器。按以下顺序查找 app.properties：
 * 1. 系统属性 -Dapp.config=/path/to/app.properties
 * 2. 当前工作目录 ./conf/app.properties
 * 3. 项目目录 ./movie/conf/app.properties（IDEA 默认 cwd 在仓库根时）
 * 4. classpath 内的 /app.properties（兜底）
 *
 * <p>所有配置统一通过 {@link #get(String)} / {@link #get(String, String)} 访问。
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();
    private static String loadedFrom = "(none)";

    static {
        load();
    }

    private AppConfig() {}

    private static void load() {
        // 1. 显式指定路径
        String sysPath = System.getProperty("app.config");
        if (sysPath != null && tryLoadFile(Paths.get(sysPath))) return;

        // 2/3. 常见相对路径
        Path[] candidates = {
                Paths.get("conf", "app.properties"),
                Paths.get("movie", "conf", "app.properties"),
                Paths.get("..", "conf", "app.properties")
        };
        for (Path p : candidates) {
            if (tryLoadFile(p)) return;
        }

        // 4. classpath 兜底
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                PROPS.load(in);
                loadedFrom = "classpath:/app.properties";
                return;
            }
        } catch (IOException ignored) {}

        System.err.println("[AppConfig] WARN: 未找到 app.properties，配置将全部为空。请把 conf/app.properties.example 复制为 conf/app.properties 并填写。");
    }

    private static boolean tryLoadFile(Path p) {
        if (!Files.isRegularFile(p)) return false;
        try (InputStream in = Files.newInputStream(p)) {
            PROPS.load(in);
            loadedFrom = p.toAbsolutePath().toString();
            System.out.println("[AppConfig] loaded " + loadedFrom);
            return true;
        } catch (IOException e) {
            System.err.println("[AppConfig] failed to read " + p + ": " + e.getMessage());
            return false;
        }
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isEmpty()) return defaultValue;
        return v.trim();
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key);
        if (v == null) return defaultValue;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultValue; }
    }

    public static String require(String key) {
        String v = get(key);
        if (v == null) throw new IllegalStateException("配置缺失：" + key + "（来源 " + loadedFrom + "）");
        return v;
    }

    public static String source() { return loadedFrom; }
}
