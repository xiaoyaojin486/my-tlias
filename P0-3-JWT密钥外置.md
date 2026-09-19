# P0-3 JWT 签名密钥外置与加固

> 分支：`feature/p0-3-jwt-key`
> 完成日期：2026-09-19
> 对应改进清单：P0-3（注：清单里的 P0-2「数据库密码明文」在初始化仓库时已随 `.env` 改造完成）

---

## 一、改之前是什么问题

```java
public class JwtUtils {
    private static String signKey = "SVRIRUlNQQ==";
    private static Long expire = 43200000L;
```

三个问题，一个比一个严重。

### 问题 1：密钥进了仓库

密钥硬编码在源码里，而仓库是 **public**。任何人都能拿到它 → 能签发任意用户的令牌 → **可以冒充任何人登录你的系统**。这比数据库密码泄漏还严重：密码泄漏丢的是数据，密钥泄漏丢的是**整个身份体系**。

### 问题 2：这个密钥只有 56 bit —— 实测确认

`"SVRIRUlNQQ=="` 看着像一串随机字符，实际上 **它是 Base64 编码的**：

```
$ echo -n "SVRIRUlNQQ==" | base64 -d
ITHEIMA          ← 培训机构的名字
字节数: 7  比特数: 56
```

而 HS256 要求密钥至少 **256 bit**。

**为什么它会被 Base64 解码？** 因为 jjwt 0.9.1 的 `signWith(SignatureAlgorithm, String)` 重载内部就是按 Base64 处理的。我反编译字节码确认了这一点：

```
$ javap -c io.jsonwebtoken.impl.DefaultJwtBuilder
  public JwtBuilder signWith(SignatureAlgorithm, java.lang.String);
      15: getstatic     #28  // Field io/jsonwebtoken/impl/TextCodec.BASE64
      19: invokeinterface #29 // InterfaceMethod TextCodec.decode:(Ljava/lang/String;)[B
```

所以传给它的字符串会被 `TextCodec.BASE64.decode()` 解码。**即使它不解码**，当成字面量也只有 12 字节 = 96 bit，同样远低于 256 bit。两条路都是弱的。

56 bit 的 HMAC 密钥在现代算力下是可以离线暴力破解的 —— 攻击者不需要碰你的服务器，拿一条自己签发的合法 token 就能在本地跑字典。破解出来之后，他就能伪造任意用户的身份。

### 问题 3：`static` 字段无法注入配置、无法测试

`private static String signKey` 意味着只能硬编码。既不能从配置读，也没法在单元测试里替换成测试密钥。

---

## 二、改动清单

| 文件 | 类型 | 说明 |
|---|---|---|
| [`utils/JwtProperties.java`](tlias-web-management/src/main/java/com/itheima/utils/JwtProperties.java) | 新增 | `@ConfigurationProperties` 配置类 |
| [`utils/JwtUtils.java`](tlias-web-management/src/main/java/com/itheima/utils/JwtUtils.java) | 重写 | 静态工具类 → Spring Bean，含密钥长度校验 |
| [`filter/TokenFilter.java`](tlias-web-management/src/main/java/com/itheima/filter/TokenFilter.java) | 修改 | 改为注入 Bean |
| [`interceptor/TokenInterceptor.java`](tlias-web-management/src/main/java/com/itheima/interceptor/TokenInterceptor.java) | 修改 | 改为注入 Bean（该类当前是死代码，见 P0-7） |
| [`service/impl/EmpServiceImpl.java`](tlias-web-management/src/main/java/com/itheima/service/impl/EmpServiceImpl.java) | 修改 | 改为注入 Bean |
| [`application.yml`](tlias-web-management/src/main/resources/application.yml) | 修改 | 新增 `tlias.jwt` 配置段 |
| `.env` / `.env.example` | 修改 | 存放密钥 + 生成方式说明 |

---

## 三、核心实现

### JwtProperties —— 走项目已有的配置类风格

项目里已经有 [`AliyunOSSProperties`](tlias-web-management/src/main/java/com/itheima/utils/AliyunOSSProperties.java) 这个 `@ConfigurationProperties` 范例，新的配置类照着它写，保持一致：

```java
@Data
@Component
@ConfigurationProperties(prefix = "tlias.jwt")
public class JwtProperties {
    private String signKey;                    // Base64 编码的密钥
    private Long expire = 43200000L;           // 默认 12 小时
}
```

### JwtUtils —— 改成 Bean + 启动时校验密钥强度

