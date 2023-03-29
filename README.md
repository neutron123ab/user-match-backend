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

#### 增加了mybatis数据类型转换器

由于user表中保存了关于标签的json列表数据，所以我们每次取数据的时候都要多写一段反序列化的代码，为了增加复用，我自定义了一个`json转Set<String>`的`TypeHandler`并将其以注解的形式定义在实体类的json字段上（要在配置文件中注册这个`TypeHandler`），之后每次取数据的时候，它都会自动帮我把json数据以`Set<String>`的形式注入到tags字段中。

**json与Set\<String>的类型转换器**

继承BaseTypeHandler类，重写其中的四个方法，把json序列化和反序列化的操作放到了这里面。

```java
public class StringSetTypeHandler extends BaseTypeHandler<Set<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, Set<String> strings, JdbcType jdbcType) throws SQLException {
        Gson gson = new Gson();
        String content = CollectionUtils.isEmpty(strings) ? null : gson.toJson(strings);
        preparedStatement.setString(i, content);
    }

    @Override
    public Set<String> getNullableResult(ResultSet resultSet, String s) throws SQLException {
        Gson gson = new Gson();
        String string = resultSet.getString(s);
        return StringUtils.isBlank(string) ? new HashSet<>() : gson.fromJson(string, new TypeToken<Set<String>>() {}.getType());
    }

    @Override
    public Set<String> getNullableResult(ResultSet resultSet, int i) throws SQLException {
        Gson gson = new Gson();
        String string = resultSet.getString(i);
        return StringUtils.isBlank(string) ? new HashSet<>() : gson.fromJson(string, new TypeToken<Set<String>>() {}.getType());
    }

    @Override
    public Set<String> getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        Gson gson = new Gson();
        String string = callableStatement.getString(i);
        return StringUtils.isBlank(string) ? new HashSet<>() : gson.fromJson(string, new TypeToken<Set<String>>() {}.getType());
    }
}
```

之后将该`TypeHandler`注册

```yaml
mybatis-plus:
	# 扫描自定义TypeHandler所在包
    type-handlers-package: com.neutron.usermatchbackend.handler
```

最后再实体类的tags字段上标注`TypeHandler`即可

```java
@TableField(value = "tags", typeHandler = StringSetTypeHandler.class)
private Set<String> tags;
```



### 批量导入数据

批量导入100万条数据

1. 每次导入1条数据，导入100万次（这种方式程序执行时间过长，且由于数据量很大，程序会变得不可控）
2. 批量导入数据，将100万条数据分成很多组，每次导入一组（我这里每组设置10000条数据）

![image-20230329144851789](D:\zProject\图片\README\image-20230329144851789.png)

​	用时156秒

3. 通过多线程并发插入数据（我这里开了100个线程，每个线程处理10000条数据，需要配置数据库连接池的最大连接数，否则会报错）

![image-20230329151817848](D:\zProject\图片\README\image-20230329151817848.png)

​	耗时45秒

```java
@Slf4j
@Component
public class InsertUsers {

    private final ExecutorService executorService = new ThreadPoolExecutor(100, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    /**
     * 并发插入数据
     */
    public void insertUsersConcurrent() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            List<User> userList = new ArrayList<>();
            while(true) {
                j++;
                User user = getUser();//设置要插入数据的信息
                userList.add(user);
                if(j % 10000 == 0) {
                    break;
                }
            }
            //异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList, 10000);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println("execute time(concurrent): " + stopWatch.getTotalTimeMillis());
    }

}
```









