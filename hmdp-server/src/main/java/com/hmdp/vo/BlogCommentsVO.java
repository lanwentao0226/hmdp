package com.hmdp.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogCommentsVO {

	public Long id;

	public boolean isLike;

	public String icon;

	public Long userId;

	public String content;

	public String name;

	public LocalDateTime createTime;

	public Long blogId;

}