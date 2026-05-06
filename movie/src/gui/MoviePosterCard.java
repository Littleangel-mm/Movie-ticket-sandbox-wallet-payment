package gui;

import model.Movie;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * 电影海报卡片：海报 + 标题 + 时间 + 价格 + 剩余票，整张卡可点击进详情。
 * 海报异步加载，避免阻塞 EDT。
 */
public class MoviePosterCard extends JPanel {

    private static final int POSTER_W = 200;
    private static final int POSTER_H = 280;
    private static final int CARD_W   = POSTER_W + 24;     // 含内边距
    private static final int CARD_H   = POSTER_H + 130;    // 含底部信息

    private final JLabel posterLabel;

    public MoviePosterCard(Movie movie, ClientMainFrame parent) {
        setLayout(new BorderLayout(0, 8));
        setBackground(UIUtils.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 12, 14, 12)));
        setPreferredSize(new Dimension(CARD_W, CARD_H));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 海报占位
        posterLabel = new JLabel(placeholder(POSTER_W, POSTER_H));
        posterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        posterLabel.setBorder(new UIUtils.RoundedLineBorder(UIUtils.BORDER, 8, 1));
        posterLabel.setPreferredSize(new Dimension(POSTER_W, POSTER_H));
        add(posterLabel, BorderLayout.CENTER);

        // 异步加载海报
        loadPosterAsync(movie.getImageUrl());

        // 底部信息
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel title = new JLabel(movie.getName());
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
        JLabel seats = new JLabel("剩 " + movie.getAvailableSeats() + " 座");
        seats.setFont(UIUtils.FONT_SMALL);
        seats.setForeground(movie.getAvailableSeats() > 0 ? UIUtils.TEXT_MUTED : UIUtils.PRIMARY);
        row.add(price, BorderLayout.WEST);
        row.add(seats, BorderLayout.EAST);

        info.add(title);
        info.add(Box.createVerticalStrut(4));
        info.add(meta);
        info.add(Box.createVerticalStrut(8));
        info.add(row);
        add(info, BorderLayout.SOUTH);

        // 整卡点击：内嵌切换到详情页，不弹新窗口
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (parent != null) parent.showDetail(movie);
            }
        });
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
                } catch (Exception ex) {
                    return null;
                }
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) posterLabel.setIcon(icon);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private static ImageIcon placeholder(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0xF3F4F6), 0, h, new Color(0xE5E7EB)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x9CA3AF));
        g.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        FontMetrics fm = g.getFontMetrics();
        String s = "加载中…";
        g.drawString(s, (w - fm.stringWidth(s)) / 2, h / 2);
        g.dispose();
        return new ImageIcon(img);
    }
}
