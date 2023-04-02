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
7. 并发
8. Spring Schedule 定时任务

## 设计

### 整合 Knife4j 接口文档

在配置文件中添加`@Profile({"dev"})`注解，让该接口文档只有在本地开发环境下才能够被访问

### 数据库表设计

本来想要增加一张标签表，专门存储标签数据，但这样的话又要增加一张标签和用户的关联表，需要在多处联表查询。所以这里直接在用户表中增加一个字段，以 json 格式存储标签数据（使用mysql中的 json 数据类型，保存一个 json 数组）

由于前端页面需要根据标签来选择数据，这里还是增加了一张标签表，但并不与用户表关联，只用与查找系统中有哪些标签，如果这个功能交给用户表完成的话，需要遍历所有用户找出不相同的标签，开销会很大。而且由于用户标签大多数情况都是不变的，所以后面可以直接在缓存中查找数据。

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

#### 标签表

```sql
create table tags
(
    id          bigint auto_increment primary key,
    tag_name    varchar(256)                       not null comment '标签名',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '逻辑删除',
    constraint tag_name
        unique (tag_name)
)
    comment '标签表';
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

#### 更新用户信息

用户登录后，用户信息就会存储在前端的pinia中，更新操作会修改pinia中的user状态，用户修改了信息后，会将该参数传递过来，后端用User实体类接收，根据id修改。

#### 用户上传头像

前端上传图片（要更改请求头），后端使用`MutipartFile`接收，随后调用腾讯云cos的SDK来上传图片到腾讯云图床。要使用腾讯云的SDK就必须在代码中配置id、key和SessionToken，我是用一个定时任务每隔两小时执行一次，生成临时的密钥。

由于这里的定时任务只需要生成三个数据，并且为了安全性考虑都保存在本地的IOC容器中，所以没考虑用分布式锁。

#### 分页展示用户数据

使用mybatis的分页插件，前端每次都要传递页号与页面大小

#### 推荐与用户兴趣最相似的用户

根据用户表中的标签来进行匹配，这里使用的是编辑距离算法（字符串1经过多少次增删改字符操作可以变成字符串2，操作最少的用户即即为最匹配的用户）

将编辑距离算法更改为如下形式：

* 方法传递两个参数，一个是当前登录用户的标签列表，另一个是要比较的用户的标签列表
* 先对两个列表排序（按首字母排序，如果首字符相同则按后面的字符排序，可以使用java8 stream的sorted方法），如果列表没有排序的话有可能出现两个用户的标签内容相同但顺序不同，结果相似度就非常低的情况
* 比较两个列表中的字符串

具体流程如下：

1. 先获取表中所有的用户（只查id和tags两个字段）
2. 用当前登录用户的tags与其它所有用户的tags比较，执行编辑距离算法，比较完成后将用户id与相似度存放到一个Pair结构中，让后将pair再存放入List集合
3. 对上面的List结合中的所有pair以相似度大小进行排序，得到只包含用户id的list集合
4. 根据上面的排好序的list集合去查找对应用户
5. 获得用户信息后再根据前面得到的排好序的idList对用户进行排序，可以通过steam的skip和limit对用户进行分页操作，最后返回结果。

#### 获取所有标签

先从redis中取，取不到就取查数据库，再将数据加入缓存

#### 用户上传标签

如果用户上传的标签已存在于数据库中，则不添加。判断标签是否存在于数据库中的操作可以转到redis中进行，使用redis的set集合，如果redis中已存在该标签，就不操作数据库，否则先数据库中添加信息。因为这个数据变化不频繁，我设置过期时间为1小时

#### 用户创建队伍

要对传入的数据进行校验

#### 队长或系统管理员修改队伍信息

先判断传入的数据是否正常，在判断当前队伍的状态，如果当前队伍是加密的，那么传入的密码就不能为空

#### 根据id查找队伍

#### 查询队伍

由于查询队伍可以有多种方式，例如：根据一个队伍id来查、根据多个id同时查找多个队伍、根据队名来查、根据队伍描述来查、根据队伍人数来查等等，这里可以把这些查询方式全部组合到一起，只对外提供一个接口就可以完成全部功能。



### 批量导入100万条数据

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

### 性能优化











