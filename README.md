# shop_mvc

基于 Spring MVC、JSP 和 ECharts 的实时数仓可视化项目，使用 Maven 打包为 WAR。下面记录本项目的本地运行配置及常见问题。

## 运行环境

| 项目 | 当前本地配置 | 注意事项 |
| --- | --- | --- |
| JDK | Java 8 | 项目编译版本、Maven 使用的 JDK、Tomcat 的 JRE 均使用 1.8 |
| Maven | 3.9.9 | 通过根目录 `pom.xml` 导入依赖 |
| Tomcat | 8.5.73 | 当前代码使用 Spring 5 和 `javax.servlet`，切换容器版本时需核对兼容性 |
| HTTP 端口 | 8081 | 当前机器的 8080 被占用，因此改用 8081 |
| JMX 端口 | 默认 1099 | IDEA 用于连接 Tomcat 的管理端口，与浏览器访问的 HTTP 端口不同 |
| 应用上下文 | `/` | 当前首页链接写为 `/hello`，按根路径部署可直接使用 |
| 文件编码 | UTF-8 | Java、XML、JSP 文件及响应编码应保持一致 |

Tomcat 10.1 要求 Java 11 及以上，不能用 JRE 1.8 启动；Tomcat 10 起 Servlet 包名改为 `jakarta.servlet`，当前项目也不能仅通过升级 JDK 直接切换过去。

本机 Tomcat 安装目录为 `D:\Java\apache-tomcat-8.5.73`，其他机器请使用自己的安装路径。

## IDEA 启动配置

1. 用 IDEA 打开项目根目录，将 `pom.xml` 导入为 Maven 项目并加载依赖。
2. 将项目 SDK、模块 SDK 和 Maven Runner 的 JRE 设置为 JDK 1.8。Maven Home 指向本机 Maven 安装目录。
3. 在“运行 → 编辑配置”中添加 **Tomcat Server → Local**，名称可设为 `shop_mvc`。
4. 在应用程序服务器中选择 Tomcat 8.5.73，运行 JRE 选择 1.8，HTTP 端口设为 **8081**。
5. 在 Deployment 中添加 `shop_mvc:war`，Application context 设为 **`/`**。确保启动前执行对应 Artifact 的构建。
6. 应用配置并启动，打开 `http://localhost:8081/`；点击“跳转”，或直接访问 `http://localhost:8081/hello` 进入看板。

`.idea/` 未上传到 GitHub，克隆项目后需要重新添加本机 Tomcat、JDK 和部署配置。直接修改 IDEA 配置文件后，应重启 IDEA 或在运行配置界面确认配置已重新加载。

### Maven 构建

配置 `JAVA_HOME` 指向 JDK 8，并把 JDK 和 Maven 的 `bin` 目录加入 `PATH` 后执行：

```shell
java -version
mvn -v
mvn test
mvn package
```

检查 `mvn -v` 显示的 Java 版本也为 1.8。打包产物为 `target/shop_mvc-1.0-SNAPSHOT.war`。

如果手动将 WAR 放入 Tomcat 的 `webapps`，默认上下文通常为 WAR 文件名；本项目当前按 `/` 部署。使用其他上下文时，需要同步调整首页的 `/hello` 链接和访问地址。

## Redis 和 Druid 配置

看板会在上一次 `dashboard/list` 请求结束后等待一秒再发起下一次请求，避免慢查询时请求重叠。页面能打开，并不代表 Redis、Druid 已连接或指标数据已准备好。

Avatica 1.12 默认 HTTP 客户端会为每个 JDBC 连接创建连接池。本项目通过 `DruidHelper` 的 `httpclient_impl` 属性使用 `DruidHttpClient`，每次 HTTP 请求都关闭流和连接，并设置 3 秒连接超时、10 秒读取超时。修改此配置后应重新构建并部署；若已有大量旧连接堆积，需重启 Tomcat 清理旧连接。

### Redis

当前连接地址为 **`hadoop-13:6379`**，需检查以下两处：

- `src/main/java/com/cn/mvc/shopmvc/service/impl/DashboardServiceImpl.java`：业务类显式调用 `RedisUtil.build("hadoop-13", 6379)`，运行看板时使用此地址。
- `src/main/java/com/cn/mvc/shopmvc/utils/RedisUtil.java`：默认 `host`、`port` 和连接超时配置；独立运行其中的 `main` 方法时，也需检查其连接地址。

只修改 `RedisUtil` 的默认地址，不能覆盖业务类显式传入的地址。使用实际服务器的主机名或 IP；若保留 `hadoop-13`，需保证本机 DNS 或 hosts 能正确解析，并能访问 6379 端口。当前连接代码未传入 Redis 密码，有认证要求时需调整连接配置。

`RedisUtil` 的封装方法会为每次操作借用并归还 Jedis 连接，不能将单个 Jedis 实例共享给多个请求线程。若直接调用 `getClient()`，调用方需使用 `try-with-resources` 关闭连接并归还连接池。

