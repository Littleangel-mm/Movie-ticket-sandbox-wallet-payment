import dao.MovieDAO;
import model.Movie;

public class TestMovieDAO2 {
    public static void main(String[] args) {
        MovieDAO dao = new MovieDAO();

        // 添加电影
        Movie m = new Movie();
        m.setName("流浪地球2");
        m.setHall("3号厅");
        m.setShowTime("2025-07-02 20:00:00");
        m.setPrice(60.0);
        m.setTotalSeats(120);
        m.setAvailableSeats(120);
        m.setImageUrl("https://example.com/posters/liulang.jpg");

        boolean success = dao.addMovie(m);
        System.out.println("添加成功？" + success);

        //删除电影
        boolean deleted = dao.deleteMovie(1); // 删除 ID 为 1 的电影
        System.out.println("删除成功？" + deleted);

        // 更新电影
        m.setId(2); // 假设更新 ID 为 2 的电影
        m.setName("流浪地球2（导演剪辑版）");
        boolean updated = dao.updateMovie(m);
        System.out.println("更新成功？" + updated);
    }
}