```java
@Component
public class JwtUtils {

    private static final int MIN_KEY_BYTES = 32;   // 256 bit

    private final SecretKey signKey;
    private final Long expire;

    public JwtUtils(JwtProperties jwtProperties) {
        this.signKey = buildSignKey(jwtProperties.getSignKey());
        this.expire = jwtProperties.getExpire();
    }

    private static SecretKey buildSignKey(String base64SignKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64SignKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT 签名密钥不是合法的 Base64 字符串, ...", e);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(String.format(
                    "JWT 签名密钥长度不足: HS256 要求至少 %d 字节(%d bit), 当前只有 %d 字节(%d bit), ...",
                    MIN_KEY_BYTES, MIN_KEY_BYTES * 8, keyBytes.length, keyBytes.length * 8));
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
```

**关键设计：弱密钥直接让应用启动失败（fail-fast）。**

弱密钥的危害是"平时完全看不出来"—— 系统跑得好好的，没人会发现签名可以被伪造。所以不能只是打个 warning 日志了事，必须让它**起不来**。宁可启动失败，也不能带着弱密钥上线。

### 一个技术细节：为什么没用 jjwt 自带的 `Keys.hmacShaKeyFor()`

`io.jsonwebtoken.security.Keys.hmacShaKeyFor()` 是 jjwt 提供的密钥校验工具，本来是最顺手的选择。但我写完之后 IDE 报错找不到这个类，一查发现：

```
$ unzip -l jjwt-0.9.1.jar | grep -oE "io/jsonwebtoken/[a-z]+/" | sort -u
io/jsonwebtoken/impl/
io/jsonwebtoken/jjwt/
io/jsonwebtoken/lang/          ← 只有这三个包
```

**jjwt 0.9.1 里根本没有 `security` 包**（那是 0.10+ 才有的）。所以改用纯 JDK API 实现：

- `java.util.Base64` 做解码
- `javax.crypto.spec.SecretKeySpec` 构造密钥
- 长度校验自己写（反而能给出更清楚的中文错误信息）

同时反编译确认了 `signWith(SignatureAlgorithm, Key)` 和 `setSigningKey(Key)` 这两个接收 `Key` 对象的重载在 0.9.1 里存在，所以不需要额外依赖。

> 这也说明一个道理：**不要凭记忆假设某个 API 存在**。我原本以为 0.9.1 有 `Keys` 类，实际验证才发现没有。

### 配置：密钥不进代码，走 `.env`

```yaml
#JWT配置
tlias:
  jwt:
    sign-key: ${JWT_SIGN_KEY}          #从 .env 读取
    expire: ${JWT_EXPIRE:43200000}     #毫秒, 默认12小时
```

`.env`（**已 gitignore，真实密钥不提交、本文档也不写出**）：
```
JWT_SIGN_KEY=<openssl rand -base64 32 生成的 256bit 密钥>
```

> 本文档本身会提交进公开仓库，所以**不能**把真实密钥写在这里 ——
> 否则密钥又会通过文档泄漏出去，前面所有的改动就白做了。
> 这个道理同样适用于任何"改造总结"类文档：脱敏要覆盖文档，不只是代码。

---

## 四、你问的：企业中 JWT 密钥是怎么生成的？

分几个层次答，因为"生成"和"管理"是两个问题。

### 4.1 生成：必须用密码学安全的随机源

| 算法 | 密钥类型 | 生成方式 |
|---|---|---|
| HS256/HS384/HS512 | 对称密钥 | `openssl rand -base64 32`（256bit）/ `48`（384bit）/ `64`（512bit） |
| RS256 | RSA 非对称 | `openssl genrsa -out private.pem 2048` |
| ES256 | 椭圆曲线 | `openssl ecparam -genkey -name prime256v1 -noout -out private.pem` |

也可以用代码生成，但**必须用 CSPRNG**：
```java
byte[] key = new byte[32];
new SecureRandom().nextBytes(key);           // Java：正确
```
```python
secrets.token_bytes(32)                      # Python：正确
```

**绝对不能用**：
- `Math.random()` —— 伪随机，种子可预测
- 时间戳 / 自增 ID / 手敲的字符串 —— 熵极低
- `UUID.randomUUID()` —— 虽然用的是 SecureRandom，但只有 122 bit 熵，且格式固定（能被穷举的搜索空间更小），不推荐当密钥

**长度要求**：HMAC 的密钥长度应 **≥ 哈希输出长度**。HS256 的哈希输出是 256 bit，所以密钥也要 ≥ 256 bit。用更长的密钥不会更安全（HMAC 会把超长密钥先哈希一次），但用更短的一定更不安全 —— 安全性被密钥长度封顶。

