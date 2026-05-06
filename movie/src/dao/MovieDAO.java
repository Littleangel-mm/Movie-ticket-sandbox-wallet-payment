package dao;

import model.Movie;
import utils.DBUtils;  // 你数据库连接工具类

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    // 查询所有电影
    public List<Movie> getAllMovies() {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT * FROM movie";
        try (Connection conn = DBUtils.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Movie m = new Movie();
                m.setId(rs.getInt("id"));
                m.setName(rs.getString("name"));
                m.setHall(rs.getString("hall"));
                m.setShowTime(rs.getString("show_time"));
                m.setPrice(rs.getDouble("price"));
                m.setTotalSeats(rs.getInt("total_seats"));
                m.setAvailableSeats(rs.getInt("available_seats"));
                m.setImageUrl(rs.getString("image_url"));
                m.setVideoObjectKey(rs.getString("video_object_key"));
                int dur = rs.getInt("duration_seconds");
                m.setDurationSeconds(rs.wasNull() ? null : dur);
                m.setPosterObjectKey(rs.getString("poster_object_key"));
                list.add(m);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 根据ID查询单个电影
    public Movie getMovieById(int id) {
        String sql = "SELECT * FROM movie WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Movie m = new Movie();
                m.setId(rs.getInt("id"));
                m.setName(rs.getString("name"));
                m.setHall(rs.getString("hall"));
                m.setShowTime(rs.getString("show_time"));
                m.setPrice(rs.getDouble("price"));
                m.setTotalSeats(rs.getInt("total_seats"));
                m.setAvailableSeats(rs.getInt("available_seats"));
                m.setImageUrl(rs.getString("image_url"));
                m.setVideoObjectKey(rs.getString("video_object_key"));
                int dur = rs.getInt("duration_seconds");
                m.setDurationSeconds(rs.wasNull() ? null : dur);
                m.setPosterObjectKey(rs.getString("poster_object_key"));
                return m;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 添加电影，返回是否成功
    public boolean addMovie(Movie m) {
        String sql = "INSERT INTO movie(name, hall, show_time, price, total_seats, available_seats, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, m.getName());
            stmt.setString(2, m.getHall());
            stmt.setString(3, m.getShowTime());
            stmt.setDouble(4, m.getPrice());
            stmt.setInt(5, m.getTotalSeats());
            stmt.setInt(6, m.getAvailableSeats());
            stmt.setString(7, m.getImageUrl());

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 更新电影，返回是否成功
    public boolean updateMovie(Movie m) {
        String sql = "UPDATE movie SET name = ?, hall = ?, show_time = ?, price = ?, total_seats = ?, " +
                "available_seats = ?, image_url = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, m.getName());
            stmt.setString(2, m.getHall());
            stmt.setString(3, m.getShowTime());
            stmt.setDouble(4, m.getPrice());
            stmt.setInt(5, m.getTotalSeats());
            stmt.setInt(6, m.getAvailableSeats());
            stmt.setString(7, m.getImageUrl());
            stmt.setInt(8, m.getId());

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 删除电影，返回是否成功
    public boolean deleteMovie(int id) {
        String sql = "DELETE FROM movie WHERE id = ?";
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

    public boolean decreaseAvailableSeats(int movieId) {
        String sql = "UPDATE movie SET available_seats = available_seats - 1 WHERE id = ? AND available_seats > 0";
        try (java.sql.Connection conn = utils.DBUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** 更新视频对象信息（M3 上传完视频后调用） */
    public boolean updateVideoInfo(int movieId, String videoKey, Integer durationSec, String posterKey) {
        String sql = "UPDATE movie SET video_object_key = ?, duration_seconds = ?, poster_object_key = ? WHERE id = ?";
        try (Connection conn = DBUtils.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, videoKey);
            if (durationSec == null) stmt.setNull(2, java.sql.Types.INTEGER);
            else stmt.setInt(2, durationSec);
            stmt.setString(3, posterKey);
            stmt.setInt(4, movieId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean increaseAvailableSeats(int movieId) {
        String sql = "UPDATE movie SET available_seats = available_seats + 1 WHERE id = ?";
        try (java.sql.Connection conn = utils.DBUtils.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, movieId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
