package com.example.demo.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ListObjectsRequest;
import com.example.demo.utils.OssPathUtils;
import com.example.demo.utils.OssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

@Component
public class OssService {
    @Autowired
    private OssProperties ossProperties;

    public String uploadFile(String localFilePath, String ossPath) {
        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());

        try {
            System.out.println("准备上传OSS: bucket=" + ossProperties.getBucketName() + ", path=" + ossPath + ", file=" + localFilePath);
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在：" + localFilePath);
            }
            ObjectMetadata metadata = new ObjectMetadata();
            String contentType = getContentTypeByExtension(file.getName());
            if (contentType != null) {
                metadata.setContentType(contentType);
            }
            metadata.setContentDisposition("inline");

            PutObjectRequest request = new PutObjectRequest(ossProperties.getBucketName(), ossPath, file);
            request.setMetadata(metadata);
            PutObjectResult result = ossClient.putObject(request);
            System.out.println("OSS上传返回: " + result.getETag());

            return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + ossPath;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            ossClient.shutdown();
        }
    }

    public boolean deleteFolder(String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            System.out.println("删除文件夹失败：路径为空");
            return false;
        }

        OSS ossClient = null;
        try {
            // 检查OSS配置
            if (ossProperties == null) {
                System.out.println("删除文件夹失败：OSS配置为空");
                return false;
            }

            System.out.println("开始删除OSS文件夹: " + folderPath);
            System.out.println("OSS配置 - Bucket: " + ossProperties.getBucketName() + ", Endpoint: " + ossProperties.getEndpoint());

            ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret());

            // 确保路径以 / 结尾
            if (!folderPath.endsWith("/")) {
                folderPath = folderPath + "/";
            }

            System.out.println("处理后的文件夹路径: " + folderPath);

            // 列出文件夹下的所有对象
            ListObjectsRequest listRequest = new ListObjectsRequest(ossProperties.getBucketName());
            listRequest.setPrefix(folderPath);
            listRequest.setDelimiter("/");

            System.out.println("发送ListObjects请求...");
            com.aliyun.oss.model.ObjectListing listing = ossClient.listObjects(listRequest);
            
            if (listing == null) {
                System.out.println("ListObjects返回null");
                return false;
            }

            List<String> objectKeys = new ArrayList<>();
            List<OSSObjectSummary> objectSummaries = listing.getObjectSummaries();
            
            System.out.println("找到 " + (objectSummaries != null ? objectSummaries.size() : 0) + " 个对象");
            
            if (objectSummaries != null) {
                for (OSSObjectSummary objectSummary : objectSummaries) {
                    if (objectSummary != null && objectSummary.getKey() != null) {
                        objectKeys.add(objectSummary.getKey());
                        System.out.println("准备删除对象: " + objectSummary.getKey());
                    }
                }
            }

            // 如果有对象需要删除
            if (!objectKeys.isEmpty()) {
                System.out.println("开始批量删除 " + objectKeys.size() + " 个对象");
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(ossProperties.getBucketName());
                deleteRequest.setKeys(objectKeys);
                
                com.aliyun.oss.model.DeleteObjectsResult deleteResult = ossClient.deleteObjects(deleteRequest);
                if (deleteResult != null) {
                    System.out.println("成功删除OSS文件夹: " + folderPath + ", 删除了 " + objectKeys.size() + " 个文件");
                    System.out.println("删除结果: " + deleteResult.getDeletedObjects());
                    return true;
                } else {
                    System.out.println("删除操作返回null结果");
                    return false;
                }
            } else {
                System.out.println("OSS文件夹为空或不存在: " + folderPath);
                return false;
            }
        } catch (Exception e) {
            System.err.println("删除OSS文件夹时发生异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (ossClient != null) {
                try {
                    ossClient.shutdown();
                    System.out.println("OSS客户端已关闭");
                } catch (Exception e) {
                    System.err.println("关闭OSS客户端时发生异常: " + e.getMessage());
                }
            }
        }
    }

    private String getContentTypeByExtension(String filename) {
        if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        } else if (filename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.endsWith(".txt")) {
            return "text/plain";
        } else if (filename.endsWith(".html")) {
            return "text/html";
        } else if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".js")) {
            return "application/javascript";
        } else {
            return "application/octet-stream"; // 默认值，可能仍会触发下载
        }
    }

    public String generateUrl(String ossPath) {
        return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + ossPath;
    }
}