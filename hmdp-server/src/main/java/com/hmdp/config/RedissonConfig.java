package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
	@Bean
	public RedissonClient redissonClient(){
		Config config = new Config();
//		config.useSingleServer().setAddress("redis://192.168.100.137:6379").setPassword("123456");
		config.useSingleServer().setAddress("redis://hmdp-redis:6379").setPassword("123456");
		return Redisson.create(config);
	}
}
