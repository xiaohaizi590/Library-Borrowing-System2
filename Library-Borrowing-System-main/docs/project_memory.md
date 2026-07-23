# Library-Borrowing-System 项目记忆

## 技术栈
- Spring Boot 3.2.0 (Java 17)
- Spring Cloud 2023.0.1 + Alibaba Nacos
- Spring Data Redis / JPA / Security
- Jackson (Spring Boot 默认版本)
- Lombok

## 重要经验

### 1. Jackson Redis 序列化配置
`activateDefaultTyping` 在 Jackson 2.10+ 签名变更，必须传入 `PolymorphicTypeValidator`：
```java
objectMapper.activateDefaultTyping(
    BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("net.togogo")
            .allowIfSubType("java.util")
            .allowIfSubType("java.time")
            .build(),
    ObjectMapper.DefaultTyping.NON_FINAL);
```
- 旧版：`activateDefaultTyping(DefaultTyping)` 允许所有类型
- 新版：`activateDefaultTyping(PolymorphicTypeValidator, DefaultTyping)` 强制类型校验防 RCE

### 2. 分层架构原则
- **Controller** 只负责：接收请求、参数校验、返回响应
- **Service** 负责：业务逻辑（包括缓存操作）
- RedisTemplate 应在 Service 层注入，Controller 不应该直接操作缓存
- 验证码操作（生成、校验）应封装在 Service 层

### 3. Redis 原子操作
`GET → 比较 → DELETE` 三步操作存在竞态条件，应用 Lua 脚本在 Redis 服务端原子执行：
```lua
local stored = redis.call('GET', KEYS[1])
if not stored then return -1  end
if stored == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return 1
end
return 0
```
Spring Data Redis 通过 `DefaultRedisScript<Long>` + `redisTemplate.execute()` 执行。

### 4. Java 常见错误
- 实例方法误用类名调用：`ObjectMapper.registerModule(...)` 应为 `objectMapper.registerModule(...)`
- `@RequiredArgsConstructor` 误删导致 `final` 字段未初始化
- `@Autowired` 字段应优先改为构造器注入（配合 `@RequiredArgsConstructor`）
