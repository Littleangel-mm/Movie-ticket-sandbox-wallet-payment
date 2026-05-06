package gui;

import dao.MovieDAO;
import model.Movie;
import utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

public class MovieTablePanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private final MovieDAO dao = new MovieDAO();

    public MovieTablePanel() {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BG);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // 表头列名，最后一列显示图片
        model = new DefaultTableModel(new String[]{
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

        table = new JTable(model);
        UIUtils.styleTable(table);
        // 列宽优化
        int[] widths = {60, 220, 90, 170, 80, 80, 90, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UIUtils.CARD);

        // 顶部工具栏（卡片）
        JButton addBtn = UIUtils.primaryButton("+ 新增影票");
        JButton updateBtn = UIUtils.secondaryButton("编辑");
        JButton deleteBtn = UIUtils.dangerButton("删除");
        JButton refreshBtn = UIUtils.ghostButton("刷新");

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolBar.setOpaque(false);
        toolBar.add(addBtn);
        toolBar.add(updateBtn);
        toolBar.add(deleteBtn);
        toolBar.add(refreshBtn);

        JPanel toolCard = new JPanel(new BorderLayout());
        toolCard.setBackground(UIUtils.CARD);
        toolCard.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        toolCard.add(toolBar, BorderLayout.WEST);

        JLabel countLbl = new JLabel();
        countLbl.setForeground(UIUtils.TEXT_MUTED);
        countLbl.setFont(UIUtils.FONT_SMALL);
        toolCard.add(countLbl, BorderLayout.EAST);

        // 表格卡片
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UIUtils.CARD);
        tableCard.setBorder(new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1));
        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(toolCard, BorderLayout.NORTH);
        center.add(tableCard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // 按钮监听
        addBtn.addActionListener(e -> {
            MovieFormDialog dialog = new MovieFormDialog(null);
            dialog.setVisible(true);
            reloadData(countLbl);
        });

        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "请选择一行进行修改！");
                return;
            }
            int id = (int) model.getValueAt(row, 0);
            Movie movie = dao.getMovieById(id);
            if (movie == null) {
                JOptionPane.showMessageDialog(this, "未找到该电影信息！");
                return;
            }
            MovieFormDialog dialog = new MovieFormDialog(movie);
            dialog.setVisible(true);
            reloadData(countLbl);
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "请选择一行进行删除！");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "确认删除该影票吗？", "确认删除", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int id = (int) model.getValueAt(row, 0);
                dao.deleteMovie(id);
                reloadData(countLbl);
            }
        });

        refreshBtn.addActionListener(e -> reloadData(countLbl));

        reloadData(countLbl);
    }

    private void reloadData(JLabel countLbl) {
        model.setRowCount(0);
        List<Movie> movies = dao.getAllMovies();
        for (Movie m : movies) {
            ImageIcon icon = getImageIcon(m.getImageUrl());
            model.addRow(new Object[]{
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
        if (countLbl != null) countLbl.setText("共 " + movies.size() + " 条记录");
    }

    private ImageIcon getImageIcon(String urlStr) {
        try {
            URL url = new URL(urlStr);
            ImageIcon original = new ImageIcon(url);
            Image scaled = original.getImage().getScaledInstance(46, 60, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return new ImageIcon(new BufferedImage(46, 60, BufferedImage.TYPE_INT_ARGB));
        }
    }
}
