import model.Movie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import utils.DBUtils;


public class TestMovieDAO {
    public static void main(String[] args) {
        MovieDAO dao = new MovieDAO();
        List<Movie> movies = dao.getAllMovies();

        for (Movie m : movies) {
            System.out.println(m.getName() + " - " + m.getShowTime() + " - ￥" + m.getPrice());
        }
    }

    public static class MovieDAO {

        public List<Movie> getAllMovies() {
            List<Movie> movies = new ArrayList<>();
            String sql = "SELECT * FROM movie";

            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Movie movie = new Movie();
                    movie.setId(rs.getInt("id"));
                    movie.setName(rs.getString("name"));
                    movie.setHall(rs.getString("hall"));
                    movie.setShowTime(rs.getString("show_time"));
                    movie.setPrice(rs.getDouble("price"));
                    movie.setTotalSeats(rs.getInt("total_seats"));
                    movie.setAvailableSeats(rs.getInt("available_seats"));
                    movie.setImageUrl(rs.getString("image_url"));
                    movies.add(movie);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return movies;
        }
    }
}

