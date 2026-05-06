package gui;

import dao.ReservationDAO;
import dao.MovieDAO;
import model.Movie;
import model.Reservation;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

public class ReservationDialog extends JDialog {
    private Movie movie;
    private JTextField tfCustomerName;
    private JTextField tfSeatNumber;
    private JButton btnReserve;
    private JButton btnCancel;

    public ReservationDialog(JFrame parent, Movie movie) {
        super(parent, "预约选座", true);
        this.movie = movie;
        setSize(440, 420);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG);

        // 顶部标题
        JPanel header = UIUtils.header("预约选座", movie.getName());
        add(header, BorderLayout.NORTH);

        // 表单卡片
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIUtils.CARD);
        form.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        addReadOnly(form, g, 0, "电影名", movie.getName());
        addReadOnly(form, g, 1, "影厅",   movie.getHall());
        addReadOnly(form, g, 2, "放映时间", movie.getShowTime());
        addReadOnly(form, g, 3, "票价",   "￥" + movie.getPrice());

        tfCustomerName = UIUtils.textField();
        if (ClientSession.isLoggedIn()) {
            tfCustomerName.setText(ClientSession.customerName);
            tfCustomerName.setEditable(false);
        }
        addInput(form, g, 4, "客户姓名", tfCustomerName);
        tfSeatNumber = UIUtils.textField();
        addInput(form, g, 5, "座位号",   tfSeatNumber);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UIUtils.BG);
        center.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        center.add(form, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        btnReserve = UIUtils.primaryButton("确认预约");
        btnCancel = UIUtils.secondaryButton("取消");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(UIUtils.BG);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        btnPanel.add(btnCancel);
        btnPanel.add(btnReserve);
        add(btnPanel, BorderLayout.SOUTH);

        btnReserve.addActionListener(e -> onReserve());
        btnCancel.addActionListener(e -> dispose());
    }

    private void addReadOnly(JPanel form, GridBagConstraints g, int row, String label, String value) {
        g.gridy = row; g.gridx = 0; g.weightx = 0;
        JLabel l = new JLabel(label); l.setForeground(UIUtils.TEXT_MUTED); l.setFont(UIUtils.FONT_BASE);
        form.add(l, g);
        g.gridx = 1; g.weightx = 1;
        JLabel v = new JLabel(value); v.setForeground(UIUtils.TEXT_TITLE); v.setFont(UIUtils.FONT_BOLD);
        form.add(v, g);
    }

    private void addInput(JPanel form, GridBagConstraints g, int row, String label, JComponent input) {
        g.gridy = row; g.gridx = 0; g.weightx = 0;
        JLabel l = new JLabel(label); l.setForeground(UIUtils.TEXT_MUTED); l.setFont(UIUtils.FONT_BASE);
        form.add(l, g);
        g.gridx = 1; g.weightx = 1;
        form.add(input, g);
    }

    private void onReserve() {
        String customerName = tfCustomerName.getText().trim();
        String seatNumber = tfSeatNumber.getText().trim();
        if (customerName.isEmpty() || seatNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入客户姓名和座位号！");
            return;
        }
        // 校验座位是否被占用
        dao.ReservationDAO reservationDAO = new dao.ReservationDAO();
        model.Reservation exist = reservationDAO.getReservationByMovieAndSeat(movie.getId(), seatNumber);
        if (exist != null) {
            JOptionPane.showMessageDialog(this, "该座位已被预约，请选择其他座位！");
            return;
        }
        // 新增预约
        model.Reservation reservation = new model.Reservation();
        reservation.setMovieId(movie.getId());
        reservation.setCustomerName(customerName);
        reservation.setSeatNumber(seatNumber);
        reservation.setReserveTime(new java.util.Date());
        boolean success = reservationDAO.addReservation(reservation);
        if (success) {
            // 预约成功后立即减少剩余座位
            new MovieDAO().decreaseAvailableSeats(movie.getId());
            // 记住客户姓名，避免后续重复输入
            ClientSession.customerName = customerName;
            // 通知父窗口刷新顶部用户区
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner instanceof ClientMainFrame) {
                ((ClientMainFrame) owner).refreshHeader();
            }
            JOptionPane.showMessageDialog(this, "预约成功！");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "预约失败，请重试！");
        }
    }
} 