-- 用户表
create table user
(
    id            bigint auto_increment comment 'id'
        primary key,
    username      varchar(256)                       null comment '用户昵称',
    user_account  varchar(256)                       null comment '账号',
    avatar_url    varchar(1024)                      null comment '用户头像',
    gender        tinyint                            null comment '性别',
    user_password varchar(512)                       not null comment '密码',
    phone         varchar(128)                       null comment '电话',
    email         varchar(512)                       null comment '邮箱',
    user_status   int      default 0                 not null comment '状态 0 - 正常',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint  default 0                 not null comment '是否删除',
    user_role     tinyint  default 0                 null comment '用户角色 0 - 普通用户 1 - 管理员',
    user_code     varchar(512)                       null comment '公司编号',
    tags          json                               null comment '标签 json 列表'
)
    comment '用户表';

-- 队伍表
create table team
(
    id               bigint auto_increment primary key,
    team_name        varchar(256)                       not null comment '队伍名称',
    team_description varchar(1024)                      null comment '描述',
    expire_time      datetime                           null comment '过期时间',
    captain_id       bigint                             not null comment '队长id',
    team_status      tinyint  default 0                 not null comment '队伍状态（0 公开，1 私有，2 加密）',
    team_password    varchar(512)                       null comment '入队密码',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete        int      default 0                 not null comment '是否删除（逻辑删除）'
)
    comment '队伍';

-- 用户队伍关联表
create table user_team
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '用户id',
    team_id     bigint                             not null comment '队伍id',
    join_time   datetime                           null comment '用户加入时间',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   int      default 0                 not null comment '是否删除'
)
    comment '用户队伍关系';