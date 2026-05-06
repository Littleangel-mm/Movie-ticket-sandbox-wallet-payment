package gui;

import dao.OrderDAO;
import dao.PaymentDAO;
import model.OrderView;
import model.Payment;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 管理员订单管理面板：全部预约 + 支付状态联动视图。
 * 支持搜索、状态筛选、自动刷新、标记退款。
 */
public class OrderManagePanel extends JPanel {

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private DefaultTableModel model;
    private JTable table;
    private JTextField tfKeyword;
    private JComboBox<String> cbStatus;
    private JLabel lblSummary;
    private Timer autoRefreshTimer;

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public OrderManagePanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtils.BG);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        reload();
        startAutoRefresh();
    }

    private JComponent buildToolbar() {
        tfKeyword = UIUtils.textField();
        tfKeyword.setPreferredSize(new Dimension(200, 32));
        tfKeyword.putClientProperty("JTextField.placeholderText", "搜索电影/客户/座位");
        cbStatus = UIUtils.comboBox(new String[]{"全部", "已支付", "未支付", "已取消"});

        JButton btnSearch = UIUtils.primaryButton("搜索");
        JButton btnReset = UIUtils.ghostButton("重置");
        JButton btnRefresh = UIUtils.secondaryButton("刷新");

        btnSearch.addActionListener(e -> reload());
        btnReset.addActionListener(e -> {
            tfKeyword.setText("");
            cbStatus.setSelectedIndex(0);
            reload();
        });
        btnRefresh.addActionListener(e -> reload());
        tfKeyword.addActionListener(e -> reload());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(label("搜索"));
        filters.add(tfKeyword);
        filters.add(label("状态"));
        filters.add(cbStatus);
        filters.add(btnSearch);
        filters.add(btnReset);
        filters.add(btnRefresh);

        lblSummary = new JLabel(" ");
        lblSummary.setForeground(UIUtils.TEXT_MUTED);
        lblSummary.setFont(UIUtils.FONT_SMALL);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIUtils.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.add(filters, BorderLayout.WEST);
        card.add(lblSummary, BorderLayout.EAST);
        return card;
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(UIUtils.TEXT_MUTED);
        l.setFont(UIUtils.FONT_BASE);
        return l;
    }

    private JComponent buildTable() {
        model = new DefaultTableModel(
                new String[]{"订单号", "电影", "影厅", "放映时间", "客户", "座位", "票价", "下单时间", "支付方式", "状态", "支付时间"},
                0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIUtils.styleTable(table);
        table.setRowSorter(new TableRowSorter<>(model));
        table.getColumnModel().getColumn(9).setCellRenderer(UIUtils.statusBadgeRenderer());

        int[] widths = {70, 180, 80, 130, 90, 70, 70, 130, 90, 80, 130};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // 右键菜单
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miRefund = new JMenuItem("标记退款 / 取消支付");
        JMenuItem miDetail = new JMenuItem("查看详情");
        menu.add(miDetail);
        menu.add(miRefund);
        table.setComponentPopupMenu(menu);

        miDetail.addActionListener(e -> showDetail());
        miRefund.addActionListener(e -> markRefund());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0 && r < table.getRowCount()) table.setRowSelectionInterval(r, r);
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIUtils.CARD);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIUtils.CARD);
        card.setBorder(new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private void reload() {
        String kw = tfKeyword.getText();
        String st = (String) cbStatus.getSelectedItem();
        List<OrderView> list = orderDAO.findOrders(kw, st);

        model.setRowCount(0);
        int paid = 0; double rev = 0;
        for (OrderView v : list) {
            model.addRow(new Object[]{
                    v.reservationId,
                    v.movieName,
                    v.hall,
                    v.showTime,
                    v.customerName,
                    v.seatNumber,
                    "￥" + v.price,
                    v.reserveTime != null ? FMT.format(v.reserveTime) : "",
                    v.payMethod == null ? "" : v.payMethod,
                    v.displayStatus(),
                    v.payTime != null ? FMT.format(v.payTime) : ""
            });
            if ("已支付".equals(v.payStatus)) { paid++; rev += v.price; }
        }
        lblSummary.setText(String.format("共 %d 单，已支付 %d 单，营收 ￥%.2f", list.size(), paid, rev));
    }

    private OrderView selectedOrder() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        int rid = (int) model.getValueAt(modelRow, 0);
        for (OrderView v : orderDAO.findOrders(null, "全部")) {
            if (v.reservationId == rid) return v;
        }
        return null;
    }

    private void showDetail() {
        OrderView v = selectedOrder();
        if (v == null) { JOptionPane.showMessageDialog(this, "请先选择一行"); return; }
        String msg = "<html><body style='width:280px'>"
                + "<b>订单 #" + v.reservationId + "</b><br/>"
                + "电影：" + v.movieName + "（" + v.hall + "）<br/>"
                + "放映时间：" + v.showTime + "<br/>"
                + "客户：" + v.customerName + "，座位 " + v.seatNumber + "<br/>"
                + "票价：￥" + v.price + "<br/>"
                + "下单时间：" + (v.reserveTime != null ? FMT.format(v.reserveTime) : "") + "<br/>"
                + "支付方式：" + (v.payMethod == null ? "未支付" : v.payMethod) + "<br/>"
                + "支付状态：" + v.displayStatus() + "<br/>"
                + "支付时间：" + (v.payTime != null ? FMT.format(v.payTime) : "")
                + "</body></html>";
        JOptionPane.showMessageDialog(this, msg, "订单详情", JOptionPane.INFORMATION_MESSAGE);
    }

    private void markRefund() {
        OrderView v = selectedOrder();
        if (v == null) { JOptionPane.showMessageDialog(this, "请先选择一行"); return; }
        if (v.paymentId == null) {
            JOptionPane.showMessageDialog(this, "该订单尚未支付，无需退款"); return;
        }
        if ("已取消".equals(v.payStatus)) {
            JOptionPane.showMessageDialog(this, "该订单已经是已取消状态"); return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "确认将订单 #" + v.reservationId + " 标记为退款（已取消）？",
                "确认操作", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        boolean done = paymentDAO.updatePaymentStatus(v.paymentId, "已取消");
        if (done) {
            JOptionPane.showMessageDialog(this, "已标记为退款");
            reload();
        } else {
            JOptionPane.showMessageDialog(this, "操作失败，请重试");
        }
    }

    private void startAutoRefresh() {
        autoRefreshTimer = new Timer(8000, e -> reload());
        autoRefreshTimer.start();
    }

    /** 父容器关闭时调用 */
    public void stop() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
    }
}
