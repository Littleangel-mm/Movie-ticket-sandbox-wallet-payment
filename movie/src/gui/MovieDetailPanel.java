package gui;

import dao.MovieDAO;
import dao.PaymentDAO;
import model.Movie;
import model.Payment;
import utils.MinioService;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * 电影详情内嵌面板。挂在 ClientMainFrame 的 CardLayout 上，点击海报时直接切换显示，
 * 不再弹出独立窗口，体验无感。
 */
public class MovieDetailPanel extends JPanel {

    public MovieDetailPanel(ClientMainFrame parent, Movie movie) {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        // 顶部返回栏
        JButton btnBack = UIUtils.ghostButton("← 返回排片");
        JLabel sub = new JLabel(movie.getName());
        sub.setForeground(UIUtils.TEXT_MUTED);
        sub.setFont(UIUtils.FONT_SMALL);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIUtils.CARD);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 16)));
        topBar.add(btnBack, BorderLayout.WEST);
        topBar.add(sub, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // 主体卡片
        JPanel content = new JPanel(new BorderLayout(24, 0));
        content.setBackground(UIUtils.CARD);
        content.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 14, 1),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));

        // 左：大海报
        JLabel poster = new JLabel(loadPoster(movie.getImageUrl(), 320, 460));
        poster.setBorder(new UIUtils.RoundedLineBorder(UIUtils.BORDER, 10, 1));
        poster.setHorizontalAlignment(SwingConstants.CENTER);
        poster.setVerticalAlignment(SwingConstants.CENTER);
        poster.setPreferredSize(new Dimension(320, 460));
        content.add(poster, BorderLayout.WEST);

        // 右：信息 + 按钮
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(movie.getName());
        title.setFont(new Font(UIUtils.FONT_TITLE.getName(), Font.BOLD, 28));
        title.setForeground(UIUtils.TEXT_TITLE);
        title.setAlignmentX(LEFT_ALIGNMENT);
        right.add(title);
        right.add(Box.createVerticalStrut(20));

        right.add(infoRow("影厅", movie.getHall()));
        right.add(infoRow("放映时间", movie.getShowTime()));
        right.add(infoRow("票价", "￥" + movie.getPrice()));
        right.add(infoRow("剩余座位", movie.getAvailableSeats() + " / " + movie.getTotalSeats()));
        right.add(infoRow("视频", movie.hasVideo() ? "已上线，购票后可观看" : "暂未上线"));

        right.add(Box.createVerticalGlue());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(LEFT_ALIGNMENT);
        JButton btnWatch = UIUtils.primaryButton(
                movie.getAvailableSeats() > 0 ? "立即观看" : "已售罄");
        btnWatch.setEnabled(movie.getAvailableSeats() > 0);
        JButton btnMy = UIUtils.secondaryButton("我的预约");
        btns.add(btnWatch);
        btns.add(btnMy);
        right.add(btns);

        content.add(right, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // 事件
        btnBack.addActionListener(e -> parent.showMoviePanel());
        btnWatch.addActionListener(e -> startWatchFlow(parent, movie));
        btnMy.addActionListener(e -> parent.openMyReservation());
    }

    /**
     * 一键观看流程：选座 → 支付 → 激活 → 播放。每一步用户取消都会中断。
     */
    private void startWatchFlow(ClientMainFrame parent, Movie movie) {
        if (!movie.hasVideo()) {
            JOptionPane.showMessageDialog(this,
                    "该影片暂未上线视频，请管理员上传后再来", "暂无视频", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. 预约选座
        ReservationDialog rdlg = new ReservationDialog(parent, movie);
        rdlg.setVisible(true);
        int rid = rdlg.newReservationId;
        if (rid <= 0) return; // 用户取消或失败

        parent.loadMovies(); // 刷新剩余座位

        // 2. 支付
        PaymentDialog pdlg = new PaymentDialog(parent, rid, null);
        pdlg.setVisible(true);

        // 3. 校验是否真的已支付
        PaymentDAO paymentDAO = new PaymentDAO();
        Payment payment = paymentDAO.getPaymentByReservationId(rid);
        if (payment == null || !"已支付".equals(payment.getStatus())) {
            // 未支付或被取消，跳到「我的预约」让用户后续处理
            parent.openMyReservation();
            return;
        }

        // 4. 激活票（首次必激活）
        if (payment.getActivatedAt() == null) {
            paymentDAO.activateTicket(payment.getId());
        }

        // 5. 直接打开播放器
        try {
            String url = MinioService.presignedGetUrl(movie.getVideoObjectKey());
            PlayerDialog dlg = new PlayerDialog(SwingUtilities.getWindowAncestor(this),
                    "正在观看：" + movie.getName(), url);
            dlg.setVisible(true);
            parent.showMoviePanel();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "生成播放地址失败：" + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JComponent infoRow(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel l = new JLabel(label + "：");
        l.setForeground(UIUtils.TEXT_MUTED);
        l.setFont(UIUtils.FONT_BODY);
        l.setPreferredSize(new Dimension(80, 24));

        JLabel v = new JLabel(value);
        v.setForeground(UIUtils.TEXT_TITLE);
        v.setFont(UIUtils.FONT_BOLD);

        p.add(l);
        p.add(v);

        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.add(p);
        wrap.add(Box.createVerticalStrut(8));
        return wrap;
    }

    private static ImageIcon loadPoster(String urlStr, int w, int h) {
        try {
            ImageIcon raw = new ImageIcon(new java.net.URL(urlStr));
            if (raw.getIconWidth() <= 0) throw new IllegalStateException();
            Image scaled = raw.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xE5E7EB));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(0x9CA3AF));
            g.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
            FontMetrics fm = g.getFontMetrics();
            String s = "暂无海报";
            g.drawString(s, (w - fm.stringWidth(s)) / 2, h / 2);
            g.dispose();
            return new ImageIcon(img);
        }
    }
}
