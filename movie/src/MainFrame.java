import gui.MovieTablePanel;
import gui.OrderManagePanel;
import gui.RevenuePanel;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class MainFrame {
    public static void main(String[] args) {
        UIUtils.applyTheme();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("电影售票系统管理后台");
            frame.setSize(1280, 760);
            frame.setMinimumSize(new Dimension(1080, 640));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(UIUtils.BG);
            root.add(UIUtils.header("管理后台", "影票、订单与营收管理"), BorderLayout.NORTH);

            MovieTablePanel moviePanel = new MovieTablePanel();
            OrderManagePanel orderPanel = new OrderManagePanel();
            RevenuePanel revenuePanel = new RevenuePanel();

            UIUtils.TabBar tabs = new UIUtils.TabBar();
            tabs.addTab("影票管理", wrap(moviePanel));
            tabs.addTab("订单管理", wrap(orderPanel));
            tabs.addTab("营收看板", wrap(revenuePanel));

            // 头部下方放 Tab 标签，再放内容区
            JPanel tabHeader = new JPanel(new BorderLayout());
            tabHeader.setBackground(UIUtils.CARD);
            tabHeader.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0xE5E7EB)),
                    BorderFactory.createEmptyBorder(0, 16, 0, 16)));
            tabHeader.add(tabs.getHeader(), BorderLayout.WEST);

            JPanel content = new JPanel(new BorderLayout());
            content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            content.setBackground(UIUtils.BG);
            content.add(tabs.getContent(), BorderLayout.CENTER);

            JPanel center = new JPanel(new BorderLayout());
            center.setBackground(UIUtils.BG);
            center.add(tabHeader, BorderLayout.NORTH);
            center.add(content, BorderLayout.CENTER);
            root.add(center, BorderLayout.CENTER);

            frame.setContentPane(root);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) {
                    orderPanel.stop();
                    revenuePanel.stop();
                }
            });
            frame.setVisible(true);
        });
    }

    private static JComponent wrap(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIUtils.BG);
        p.setBorder(BorderFactory.createEmptyBorder(16, 4, 4, 4));
        p.add(c, BorderLayout.CENTER);
        return p;
    }
}
