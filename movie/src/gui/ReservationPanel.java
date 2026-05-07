package gui;

import dao.MovieDAO;
import dao.ReservationDAO;
import model.Movie;
import model.Reservation;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * 预约选座的内嵌面板，挂在 ClientMainFrame 的 CardLayout 上，避免弹独立窗口。
 * 顶部 header 与详情页一致；底部按钮：返回 / 确认预约。
 */
public class ReservationPanel extends JPanel {

    public ReservationPanel(ClientMainFrame parent, Movie movie, Runnable onSuccess) {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        // 顶部返回栏
        JButton btnBack = UIUtils.ghostButton("← 返回详情");
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIUtils.CARD);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 16)));
        topBar.add(btnBack, BorderLayout.WEST);
        JLabel sub = new JLabel("预约选座 · " + movie.getName());
        sub.setForeground(UIUtils.TEXT_MUTED);
        sub.setFont(UIUtils.FONT_SMALL);
        topBar.add(sub, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // 主体卡片
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(UIUtils.CARD);
        content.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 14, 1),
                BorderFactory.createEmptyBorder(28, 32, 28, 32)));

        JLabel title = new JLabel("预约选座");
        title.setFont(new Font(UIUtils.FONT_TITLE.getName(), Font.BOLD, 24));
        title.setForeground(UIUtils.TEXT_TITLE);
        content.add(title, BorderLayout.NORTH);

        // 表单
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 8, 10, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        addReadOnly(form, g, 0, "电影名",   movie.getName());
        addReadOnly(form, g, 1, "影厅",     movie.getHall());
        addReadOnly(form, g, 2, "放映时间", movie.getShowTime());
        addReadOnly(form, g, 3, "票价",     "￥" + movie.getPrice());

        JTextField tfName = UIUtils.textField();
        if (ClientSession.isLoggedIn()) {
            tfName.setText(ClientSession.customerName);
            tfName.setEditable(false);
        }
        addInput(form, g, 4, "客户姓名", tfName);

        JTextField tfSeat = UIUtils.textField();
        addInput(form, g, 5, "座位号",   tfSeat);

        content.add(form, BorderLayout.CENTER);

        // 底部按钮
        JButton btnCancel  = UIUtils.secondaryButton("取消");
        JButton btnConfirm = UIUtils.primaryButton("确认预约");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        btns.add(btnCancel);
        btns.add(btnConfirm);
        content.add(btns, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        // 事件
        btnBack.addActionListener(e -> parent.showDetail(movie));
        btnCancel.addActionListener(e -> parent.showDetail(movie));
        btnConfirm.addActionListener(e -> {
            int rid = doReserve(movie, tfName.getText().trim(), tfSeat.getText().trim());
            if (rid > 0) {
                parent.loadMovies();
                if (onSuccess != null) onSuccess.run();
                else parent.openMyReservation();
            }
        });
    }

    /** 返回新预约 id；失败/校验不通过返回 -1。 */
    private int doReserve(Movie movie, String name, String seat) {
        if (name.isEmpty() || seat.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入客户姓名和座位号！");
            return -1;
        }
        ReservationDAO dao = new ReservationDAO();
        if (dao.getReservationByMovieAndSeat(movie.getId(), seat) != null) {
            JOptionPane.showMessageDialog(this, "该座位已被预约，请选择其他座位！");
            return -1;
        }
        Reservation r = new Reservation();
        r.setMovieId(movie.getId());
        r.setCustomerName(name);
        r.setSeatNumber(seat);
        r.setReserveTime(new java.util.Date());
        if (!dao.addReservation(r)) {
            JOptionPane.showMessageDialog(this, "预约失败，请重试！");
            return -1;
        }
        new MovieDAO().decreaseAvailableSeats(movie.getId());
        ClientSession.customerName = name;
        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner instanceof ClientMainFrame) {
            ((ClientMainFrame) owner).refreshHeader();
        }
        JOptionPane.showMessageDialog(this, "预约成功！");
        return r.getId();
    }

    private void addReadOnly(JPanel form, GridBagConstraints g, int row, String label, String value) {
        g.gridy = row; g.gridx = 0; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(UIUtils.TEXT_MUTED);
        l.setFont(UIUtils.FONT_BODY);
        l.setPreferredSize(new Dimension(96, 24));
        form.add(l, g);
        g.gridx = 1; g.weightx = 1;
        JLabel v = new JLabel(value);
        v.setForeground(UIUtils.TEXT_TITLE);
        v.setFont(UIUtils.FONT_BOLD);
        form.add(v, g);
    }

    private void addInput(JPanel form, GridBagConstraints g, int row, String label, JComponent input) {
        g.gridy = row; g.gridx = 0; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(UIUtils.TEXT_MUTED);
        l.setFont(UIUtils.FONT_BODY);
        l.setPreferredSize(new Dimension(96, 24));
        form.add(l, g);
        g.gridx = 1; g.weightx = 1;
        input.setPreferredSize(new Dimension(280, 32));
        form.add(input, g);
    }
}
