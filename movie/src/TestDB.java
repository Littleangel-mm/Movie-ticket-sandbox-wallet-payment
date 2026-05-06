import utils.DBUtils;

import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        try {
            Connection conn = DBUtils.getConnection();
            if (conn != null) {
                System.out.println("数据库连接成功！");
                DBUtils.close(conn);
            }
        } catch (Exception e) {
            System.out.println("连接失败：" + e.getMessage());
        }
    }
}
