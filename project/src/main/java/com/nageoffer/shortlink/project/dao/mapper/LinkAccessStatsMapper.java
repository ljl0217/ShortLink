package com.nageoffer.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkGroupStatsReqDTO;
import com.nageoffer.shortlink.project.dto.rep.ShortLinkStatsReqDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * uv pv uip 统计
 */
public interface LinkAccessStatsMapper extends BaseMapper<LinkAccessStatsDO> {
    /**
     * 记录基础访问监控数据
     */
    @Insert("""
                INSERT INTO t_link_access_stats (full_short_url,date,hour,weekday,pv,uv,uip,create_time,update_time,del_flag )
                VALUES ( #{linkAccessStats.fullShortUrl},#{linkAccessStats.date},#{linkAccessStats.hour},#{linkAccessStats.weekday},
                #{linkAccessStats.pv},#{linkAccessStats.uv},#{linkAccessStats.uip}, NOW(), NOW(), 0) ON DUPLICATE KEY UPDATE pv = pv + #{linkAccessStats.pv}, uv = uv + #{linkAccessStats.uv},
                uip = uip + #{linkAccessStats.uip},update_time = NOW()
                """)
    void linkAccessStatsInsert(@Param("linkAccessStats") LinkAccessStatsDO linkAccessStatsDO);

    /**
     * 根据短链接获取指定日期内基础监控数据
     */
    @Select("""
                SELECT tlas.date, SUM(tlas.pv) AS pv, SUM(tlas.uv) AS uv, SUM(tlas.uip) AS uip
                FROM t_link t1 INNER JOIN t_link_access_stats tlas
                ON t1.full_short_url = tlas.full_short_url
                WHERE  tlas.full_short_url = #{param.fullShortUrl}
                        AND t1.gid = #{param.gid}
                        AND t1.del_flag = '0'
                        AND t1.enable_status = #{param.enableStatus}
                        AND tlas.date BETWEEN #{param.startDate} and #{param.endDate}
                GROUP BY tlas.full_short_url, t1.gid, tlas.date;
                """)
    List<LinkAccessStatsDO> listStatsByShortLink(@Param("param")ShortLinkStatsReqDTO request);

    /**
     * 根据分组获取指定日期内基础监控数据
     */
    @Select("""
                SELECT tlas.date, SUM(tlas.pv) AS pv, SUM(tlas.uv) AS uv, SUM(tlas.uip) AS uip
                FROM t_link t1 INNER JOIN t_link_access_stats tlas
                ON t1.full_short_url = tlas.full_short_url
                WHERE  t1.gid = #{param.gid}
                        AND t1.del_flag = '0'
                        AND t1.enable_status = '0'
                        AND tlas.date BETWEEN #{param.startDate} and #{param.endDate}
                GROUP BY t1.gid, tlas.date;
                """)
    List<LinkAccessStatsDO> listStatsByGroup(@Param("param") ShortLinkGroupStatsReqDTO request);

    /**
     * 根据短链接获取指定日期小时内基础监控数据
     */
    @Select("""
                SELECT tlas.hour, SUM(tlas.pv) AS pv
                FROM t_link t1 INNER JOIN t_link_access_stats tlas
                ON t1.full_short_url = tlas.full_short_url
                WHERE   tlas.full_short_url = #{param.fullShortUrl}
                        AND t1.gid = #{param.gid}
                        AND t1.del_flag = '0'
                        AND t1.enable_status = #{param.enableStatus}
                        AND tlas.date BETWEEN #{param.startDate} and #{param.endDate}
                GROUP BY tlas.full_short_url, t1.gid, tlas.hour;
                """)
    List<LinkAccessStatsDO> listHourStatsByShortLink(@Param("param")ShortLinkStatsReqDTO request);

    /**
     * 根据分组获取指定日期小时内基础监控数据
     */
    @Select("""
                SELECT tlas.hour, SUM(tlas.pv) AS pv
                FROM t_link t1 INNER JOIN t_link_access_stats tlas
                ON t1.full_short_url = tlas.full_short_url
                WHERE   t1.gid = #{param.gid}
                        AND t1.del_flag = '0'
                        AND t1.enable_status = '0'
                        AND tlas.date BETWEEN #{param.startDate} and #{param.endDate}
                GROUP BY t1.gid, tlas.hour;
                """)
    List<LinkAccessStatsDO> listHourStatsByGroup(@Param("param")ShortLinkGroupStatsReqDTO request);

    /**
     * 根据短链接获取指定日期星期内基础监控数据
     */
    @Select("""
                SELECT tlas.weekday, SUM(tlas.pv) AS pv
                FROM t_link t1 INNER JOIN t_link_access_stats tlas
                ON t1.full_short_url = tlas.full_short_url
                WHERE  tlas.full_short_url = #{param.fullShortUrl}
                        AND t1.gid = #{param.gid}
                        AND t1.del_flag = '0'
                        AND t1.enable_status = #{param.enableStatus}
                        AND tlas.date BETWEEN #{param.startDate} and #{param.endDate}
                GROUP BY tlas.full_short_url, t1.gid, tlas.weekday;
                """)
    List<LinkAccessStatsDO> listWeekdayStatsByShortLink(@Param("param")ShortLinkStatsReqDTO request);

    /**
     * 根据分组获取指定日期星期内基础监控数据
     */
    @Select("""
                SELECT tlas.weekday, SUM(tlas.pv) AS pv
                FROM t_link t1 INNER JOIN t_link_access_stats tlas
                ON t1.full_short_url = tlas.full_short_url
                WHERE  t1.gid = #{param.gid}
                        AND t1.del_flag = '0'
                        AND t1.enable_status = '0'
                        AND tlas.date BETWEEN #{param.startDate} and #{param.endDate}
                GROUP BY t1.gid, tlas.weekday;
                """)
    List<LinkAccessStatsDO> listWeekdayStatsByGroup(@Param("param")ShortLinkGroupStatsReqDTO request);
}
