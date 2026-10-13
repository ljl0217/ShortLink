package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dto.rep.ShortLinkGroupSortRepDTO;
import com.nageoffer.shortlink.admin.dto.rep.ShortLinkGroupUpdateRepDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;

import java.util.List;

public interface GroupService extends IService<GroupDO> {
    /**
     * 新增短链接分组
     * @param groupName 分组名称
     */
    void saveGroup(String groupName);
    /**
     * 新增短链接分组
     * @param userName 用户名
     * @param groupName 分组名称
     */
    void saveGroup(String userName, String groupName);
    /**
     * 查询用户短链接分组
     * @return 短链接分组集合
     */
    List<ShortLinkGroupRespDTO> listGroup();

    /**
     * 修改短链接分组
     * @param requestParam 修改请求参数
     */
    void update(ShortLinkGroupUpdateRepDTO requestParam);

    /**
     * 删除短链接分组
     * @param gid 短链接分组ID
     */
    void delete(String gid);

    /**
     * 短链接分组排序
     * @param requestParam 短链接分组排序请求参数
     */
    void sortGroup(List<ShortLinkGroupSortRepDTO> requestParam);
}
