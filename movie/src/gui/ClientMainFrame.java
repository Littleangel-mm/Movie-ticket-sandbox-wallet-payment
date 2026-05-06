package gui;

import dao.MovieDAO;
import model.Movie;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientMainFrame extends JFrame {
    private JTable movieTable;
    private DefaultTableModel tableModel;
    private JButton btnReserve;
    private JButton btnMyReservation;
    private JPanel mainPanel;
    private JPanel moviePanel;
    private MyReservationPanel myReservationPanel;
    private String currentCustomerName = "";
    private int currentPage = 1;
    private int pageSize = 5;
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

        tableModel = new DefaultTableModel(new String[]{
                "ID", "电影名", "影厅", "放映时间", "票价", "总座位", "剩余座位", "封面"
        }, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 7 ? ImageIcon.class : Object.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        movieTable = new JTable(tableModel);
        UIUtils.styleTable(movieTable);
        int[] widths = {60, 220, 90, 170, 80, 80, 90, 90};
        for (int i = 0; i < widths.length; i++) {
            movieTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scrollPane = new JScrollPane(movieTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIUtils.CARD);

        // 顶部操作卡片
        btnReserve = UIUtils.primaryButton("预约选座");
        btnMyReservation = UIUtils.secondaryButton("我的预约");
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionBar.setOpaque(false);
        actionBar.add(btnReserve);
        actionBar.add(btnMyReservation);

        JPanel actionCard = new JPanel(new BorderLayout());
        actionCard.setBackground(UIUtils.CARD);
        actionCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        actionCard.add(actionBar, BorderLayout.WEST);
        JLabel hint = new JLabel("选中一部电影后点击“预约选座”");
        hint.setForeground(UIUtils.TEXT_MUTED);
        hint.setFont(UIUtils.FONT_SMALL);
        actionCard.add(hint, BorderLayout.EAST);
        moviePanel.add(actionCard, BorderLayout.NORTH);

        // 表格卡片
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UIUtils.CARD);
        tableCard.setBorder(new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1));
        tableCard.add(scrollPane, BorderLayout.CENTER);
        moviePanel.add(tableCard, BorderLayout.CENTER);

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

        // 预约按钮事件
        btnReserve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = movieTable.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(ClientMainFrame.this, "请先选择一部电影！");
                    return;
                }
                int movieId = (int) tableModel.getValueAt(row, 0);
                MovieDAO movieDAO = new MovieDAO();
                Movie movie = movieDAO.getMovieById(movieId);
                if (movie == null) {
                    JOptionPane.showMessageDialog(ClientMainFrame.this, "未找到该电影信息！");
                    return;
                }
                // 打开预约对话框（不再传递客户名）
                ReservationDialog dialog = new ReservationDialog(ClientMainFrame.this, movie);
                dialog.setVisible(true);
                loadMovies(); // 预约后刷新电影座位
            }
        });

        // 我的预约按钮事件
        btnMyReservation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!ClientSession.isLoggedIn()) {
                    String name = JOptionPane.showInputDialog(ClientMainFrame.this,
                            "请输入您的姓名以查看预约记录：", "验证身份", JOptionPane.QUESTION_MESSAGE);
                    if (name == null || name.trim().isEmpty()) return;
                    ClientSession.customerName = name.trim();
                    refreshHeader();
                }
                currentCustomerName = ClientSession.customerName;
                if (myReservationPanel == null || !currentCustomerName.equals(myReservationPanel.customerName)) {
                    if (myReservationPanel != null) mainPanel.remove(myReservationPanel);
                    myReservationPanel = new MyReservationPanel(ClientMainFrame.this, currentCustomerName);
                    mainPanel.add(myReservationPanel, "myreservation");
                } else {
                    myReservationPanel.reloadData();
                }
                CardLayout cl = (CardLayout) mainPanel.getLayout();
                cl.show(mainPanel, "myreservation");
            }
        });

        // 双击表格返回电影浏览
        movieTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    CardLayout cl = (CardLayout) mainPanel.getLayout();
                    cl.show(mainPanel, "movie");
                }
            }
        });

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

    public void loadMovies() {
        // 每次都重新从数据库加载 allMovies，确保剩余座位刷新
        MovieDAO dao = new MovieDAO();
        allMovies = dao.getAllMovies();
        tableModel.setRowCount(0);
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allMovies.size());
        for (int i = start; i < end; i++) {
            Movie m = allMovies.get(i);
            ImageIcon icon = getImageIcon(m.getImageUrl());
            tableModel.addRow(new Object[]{
                    m.getId(),
                    m.getName(),
                    m.getHall(),
                    m.getShowTime(),
                    "￥" + m.getPrice(),
                    m.getTotalSeats(),
                    m.getAvailableSeats(),
                    icon
            });
        }
        lblPageInfo.setText("第 " + currentPage + " / " + totalPages + " 页");
        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }

    private ImageIcon getImageIcon(String urlStr) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            ImageIcon original = new ImageIcon(url);
            Image scaled = original.getImage().getScaledInstance(46, 60, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return new ImageIcon(new java.awt.image.BufferedImage(46, 60, java.awt.image.BufferedImage.TYPE_INT_ARGB));
        }
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