### 4.2 企业主流选择：非对称密钥（RS256 / ES256）

个人项目常用 HS256（对称），但**企业里更常见的是 RS256/ES256（非对称）**。核心原因是：

> 对称密钥的致命问题 —— **验证方必须持有密钥，而持有密钥就等于能签发 token**。

单体应用只有一个服务，无所谓。但企业是几十个微服务都要验 token，密钥就得复制到几十个地方，多一个副本就多一个泄漏面，而且任何一处泄漏都能伪造全局身份。

非对称密钥解决这个问题：
- **私钥**只存在于认证服务一处，负责签发
- **公钥**分发给所有需要验证的服务，**公钥泄漏无所谓** —— 拿公钥推不出私钥，伪造不了签名

配合 **JWKS**（`/.well-known/jwks.json`）端点做公钥的自动分发与轮换，验证方按需拉取。

### 4.3 密钥存哪里

这是企业实践和个人项目差距最大的地方：

| 方案 | 适用 |
|---|---|
| 环境变量 | 小团队、单体应用（**本项目就属于这档**） |
| **KMS**（阿里云/AWS KMS） | 密钥**永不落盘**，签名时调用 KMS API，密钥本身拿不到 |
| **Vault**（HashiCorp） | 动态密钥、租约 (lease)、自动轮换、完整审计 |
| K8s Secret + Sealed Secrets / External Secrets | 容器化部署的标准做法 |

绝对不做：写进代码、写进 `application.yml`、提交进 Git。

### 4.4 密钥轮换（rotation）—— 这才是关键差异点

**为什么必须轮换**：JWT 是无状态的，密钥一旦泄漏，**已经签发出去、还没过期的 token 全部无法撤销**。你不知道谁拿走了密钥、签发了什么。唯一能做的就是换密钥，让旧密钥签的 token 全部作废。

**怎么做到平滑轮换**（不能让所有用户突然掉线）：

靠 JWT header 里的 **`kid`（key id）**：

```
Header: { "alg": "RS256", "kid": "2026-09" }     ← 标明用了哪把密钥
```

1. 签发时用**最新的**密钥，并在 header 里写上它的 `kid`
2. 验证时**按 `kid` 去密钥表里找对应的公钥**（而不是只用一把固定密钥）
3. 于是新旧密钥可以**同时存在**：新 token 用新钥签，老 token 还能用旧钥验 —— 用户无感
4. 等旧 token 全部过期（超过 `expire` 时长）后，把旧密钥从表里下线

**频率**：定期轮换（常见 90 天）+ 紧急轮换预案（怀疑泄漏时立刻执行）。

### 4.5 补充：企业其实常常不用 JWT 做会话

这一点面试时很加分 —— 说明你知道 JWT 的边界：

JWT 的卖点是**无状态、可自验证**（服务端不用存 session），代价是**无法即时撤销**。用户点了"登出"、账号被封、改了密码 —— 旧 token 在过期前**依然有效**。

所以很多企业实际用的是 **不透明 token（opaque token）**：token 只是一串随机 ID，真实的会话状态存在 Redis 里。好处是能随时撤销、能看在线用户、能强制下线。

常见折中方案综合了两者：
- **JWT + Redis 黑名单**：登出时把该 token 的 `jti` 写进 Redis 黑名单（TTL 设为 token 剩余有效期），验证时先查黑名单。既保留了无状态，又补上了撤销能力。
- **短有效期 access token + 长有效期 refresh token**：access token 只活 15 分钟，泄漏了损失也有限；refresh token 存服务端可控。

JWT 最合适的场景其实是**服务间调用**（service-to-service），尤其是非对称签名 —— 资源服务只需持有公钥就能独立验证，不用回调认证中心。

### 4.6 本项目该怎么选（面试可以这么说）

这个项目是**单体应用**，不存在"多服务共享密钥"的问题，所以 **HS256 + 环境变量** 是合适的选择 —— 不是所有项目都该上 KMS，过度设计同样是问题。

**面试话术**：
> "这个项目是单体，用 HS256 + 环境变量就够了，密钥 256 bit、启动时强制校验长度。如果是多服务架构，我会换成 RS256 非对称密钥 —— 私钥只在认证服务，各业务服务只拿公钥验签，再配合 JWKS 端点做公钥分发和 `kid` 轮换。"

---

## 五、验证过程

### 5.1 正常路径

