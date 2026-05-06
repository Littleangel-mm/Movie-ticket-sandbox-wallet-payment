package alipay;

/**
 * 简单连通性测试：调用 alipay.trade.precreate 看能否拿到二维码链接。
 * 运行：在 IDEA 中 Run 该类的 main 方法即可。
 */
public class AlipaySmokeTest {
    public static void main(String[] args) {
        AlipayService svc = new AlipayService();
        String outTradeNo = AlipayService.generateOutTradeNo(0);
        System.out.println("商户订单号: " + outTradeNo);

        AlipayService.PrecreateResult r = svc.precreate(outTradeNo, "电影票-沙箱测试", "0.01", "smoke test");
        System.out.println("success=" + r.success);
        System.out.println("code=" + r.code);
        System.out.println("msg=" + r.msg);
        System.out.println("subMsg=" + r.subMsg);
        System.out.println("qrCode=" + r.qrCode);
        System.out.println("--- raw ---");
        System.out.println(r.raw);

        if (r.success) {
            System.out.println("\n查询订单状态:");
            AlipayService.QueryResult q = svc.query(outTradeNo);
            System.out.println("code=" + q.code + " msg=" + q.msg + " status=" + q.tradeStatus);
        }
    }
}
