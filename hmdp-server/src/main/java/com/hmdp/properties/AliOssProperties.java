package com.hmdp.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "dianping.alioss")
@Component
public class AliOssProperties {
	private String endpoint;
	private String bucketName;
	private String region;
	private String accessKeyId;
	private String accessKeySecret;
}