package gui;

import dao.MovieDAO;
import model.Movie;
import utils.MinioService;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MovieTablePanel extends JPanel implements MovieAdminCard.Actions {
    private final MovieDAO dao = new MovieDAO();
    private JPanel grid;
    private JLabel countLbl;

    public MovieTablePanel() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BG);

        // 顶部工具栏（卡片）
        JButton addBtn     = UIUtils.primaryButton("+ 新增影票");
        JButton refreshBtn = UIUtils.ghostButton("刷新");

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolBar.setOpaque(false);
        toolBar.add(addBtn);
        toolBar.add(refreshBtn);

        JPanel toolCard = new JPanel(new BorderLayout());
        toolCard.setBackground(UIUtils.CARD);
        toolCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        toolCard.add(toolBar, BorderLayout.WEST);

        countLbl = new JLabel();
        countLbl.setForeground(UIUtils.TEXT_MUTED);
        countLbl.setFont(UIUtils.FONT_SMALL);
        toolCard.add(countLbl, BorderLayout.EAST);

        // 海报网格
        grid = new JPanel(new GridLayout(0, 4, 16, 16));
        grid.setBackground(UIUtils.BG);
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIUtils.BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(toolCard, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        addBtn.addActionListener(e -> {
            MovieFormDialog dialog = new MovieFormDialog(null);
            dialog.setVisible(true);
            reloadData();
        });
        refreshBtn.addActionListener(e -> reloadData());

        reloadData();
    }

    private void reloadData() {
        grid.removeAll();
        List<Movie> movies = dao.getAllMovies();
        for (Movie m : movies) {
            grid.add(new MovieAdminCard(m, this));
        }
        // 末行不足 4 列时补占位避免拉伸
        int filler = (4 - movies.size() % 4) % 4;
        for (int i = 0; i < filler; i++) {
            JPanel p = new JPanel();
            p.setOpaque(false);
            grid.add(p);
        }
        grid.revalidate();
        grid.repaint();
        countLbl.setText("共 " + movies.size() + " 部影片");
    }

    /* ============== MovieAdminCard.Actions 实现 ============== */

    @Override
    public void onEdit(Movie m) {
        MovieFormDialog dialog = new MovieFormDialog(m);
        dialog.setVisible(true);
        reloadData();
    }

    @Override
    public void onDelete(Movie m) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "确认删除影片 #" + m.getId() + " " + m.getName() + " 吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dao.deleteMovie(m.getId());
            reloadData();
        }
    }

    @Override
    public void onUpload(Movie movie) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择视频文件");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("视频文件 (mp4, mkv, mov)", "mp4", "mkv", "mov"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        long sizeMB = file.length() / 1024 / 1024;

        Integer durationSec = askDurationSeconds(movie);
        if (durationSec == null) return; // 取消

        // 进度弹窗（不能取消，MinIO SDK 不提供进度回调）
        JDialog progress = new JDialog(SwingUtilities.getWindowAncestor(this), "上传中", Dialog.ModalityType.APPLICATION_MODAL);
        progress.setLayout(new BorderLayout());
        JLabel msg = new JLabel("正在上传 " + file.getName() + "  (" + sizeMB + " MB)\u2026", SwingConstants.CENTER);
        msg.setBorder(BorderFactory.createEmptyBorder(20, 28, 8, 28));
        progress.add(msg, BorderLayout.CENTER);
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setBorder(BorderFactory.createEmptyBorder(0, 28, 20, 28));
        progress.add(pb, BorderLayout.SOUTH);
        progress.pack();
        progress.setLocationRelativeTo(this);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                String prefix = "movies/" + movie.getId();
                return MinioService.upload(prefix, file, "video/" + extOf(file.getName()));
            }
            @Override protected void done() {
                progress.dispose();
                try {
                    String key = get();
                    boolean ok = dao.updateVideoInfo(movie.getId(), key, durationSec, null);
                    if (ok) {
                        JOptionPane.showMessageDialog(MovieTablePanel.this, "上传成功\nObjectKey: " + key);
                        reloadData();
                    } else {
                        JOptionPane.showMessageDialog(MovieTablePanel.this, "上传成功但写库失败\nObjectKey: " + key, "警告", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(MovieTablePanel.this,
                            "上传失败：" + cause.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                    cause.printStackTrace();
                }
            }
        };
        worker.execute();
        progress.setVisible(true);
    }

    private Integer askDurationSeconds(Movie movie) {
        String preset = movie.getDurationSeconds() != null ? String.valueOf(movie.getDurationSeconds()) : "";
        String input = JOptionPane.showInputDialog(this,
                "请输入影片时长（秒），可留空：",
                preset);
        if (input == null) return null; // 取消
        input = input.trim();
        if (input.isEmpty()) return 0;
        try { return Integer.parseInt(input); }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入整数秒数");
            return askDurationSeconds(movie);
        }
    }

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "mp4" : name.substring(i + 1).toLowerCase();
    }

}
