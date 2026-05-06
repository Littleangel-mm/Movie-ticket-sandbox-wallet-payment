package gui;

/** 客户端全局会话：记录当前登录的客户姓名。 */
public class ClientSession {
    public static String customerName = "";

    public static boolean isLoggedIn() {
        return customerName != null && !customerName.trim().isEmpty();
    }

    public static void logout() {
        customerName = "";
    }
}
