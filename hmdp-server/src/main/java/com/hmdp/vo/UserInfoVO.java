package com.hmdp.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVO {
	private Long userId;
	private String nickName;
	private String icon;
	private String introduce;
	private Boolean gender;
	private LocalDate birthday;
	private String city;
}
