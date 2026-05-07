package gui;

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
        boolean hasSeats = movie.getAvailableSeats() > 0;
        JButton btnWatch = UIUtils.primaryButton(hasSeats ? "立即观看" : "已售罄");
        btnWatch.setEnabled(hasSeats);
        JButton btnReserve = UIUtils.secondaryButton("立即预约");
        btnReserve.setEnabled(hasSeats);
        btns.add(btnWatch);
        btns.add(btnReserve);
        right.add(btns);

        content.add(right, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // 事件
        btnBack.addActionListener(e -> parent.showMoviePanel());

        // 「立即观看」：预约（内嵌） → 支付 → 自动激活 → 直接播放，一条龙
        btnWatch.addActionListener(e -> startWatchFlow(parent, movie));

        // 「立即预约」：仅内嵌打开预约面板，预约完成跳「查看预约」
        btnReserve.addActionListener(e ->
                parent.showReservation(movie, () -> parent.openMyReservation()));
    }

    /**
     * 一键观看流程：内嵌预约 → 支付 → 激活 → 播放。预约/支付任何一步取消都会中断。
     */
    private void startWatchFlow(ClientMainFrame parent, Movie movie) {
        if (!movie.hasVideo()) {
            JOptionPane.showMessageDialog(this,
                    "该影片暂未上线视频，请管理员上传后再来", "暂无视频", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 内嵌预约，成功回调里继续支付 + 激活 + 播放
        parent.showReservation(movie, () -> continueAfterReserve(parent, movie));
    }

    private void continueAfterReserve(ClientMainFrame parent, Movie movie) {
        // 拿到刚刚生成的预约 id（按客户姓名+座位倒序找最新一条比较稳）。
        // 这里简单做法：取该客户最近一条 reservation。
        int rid = findLatestReservationId(movie);
        if (rid <= 0) {
            parent.openMyReservation();
            return;
        }

        // 支付（暂保留对话框，支付链路依赖支付宝沙箱回调，不适合内嵌）
        PaymentDialog pdlg = new PaymentDialog(parent, rid, null);
        pdlg.setVisible(true);

        PaymentDAO paymentDAO = new PaymentDAO();
        Payment payment = paymentDAO.getPaymentByReservationId(rid);
        if (payment == null || !"已支付".equals(payment.getStatus())) {
            parent.openMyReservation();
            return;
        }
        if (payment.getActivatedAt() == null) {
            paymentDAO.activateTicket(payment.getId());
        }
        try {
            String url = MinioService.presignedGetUrl(movie.getVideoObjectKey());
            PlayerDialog dlg = new PlayerDialog(SwingUtilities.getWindowAncestor(parent),
                    "正在观看：" + movie.getName(), url);
            dlg.setVisible(true);
            parent.showMoviePanel();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent, "生成播放地址失败：" + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int findLatestReservationId(Movie movie) {
        if (!ClientSession.isLoggedIn()) return -1;
        java.util.List<model.Reservation> list =
                new dao.ReservationDAO().getReservationsByCustomer(ClientSession.customerName);
        for (model.Reservation r : list) {
            if (r.getMovieId() == movie.getId()) return r.getId();
        }
        return -1;
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
