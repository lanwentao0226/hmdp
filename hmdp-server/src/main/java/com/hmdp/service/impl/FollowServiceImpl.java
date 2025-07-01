package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private IUserService userService;

	@Override
	@Transactional
	public Result follow(Long followUserId, Boolean isFollow) {
		Long userId = UserHolder.getUser().getId();
		if(isFollow){
			Follow follow = new Follow();
			follow.setUserId(userId);
			follow.setFollowUserId(followUserId);
			boolean isSuccess = save(follow);
			if(isSuccess){
				try {
					String key = "follows:"+userId;
					stringRedisTemplate.opsForSet().add(key,followUserId.toString());
					String key1="fans:"+followUserId;
					stringRedisTemplate.opsForSet().add(key1, userId.toString());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			}

		}
		else{
			boolean isSuccess = remove(new LambdaQueryWrapper<Follow>()
					.eq(Follow::getUserId, userId)
					.eq(Follow::getFollowUserId, followUserId)
			);
			if(isSuccess){
				try {
					String key = "follows:"+userId;
					stringRedisTemplate.opsForSet().remove(key,followUserId.toString());

					String key1="fans:"+followUserId;
					stringRedisTemplate.opsForSet().remove(key1,userId.toString());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		return Result.ok();
	}

	@Override
	public Result isFollow(Long followUserId) {
		Long userId = UserHolder.getUser().getId();
		Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
		return Result.ok(count>0);
	}

	@Override
	public Result followCommons(Long id) {
		UserDTO user1 = UserHolder.getUser();
		if(user1 == null){
			return Result.fail("未登录,请先登录！");
		}
		else{
			Long userId=user1.getId();
			String key = "follows:"+userId;
			String key2 = "follows:"+id;
			Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key2, key);
			if(intersect == null || intersect.isEmpty()){
				return Result.ok(Collections.emptyList());
			}
			List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

			List<UserDTO> users = userService.listByIds(ids)
					.stream()
					.map(user -> BeanUtil.copyProperties(user, UserDTO.class))
					.collect(Collectors.toList());
			return Result.ok(users);
		}
	}
}
