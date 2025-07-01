package com.hmdp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

	@Bean(name = "taskExecutor")
	public ExecutorService taskExecutor() {
		// 创建一个固定大小的线程池
		return Executors.newFixedThreadPool(10);

		// 或者使用更灵活的配置：
		// ThreadPoolExecutor executor = new ThreadPoolExecutor(
		//     5, // 核心线程数
		//     10, // 最大线程数
		//     60, // 空闲线程存活时间
		//     TimeUnit.SECONDS,
		//     new LinkedBlockingQueue<>(100) // 任务队列
		// );
		// return executor;
	}
}
