package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public Result queryTypeList() {
		String key=CACHE_SHOPTYPE_KEY ;
		String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
		if(StrUtil.isNotBlank(shopTypeJson)){
			List<ShopType> shopTypeList = JSONUtil.toList(shopTypeJson, ShopType.class);
			return Result.ok(shopTypeList);
		}

		List<ShopType> typeList = query().orderByAsc("sort").list();
		if(typeList==null){
			return Result.fail("店铺类型不存在！");
		}
		stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(typeList),CACHE_SHOPTYPE_TTL, TimeUnit.HOURS);

		return Result.ok(typeList);
	}
}
