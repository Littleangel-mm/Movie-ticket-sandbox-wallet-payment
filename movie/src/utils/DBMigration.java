package utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 一次性数据库迁移脚本：为新功能（影片视频对象 + 票务生命周期）添加字段。
 * 幂等：列存在则跳过，可重复执行。
 *
 * <p>运行方式：在 IDEA 中右键运行 main，或命令行：
 * java -cp out;mysql.jar utils.DBMigration
 */
public class DBMigration {

    public static void main(String[] args) throws Exception {
        try (Connection conn = DBUtils.getConnection()) {
            System.out.println("连接到: " + conn.getMetaData().getURL());

            // 先列出所有表，确认 schema
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SHOW TABLES")) {
                System.out.println("现有表：");
                while (rs.next()) System.out.println("  - " + rs.getString(1));
            }

            ensureColumn(conn, "movie",   "video_object_key",  "VARCHAR(255) NULL");
            ensureColumn(conn, "movie",   "duration_seconds",  "INT NULL");
            ensureColumn(conn, "movie",   "poster_object_key", "VARCHAR(255) NULL");

            ensureColumn(conn, "payment", "activated_at",      "DATETIME NULL");
            ensureColumn(conn, "payment", "expires_at",        "DATETIME NULL");
            ensureColumn(conn, "payment", "watch_status",      "VARCHAR(16) NOT NULL DEFAULT '未使用'");

            System.out.println("迁移完成。");

            // 打印两表当前结构
            printColumns(conn, "movie");
            printColumns(conn, "payment");
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rs = md.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }

    private static void ensureColumn(Connection conn, String table, String column, String typeDecl) throws Exception {
        if (columnExists(conn, table, column)) {
            System.out.println("[skip] " + table + "." + column + " 已存在");
            return;
        }
        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + typeDecl;
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            System.out.println("[ok]   " + sql);
        }
    }

    private static void printColumns(Connection conn, String table) throws Exception {
        System.out.println("---- " + table + " ----");
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rs = md.getColumns(conn.getCatalog(), null, table, null)) {
            while (rs.next()) {
                System.out.printf("  %-20s %s(%s)%n",
                        rs.getString("COLUMN_NAME"),
                        rs.getString("TYPE_NAME"),
                        rs.getString("COLUMN_SIZE"));
            }
        }
    }
}
