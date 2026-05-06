package utils;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 客户端门面。配置读自 conf/app.properties：
 * <ul>
 *   <li>minio.endpoint</li>
 *   <li>minio.accessKey / minio.secretKey</li>
 *   <li>minio.bucket</li>
 *   <li>minio.presignSeconds（默认 18000 = 5 小时）</li>
 * </ul>
 */
public final class MinioService {

    private static volatile MinioClient client;

    private MinioService() {}

    public static MinioClient client() {
        if (client == null) {
            synchronized (MinioService.class) {
                if (client == null) {
                    client = MinioClient.builder()
                            .endpoint(AppConfig.require("minio.endpoint"))
                            .credentials(AppConfig.require("minio.accessKey"),
                                         AppConfig.require("minio.secretKey"))
                            .build();
                }
            }
        }
        return client;
    }

    public static String bucket() { return AppConfig.require("minio.bucket"); }

    public static int presignSeconds() {
        return AppConfig.getInt("minio.presignSeconds", 18000);
    }

    /**
     * 上传文件到 MinIO，返回 objectKey。
     *
     * @param prefix          对象前缀，如 "movies/12"
     * @param localFile       本地文件
     * @param contentType     MIME 类型，如 "video/mp4"
     */
    public static String upload(String prefix, File localFile, String contentType) throws Exception {
        String suffix = extOf(localFile.getName());
        String objectKey = prefix.replaceAll("/+$", "") + "/" + UUID.randomUUID() + suffix;
        client().uploadObject(UploadObjectArgs.builder()
                .bucket(bucket())
                .object(objectKey)
                .filename(localFile.getAbsolutePath())
                .contentType(contentType == null ? "application/octet-stream" : contentType)
                .build());
        return objectKey;
    }

    /** 生成可临时访问的 GET 预签名 URL（默认 5 小时有效）。 */
    public static String presignedGetUrl(String objectKey) throws Exception {
        return presignedGetUrl(objectKey, presignSeconds());
    }

    public static String presignedGetUrl(String objectKey, int expirySeconds) throws Exception {
        return client().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket())
                .object(objectKey)
                .expiry(expirySeconds, TimeUnit.SECONDS)
                .build());
    }

    /** 取扩展名（含点），无扩展名返回空串。 */
    private static String extOf(String filename) {
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i);
    }

    /** 简单的连接自检：尝试预签名一个虚拟 key，验证 endpoint/凭据/桶名是否能跑通。 */
    public static void main(String[] args) throws Exception {
        System.out.println("endpoint = " + AppConfig.get("minio.endpoint"));
        System.out.println("bucket   = " + bucket());
        String url = presignedGetUrl("__connectivity_test__.txt", 60);
        System.out.println("presigned URL = " + url);
        System.out.println("OK，凭据/endpoint 可达。");
    }
}
