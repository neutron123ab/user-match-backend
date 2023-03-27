# 用户匹配系统
## 项目介绍：
帮助大家找到志同道合的伙伴，包括用户注册登录、更新个人信息、按标签搜索用户、推荐相似用户、组队、聊天室等功能

## 需求分析
1. 用户自己添加标签，对标签分类
2. 用户可编辑个人信息
3. 允许用户根据标签去搜索其他用户
4. 组队（创建队伍、加入队伍、根据标签检索队伍、邀请别人加入队伍、队长踢出队伍）
5. 向用户推荐队伍/其他用户（相似度匹配算法）
6. 队伍中的用户可群聊

## 技术栈

### 前端

1. Vue3
2. Vant UI
3. Vite

### 后端

1. SpringBoot
2. MySQL
3. Redis
4. WebSocket
5. knife4j
6. Redisson分布式锁
7. Easy Excel
8. 并发

## 设计

### 整合 Knife4j 接口文档

在配置文件中添加`@Profile({"dev"})`注解，让该接口文档只有在本地开发环境下才能够被访问

### 数据库表设计

本来想要增加一张标签表，专门存储标签数据，但这样的话又要增加一张标签和用户的关联表，需要在多处联表查询。所以这里直接在用户表中增加一个字段，以 json 格式存储标签数据（使用mysql中的 json 数据类型，保存一个 json 数组）

#### 用户表

```sql
-- auto-generated definition
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
    tags          json                               null comment '标签 json 列表'
)
    comment '用户表';
```

#### 队伍表

id、队名、描述、队伍最大人数、队伍过期时间、队长id、队伍状态（公开、私有、加密）、密码（加密时才需要）、创建时间、更新时间、是否删除

```sql
-- auto-generated definition
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
```

#### 用户队伍关联表

id、用户id、队伍id、加入时间、创建时间、更新时间、是否删除

```sql
-- auto-generated definition
create table user_team
(
    id          bigint auto_increment primary key,
    user_id     bigint                             not null comment '用户id',
    team_id     bigint                             not null comment '队伍id',
    join_time   datetime                           null comment '用户加入时间',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   int      default 0                 not null comment '是否删除'
)
    comment '用户队伍关系';
```



### 后端接口开发

#### 根据标签搜索用户

用户可以传入多个标签，当这些标签都存在时才能搜索出用户

这里有两种查找方式：

1. 通过 sql 在数据库中查找，直接得到结果
2. 内存查找















