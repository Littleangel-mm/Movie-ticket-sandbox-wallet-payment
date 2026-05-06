package gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 内嵌 JavaFX MediaPlayer 的视频播放窗口。
 * 仅支持 H.264+AAC 编码的 mp4。
 */
public class PlayerDialog extends JDialog {

    private final JFXPanel fxPanel = new JFXPanel();
    private MediaPlayer player;
    private final String videoUrl;

    public PlayerDialog(Window owner, String title, String videoUrl) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.videoUrl = videoUrl;
        setSize(960, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.BLACK);
        add(fxPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                Platform.runLater(() -> { if (player != null) player.dispose(); });
            }
        });

        Platform.runLater(this::initFx);
    }

    private void initFx() {
        try {
            Media media = new Media(videoUrl);
            player = new MediaPlayer(media);
            MediaView view = new MediaView(player);
            view.setPreserveRatio(true);

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: black;");
            root.setCenter(view);
            view.fitWidthProperty().bind(root.widthProperty());
            view.fitHeightProperty().bind(root.heightProperty().subtract(50));

            // 控制栏
            Button playBtn = new Button("\u23F8");          // ⏸
            playBtn.setStyle("-fx-font-size: 14;");
            Slider progress = new Slider(0, 100, 0);
            HBox.setHgrow(progress, Priority.ALWAYS);
            Label time = new Label("00:00 / 00:00");
            time.setStyle("-fx-text-fill: white;");

            HBox bar = new HBox(8, playBtn, progress, time);
            bar.setStyle("-fx-background-color: #1F2937; -fx-padding: 8 12 8 12; -fx-alignment: center-left;");
            root.setBottom(bar);

            playBtn.setOnAction(e -> {
                if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                    player.pause();
                    playBtn.setText("\u25B6"); // ▶
                } else {
                    player.play();
                    playBtn.setText("\u23F8");
                }
            });

            // 进度更新
            ChangeListener<Duration> timeListener = (obs, ov, nv) -> {
                if (player.getMedia().getDuration().isUnknown()) return;
                double total = player.getMedia().getDuration().toMillis();
                if (!progress.isValueChanging()) {
                    progress.setValue(nv.toMillis() / total * 100);
                }
                time.setText(format(nv) + " / " + format(player.getMedia().getDuration()));
            };
            player.currentTimeProperty().addListener(timeListener);

            progress.valueChangingProperty().addListener((obs, was, isChanging) -> {
                if (!isChanging) {
                    double total = player.getMedia().getDuration().toMillis();
                    player.seek(Duration.millis(progress.getValue() / 100 * total));
                }
            });

            player.setOnError(() -> SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "播放器错误：" + player.getError().getMessage()
                                    + "\n\n仅支持 H.264 + AAC 编码的 mp4，其他格式无法播放。",
                            "播放失败", JOptionPane.ERROR_MESSAGE)));

            Scene scene = new Scene(root, 960, 540);
            fxPanel.setScene(scene);
            player.play();
        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this,
                            "无法初始化播放器：" + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE));
        }
    }

    private static String format(Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite()) return "--:--";
        int sec = (int) d.toSeconds();
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
