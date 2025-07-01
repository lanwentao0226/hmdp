package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.BlogCommentsDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private IUserService userService;

	@Resource
	private IFollowService followService;

	@Resource
	private IBlogCommentsService blogCommentsService;

	@Override
	public Result queryBlogById(Long id) {
		Blog blog=getById(id);
		if(blog==null){
			return Result.fail("笔记不存在");
		}
		Long userId = blog.getUserId();
		User user = userService.getById(userId);
		blog.setName(user.getNickName());
		blog.setIcon(user.getIcon());

		isBlogLiked(blog);
		return Result.ok(blog);
	}

	private void isBlogLiked(Blog blog) {
		UserDTO user = UserHolder.getUser();
		if(user == null){
			return;
		}
		Long userId = user.getId();

		String key = "blog:liked:"+blog.getId();
		Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
		blog.setIsLike(score != null);
	}

	@Override
	public Result likeBlog(Long id) {
		Long userId = UserHolder.getUser().getId();

		String key = BLOG_LIKED_KEY+id;
		Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
		if(score == null){
			boolean isSuccess = update().setSql("liked = liked +1 ").eq("id", id).update();
			if(isSuccess){
				stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
			}
		}else{
			boolean isSuccess = update().setSql("liked = liked -1 ").eq("id", id).update();
			if(isSuccess){
				stringRedisTemplate.opsForZSet().remove(key,userId.toString());
			}
		}
		return Result.ok();
	}

	@Override
	public Result queryHotBlog(Integer current) {
		Page<Blog> page = query()
				.orderByDesc("liked")
				.page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
		// 获取当前页数据
		List<Blog> records = page.getRecords();
		// 查询用户
		records.forEach(blog ->{
			Long userId = blog.getUserId();
			User user = userService.getById(userId);
			blog.setName(user.getNickName());
			blog.setIcon(user.getIcon());
			this.isBlogLiked(blog);
		});
		return Result.ok(records);
	}

	@Override
	public Result queryBlogLikes(Long id) {
		String key = BLOG_LIKED_KEY+id;
		Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
		if(top5==null || top5.isEmpty()){
			return Result.ok(Collections.emptyList());
		}
		List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
		String idStr = StrUtil.join(",", ids);

		List<UserDTO> userDTOS = userService.query()
				.in("id",ids)
				.last("ORDER BY FIELD(id,"+idStr+")").list()
				.stream()
				.map(user -> BeanUtil.copyProperties(user, UserDTO.class))
				.collect(Collectors.toList());
		return Result.ok(userDTOS);
	}

	@Override
	public Result saveBlog(Blog blog) {
		UserDTO user = UserHolder.getUser();
		blog.setUserId(user.getId());

		boolean isSuccess = save(blog);
		if(!isSuccess){
			return Result.fail("新增笔记失败!");
		}
//		String jsonStr = JSONUtil.toJsonStr(blog);
//		stringRedisTemplate.opsForValue().set(BLOG_CONTENTS_KEY+blog.getId(), jsonStr);

		List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
		for (Follow follow : follows) {
			Long userId = follow.getUserId();

			String key = FEED_KEY+userId;
			stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
		}
		return Result.ok(blog.getId());
	}

	@Override
	public Result queryBlogOfFollow(Long max, Integer offset) {
		Long userId = UserHolder.getUser().getId();

		String key = FEED_KEY+userId;
		Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);

		if(typedTuples == null || typedTuples.isEmpty()){
			return Result.ok();
		}
		List<Long> ids = new ArrayList<>(typedTuples.size());
		long minTime = 0;
		int os =1;
		for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
			ids.add(Long.valueOf(tuple.getValue()));
			long time=tuple.getScore().longValue();
			if(time == minTime){
				os++;
			}else{
				minTime=time;
				os=1;
			}
		}
		String idStr=StrUtil.join(",",ids);
		List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();

		for (Blog blog : blogs) {

			User user = userService.getById(blog.getUserId());
			blog.setName(user.getNickName());
			blog.setIcon(user.getIcon());

			isBlogLiked(blog);

		}

		ScrollResult r= new ScrollResult();
		r.setList(blogs);
		r.setOffset(os);
		r.setMinTime(minTime);

		return Result.ok(r);
	}

	@Override
	public Result updateBlogById(Blog blog) {
		UserDTO user = UserHolder.getUser();
		// 设置用户ID（确保更新者信息）
		blog.setUserId(user.getId());
		blog.setUpdateTime(LocalDateTime.now());

		// 校验博客ID是否存在
		if (blog.getId() == null) {
			return Result.fail("博客ID不能为空");
		}

		// 执行更新操作（通过ID更新，条件为blog.id）
		boolean isSuccess = updateById(blog);

		// 处理更新结果
		if (!isSuccess) {
			return Result.fail("更新博客失败");
		}

		// 返回成功结果
		return Result.ok("更新博客成功");
	}

	@Override
	@Transactional
	public Result deleteBlogById(Long id) {
		if (id == null) {
			return Result.fail("博客ID不能为空");
		}
		// 查询博客是否存在
		Blog blog = getById(id);
		if (blog == null) {
			return Result.fail("博客不存在");
		}
		// 检查博客归属（确保只能删除自己的博客）
		UserDTO user = UserHolder.getUser();
		if (!Objects.equals(blog.getUserId(), user.getId())) {
			return Result.fail("无权限删除此博客");
		}

		try {
			removeById(id);
			blogCommentsService.lambdaUpdate()
					.eq(BlogComments::getBlogId, id)
					.remove();
			// 删除缓存
			try {
				stringRedisTemplate.delete(BLOG_LIKED_KEY + id);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return Result.ok("博客删除成功");
		} catch (RuntimeException e) {
			throw new RuntimeException(e);
		}
	}


}
