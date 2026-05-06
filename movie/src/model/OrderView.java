package model;

import java.util.Date;

/**
 * 管理员订单视图 DTO：reservation × movie × payment 联合查询结果。
 */
public class OrderView {
    public int reservationId;
    public String customerName;
    public String seatNumber;
    public Date reserveTime;

    public int movieId;
    public String movieName;
    public String hall;
    public String showTime;
    public double price;

    public Integer paymentId;        // null = 未支付
    public String payMethod;         // 支付宝 / 微信支付 / 银行卡
    public String payStatus;         // 已支付 / 已取消 / null=未支付
    public Date payTime;

    public String displayStatus() {
        if (payStatus == null) return "未支付";
        return payStatus;
    }
}
