package dao;

import model.Reservation;
import utils.DBUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {
    // 新增预约
    public boolean addReservation(Reservation r) {
        String sql = "INSERT INTO reservation (movie_id, customer_name, seat_number, reserve_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, r.getMovieId());
            stmt.setString(2, r.getCustomerName());
            stmt.setString(3, r.getSeatNumber());
            stmt.setTimestamp(4, r.getReserveTime() == null ? new Timestamp(System.currentTimeMillis()) : new Timestamp(r.getReserveTime().getTime()));
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 根据客户名查询预约
    public List<Reservation> getReservationsByCustomer(String customerName) {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservation WHERE customer_name = ? ORDER BY reserve_time DESC";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Reservation r = new Reservation();
                r.setId(rs.getInt("id"));
                r.setMovieId(rs.getInt("movie_id"));
                r.setCustomerName(rs.getString("customer_name"));
                r.setSeatNumber(rs.getString("seat_number"));
                r.setReserveTime(rs.getTimestamp("reserve_time"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 删除预约
    public boolean deleteReservation(int id) {
        String sql = "DELETE FROM reservation WHERE id = ?";
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

    // 根据电影和座位号查找预约（用于校验座位是否被占用）
    public Reservation getReservationByMovieAndSeat(int movieId, String seatNumber) {
        String sql = "SELECT * FROM reservation WHERE movie_id = ? AND seat_number = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            stmt.setString(2, seatNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Reservation r = new Reservation();
                r.setId(rs.getInt("id"));
                r.setMovieId(rs.getInt("movie_id"));
                r.setCustomerName(rs.getString("customer_name"));
                r.setSeatNumber(rs.getString("seat_number"));
                r.setReserveTime(rs.getTimestamp("reserve_time"));
                return r;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Reservation getReservationById(int id) {
        String sql = "SELECT * FROM reservation WHERE id = ?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                model.Reservation r = new model.Reservation();
                r.setId(rs.getInt("id"));
                r.setMovieId(rs.getInt("movie_id"));
                r.setCustomerName(rs.getString("customer_name"));
                r.setSeatNumber(rs.getString("seat_number"));
                r.setReserveTime(rs.getTimestamp("reserve_time"));
                return r;
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
} 