# 图书管理系统

简洁的图书借阅管理系统，基于 Spring Boot（后端）与 Vue 3（前端）构建。提供用户注册/登录、权限控制、图书管理与借阅/归还流程，适合作为教学示例或小型内部系统。
添加软删除功能，做到删除后可恢复，保存记录到一定的天数
添加管理员维持功能，可以帮助普通用户修改密码
添加个人修改个人的密码的功能
这边要做到管理员界面能做到大多数的信息修改等（还得想想）

## 主要特性

- 用户注册、登录、权限（USER / ADMIN）
- JWT 认证与拦截器
- 图书增删改查、分页与搜索
- 借阅、归还、续借与逾期记录管理
- 后端使用 Spring Boot + JPA，前端使用 Vue 3 + Vite

## 技术栈（精简）

- Java 17、Spring Boot 3.x、Spring Security、Spring Data JPA
- MySQL 8.x
- Vue 3、Vite、TailwindCSS
- JWT（JJWT）、Lombok、Axios

## 快速开始

1. 准备依赖
   - 安装并启动 MySQL（示例使用 3306）
   - Node.js（用于前端）

2. 创建数据库

```sql
CREATE DATABASE nacon7_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 修改后端数据库配置（application.yml 或 application.properties）

- 数据库名：`nacon7_db`
- 用户名：`root`
- 密码：`123456`
- JDBC URL 示例：
  `jdbc:mysql://localhost:3306/nacon7_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true`

4. 启动顺序

- 启动 Nacos（如果使用配置中心）
- 启动 MySQL
- 启动后端：

```bash
mvn -pl server spring-boot:run
```

- 启动前端：

```bash
# 在项目根目录
npm install
npm run dev
```

默认后端地址： http://localhost:8081
前端开发地址： http://localhost:5173

## 初始化账号（示例）

- admin / admin123 （角色：ADMIN）
- user / user123 （角色：USER）

## 主要接口概览

- POST /api/users/register — 用户注册
- POST /api/users/login — 用户登录（返回 JWT）
- GET /api/books/getAll — 获取图书（分页）
- POST /api/books/borrow — 借阅（需登录，从 token 获取用户）
- POST /api/books/return/{recordId} — 归还（需登录）

（更多接口请参见代码中的 controller 文档）

## 常用命令

```bash
# 后端编译
mvn clean compile

# 启动后端
mvn -pl server spring-boot:run

# 前端安装依赖
npm install

# 启动前端开发服务器
npm run dev

# 构建前端生产包
npm run build
```

## 说明与待办

- 建议将密钥、数据库等敏感配置移到环境变量或配置中心（避免硬编码）//解决
- 可考虑为删除操作实现“软删除”以便恢复数据//待解决
- 生产环境需做安全加固、并发压测与错误日志收集//已加固，未压测，未添加日志收集
