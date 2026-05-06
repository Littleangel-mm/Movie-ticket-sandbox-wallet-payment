package gui;

import dao.OrderDAO;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * 营收看板：关键指标 + 电影销量 Top5。
 */
public class RevenuePanel extends JPanel {

    private final OrderDAO orderDAO = new OrderDAO();
    private JLabel lblTodayOrders, lblTodayPaid, lblTodayRevenue,
                   lblTotalOrders, lblTotalPaid, lblTotalRevenue;
    private DefaultTableModel topModel;
    private Timer refreshTimer;

    public RevenuePanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UIUtils.BG);

        add(buildStatsCards(), BorderLayout.NORTH);
        add(buildTopMovies(), BorderLayout.CENTER);

        reload();
        refreshTimer = new Timer(15000, e -> reload());
        refreshTimer.start();
    }

    private JComponent buildStatsCards() {
        JPanel grid = new JPanel(new GridLayout(1, 6, 12, 0));
        grid.setOpaque(false);

        lblTodayOrders   = new JLabel("0");
        lblTodayPaid     = new JLabel("0");
        lblTodayRevenue  = new JLabel("￥0");
        lblTotalOrders   = new JLabel("0");
        lblTotalPaid     = new JLabel("0");
        lblTotalRevenue  = new JLabel("￥0");

        grid.add(card("今日订单", lblTodayOrders,   UIUtils.INFO));
        grid.add(card("今日已支付", lblTodayPaid,     UIUtils.SUCCESS));
        grid.add(card("今日营收", lblTodayRevenue,  UIUtils.PRIMARY));
        grid.add(card("累计订单", lblTotalOrders,   UIUtils.TEXT_TITLE));
        grid.add(card("累计已支付", lblTotalPaid,     UIUtils.SUCCESS));
        grid.add(card("累计营收", lblTotalRevenue,  UIUtils.PRIMARY));
        return grid;
    }

    private JPanel card(String title, JLabel valueLbl, Color valueColor) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UIUtils.CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel t = new JLabel(title);
        t.setFont(UIUtils.FONT_SMALL);
        t.setForeground(UIUtils.TEXT_MUTED);
        t.setAlignmentX(LEFT_ALIGNMENT);

        valueLbl.setFont(new Font(UIUtils.FONT_TITLE.getFamily(), Font.BOLD, 26));
        valueLbl.setForeground(valueColor);
        valueLbl.setAlignmentX(LEFT_ALIGNMENT);

        p.add(t);
        p.add(Box.createVerticalStrut(6));
        p.add(valueLbl);
        return p;
    }

    private JComponent buildTopMovies() {
        topModel = new DefaultTableModel(
                new String[]{"排名", "电影", "影厅", "已售", "营收"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(topModel);
        UIUtils.styleTable(t);

        int[] widths = {60, 260, 120, 100, 120};
        for (int i = 0; i < widths.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(UIUtils.CARD);

        JLabel title = new JLabel("电影销量 Top 5");
        title.setFont(UIUtils.FONT_H2);
        title.setForeground(UIUtils.TEXT_TITLE);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(UIUtils.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private void reload() {
        OrderDAO.Stats s = orderDAO.getStats();
        lblTodayOrders.setText(String.valueOf(s.todayOrders));
        lblTodayPaid.setText(String.valueOf(s.todayPaid));
        lblTodayRevenue.setText(String.format("￥%.2f", s.todayRevenue));
        lblTotalOrders.setText(String.valueOf(s.totalOrders));
        lblTotalPaid.setText(String.valueOf(s.paidOrders));
        lblTotalRevenue.setText(String.format("￥%.2f", s.totalRevenue));

        List<OrderDAO.MovieSales> top = orderDAO.getTopMovies(5);
        topModel.setRowCount(0);
        int rank = 1;
        for (OrderDAO.MovieSales ms : top) {
            topModel.addRow(new Object[]{
                    "#" + rank++,
                    ms.movieName,
                    ms.hall,
                    ms.paidCount + " 张",
                    String.format("￥%.2f", ms.revenue)
            });
        }
    }

    public void stop() {
        if (refreshTimer != null) refreshTimer.stop();
    }
}
