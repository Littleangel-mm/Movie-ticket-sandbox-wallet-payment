package gui;

import dao.MovieDAO;
import model.Movie;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientMainFrame extends JFrame {
    private JButton btnMyReservation;
    private JPanel mainPanel;
    private JPanel moviePanel;
    private JPanel posterGrid;
    private MyReservationPanel myReservationPanel;
    private String currentCustomerName = "";
    private int currentPage = 1;
    private int pageSize = 8;
    private int totalPages = 1;
    private JButton btnPrev;
    private JButton btnNext;
    private JLabel lblPageInfo;
    private java.util.List<Movie> allMovies;

    public ClientMainFrame() {
        setTitle("电影票预约系统");
        setSize(1100, 680);
        setMinimumSize(new Dimension(960, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG);

        // 顶部 Header带用户信息
        add(buildHeader(), BorderLayout.NORTH);

        // 主面板（卡片布局）
        mainPanel = new JPanel(new CardLayout());
        mainPanel.setBackground(UIUtils.BG);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 电影浏览面板
        moviePanel = new JPanel(new BorderLayout(0, 12));
        moviePanel.setOpaque(false);

        // 顶部操作卡片
        btnMyReservation = UIUtils.secondaryButton("查看预约");
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionBar.setOpaque(false);
        actionBar.add(btnMyReservation);

        JPanel actionCard = new JPanel(new BorderLayout());
        actionCard.setBackground(UIUtils.CARD);
        actionCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        actionCard.add(actionBar, BorderLayout.WEST);
        JLabel hint = new JLabel("点击海报查看详情并预约");
        hint.setForeground(UIUtils.TEXT_MUTED);
        hint.setFont(UIUtils.FONT_SMALL);
        actionCard.add(hint, BorderLayout.EAST);
        moviePanel.add(actionCard, BorderLayout.NORTH);

        // 海报网格（可滚动）
        posterGrid = new JPanel(new GridLayout(0, 4, 16, 16));
        posterGrid.setBackground(UIUtils.BG);
        posterGrid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scrollPane = new JScrollPane(posterGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIUtils.BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        moviePanel.add(scrollPane, BorderLayout.CENTER);

        // 底部分页卡片
        btnPrev = UIUtils.secondaryButton("上一页");
        btnNext = UIUtils.secondaryButton("下一页");
        lblPageInfo = new JLabel();
        lblPageInfo.setForeground(UIUtils.TEXT_BODY);
        lblPageInfo.setFont(UIUtils.FONT_BOLD);
        lblPageInfo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pager.setOpaque(false);
        pager.add(btnPrev);
        pager.add(lblPageInfo);
        pager.add(btnNext);

        JPanel pagerCard = new JPanel(new BorderLayout());
        pagerCard.setBackground(UIUtils.CARD);
        pagerCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        pagerCard.add(pager, BorderLayout.CENTER);
        moviePanel.add(pagerCard, BorderLayout.SOUTH);

        mainPanel.add(moviePanel, "movie");
        add(mainPanel, BorderLayout.CENTER);

        // 加载电影数据
        reloadAllMovies();

        // 我的预约按钮
        btnMyReservation.addActionListener(e -> openMyReservation());

        btnPrev.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadMovies();
            }
        });
        btnNext.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadMovies();
            }
        });
    }

    private void reloadAllMovies() {
        MovieDAO dao = new MovieDAO();
        allMovies = dao.getAllMovies();
        totalPages = (int) Math.ceil(allMovies.size() / (double) pageSize);
        if (totalPages == 0) totalPages = 1;
        currentPage = 1;
        loadMovies();
    }

    public void showMoviePanel() {
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, "movie");
    }

    /** 内嵌切换到「预约选座」面板。onSuccess 为预约成功后的回调（默认跳「查看预约」）。 */
    public void showReservation(Movie movie, Runnable onSuccess) {
        for (Component c : mainPanel.getComponents()) {
            if (c instanceof ReservationPanel) { mainPanel.remove(c); break; }
        }
        ReservationPanel panel = new ReservationPanel(this, movie, onSuccess);
        mainPanel.add(panel, "reservation");
        ((CardLayout) mainPanel.getLayout()).show(mainPanel, "reservation");
    }

    /** 内嵌切换到电影详情页，不弹新窗口。 */
    public void showDetail(Movie movie) {
        // 每次则重建（可选优化：缓存）
        for (Component c : mainPanel.getComponents()) {
            if (c instanceof MovieDetailPanel) { mainPanel.remove(c); break; }
        }
        MovieDetailPanel detail = new MovieDetailPanel(this, movie);
        mainPanel.add(detail, "detail");
        ((CardLayout) mainPanel.getLayout()).show(mainPanel, "detail");
    }

    /** 从详情页或其他处跳到「我的预约」。 */
    public void openMyReservation() {
        if (!ClientSession.isLoggedIn()) {
            String name = JOptionPane.showInputDialog(this,
                    "请输入您的姓名以查看预约记录：", "验证身份", JOptionPane.QUESTION_MESSAGE);
            if (name == null || name.trim().isEmpty()) return;
            ClientSession.customerName = name.trim();
            refreshHeader();
        }
        currentCustomerName = ClientSession.customerName;
        if (myReservationPanel == null || !currentCustomerName.equals(myReservationPanel.customerName)) {
            if (myReservationPanel != null) mainPanel.remove(myReservationPanel);
            myReservationPanel = new MyReservationPanel(this, currentCustomerName);
            mainPanel.add(myReservationPanel, "myreservation");
        } else {
            myReservationPanel.reloadData();
        }
        ((CardLayout) mainPanel.getLayout()).show(mainPanel, "myreservation");
    }

    public void loadMovies() {
        // 每次都重新从数据库加载 allMovies，确保剩余座位刷新
        MovieDAO dao = new MovieDAO();
        allMovies = dao.getAllMovies();
        totalPages = Math.max(1, (int) Math.ceil(allMovies.size() / (double) pageSize));
        if (currentPage > totalPages) currentPage = totalPages;

        posterGrid.removeAll();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allMovies.size());
        for (int i = start; i < end; i++) {
            posterGrid.add(new MoviePosterCard(allMovies.get(i), this));
        }
        // 不足 pageSize 时补透明占位，避免顶对齐
        for (int i = end - start; i < pageSize; i++) {
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            posterGrid.add(filler);
        }
        posterGrid.revalidate();
        posterGrid.repaint();

        lblPageInfo.setText("第 " + currentPage + " / " + totalPages + " 页");
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }

    private JPanel headerPanel;

    private JComponent buildHeader() {
        headerPanel = UIUtils.header("电影票预约", "浏览当前排片并进行预约");
        headerPanel.add(buildHeaderRight(), BorderLayout.EAST);
        return headerPanel;
    }

    private JComponent buildHeaderRight() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 24));
        if (ClientSession.isLoggedIn()) {
            JLabel user = new JLabel(ClientSession.customerName);
            user.setForeground(UIUtils.TEXT_TITLE);
            user.setFont(UIUtils.FONT_BOLD);
            JButton logout = new JButton("切换账号");
            logout.setFocusPainted(false);
            logout.setBorderPainted(false);
            logout.setContentAreaFilled(false);
            logout.setForeground(UIUtils.PRIMARY);
            logout.setFont(UIUtils.FONT_SMALL);
            logout.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            logout.addActionListener(e -> {
                ClientSession.logout();
                if (myReservationPanel != null) {
                    mainPanel.remove(myReservationPanel);
                    myReservationPanel = null;
                }
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "movie");
                refreshHeader();
            });
            right.add(user);
            right.add(logout);
        }
        return right;
    }

    /** 登录状态变化后重建 header 右侧 */
    public void refreshHeader() {
        if (headerPanel == null) return;
        // 移除原有 EAST
        for (Component c : headerPanel.getComponents()) {
            Object cons = ((BorderLayout) headerPanel.getLayout()).getConstraints(c);
            if (BorderLayout.EAST.equals(cons)) { headerPanel.remove(c); break; }
        }
        headerPanel.add(buildHeaderRight(), BorderLayout.EAST);
        headerPanel.revalidate();
        headerPanel.repaint();
    }

    public static void launch() {
        UIUtils.applyTheme();
        SwingUtilities.invokeLater(() -> new ClientMainFrame().setVisible(true));
    }

    public static void main(String[] args) {
        launch();
    }
} 