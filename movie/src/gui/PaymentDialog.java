package gui;

import dao.PaymentDAO;
import dao.ReservationDAO;
import dao.MovieDAO;
import model.Payment;
import model.Reservation;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

public class PaymentDialog extends JDialog {
    private int reservationId;
    private JLabel lblInfo;
    private JComboBox<String> cbMethod;
    private JButton btnPay;
    private JButton btnCancelPay;
    private JButton btnClose;
    private PaymentDAO paymentDAO = new PaymentDAO();
    private ReservationDAO reservationDAO = new ReservationDAO();
    private MyReservationPanel myReservationPanel;

    public PaymentDialog(JFrame parent, int reservationId, MyReservationPanel myReservationPanel) {
        super(parent, "支付", true);
        this.reservationId = reservationId;
        this.myReservationPanel = myReservationPanel;
        setSize(460, 360);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG);

        Reservation reservation = reservationDAO.getReservationsByCustomer("")
                .stream().filter(r -> r.getId() == reservationId).findFirst().orElse(null);

        String subtitle = reservation == null
                ? "未找到预约信息"
                : "预约单号 " + reservationId + "，座位 " + reservation.getSeatNumber();
        add(UIUtils.header("订单支付", subtitle), BorderLayout.NORTH);
        lblInfo = new JLabel(); // 保留以免空引用

        // 中间卡片：选择支付方式
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIUtils.CARD);
        form.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        JLabel lblM = new JLabel("支付方式");
        lblM.setForeground(UIUtils.TEXT_MUTED);
        lblM.setFont(UIUtils.FONT_BASE);
        form.add(lblM, g);
        g.gridx = 1; g.weightx = 1;
        cbMethod = UIUtils.comboBox(new String[]{"支付宝", "微信支付", "银行卡"});
        form.add(cbMethod, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        JLabel lblN = new JLabel("订单号");
        lblN.setForeground(UIUtils.TEXT_MUTED);
        lblN.setFont(UIUtils.FONT_BASE);
        form.add(lblN, g);
        g.gridx = 1; g.weightx = 1;
        JLabel lblOrder = new JLabel("#" + reservationId);
        lblOrder.setForeground(UIUtils.TEXT_TITLE);
        lblOrder.setFont(UIUtils.FONT_BOLD);
        form.add(lblOrder, g);

        if (reservation != null) {
            g.gridx = 0; g.gridy = 2; g.weightx = 0;
            JLabel lblS = new JLabel("座位");
            lblS.setForeground(UIUtils.TEXT_MUTED);
            lblS.setFont(UIUtils.FONT_BASE);
            form.add(lblS, g);
            g.gridx = 1; g.weightx = 1;
            JLabel lblSeat = new JLabel(reservation.getSeatNumber());
            lblSeat.setForeground(UIUtils.TEXT_TITLE);
            lblSeat.setFont(UIUtils.FONT_BOLD);
            form.add(lblSeat, g);
        }

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UIUtils.BG);
        center.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        center.add(form, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        btnPay = UIUtils.primaryButton("立即支付");
        btnCancelPay = UIUtils.dangerButton("取消支付");
        btnClose = UIUtils.secondaryButton("关闭");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(UIUtils.BG);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        btnPanel.add(btnClose);
        btnPanel.add(btnCancelPay);
        btnPanel.add(btnPay);
        add(btnPanel, BorderLayout.SOUTH);

        Payment payment = paymentDAO.getPaymentByReservationId(reservationId);
        if (payment != null && "已支付".equals(payment.getStatus())) {
            btnPay.setEnabled(false);
            btnCancelPay.setEnabled(true);
        } else {
            btnPay.setEnabled(true);
            btnCancelPay.setEnabled(false);
        }

        btnPay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPay();
            }
        });
        btnCancelPay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancelPay();
            }
        });
        btnClose.addActionListener(e -> dispose());
    }

    private void onPay() {
        String method = (String) cbMethod.getSelectedItem();
        if ("支付宝".equals(method)) {
            payByAlipay();
            return;
        }
        // 其它方式：保留原来的直接登记逻辑
        Payment payment = new Payment();
        payment.setReservationId(reservationId);
        payment.setMethod(method);
        payment.setStatus("已支付");
        payment.setPayTime(new java.util.Date());
        boolean success = new PaymentDAO().addPayment(payment);
        if (success) {
            JOptionPane.showMessageDialog(this, "支付成功！");
            btnPay.setEnabled(false);
            btnCancelPay.setEnabled(true);
            if (myReservationPanel != null) {
                myReservationPanel.reloadData();
                if (myReservationPanel.parentFrame != null) {
                    myReservationPanel.parentFrame.loadMovies();
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "支付失败，请重试！");
        }
    }

    /** 调起支付宝沙箱扫码支付：拉起二维码弹窗并轮询订单状态 */
    private void payByAlipay() {
        // 取真实金额：从预约对应的 Movie 拿 price
        Reservation reservation = reservationDAO.getReservationById(reservationId);
        if (reservation == null) {
            JOptionPane.showMessageDialog(this, "未找到预约记录！");
            return;
        }
        model.Movie movie = new MovieDAO().getMovieById(reservation.getMovieId());
        if (movie == null) {
            JOptionPane.showMessageDialog(this, "未找到对应电影信息！");
            return;
        }
        String amount = String.format(java.util.Locale.US, "%.2f", movie.getPrice());
        String subject = "电影票 - " + movie.getName();
        String outTradeNo = alipay.AlipayService.generateOutTradeNo(reservationId);

        AlipayQrDialog qrDialog = new AlipayQrDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                reservationId, outTradeNo, subject, amount, myReservationPanel);
        qrDialog.start();
        qrDialog.setVisible(true);

        // 弹窗关闭后回到 PaymentDialog，刷新按钮状态
        Payment payment = paymentDAO.getPaymentByReservationId(reservationId);
        if (payment != null && "已支付".equals(payment.getStatus())) {
            btnPay.setEnabled(false);
            btnCancelPay.setEnabled(true);
            dispose(); // 已支付直接关闭支付对话框
        }
    }

    private void onCancelPay() {
        Payment payment = paymentDAO.getPaymentByReservationId(reservationId);
        if (payment == null || !"已支付".equals(payment.getStatus())) {
            JOptionPane.showMessageDialog(this, "当前无已支付记录！");
            return;
        }
        boolean success = paymentDAO.updatePaymentStatus(payment.getId(), "已取消");
        if (success) {
            JOptionPane.showMessageDialog(this, "已取消支付！");
            btnPay.setEnabled(true);
            btnCancelPay.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, "取消支付失败！");
        }
    }
} 