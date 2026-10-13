package com.nageoffer.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@TableName(value = "t_link_goto")
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkGotoDO {
    /**
     * ID
     */
    private Long id;
    /**
     * 分组ID
     */
    private String gid;
    /**
     * 短链接
     */
    private String fullShortUrl;
}
