package gui;

import model.Movie;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * 管理员视角的影片卡片：海报 + 信息 + 三个操作按钮（编辑 / 上传视频 / 删除）。
 * 比客户端 {@link MoviePosterCard} 多一行操作区，海报异步加载。
 */
public class MovieAdminCard extends JPanel {

    private static final int POSTER_W = 200;
    private static final int POSTER_H = 260;

    public interface Actions {
        void onEdit(Movie m);
        void onUpload(Movie m);
        void onDelete(Movie m);
    }

    private final JLabel posterLabel;

    public MovieAdminCard(Movie movie, Actions actions) {
        setLayout(new BorderLayout(0, 8));
        setBackground(UIUtils.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 12, 14, 12)));
        setPreferredSize(new Dimension(POSTER_W + 24, POSTER_H + 200));

        // 海报
        posterLabel = new JLabel(placeholder());
        posterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        posterLabel.setBorder(new UIUtils.RoundedLineBorder(UIUtils.BORDER, 8, 1));
        posterLabel.setPreferredSize(new Dimension(POSTER_W, POSTER_H));
        add(posterLabel, BorderLayout.CENTER);
        loadPosterAsync(movie.getImageUrl());

        // 中下部信息 + 按钮
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("#" + movie.getId() + " " + movie.getName());
        title.setFont(UIUtils.FONT_BOLD);
        title.setForeground(UIUtils.TEXT_TITLE);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel meta = new JLabel(movie.getHall() + " · " + movie.getShowTime());
        meta.setFont(UIUtils.FONT_SMALL);
        meta.setForeground(UIUtils.TEXT_MUTED);
        meta.setAlignmentX(LEFT_ALIGNMENT);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel price = new JLabel("￥" + movie.getPrice());
        price.setFont(new Font(UIUtils.FONT_BOLD.getName(), Font.BOLD, 16));
        price.setForeground(UIUtils.PRIMARY);
        JLabel seats = new JLabel(movie.getAvailableSeats() + " / " + movie.getTotalSeats() + " 座");
        seats.setFont(UIUtils.FONT_SMALL);
        seats.setForeground(UIUtils.TEXT_MUTED);
        row.add(price, BorderLayout.WEST);
        row.add(seats, BorderLayout.EAST);

        JLabel video = new JLabel(movie.hasVideo() ? "● 视频已上传" : "○ 视频未上传");
        video.setFont(UIUtils.FONT_SMALL);
        video.setForeground(movie.hasVideo() ? new Color(0x16A34A) : UIUtils.TEXT_MUTED);
        video.setAlignmentX(LEFT_ALIGNMENT);

        // 操作按钮
        JButton btnEdit   = compactBtn("编辑", false);
        JButton btnUpload = compactBtn(movie.hasVideo() ? "换视频" : "上传视频", true);
        JButton btnDelete = compactDanger("删除");

        JPanel btns = new JPanel(new GridLayout(1, 3, 6, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(LEFT_ALIGNMENT);
        btns.add(btnEdit);
        btns.add(btnUpload);
        btns.add(btnDelete);

        south.add(title);
        south.add(Box.createVerticalStrut(4));
        south.add(meta);
        south.add(Box.createVerticalStrut(6));
        south.add(row);
        south.add(Box.createVerticalStrut(4));
        south.add(video);
        south.add(Box.createVerticalStrut(10));
        south.add(btns);
        add(south, BorderLayout.SOUTH);

        btnEdit.addActionListener(e   -> actions.onEdit(movie));
        btnUpload.addActionListener(e -> actions.onUpload(movie));
        btnDelete.addActionListener(e -> actions.onDelete(movie));
    }

    private static JButton compactBtn(String text, boolean primary) {
        JButton b = primary ? UIUtils.primaryButton(text) : UIUtils.secondaryButton(text);
        b.setFont(UIUtils.FONT_SMALL);
        b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return b;
    }

    private static JButton compactDanger(String text) {
        JButton b = UIUtils.dangerButton(text);
        b.setFont(UIUtils.FONT_SMALL);
        b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return b;
    }

    private void loadPosterAsync(String url) {
        if (url == null || url.isEmpty()) return;
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() {
                try {
                    ImageIcon raw = new ImageIcon(new URL(url));
                    if (raw.getIconWidth() <= 0) return null;
                    Image scaled = raw.getImage().getScaledInstance(POSTER_W, POSTER_H, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                } catch (Exception ex) { return null; }
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) posterLabel.setIcon(icon);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private static ImageIcon placeholder() {
        BufferedImage img = new BufferedImage(POSTER_W, POSTER_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0xF3F4F6), 0, POSTER_H, new Color(0xE5E7EB)));
        g.fillRect(0, 0, POSTER_W, POSTER_H);
        g.setColor(new Color(0x9CA3AF));
        g.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        FontMetrics fm = g.getFontMetrics();
        String s = "加载中…";
        g.drawString(s, (POSTER_W - fm.stringWidth(s)) / 2, POSTER_H / 2);
        g.dispose();
        return new ImageIcon(img);
    }
}
