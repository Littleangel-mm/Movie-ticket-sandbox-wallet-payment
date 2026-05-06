package utils;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 统一的 UI 主题与组件工厂。
 * 通过在程序入口调用 {@link #applyTheme()} 安装 Nimbus 外观、全局字体与配色。
 */
public final class UIUtils {

    /* ==================== 配色 ==================== */
    public static final Color PRIMARY        = new Color(0xE50914); // 电影红
    public static final Color PRIMARY_HOVER  = new Color(0xB0060F);
    public static final Color PRIMARY_PRESS  = new Color(0x8A040B);

    public static final Color ACCENT         = new Color(0x221F1F);
    public static final Color BG             = new Color(0xF5F6FA);
    public static final Color CARD           = Color.WHITE;
    public static final Color BORDER         = new Color(0xE5E7EB);
    public static final Color DIVIDER        = new Color(0xEEF0F4);

    public static final Color TEXT_TITLE     = new Color(0x1F2937);
    public static final Color TEXT_BODY      = new Color(0x374151);
    public static final Color TEXT_MUTED     = new Color(0x6B7280);

    public static final Color SUCCESS        = new Color(0x10B981);
    public static final Color WARNING        = new Color(0xF59E0B);
    public static final Color DANGER         = new Color(0xEF4444);
    public static final Color INFO           = new Color(0x3B82F6);

    /* ==================== 字体 ==================== */
    public static final Font FONT_BASE  = pickFont(13, Font.PLAIN);
    public static final Font FONT_BODY  = pickFont(14, Font.PLAIN);
    public static final Font FONT_BOLD  = pickFont(14, Font.BOLD);
    public static final Font FONT_TITLE = pickFont(20, Font.BOLD);
    public static final Font FONT_H2    = pickFont(16, Font.BOLD);
    public static final Font FONT_SMALL = pickFont(12, Font.PLAIN);

    private UIUtils() {}

    private static Font pickFont(int size, int style) {
        String[] candidates = {"Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC", "Source Han Sans CN", "SansSerif"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String name : candidates) {
            for (String a : available) {
                if (a.equalsIgnoreCase(name)) return new Font(name, style, size);
            }
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    /* ==================== 主题应用 ==================== */
    public static void applyTheme() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception ignored) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored2) {}
        }

        UIManager.put("control",                BG);
        UIManager.put("info",                   CARD);
        UIManager.put("nimbusBase",             ACCENT);
        UIManager.put("nimbusBlueGrey",         new Color(0xC9CDD4));
        UIManager.put("nimbusLightBackground",  CARD);
        UIManager.put("nimbusFocus",            PRIMARY);
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("nimbusSelection",        PRIMARY);
        UIManager.put("nimbusOrange",           PRIMARY);
        UIManager.put("text",                   TEXT_BODY);

        // 设置全局默认字体
        setUIFont(FONT_BODY);

        UIManager.put("Table.background",          CARD);
        UIManager.put("Table.alternateRowColor",   new Color(0xFAFBFC));
        UIManager.put("Table.gridColor",           DIVIDER);
        UIManager.put("Table.selectionBackground", new Color(0xFFE5E7));
        UIManager.put("Table.selectionForeground", TEXT_TITLE);
        UIManager.put("Table.foreground",          TEXT_BODY);
        UIManager.put("Table.font",                FONT_BODY);
        UIManager.put("TableHeader.background",    new Color(0xFAFAFA));
        UIManager.put("TableHeader.foreground",    TEXT_TITLE);
        UIManager.put("TableHeader.font",          FONT_BOLD);

        UIManager.put("Label.foreground",         TEXT_BODY);
        UIManager.put("OptionPane.background",    CARD);
        UIManager.put("Panel.background",         BG);
        UIManager.put("ScrollPane.background",    CARD);
        UIManager.put("Viewport.background",      CARD);

        UIManager.put("TextField.font",           FONT_BODY);
        UIManager.put("ComboBox.font",            FONT_BODY);
        UIManager.put("Button.font",              FONT_BOLD);
    }

    private static void setUIFont(Font f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource(f));
            }
        }
    }

    /* ==================== 组件工厂 ==================== */

    /** 主操作按钮（红底白字，圆角） */
    public static JButton primaryButton(String text) {
        return new RoundButton(text, PRIMARY, PRIMARY_HOVER, PRIMARY_PRESS, Color.WHITE);
    }

    /** 次级按钮（白底深色字，描边） */
    public static JButton secondaryButton(String text) {
        RoundButton b = new RoundButton(text, CARD, new Color(0xF3F4F6), new Color(0xE5E7EB), TEXT_TITLE);
        b.setBorderColor(BORDER);
        return b;
    }

    /** 危险按钮（红字描边） */
    public static JButton dangerButton(String text) {
        RoundButton b = new RoundButton(text, CARD, new Color(0xFEF2F2), new Color(0xFEE2E2), DANGER);
        b.setBorderColor(new Color(0xFCA5A5));
        return b;
    }

    /** 文本/链接按钮（无背景） */
    public static JButton ghostButton(String text) {
        RoundButton b = new RoundButton(text, BG, new Color(0xEDEEF2), new Color(0xE2E4EA), TEXT_BODY);
        return b;
    }

    /** 顶部 Header 区域：大标题 + 副标题 */
    public static JPanel header(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(18, 24, 18, 24)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(TEXT_TITLE);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(lblTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            left.add(Box.createVerticalStrut(4));
            JLabel lblSub = new JLabel(subtitle);
            lblSub.setFont(FONT_BASE);
            lblSub.setForeground(TEXT_MUTED);
            lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
            left.add(lblSub);
        }

        p.add(left, BorderLayout.WEST);
        return p;
    }

    /** 卡片容器（白底圆角阴影边） */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER, 12, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return p;
    }

    /** 包装一个组件为卡片 */
    public static JPanel wrapCard(JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(new RoundedLineBorder(BORDER, 12, 1));
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    /** 应用统一的表格样式 */
    public static void styleTable(JTable table) {
        table.setRowHeight(64);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(0xFFE5E7));
        table.setSelectionForeground(TEXT_TITLE);
        table.setBackground(CARD);
        table.setForeground(TEXT_BODY);
        table.setFont(FONT_BODY);
        table.setGridColor(DIVIDER);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setBackground(new Color(0xFAFAFA));
        header.setForeground(TEXT_TITLE);
        header.setFont(FONT_BOLD);
        header.setReorderingAllowed(false);

        // 默认渲染器：左侧留白 + 居中
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!sel) {
                    lbl.setBackground(r % 2 == 0 ? CARD : new Color(0xFAFBFC));
                }
                lbl.setForeground(TEXT_BODY);
                return lbl;
            }
        };
        cell.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < table.getColumnCount(); i++) {
            // 不覆盖图片列（ImageIcon 列由表格默认渲染器自行处理）
            if (table.getColumnClass(i) != ImageIcon.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(cell);
            }
        }
    }

    /** 状态徽章渲染器：根据文本上色 */
    public static TableCellRenderer statusBadgeRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                String text = v == null ? "" : v.toString();
                JLabel lbl = new JLabel(text, SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setFont(FONT_BOLD);
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

                Color fg, bg;
                switch (text) {
                    case "已支付": fg = SUCCESS; bg = new Color(0xE7FBF3); break;
                    case "未支付": fg = WARNING; bg = new Color(0xFFF7E6); break;
                    case "已取消": fg = TEXT_MUTED; bg = new Color(0xF1F2F4); break;
                    default:        fg = INFO; bg = new Color(0xEAF2FE); break;
                }
                lbl.setForeground(fg);
                lbl.setBackground(bg);

                JPanel wrap = new JPanel(new GridBagLayout());
                wrap.setBackground(sel ? new Color(0xFFE5E7) : (r % 2 == 0 ? CARD : new Color(0xFAFBFC)));
                wrap.add(lbl);
                return wrap;
            }
        };
    }

    /** 圆角描边 + 内边距的输入框 */
    public static JTextField textField() {
        JTextField tf = new JTextField();
        tf.setFont(FONT_BODY);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER, 8, 1),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        return tf;
    }

    /** 圆角描边 ComboBox */
    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<T>(items) {
            @Override public void updateUI() {
                super.updateUI();
                setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                    @Override protected JButton createArrowButton() {
                        JButton b = new JButton() {
                            @Override protected void paintComponent(Graphics g) {
                                Graphics2D g2 = (Graphics2D) g.create();
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                int w = getWidth(), h = getHeight();
                                int cx = w / 2, cy = h / 2;
                                int s = 4; // 三角半宽
                                int[] xs = {cx - s, cx + s, cx};
                                int[] ys = {cy - 2, cy - 2, cy + 3};
                                g2.setColor(TEXT_MUTED);
                                g2.fillPolygon(xs, ys, 3);
                                g2.dispose();
                            }
                        };
                        b.setPreferredSize(new Dimension(24, 24));
                        b.setBorder(BorderFactory.createEmptyBorder());
                        b.setContentAreaFilled(false);
                        b.setFocusPainted(false);
                        b.setBorderPainted(false);
                        b.setOpaque(false);
                        return b;
                    }
                });
            }
        };
        cb.setFont(FONT_BODY);
        cb.setBackground(CARD);
        cb.setOpaque(true);
        cb.setFocusable(false);
        cb.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER, 8, 1),
                BorderFactory.createEmptyBorder(2, 10, 2, 4)));
        cb.setPreferredSize(new Dimension(Math.max(110, cb.getPreferredSize().width), 40));
        return cb;
    }

    /**
     * 简洁分段 Tab 控件：取代 Nimbus 默认丑陋的 JTabbedPane。
     * 用法：
     * <pre>
     *   TabBar bar = new TabBar();
     *   bar.addTab("影票管理", moviePanel);
     *   bar.addTab("订单管理", orderPanel);
     *   container.add(bar.getHeader(), BorderLayout.NORTH);
     *   container.add(bar.getContent(), BorderLayout.CENTER);
     * </pre>
     */
    public static class TabBar {
        private final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        private final JPanel content = new JPanel(new CardLayout());
        private final java.util.List<JButton> buttons = new java.util.ArrayList<>();
        private int activeIndex = -1;

        public TabBar() {
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            content.setBackground(BG);
        }

        public void addTab(String title, JComponent panel) {
            final int idx = buttons.size();
            final String key = "tab_" + idx;
            content.add(panel, key);

            JButton b = new JButton(title);
            b.setFont(FONT_BOLD);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setOpaque(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            b.addActionListener(e -> select(idx));
            // 自定义绘制：选中 → 红色下划线；未选中 → 灰色文字
            b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
                @Override public void paint(Graphics g, JComponent c) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean active = (idx == activeIndex);
                    b.setForeground(active ? TEXT_TITLE : TEXT_MUTED);
                    super.paint(g2, c);
                    if (active) {
                        g2.setColor(PRIMARY);
                        g2.fillRoundRect(20, c.getHeight() - 3, c.getWidth() - 40, 3, 2, 2);
                    }
                    g2.dispose();
                }
            });
            buttons.add(b);
            header.add(b);
            if (activeIndex < 0) select(0);
        }

        public void select(int i) {
            if (i < 0 || i >= buttons.size()) return;
            activeIndex = i;
            ((CardLayout) content.getLayout()).show(content, "tab_" + i);
            for (JButton b : buttons) b.repaint();
        }

        public JComponent getHeader()  { return header; }
        public JComponent getContent() { return content; }
    }

    /* ==================== 内部组件 ==================== */

    /** 圆角线边框 */
    public static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final int thickness;

        public RoundedLineBorder(Color color, int radius, int thickness) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2, w - thickness, h - thickness, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(6, 8, 6, 8); }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(6, 8, 6, 8);
            return insets;
        }
    }

    /** 圆角按钮，带 hover/press 状态 */
    public static class RoundButton extends JButton {
        private final Color bg;
        private final Color hover;
        private final Color press;
        private Color borderColor = null;
        private boolean mouseOver = false;
        private boolean mouseDown = false;

        public RoundButton(String text, Color bg, Color hover, Color press, Color fg) {
            super(text);
            this.bg = bg;
            this.hover = hover;
            this.press = press;
            setForeground(fg);
            setFont(FONT_BOLD);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { mouseOver = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { mouseOver = false; mouseDown = false; repaint(); }
                @Override public void mousePressed(MouseEvent e)  { mouseDown = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { mouseDown = false; repaint(); }
            });
        }

        public void setBorderColor(Color c) { this.borderColor = c; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = bg;
            if (!isEnabled()) fill = new Color(0xE5E7EB);
            else if (mouseDown) fill = press;
            else if (mouseOver) fill = hover;

            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 88), Math.max(d.height, 40));
        }
    }
}
