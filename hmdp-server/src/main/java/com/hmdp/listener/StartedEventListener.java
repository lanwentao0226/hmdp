package com.hmdp.listener;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Shop;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StartedEventListener implements ApplicationListener<ApplicationStartedEvent> {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private IShopService shopService;

	@Resource
	private ISeckillVoucherService seckillVoucherService;

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		Set<String> keys = stringRedisTemplate.keys("shop:geo:*");
		Boolean isExist = stringRedisTemplate.hasKey("seckill:stock:14");
		if (keys != null && !keys.isEmpty() && isExist) {
			System.out.println("应用启动完成");
			return;
		}
		if(keys == null || keys.isEmpty())
			{

			List<Shop> list = shopService.list();

			Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));

			for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
				Long typeId = entry.getKey();
				String key = "shop:geo:" + typeId;

				List<Shop> value = entry.getValue();
				List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
				for (Shop shop : value) {
					locations.add(new RedisGeoCommands.GeoLocation<>(
							shop.getId().toString(), new Point(shop.getX(), shop.getY())
					));
				}
				stringRedisTemplate.opsForGeo().add(key, locations);
			}
		}
		if(!isExist){
			SeckillVoucher seckillVoucher = seckillVoucherService.query().eq("voucher_id", 14).one();
			stringRedisTemplate.opsForValue().set("seckill:stock:14",seckillVoucher.getStock().toString());
		}
	}
}

