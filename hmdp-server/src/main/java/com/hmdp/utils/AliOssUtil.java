package com.hmdp.utils;


import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.hmdp.properties.AliOssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.InputStream;

@Component
public class AliOssUtil {

	@Resource
	private AliOssProperties aliOssProperties;


	private String ENDPOINT;
	private String BUCKET_NAME;
	private String REGION;
	private String ACCESS_KEY_ID;
	private String ACCESS_KEY_SECRET;

	public AliOssUtil(AliOssProperties aliOssProperties) {
		this.ENDPOINT = aliOssProperties.getEndpoint();
		this.BUCKET_NAME = aliOssProperties.getBucketName();
		this.REGION = aliOssProperties.getRegion();
		this.ACCESS_KEY_ID = aliOssProperties.getAccessKeyId();
		this.ACCESS_KEY_SECRET = aliOssProperties.getAccessKeySecret();
	}
	public String upload(String objectName, InputStream in) throws Exception {

		String url="";

		// 创建OSSClient实例。
		ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
		clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

		OSS ossClient = OSSClientBuilder.create()
				.endpoint(ENDPOINT)
				.credentialsProvider(new DefaultCredentialProvider(ACCESS_KEY_ID, ACCESS_KEY_SECRET))
				.clientConfiguration(clientBuilderConfiguration)
				.region(REGION)
				.build();
		try {
			// 创建PutObjectRequest对象。
			PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET_NAME, objectName, in);

			// 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
			// ObjectMetadata metadata = new ObjectMetadata();
			// metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
			// metadata.setObjectAcl(CannedAccessControlList.Private);
			// putObjectRequest.setMetadata(metadata);

			// 上传字符串。
			PutObjectResult result = ossClient.putObject(putObjectRequest);
			url="https://"+BUCKET_NAME+"."+ENDPOINT.substring(ENDPOINT.lastIndexOf("/")+1)+"/"+objectName;
		} catch (OSSException oe) {
			System.out.println("Caught an OSSException, which means your request made it to OSS, "
					+ "but was rejected with an error response for some reason.");
			System.out.println("Error Message:" + oe.getErrorMessage());
			System.out.println("Error Code:" + oe.getErrorCode());
			System.out.println("Request ID:" + oe.getRequestId());
			System.out.println("Host ID:" + oe.getHostId());
		} catch (ClientException ce) {
			System.out.println("Caught an ClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with OSS, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message:" + ce.getMessage());
		} finally {
			if (ossClient != null) {
				ossClient.shutdown();
			}
		}
		return url;
	}
}

