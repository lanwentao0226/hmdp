package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import com.hmdp.vo.SignVO;
import com.hmdp.vo.UserInfoVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

	@Resource
	private IUserService userService;
	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Override
	public Result getUserInfo() {
		UserDTO user = UserHolder.getUser();
		if(user==null){
			return Result.fail("用户未登录");
		}
		String key=user.getLoginToken();
		Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
		UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), true);

		UserInfoVO userInfoVO = new UserInfoVO();
		userInfoVO.setUserId(userDTO.getId());
		userInfoVO.setIcon(userDTO.getIcon());
		userInfoVO.setNickName(userDTO.getNickName());

		UserInfo userInfo = query().eq("user_id", user.getId()).one();
		if(userInfo==null){
			return Result.ok(userInfoVO);
		}
		userInfoVO.setIntroduce(userInfo.getIntroduce());
		userInfoVO.setGender(userInfo.getGender());
		userInfoVO.setCity(userInfo.getCity());
		userInfoVO.setBirthday(userInfo.getBirthday());

		return Result.ok(userInfoVO);
	}

	@Override
	@Transactional
	public Result updateInfo(UserInfoVO userInfoVO) {
		String loginToken = UserHolder.getUser().getLoginToken();

		String key=loginToken;
		Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
		UserDTO user = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), true);

		if(user.getId()==null||!userInfoVO.getUserId().equals(user.getId())){
			return Result.fail("不合法请求");
		}
		if(userInfoVO.getIcon()==null||userInfoVO.getIcon()==""||userInfoVO.getNickName()==null||userInfoVO.getNickName()==""){
			return Result.fail("头像和昵称不能为空");
		}
		boolean isSuccess = userService.update().eq("id", userInfoVO.getUserId())
				.set("nick_name", userInfoVO.getNickName())
				.set("icon", userInfoVO.getIcon())
				.set("update_time", LocalDateTime.now())
				.update();
		if(!isSuccess){
			return Result.fail("保存用户信息失败");
		}
		UserInfo userInfo = query().eq("user_id", userInfoVO.getUserId()).one();
		if(userInfo==null){
			userInfo = BeanUtil.copyProperties(userInfoVO, UserInfo.class);
			userInfo.setUpdateTime(LocalDateTime.now());
			userInfo.setCreateTime(LocalDateTime.now());
			Long size = stringRedisTemplate.opsForSet().size("follows:" + userInfo.getUserId());
			userInfo.setFollowee(Math.toIntExact(size));

			Long size1 = stringRedisTemplate.opsForSet().size("fans:" + userInfo.getUserId());
			userInfo.setFans(Math.toIntExact(size1));

			boolean save = save(userInfo);
			if(!save){
				return Result.fail("保存用户信息失败");
			}
		}
		else{
			int size = Math.toIntExact(stringRedisTemplate.opsForSet().size("follows:" + userInfo.getUserId()));

			int size1 = Math.toIntExact(stringRedisTemplate.opsForSet().size("fans:" + userInfo.getUserId()));

			boolean isUpdate = update().eq("user_id", userInfo.getUserId())
					.set("city", userInfoVO.getCity())
					.set("introduce", userInfoVO.getIntroduce())
					.set("fans", size1)
					.set("followee", size)
					.set("gender", userInfoVO.getGender())
					.set("birthday", userInfoVO.getBirthday())
					.set("update_time", LocalDateTime.now())
					.update();
			if(!isUpdate){
				return Result.fail("保存用户信息失败");
			}
		}
		try {

			Map<String, String> updates = new HashMap<>();
			updates.put("nickName", userInfoVO.getNickName());
			updates.put("icon", userInfoVO.getIcon());
			stringRedisTemplate.opsForHash().putAll(key, updates);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return Result.ok();
	}


}