| # | 验证项 | 结果 |
|---|---|---|
| 1 | 应用启动 | ✅ `Started TliasWebManagementApplication in 1.892 seconds` |
| 2 | 登录签发新 token | ✅ 返回宋江 + 新 token |
| 3 | 携带新 token 访问 `/depts` | ✅ HTTP 200，返回部门数据 |
| 4 | 不带 token | ✅ 401 |
| 5 | 携带伪造 token | ✅ 401 |
| 6 | 后端日志有无异常 | ✅ 无 NPE、无 Exception |

**第 3 条额外验证了一件事**：`@WebFilter` 标注的过滤器**能不能用 `@Autowired` 注入 Bean**。

这一点我之前并不确定 —— `@WebFilter` 的实例是由 Servlet 容器（Tomcat）创建的，不是 Spring 容器，理论上可能注入不进去。实测结论是**可以**：因为如果注入失败，`jwtUtils` 会是 `null`，`jwtUtils.parseJWT(token)` 抛 NPE 后被过滤器自己的 `catch (Exception e)` 捕获，最终返回的是 **401**，而不是 200。既然返回 200，就说明 Bean 确实注入成功了。

### 5.2 失败路径 —— 弱密钥必须被拦住

把 `.env` 里的密钥**临时改回旧值** `SVRIRUlNQQ==`，重启：

```
[INFO] BUILD FAILURE

Caused by: java.lang.IllegalStateException: JWT 签名密钥长度不足:
HS256 要求至少 32 字节(256 bit), 当前只有 7 字节(56 bit), 生成方式见仓库根目录 .env.example
```

✅ 应用**拒绝启动**，8080 端口未被占用。

这条验证同时也证明了**新密钥确实生效了** —— 如果代码还在用旧密钥，应用根本起不来。也就是说"启动成功"本身就是新密钥已生效的证据。

验证完已把 `.env` 恢复为 256 bit 的正式密钥，并重新启动确认恢复正常。

---

## 六、本次**没有**做的事（范围说明）

### 6.1 没有升级 jjwt 版本

项目用的是 **jjwt 0.9.1（2018 年发布）**，相当老旧。它的问题：

- 缺少 `io.jsonwebtoken.security` 包（本次已实测确认）
- 依赖 `javax.xml.bind`、`javax.activation` 这些已经从 JDK 移除的模块，所以 pom 里被迫手工补了三个 JAXB 依赖
- 依赖 `javax.` 命名空间（Spring Boot 3 用的是 `jakarta.`），生态上已经脱节
- 新版 **0.12.x** 拆成 `jjwt-api` + `jjwt-impl` + `jjwt-jackson` 三个 artifact，API 也重新设计过（`Jwts.builder()...signWith(key)` 用单参数重载自动推断算法）

升级是**独立的改进项**，需要重写 `JwtUtils` 并移除那几个 JAXB 依赖，不适合混在这次改动里。建议单列一项，也可以等做到 P0-4（密码加密，要引 `spring-security-crypto`）时一起处理依赖。

### 6.2 没有做密钥轮换（kid 机制）

前面 4.4 讲的 `kid` 轮换，本项目没实现 —— 对一个单体练手项目来说，"能平滑轮换"没有实际收益，反而增加复杂度。**知道这个概念、能讲清楚原理**，对面试来说已经够了。

### 6.3 没有动 `TokenInterceptor`

它是死代码（[`WebConfig`](tlias-web-management/src/main/java/com/itheima/config/WebConfig.java) 里注册代码全被注释），但为了让项目能编译，我把它一起改成了注入 Bean 的写法。真正删除它属于 P0-7。

---

## 七、顺带发现

- **同一个密钥同时用于所有环境**。现在本地开发和生产（如果部署了）会用同一把密钥。规范做法是按环境隔离密钥 —— 开发环境的密钥泄漏了，不应该影响生产。这个等有部署环节时再处理。
- **项目现在有两套"从配置读敏感信息"的机制**：`.env`（Spring 读）+ 系统环境变量（OSS SDK 直接读）。后者写在 `.env` 里无效，这个坑已经记在 `.env.example` 的注释里了。

---

## 八、下一步

改动停在分支 `feature/p0-3-jwt-key` 上，**没有合并到 main，也没有推送**。

按清单顺序，接下来是 **P0-4：密码明文存储** —— 引入 `spring-security-crypto` 用 BCrypt 加密存库，登录时用 `matches()` 比对。这一项和本次改动同属"安全三连"，建议紧接着做。
