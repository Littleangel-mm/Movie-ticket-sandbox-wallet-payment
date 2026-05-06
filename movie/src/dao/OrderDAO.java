package dao;

import model.OrderView;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员视角的订单/营收 DAO：reservation × movie × payment 联合查询。
 */
public class OrderDAO {

    private static final String BASE_SELECT =
            "SELECT r.id AS r_id, r.customer_name, r.seat_number, r.reserve_time, " +
            "       m.id AS m_id, m.name AS m_name, m.hall, m.show_time, m.price, " +
            "       p.id AS p_id, p.method, p.status, p.pay_time " +
            "FROM reservation r " +
            "JOIN movie m ON r.movie_id = m.id " +
            "LEFT JOIN payment p ON p.reservation_id = r.id ";

    /**
     * 查询全部订单。
     *
     * @param keyword     模糊匹配电影名/客户名/座位号，可空
     * @param statusFilter 全部 / 已支付 / 未支付 / 已取消，可空
     */
    public List<OrderView> findOrders(String keyword, String statusFilter) {
        List<OrderView> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (m.name LIKE ? OR r.customer_name LIKE ? OR r.seat_number LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (statusFilter != null) {
            switch (statusFilter) {
                case "已支付":
                    sql.append("AND p.status = '已支付' "); break;
                case "未支付":
                    sql.append("AND (p.status IS NULL OR p.status NOT IN ('已支付','已取消')) "); break;
                case "已取消":
                    sql.append("AND p.status = '已取消' "); break;
                default: break; // 全部
            }
        }
        sql.append("ORDER BY r.reserve_time DESC");

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                stmt.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private OrderView map(ResultSet rs) throws SQLException {
        OrderView v = new OrderView();
        v.reservationId = rs.getInt("r_id");
        v.customerName = rs.getString("customer_name");
        v.seatNumber = rs.getString("seat_number");
        v.reserveTime = rs.getTimestamp("reserve_time");
        v.movieId = rs.getInt("m_id");
        v.movieName = rs.getString("m_name");
        v.hall = rs.getString("hall");
        v.showTime = rs.getString("show_time");
        v.price = rs.getDouble("price");
        int pid = rs.getInt("p_id");
        if (!rs.wasNull()) {
            v.paymentId = pid;
            v.payMethod = rs.getString("method");
            v.payStatus = rs.getString("status");
            v.payTime = rs.getTimestamp("pay_time");
        }
        return v;
    }

    /* ===================== 营收统计 ===================== */

    public static class Stats {
        public int totalOrders;        // 累计预约（不算取消的）
        public int paidOrders;         // 累计已支付
        public double totalRevenue;    // 累计营收
        public int todayOrders;        // 今日预约
        public int todayPaid;          // 今日已支付
        public double todayRevenue;    // 今日营收
        public int seatsSold;          // 已售座位（已支付订单数）
    }

    public Stats getStats() {
        Stats s = new Stats();
        String sql =
                "SELECT " +
                " (SELECT COUNT(*) FROM reservation) AS total_orders, " +
                " (SELECT COUNT(*) FROM payment WHERE status='已支付') AS paid_orders, " +
                " (SELECT COALESCE(SUM(m.price),0) FROM payment p JOIN reservation r ON p.reservation_id=r.id JOIN movie m ON r.movie_id=m.id WHERE p.status='已支付') AS total_revenue, " +
                " (SELECT COUNT(*) FROM reservation WHERE DATE(reserve_time)=CURDATE()) AS today_orders, " +
                " (SELECT COUNT(*) FROM payment WHERE status='已支付' AND DATE(pay_time)=CURDATE()) AS today_paid, " +
                " (SELECT COALESCE(SUM(m.price),0) FROM payment p JOIN reservation r ON p.reservation_id=r.id JOIN movie m ON r.movie_id=m.id WHERE p.status='已支付' AND DATE(p.pay_time)=CURDATE()) AS today_revenue ";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                s.totalOrders = rs.getInt("total_orders");
                s.paidOrders = rs.getInt("paid_orders");
                s.totalRevenue = rs.getDouble("total_revenue");
                s.todayOrders = rs.getInt("today_orders");
                s.todayPaid = rs.getInt("today_paid");
                s.todayRevenue = rs.getDouble("today_revenue");
                s.seatsSold = s.paidOrders;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return s;
    }

    /** 电影销量 Top N（按已支付订单数排序） */
    public static class MovieSales {
        public String movieName;
        public String hall;
        public int paidCount;
        public double revenue;
    }

    public List<MovieSales> getTopMovies(int limit) {
        List<MovieSales> list = new ArrayList<>();
        String sql =
                "SELECT m.name AS m_name, m.hall AS m_hall, COUNT(*) AS cnt, COALESCE(SUM(m.price),0) AS rev " +
                "FROM payment p " +
                "JOIN reservation r ON p.reservation_id = r.id " +
                "JOIN movie m ON r.movie_id = m.id " +
                "WHERE p.status = '已支付' " +
                "GROUP BY m.id, m.name, m.hall " +
                "ORDER BY cnt DESC, rev DESC " +
                "LIMIT ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MovieSales ms = new MovieSales();
                    ms.movieName = rs.getString("m_name");
                    ms.hall = rs.getString("m_hall");
                    ms.paidCount = rs.getInt("cnt");
                    ms.revenue = rs.getDouble("rev");
                    list.add(ms);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