当前读取的数据包括：

| Redis Key | 类型 | 用途 |
| --- | --- | --- |
| `shop:uv` | Hash | UV，field 为日期字符串 |
| `shop:ip` | Hash | IP 数，field 为日期字符串 |
| `shop:pv` | Hash | PV，field 为日期字符串 |
| `quota_convert` | String | 漏斗转化 JSON，使用 `bn`、`cn`、`on`、`pn` 字段 |

`DashboardServiceImpl.dau()` 当前将查询日期固定为 **`20191016`**。接入实际数据时，应修改为数据对应日期或恢复代码中注释的当天日期计算逻辑，否则可能查不到指标。

### Druid

配置文件：`src/main/java/com/cn/mvc/shopmvc/utils/DruidHelper.java`。当前 JDBC 地址：

```text
jdbc:avatica:remote:url=http://hadoop-13:8888/druid/v2/sql/avatica/
```

此处连接的是 Apache Druid SQL 的 Avatica 接口。需要将主机和端口改为实际服务地址，确认主机名可解析、8888 端口可达，并启用对应的 Avatica 服务。

业务 SQL 使用 `dws_od` 数据源，并引用 `__time`、`count`、`totalMoney`、`areaId` 等字段。实际数据源、字段名和时间范围应与 `DashboardServiceImpl.java` 中的查询一致；地区 ID 应与 `AreaUtil.java` 中的地区映射对应。

## 中文编码

含中文的 JSP 应保存为 UTF-8，并在文件顶部声明源码编码和响应编码：

```jsp
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
```

`EncodingFilter` 设置请求和响应为 UTF-8，并在 `WEB-INF/web.xml` 中映射到 `/*`。过滤器负责请求与响应编码；JSP 的 `pageEncoding` 负责源码解码，不能省略后仅依赖过滤器。

如果出现 `è·³è½¬` 这样的文字，检查 JSP 文件实际编码及页面指令，重新构建、部署后刷新浏览器。

## Spring MVC 和资源路径

| 配置项 | 当前值 | 说明 |
| --- | --- | --- |
| Spring MVC 配置 | `src/main/resources/spring-mvc.xml` | `web.xml` 中通过 `classpath:spring-mvc.xml` 加载 |
| DispatcherServlet 映射 | `/` | 保持为 `/`，避免改为 `/*` 后影响 JSP 转发 |
| 视图前缀和后缀 | `/WEB-INF/pages/`、`.jsp` | 控制器返回 `index` 时转发到 `/WEB-INF/pages/index.jsp` |
| 静态资源映射 | `/res/**` → `/WEB-INF/` | CSS、JS 和图片通过 `/res/css/...`、`/res/js/...` 等路径访问 |
| 看板入口 | `/hello` | 由 `HelloController` 返回看板视图 |
| 指标接口 | `/dashboard/list` | 页面通过 POST 请求获取 JSON |

`WEB-INF` 下的页面不能通过浏览器直接访问，应通过控制器转发。地图脚本使用项目自带的 `/res/js/china.js`，地图不显示时，检查浏览器 Network 中该本地脚本是否加载成功。

## 数据范围与验证

访客数据日期仍为 `20191016`，与当前 Redis Hash 中的实际日期字段一致；切换为当天日期前，应先准备对应日期的数据。Druid 数据使用各 SQL 中的时间范围，可能与 Redis 示例数据日期不同。

周曲线按固定日期轴对齐，无记录的日期补 0；上周没有记录时返回 7 个零值。小时销售额查询 Druid `CURRENT_DATE` 对应的一整天，并返回 24 个小时点。Top4 销售额保留 SQL 返回的排名顺序。

Java 回归测试通过 `mvn test` 执行；页面轮询的防重叠测试需要 Node.js：

```shell
node src/test/js/dashboard-polling.test.js
```

## 常见问题

| 现象 | 检查与处理 |
| --- | --- |
| 无法在 `localhost:1099` ping 服务器 | 查看 Tomcat 控制台最早的异常，先确认容器成功启动；曾出现的原因是使用 JRE 1.8 启动 Tomcat 10.1 |
| `localhost:8080` 已在使用中 | 检查占用进程；本项目 HTTP 端口使用 8081，浏览器地址也要同步修改 |
| 首页中文乱码 | 检查 UTF-8 文件编码、JSP `pageEncoding` 和响应编码，重新部署后刷新 |
| CSS 或 JS 返回 404 | 检查 `/res/**` 映射、应用上下文和 WAR 中是否包含资源文件 |
| 看板为空或接口报错 | 检查 Redis/Druid 地址、网络、数据结构和查询日期，并查看服务端日志及浏览器 Network/Console |
| 修改 JSP 后仍显示旧内容 | 重新执行 Artifact 构建并更新部署；Maven 打包后需要让 Tomcat 加载新的 WAR |

源码和资源位于 `src/`，构建输出位于 `target/`。`.idea/` 和 `target/` 已加入 `.gitignore`，本机服务器配置和构建产物不会随源码提交。
