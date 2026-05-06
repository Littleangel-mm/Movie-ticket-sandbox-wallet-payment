package gui;

import dao.MovieDAO;
import model.Movie;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MovieFormDialog extends JDialog {

    private JTextField tfName;
    private JTextField tfHall;
    private JTextField tfShowTime;
    private JTextField tfPrice;
    private JTextField tfTotalSeats;
    private JTextField tfAvailableSeats;
    private JTextField tfImageUrl;

    private JButton btnConfirm;
    private JButton btnCancel;

    private Movie movie;  // 当前编辑的电影，null表示新增

    private MovieDAO dao = new MovieDAO();

    public MovieFormDialog(Movie movie) {
        this.movie = movie;

        setTitle(movie == null ? "新增影票" : "编辑影票");
        setModal(true);
        setSize(540, 660);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG);

        add(UIUtils.header(movie == null ? "新增影票" : "编辑影票",
                movie == null ? "填写以下信息创建一条新的影票记录" : "修改影票信息后点击保存"), BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIUtils.CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        tfName = UIUtils.textField();
        tfHall = UIUtils.textField();
        tfShowTime = UIUtils.textField();
        tfPrice = UIUtils.textField();
        tfTotalSeats = UIUtils.textField();
        tfAvailableSeats = UIUtils.textField();
        tfImageUrl = UIUtils.textField();

        addRow(panel, g, 0, "电影名称", tfName);
        addRow(panel, g, 1, "影厅",     tfHall);
        addRow(panel, g, 2, "放映时间", tfShowTime);
        addRow(panel, g, 3, "票价（元）", tfPrice);
        addRow(panel, g, 4, "总座位数", tfTotalSeats);
        addRow(panel, g, 5, "剩余座位", tfAvailableSeats);
        addRow(panel, g, 6, "图片 URL", tfImageUrl);

        // 提示行
        g.gridy = 7; g.gridx = 1; g.weightx = 1;
        g.insets = new Insets(0, 6, 0, 6);
        JLabel hint = new JLabel("放映时间格式：yyyy-MM-dd HH:mm:ss，示例：2025-07-01 19:00:00");
        hint.setFont(UIUtils.FONT_SMALL);
        hint.setForeground(UIUtils.TEXT_MUTED);
        panel.add(hint, g);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UIUtils.BG);
        center.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        center.add(panel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        btnConfirm = UIUtils.primaryButton(movie == null ? "添加" : "保存");
        btnCancel = UIUtils.secondaryButton("取消");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(UIUtils.BG);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);
        add(btnPanel, BorderLayout.SOUTH);

        // 如果是编辑，填充数据
        if (movie != null) {
            fillForm();
        }

        // 确认按钮事件
        btnConfirm.addActionListener(e -> onConfirm());

        // 取消按钮事件
        btnCancel.addActionListener(e -> dispose());

        // 关闭窗口释放资源
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void addRow(JPanel form, GridBagConstraints g, int row, String label, JComponent input) {
        g.gridy = row; g.gridx = 0; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(UIUtils.TEXT_BODY);
        l.setFont(UIUtils.FONT_BOLD);
        l.setPreferredSize(new Dimension(110, 36));
        form.add(l, g);
        g.gridx = 1; g.weightx = 1;
        form.add(input, g);
    }

    private void fillForm() {
        tfName.setText(movie.getName());
        tfHall.setText(movie.getHall());
        tfShowTime.setText(movie.getShowTime());
        tfPrice.setText(String.valueOf(movie.getPrice()));
        tfTotalSeats.setText(String.valueOf(movie.getTotalSeats()));
        tfAvailableSeats.setText(String.valueOf(movie.getAvailableSeats()));
        tfImageUrl.setText(movie.getImageUrl());
    }

    private void onConfirm() {
        String name = tfName.getText().trim();
        String hall = tfHall.getText().trim();
        String showTime = tfShowTime.getText().trim();
        String priceStr = tfPrice.getText().trim();
        String totalSeatsStr = tfTotalSeats.getText().trim();
        String availableSeatsStr = tfAvailableSeats.getText().trim();
        String imageUrl = tfImageUrl.getText().trim();

        // 简单验证
        if (name.isEmpty() || hall.isEmpty() || showTime.isEmpty() || priceStr.isEmpty()
                || totalSeatsStr.isEmpty() || availableSeatsStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写所有必填项！");
            return;
        }

        double price;
        int totalSeats, availableSeats;
        try {
            price = Double.parseDouble(priceStr);
            totalSeats = Integer.parseInt(totalSeatsStr);
            availableSeats = Integer.parseInt(availableSeatsStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "票价和座位数请输入正确的数字格式！");
            return;
        }

        if (movie == null) {
            // 新增
            Movie newMovie = new Movie();
            newMovie.setName(name);
            newMovie.setHall(hall);
            newMovie.setShowTime(showTime);
            newMovie.setPrice(price);
            newMovie.setTotalSeats(totalSeats);
            newMovie.setAvailableSeats(availableSeats);
            newMovie.setImageUrl(imageUrl);

            boolean success = dao.addMovie(newMovie);
            if (success) {
                JOptionPane.showMessageDialog(this, "添加成功！");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "添加失败！");
            }
        } else {
            // 编辑
            movie.setName(name);
            movie.setHall(hall);
            movie.setShowTime(showTime);
            movie.setPrice(price);
            movie.setTotalSeats(totalSeats);
            movie.setAvailableSeats(availableSeats);
            movie.setImageUrl(imageUrl);

            boolean success = dao.updateMovie(movie);
            if (success) {
                JOptionPane.showMessageDialog(this, "保存成功！");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "保存失败！");
            }
        }
    }
}
