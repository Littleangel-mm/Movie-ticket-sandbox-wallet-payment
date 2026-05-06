package model;

import java.util.Date;

public class Payment {
    private int id;
    private int reservationId;
    private String method;
    private String status;
    private Date payTime;
    private Date activatedAt;
    private Date expiresAt;
    private String watchStatus = "未使用";

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getPayTime() { return payTime; }
    public void setPayTime(Date payTime) { this.payTime = payTime; }

    public Date getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Date v) { this.activatedAt = v; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date v) { this.expiresAt = v; }

    public String getWatchStatus() { return watchStatus; }
    public void setWatchStatus(String v) { this.watchStatus = v; }

    /** 是否已激活（首次播放过）*/
    public boolean isActivated() { return activatedAt != null; }

    /** 当前是否在 5 小时观看窗口内 */
    public boolean isWatchable() {
        if (!"已支付".equals(status)) return false;
        if (expiresAt == null) return true; // 未激活也算可观看（点击后才激活）
        return new Date().before(expiresAt);
    }

    /** 距离过期还剩多少毫秒，未激活返回 -1 */
    public long remainingMillis() {
        if (expiresAt == null) return -1;
        return expiresAt.getTime() - System.currentTimeMillis();
    }
} 