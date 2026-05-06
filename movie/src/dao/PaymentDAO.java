package dao;

import model.Payment;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    // 新增支付
    public boolean addPayment(Payment p) {
        String sql = "INSERT INTO payment (reservation_id, method, status, pay_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getReservationId());
            stmt.setString(2, p.getMethod());
            stmt.setString(3, p.getStatus());
            stmt.setTimestamp(4, p.getPayTime() == null ? new Timestamp(System.currentTimeMillis()) : new Timestamp(p.getPayTime().getTime()));
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 根据预约ID查询支付
    public Payment getPaymentByReservationId(int reservationId) {
        String sql = "SELECT * FROM payment WHERE reservation_id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Payment p = new Payment();
                p.setId(rs.getInt("id"));
                p.setReservationId(rs.getInt("reservation_id"));
                p.setMethod(rs.getString("method"));
                p.setStatus(rs.getString("status"));
                p.setPayTime(rs.getTimestamp("pay_time"));
                p.setActivatedAt(rs.getTimestamp("activated_at"));
                p.setExpiresAt(rs.getTimestamp("expires_at"));
                p.setWatchStatus(rs.getString("watch_status"));
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 更新支付状态
    public boolean updatePaymentStatus(int id, String status) {
        String sql = "UPDATE payment SET status = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 删除支付记录
    public boolean deletePayment(int id) {
        String sql = "DELETE FROM payment WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 查询某个客户所有支付记录（可选）
    public List<Payment> getPaymentsByCustomer(String customerName) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.* FROM payment p JOIN reservation r ON p.reservation_id = r.id WHERE r.customer_name = ? ORDER BY p.pay_time DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Payment p = new Payment();
                p.setId(rs.getInt("id"));
                p.setReservationId(rs.getInt("reservation_id"));
                p.setMethod(rs.getString("method"));
                p.setStatus(rs.getString("status"));
                p.setPayTime(rs.getTimestamp("pay_time"));
                p.setActivatedAt(rs.getTimestamp("activated_at"));
                p.setExpiresAt(rs.getTimestamp("expires_at"));
                p.setWatchStatus(rs.getString("watch_status"));
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 将票激活：写入 activated_at = NOW()，expires_at = NOW() + 5h，watch_status = '使用中'。
     * 仅在 activated_at IS NULL 且 status='已支付' 时生效，防止重复激活。
     */
    public boolean activateTicket(int paymentId) {
        String sql = "UPDATE payment SET activated_at = NOW(), expires_at = DATE_ADD(NOW(), INTERVAL 5 HOUR), watch_status = '使用中' " +
                     "WHERE id = ? AND activated_at IS NULL AND status = '已支付'";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, paymentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 把过期的票批量标记为已过期。其他面板可定期调用以保持一致性。 */
    public int markExpiredTickets() {
        String sql = "UPDATE payment SET watch_status = '已过期' " +
                     "WHERE watch_status = '使用中' AND expires_at IS NOT NULL AND expires_at < NOW()";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
} 