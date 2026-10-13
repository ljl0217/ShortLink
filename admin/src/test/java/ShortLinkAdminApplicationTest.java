public class ShortLinkAdminApplicationTest {
    public static void main(String[] args) {
        String sql = "CREATE TABLE `t_group_%d`\n" +
                "(\n" +
                "    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
                "    `gid`         varchar(32)  DEFAULT NULL COMMENT '分组标识',\n" +
                "    `name`        varchar(64)  DEFAULT NULL COMMENT '分组名称',\n" +
                "    `username`    varchar(256) DEFAULT NULL COMMENT '创建分组用户名',\n" +
                "    `sort_order`  int(3) DEFAULT NULL COMMENT '分组排序',\n" +
                "    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',\n" +
                "    `update_time` datetime     DEFAULT NULL COMMENT '修改时间',\n" +
                "    `del_flag`    tinyint(1) DEFAULT NULL COMMENT '删除标识 0：未删除 1：已删除',\n" +
                "    PRIMARY KEY (`id`),\n" +
                "    KEY           `idx_username` (`username`) USING BTREE\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        for (int i = 0; i < 16; i++) {
            System.out.printf((sql) + "%n", i);
        }
    }
}
