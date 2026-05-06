package alipay;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝开放平台 OpenAPI 1.0 调用客户端（纯 JDK 实现）。
 * <p>支持 RSA2 签名 + form-urlencoded POST。
 * <p>仅依赖 JDK，无需 alipay-sdk-java。
 */
public class AlipayClient {

    /**
     * 调用支付宝接口。
     *
     * @param method     接口名（如 alipay.trade.precreate）
     * @param bizContent 业务参数 JSON 字符串
     * @return 响应原始 JSON 文本
     */
    public static String execute(String method, String bizContent) throws IOException {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("app_id", AlipayConfig.APP_ID);
        params.put("method", method);
        params.put("format", "json");
        params.put("charset", AlipayConfig.CHARSET);
        params.put("sign_type", AlipayConfig.SIGN_TYPE);
        params.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        params.put("version", "1.0");
        if (AlipayConfig.NOTIFY_URL != null && !AlipayConfig.NOTIFY_URL.isEmpty()) {
            params.put("notify_url", AlipayConfig.NOTIFY_URL);
        }
        params.put("biz_content", bizContent);

        // 1. 排序后拼接待签名串
        String signContent = buildSignContent(params);
        // 2. RSA2 签名
        String sign = rsa2Sign(signContent, AlipayConfig.APP_PRIVATE_KEY);
        params.put("sign", sign);

        // 3. URL 编码并组装 POST body
        String body = buildQueryString(params);

        // 4. 发送 POST
        URL url = new URL(AlipayConfig.GATEWAY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + AlipayConfig.CHARSET);
        conn.setRequestProperty("Accept", "application/json");

        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /** 拼接排序后的签名串：k1=v1&k2=v2（不进行 URL 编码） */
    private static String buildSignContent(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            String v = e.getValue();
            if (v == null || v.isEmpty()) continue;
            if ("sign".equals(e.getKey())) continue;
            if (!first) sb.append('&');
            sb.append(e.getKey()).append('=').append(v);
            first = false;
        }
        return sb.toString();
    }

    /** URL 编码组装 form body（包含 sign） */
    private static String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            String v = e.getValue();
            if (v == null) continue;
            if (!first) sb.append('&');
            sb.append(urlEnc(e.getKey())).append('=').append(urlEnc(v));
            first = false;
        }
        return sb.toString();
    }

    private static String urlEnc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** SHA256withRSA 签名 + Base64 */
    public static String rsa2Sign(String content, String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey pk = kf.generatePrivate(spec);
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(pk);
            signer.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new RuntimeException("RSA2 签名失败", e);
        }
    }

    /**
     * 极简 JSON 字段提取（仅支持平铺一层字符串字段）。
     * 用于读取 alipay_xxx_response 内部的 code / msg / qr_code / out_trade_no / trade_status 等。
     */
    public static String extractField(String json, String responseKey, String field) {
        if (json == null) return null;
        // 定位 responseKey 段
        int p = json.indexOf("\"" + responseKey + "\"");
        if (p < 0) return null;
        int braceStart = json.indexOf('{', p);
        if (braceStart < 0) return null;
        int braceEnd = matchBrace(json, braceStart);
        if (braceEnd < 0) braceEnd = json.length();
        String body = json.substring(braceStart, braceEnd + 1);

        String pattern = "\"" + field + "\"";
        int fp = body.indexOf(pattern);
        if (fp < 0) return null;
        int colon = body.indexOf(':', fp + pattern.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < body.length() && Character.isWhitespace(body.charAt(i))) i++;
        if (i >= body.length()) return null;
        if (body.charAt(i) == '"') {
            int end = body.indexOf('"', i + 1);
            // 简单处理转义
            while (end > 0 && body.charAt(end - 1) == '\\') end = body.indexOf('"', end + 1);
            if (end < 0) return null;
            return body.substring(i + 1, end).replace("\\/", "/").replace("\\\"", "\"");
        } else {
            int end = i;
            while (end < body.length() && ",}\n\r ".indexOf(body.charAt(end)) < 0) end++;
            return body.substring(i, end);
        }
    }

    private static int matchBrace(String s, int start) {
        int depth = 0;
        boolean inStr = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inStr = false;
            } else {
                if (c == '"') inStr = true;
                else if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) return i; }
            }
        }
        return -1;
    }
}
