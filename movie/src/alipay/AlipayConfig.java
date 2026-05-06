package alipay;

import utils.AppConfig;

/**
 * 支付宝沙箱配置。所有字段读自外部配置文件 conf/app.properties。
 * <p>不再硬编码密钥；模板见 conf/app.properties.example。
 */
public final class AlipayConfig {

    public static final String GATEWAY          = AppConfig.require("alipay.gateway");
    public static final String APP_ID           = AppConfig.require("alipay.appId");
    public static final String SELLER_ID        = AppConfig.require("alipay.sellerId");
    public static final String APP_PRIVATE_KEY  = AppConfig.require("alipay.appPrivateKey");
    public static final String ALIPAY_PUBLIC_KEY= AppConfig.require("alipay.alipayPublicKey");

    public static final String CHARSET          = AppConfig.get("alipay.charset", "UTF-8");
    public static final String SIGN_TYPE        = AppConfig.get("alipay.signType", "RSA2");
    public static final String NOTIFY_URL       = AppConfig.get("alipay.notifyUrl", "");
    public static final String TIMEOUT_EXPRESS  = AppConfig.get("alipay.timeoutExpress", "15m");

    private AlipayConfig() {}
}
