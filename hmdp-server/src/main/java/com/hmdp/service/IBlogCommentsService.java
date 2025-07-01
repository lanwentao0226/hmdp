package com.hmdp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.PageResult;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.vo.BlogCommentsVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogCommentsService extends IService<BlogComments> {

	Result addComment(String comment, Long blogId);


	Page<BlogCommentsVO> getComments(Long blogId, Integer page, Integer pageSize);

	Page<BlogCommentsVO> getSelfComments(Integer page, Integer pageSize);

	Result deleteCommentById(Long id);
}
