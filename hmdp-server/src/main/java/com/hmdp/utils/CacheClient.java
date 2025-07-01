package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

@Slf4j
@Component
public class CacheClient {

	private final StringRedisTemplate stringRedisTemplate;

	public CacheClient(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void set(String key, Object value, Long time, TimeUnit unit){
		stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
	}

	public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){

		RedisData redisData = new RedisData();
		redisData.setData(value);
		redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

		stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
	}
	public <R,ID> R queryWithPassThrough(
			String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time,TimeUnit unit){
		String key=keyPrefix + id;
		String json = stringRedisTemplate.opsForValue().get(key);

		if(StrUtil.isNotBlank(json)){
			return JSONUtil.toBean(json,type);
		}
		if(json!=null){
			return null;
		}
		R r=dbFallback.apply(id);
		if(r == null){
			this.set(key,"",time,unit);
		}

		this.set(key,r,time,unit);
		return r;
	}

	private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

	public <R,ID> R queryWithLogicalExpire(
			String keyPrefix,String lockPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time,TimeUnit unit)
	{
		String key=keyPrefix + id;
		String json = stringRedisTemplate.opsForValue().get(key);
		if(StrUtil.isBlank(json)){
			return null;
		}

		RedisData redisData = JSONUtil.toBean(json, RedisData.class);
		R r = JSONUtil.toBean((JSONObject) redisData.getData(),type);
		LocalDateTime expireTime = redisData.getExpireTime();

		if(expireTime.isAfter(LocalDateTime.now())){
			return r;
		}
		String lockKey =lockPrefix+id;
		boolean isLock = tryLock(lockKey);

		if(isLock){
			json = stringRedisTemplate.opsForValue().get(key);
			redisData = JSONUtil.toBean(json, RedisData.class);
			r = JSONUtil.toBean((JSONObject) redisData.getData(),type);
			expireTime = redisData.getExpireTime();

			if(expireTime.isAfter(LocalDateTime.now())){
				unlock(lockKey);
				return r;
			}
			CACHE_REBUILD_EXECUTOR.submit(()->{
				try {
					R r1 = dbFallback.apply(id);
					this.setWithLogicalExpire(key,r1,time,unit);

				} catch (Exception e) {
					throw new RuntimeException(e);
				}finally {
					unlock(lockKey);
				}
			});
		}
		return r;
	}

	private boolean tryLock(String key){
		Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 1, TimeUnit.SECONDS);
		return BooleanUtil.isTrue(flag);
	}
	private void unlock(String key){
		stringRedisTemplate.delete(key);
	}
}
