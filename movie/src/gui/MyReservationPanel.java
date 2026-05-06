package gui;

import dao.MovieDAO;
import dao.ReservationDAO;
import dao.PaymentDAO;
import model.Movie;
import model.Reservation;
import model.Payment;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MyReservationPanel extends JPanel {
    public String customerName;
    public ClientMainFrame parentFrame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnCancel;
    private JButton btnPay;
    private JButton btnBack;
    private ReservationDAO reservationDAO = new ReservationDAO();
    private MovieDAO movieDAO = new MovieDAO();
    private PaymentDAO paymentDAO = new PaymentDAO();

    public MyReservationPanel(ClientMainFrame parentFrame, String customerName) {
        this.parentFrame = parentFrame;
        this.customerName = customerName;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);

        // 顶部信息卡
        JLabel info = new JLabel("客户：" + customerName);
        info.setFont(UIUtils.FONT_H2);
        info.setForeground(UIUtils.TEXT_TITLE);
        JLabel sub = new JLabel("查看、支付或取消你的预约");
        sub.setFont(UIUtils.FONT_SMALL);
        sub.setForeground(UIUtils.TEXT_MUTED);
        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setOpaque(false);
        info.setAlignmentX(LEFT_ALIGNMENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        infoBox.add(info);
        infoBox.add(Box.createVerticalStrut(2));
        infoBox.add(sub);

        tableModel = new DefaultTableModel(new String[]{
                "预约ID", "电影名", "影厅", "放映时间", "座位号", "票价", "预约时间", "支付状态"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        UIUtils.styleTable(table);
        table.getColumnModel().getColumn(7).setCellRenderer(UIUtils.statusBadgeRenderer());
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIUtils.CARD);

        btnPay = UIUtils.primaryButton("支付");
        btnCancel = UIUtils.dangerButton("取消预约");
        btnBack = UIUtils.ghostButton("返回");

        // 顶部卡片
        JPanel topCard = new JPanel(new BorderLayout());
        topCard.setBackground(UIUtils.CARD);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        topCard.add(infoBox, BorderLayout.WEST);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRight.setOpaque(false);
        topRight.add(btnBack);
        topCard.add(topRight, BorderLayout.EAST);

        // 表格卡片
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UIUtils.CARD);
        tableCard.setBorder(new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1));
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // 底部操作卡片
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(UIUtils.CARD);
        btnPanel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        btnPanel.add(btnCancel);
        btnPanel.add(btnPay);

        add(topCard, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(MyReservationPanel.this, "请选择要取消的预约！");
                    return;
                }
                int reservationId = (int) tableModel.getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(MyReservationPanel.this, "确认取消该预约吗？", "确认", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    // 先查到 movieId
                    Reservation r = reservationDAO.getReservationById(reservationId);
                    boolean success = reservationDAO.deleteReservation(reservationId);
                    if (success) {
                        // 恢复剩余座位
                        if (r != null) {
                            new MovieDAO().increaseAvailableSeats(r.getMovieId());
                        }
                        JOptionPane.showMessageDialog(MyReservationPanel.this, "取消成功！");
                        reloadData();
                        if (parentFrame != null) parentFrame.loadMovies();
                    } else {
                        JOptionPane.showMessageDialog(MyReservationPanel.this, "取消失败！");
                    }
                }
            }
        });

        btnPay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(MyReservationPanel.this, "请选择要支付的预约！");
                    return;
                }
                int reservationId = (int) tableModel.getValueAt(row, 0);
                String payStatus = (String) tableModel.getValueAt(row, 7);
                if ("已支付".equals(payStatus)) {
                    JOptionPane.showMessageDialog(MyReservationPanel.this, "该预约已支付！");
                    return;
                }
                // 打开支付对话框
                PaymentDialog dialog = new PaymentDialog((JFrame) SwingUtilities.getWindowAncestor(MyReservationPanel.this), reservationId, MyReservationPanel.this);
                dialog.setVisible(true);
                reloadData();
                if (parentFrame != null) parentFrame.loadMovies();
            }
        });

        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (parentFrame != null) parentFrame.showMoviePanel();
            }
        });

        reloadData();
    }

    public void reloadData() {
        tableModel.setRowCount(0);
        List<Reservation> reservations = reservationDAO.getReservationsByCustomer(customerName);
        for (Reservation r : reservations) {
            Movie movie = movieDAO.getMovieById(r.getMovieId());
            String movieName = movie != null ? movie.getName() : "";
            String hall = movie != null ? movie.getHall() : "";
            String showTime = movie != null ? movie.getShowTime() : "";
            String price = movie != null ? ("￥" + movie.getPrice()) : "";
            Payment payment = paymentDAO.getPaymentByReservationId(r.getId());
            String payStatus = payment == null ? "未支付" : payment.getStatus();
            tableModel.addRow(new Object[]{
                    r.getId(),
                    movieName,
                    hall,
                    showTime,
                    r.getSeatNumber(),
                    price,
                    r.getReserveTime(),
                    payStatus
            });
        }
    }
} 