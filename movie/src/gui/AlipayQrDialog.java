package gui;

import alipay.AlipayService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import dao.PaymentDAO;
import model.Payment;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝沙箱扫码支付对话框：显示二维码 + 轮询订单状态。
 */
public class AlipayQrDialog extends JDialog {

    private final String outTradeNo;
    private final String subject;
    private final String totalAmount;
    private final int reservationId;
    private final MyReservationPanel parentPanel;

    private final AlipayService service = new AlipayService();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private JLabel qrLabel;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private Timer pollTimer;
    private Timer countdownTimer;
    private int remainingSec = 180; // 默认轮询 180 秒
    private boolean closed = false;
    private boolean paidLocally = false;
    private String qrUrl;

    public AlipayQrDialog(JFrame owner, int reservationId, String outTradeNo,
                          String subject, String totalAmount, MyReservationPanel parentPanel) {
        super(owner, "支付宝沙箱支付", true);
        this.reservationId = reservationId;
        this.outTradeNo = outTradeNo;
        this.subject = subject;
        this.totalAmount = totalAmount;
        this.parentPanel = parentPanel;

        setSize(440, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIUtils.BG);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        add(UIUtils.header("支付宝扫码支付", "使用沙箱版支付宝扫描下方二维码"), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { stopAll(); }
            @Override public void windowClosing(java.awt.event.WindowEvent e) { stopAll(); }
        });
    }

    private JComponent buildBody() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UIUtils.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedLineBorder(UIUtils.BORDER, 12, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JLabel title = new JLabel(subject);
        title.setFont(UIUtils.FONT_H2);
        title.setForeground(UIUtils.TEXT_TITLE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel amount = new JLabel("￥" + totalAmount);
        amount.setFont(new Font(UIUtils.FONT_TITLE.getFamily(), Font.BOLD, 28));
        amount.setForeground(UIUtils.PRIMARY);
        amount.setAlignmentX(Component.CENTER_ALIGNMENT);
        amount.setBorder(BorderFactory.createEmptyBorder(4, 0, 12, 0));

        qrLabel = new JLabel(loadingPlaceholder());
        qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        qrLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel orderLbl = new JLabel("订单号：" + outTradeNo);
        orderLbl.setFont(UIUtils.FONT_SMALL);
        orderLbl.setForeground(UIUtils.TEXT_MUTED);
        orderLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        statusLabel = new JLabel("等待支付");
        statusLabel.setFont(UIUtils.FONT_BOLD);
        statusLabel.setForeground(UIUtils.WARNING);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));

        timerLabel = new JLabel("剩余 " + remainingSec + " 秒");
        timerLabel.setFont(UIUtils.FONT_SMALL);
        timerLabel.setForeground(UIUtils.TEXT_MUTED);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(amount);
        card.add(qrLabel);
        card.add(orderLbl);
        card.add(statusLabel);
        card.add(timerLabel);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UIUtils.BG);
        wrap.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildFooter() {
        JButton btnRefresh = UIUtils.secondaryButton("刷新状态");
        JButton btnCopy = UIUtils.ghostButton("复制二维码链接");
        JButton btnClose = UIUtils.secondaryButton("关闭");

        btnRefresh.addActionListener(e -> pollOnce());
        btnCopy.addActionListener(e -> {
            if (qrUrl != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(qrUrl), null);
                JOptionPane.showMessageDialog(this, "二维码链接已复制");
            }
        });
        btnClose.addActionListener(e -> dispose());

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setBackground(UIUtils.BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));
        p.add(btnCopy);
        p.add(btnRefresh);
        p.add(btnClose);
        return p;
    }

    /** 启动：先调用 precreate 拿到 qr_code，再启动轮询 */
    public void start() {
        // 异步下单
        SwingWorker<AlipayService.PrecreateResult, Void> worker = new SwingWorker<AlipayService.PrecreateResult, Void>() {
            @Override
            protected AlipayService.PrecreateResult doInBackground() {
                return service.precreate(outTradeNo, subject, totalAmount, "电影票预约 #" + reservationId);
            }
            @Override
            protected void done() {
                try {
                    AlipayService.PrecreateResult r = get();
                    if (!r.success) {
                        statusLabel.setText("下单失败：" + safe(r.subMsg, r.msg));
                        statusLabel.setForeground(UIUtils.DANGER);
                        qrLabel.setIcon(null);
                        qrLabel.setText("<html><div style='color:#9CA3AF'>无法生成二维码</div></html>");
                        return;
                    }
                    qrUrl = r.qrCode;
                    qrLabel.setIcon(generateQr(r.qrCode, 240));
                    qrLabel.setText(null);
                    startPolling();
                    startCountdown();
                } catch (Exception ex) {
                    statusLabel.setText("下单异常：" + ex.getMessage());
                    statusLabel.setForeground(UIUtils.DANGER);
                }
            }
        };
        worker.execute();
    }

    private void startPolling() {
        // 每 3 秒查一次
        pollTimer = new Timer(3000, e -> pollOnce());
        pollTimer.setInitialDelay(3000);
        pollTimer.start();
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            remainingSec--;
            timerLabel.setText("剩余 " + remainingSec + " 秒");
            if (remainingSec <= 0) {
                stopAll();
                if (!paidLocally) {
                    statusLabel.setText("支付超时，请关闭后重试");
                    statusLabel.setForeground(UIUtils.DANGER);
                }
            }
        });
        countdownTimer.start();
    }

    private void pollOnce() {
        if (closed || paidLocally) return;
        SwingWorker<AlipayService.QueryResult, Void> w = new SwingWorker<AlipayService.QueryResult, Void>() {
            @Override protected AlipayService.QueryResult doInBackground() {
                return service.query(outTradeNo);
            }
            @Override protected void done() {
                try {
                    AlipayService.QueryResult r = get();
                    if (r.success && r.paid()) {
                        markPaid();
                    } else if ("WAIT_BUYER_PAY".equals(r.tradeStatus)) {
                        statusLabel.setText("等待买家支付");
                        statusLabel.setForeground(UIUtils.WARNING);
                    } else if ("TRADE_CLOSED".equals(r.tradeStatus)) {
                        statusLabel.setText("订单已关闭");
                        statusLabel.setForeground(UIUtils.DANGER);
                        stopAll();
                    } else if (!r.success) {
                        // 通常初始下单后查询会返回 ACQ.TRADE_NOT_EXIST，属正常
                        if (!"ACQ.TRADE_NOT_EXIST".equalsIgnoreCase(r.code)) {
                            statusLabel.setText("查询：" + safe(r.msg, r.code));
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    private void markPaid() {
        paidLocally = true;
        statusLabel.setText("支付成功");
        statusLabel.setForeground(UIUtils.SUCCESS);
        stopAll();

        // 写入支付记录
        Payment p = new Payment();
        p.setReservationId(reservationId);
        p.setMethod("支付宝");
        p.setStatus("已支付");
        p.setPayTime(new Date());
        boolean ok = paymentDAO.addPayment(p);
        if (ok && parentPanel != null) {
            parentPanel.reloadData();
            if (parentPanel.parentFrame != null) parentPanel.parentFrame.loadMovies();
        }

        // 1.5 秒后自动关闭
        Timer close = new Timer(1500, e -> dispose());
        close.setRepeats(false);
        close.start();
    }

    private void stopAll() {
        closed = true;
        if (pollTimer != null) pollTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
    }

    private static String safe(String a, String b) {
        return (a != null && !a.isEmpty()) ? a : (b == null ? "" : b);
    }

    /** 占位图（生成中） */
    private Icon loadingPlaceholder() {
        BufferedImage img = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xF3F4F6));
        g.fillRoundRect(0, 0, 240, 240, 12, 12);
        g.setColor(UIUtils.TEXT_MUTED);
        g.setFont(UIUtils.FONT_BODY);
        String t = "二维码生成中";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(t, (240 - fm.stringWidth(t)) / 2, 120 + fm.getAscent() / 2);
        g.dispose();
        return new ImageIcon(img);
    }

    /** 用 zxing 生成二维码 */
    public static ImageIcon generateQr(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    img.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            return new ImageIcon(img);
        } catch (Exception e) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            return new ImageIcon(img);
        }
    }
}
