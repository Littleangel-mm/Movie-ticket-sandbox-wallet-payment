package alipay;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 支付宝高层服务：扫码下单 + 订单查询。
 */
public class AlipayService {

    /** 扫码下单结果 */
    public static class PrecreateResult {
        public final boolean success;
        public final String code;
        public final String msg;
        public final String subMsg;
        public final String outTradeNo;
        public final String qrCode;
        public final String raw;

        public PrecreateResult(boolean success, String code, String msg, String subMsg,
                               String outTradeNo, String qrCode, String raw) {
            this.success = success;
            this.code = code;
            this.msg = msg;
            this.subMsg = subMsg;
            this.outTradeNo = outTradeNo;
            this.qrCode = qrCode;
            this.raw = raw;
        }
    }

    /** 订单查询结果 */
    public static class QueryResult {
        public final boolean success;
        public final String code;
        public final String msg;
        public final String tradeStatus;   // WAIT_BUYER_PAY / TRADE_SUCCESS / TRADE_CLOSED / TRADE_FINISHED
        public final String tradeNo;
        public final String outTradeNo;
        public final String raw;

        public QueryResult(boolean success, String code, String msg, String tradeStatus,
                           String tradeNo, String outTradeNo, String raw) {
            this.success = success;
            this.code = code;
            this.msg = msg;
            this.tradeStatus = tradeStatus;
            this.tradeNo = tradeNo;
            this.outTradeNo = outTradeNo;
            this.raw = raw;
        }

        public boolean paid() {
            return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
        }
    }

    /** 生成商户订单号：MOVIE{reservationId}{yyyyMMddHHmmss} */
    public static String generateOutTradeNo(int reservationId) {
        return "MOVIE" + reservationId + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    /**
     * 调用 alipay.trade.precreate 创建扫码订单。
     *
     * @param outTradeNo  商户订单号
     * @param subject     商品标题
     * @param totalAmount 金额（元，字符串如 "49.90"）
     * @param body        附加描述
     */
    public PrecreateResult precreate(String outTradeNo, String subject, String totalAmount, String body) {
        String biz = "{"
                + jsonField("out_trade_no", outTradeNo) + ","
                + jsonField("total_amount", totalAmount) + ","
                + jsonField("subject", subject) + ","
                + (body != null ? (jsonField("body", body) + ",") : "")
                + jsonField("timeout_express", AlipayConfig.TIMEOUT_EXPRESS)
                + "}";

        try {
            String resp = AlipayClient.execute("alipay.trade.precreate", biz);
            String code = AlipayClient.extractField(resp, "alipay_trade_precreate_response", "code");
            String msg  = AlipayClient.extractField(resp, "alipay_trade_precreate_response", "msg");
            String sub  = AlipayClient.extractField(resp, "alipay_trade_precreate_response", "sub_msg");
            String oid  = AlipayClient.extractField(resp, "alipay_trade_precreate_response", "out_trade_no");
            String qr   = AlipayClient.extractField(resp, "alipay_trade_precreate_response", "qr_code");
            boolean ok = "10000".equals(code) && qr != null && !qr.isEmpty();
            return new PrecreateResult(ok, code, msg, sub, oid, qr, resp);
        } catch (IOException e) {
            return new PrecreateResult(false, "NETWORK_ERROR", e.getMessage(), null, null, null, null);
        }
    }

    /** 查询订单状态：alipay.trade.query */
    public QueryResult query(String outTradeNo) {
        String biz = "{" + jsonField("out_trade_no", outTradeNo) + "}";
        try {
            String resp = AlipayClient.execute("alipay.trade.query", biz);
            String code   = AlipayClient.extractField(resp, "alipay_trade_query_response", "code");
            String msg    = AlipayClient.extractField(resp, "alipay_trade_query_response", "msg");
            String status = AlipayClient.extractField(resp, "alipay_trade_query_response", "trade_status");
            String tn     = AlipayClient.extractField(resp, "alipay_trade_query_response", "trade_no");
            String oid    = AlipayClient.extractField(resp, "alipay_trade_query_response", "out_trade_no");
            boolean ok = "10000".equals(code);
            return new QueryResult(ok, code, msg, status, tn, oid, resp);
        } catch (IOException e) {
            return new QueryResult(false, "NETWORK_ERROR", e.getMessage(), null, null, null, null);
        }
    }

    private static String jsonField(String k, String v) {
        return "\"" + k + "\":\"" + escape(v) + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
