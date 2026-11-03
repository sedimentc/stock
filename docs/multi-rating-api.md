# 多人评分 API（Java/MySQL）

当前已发布的共享站点运行在 Sites，使用 D1 数据库。其完整上线源码位于 `forotc/stock_data_table` 仓库的 `shared-site/` 目录。本次 Java 接口没有替换线上服务，也没有向现有数据库执行 SQL。

## Java 接口

- `POST /stockInfo/multiRating/list`：返回全部评分记录。
- `POST /stockInfo/multiRating/save`：按股票代码和评分人保存综合分、潜力分。

请求示例：

```json
{"tickerSymbol":"000001","raterName":"小雷","overallScore":5.5,"potentialScore":8}
```

支持人员：小雷、小梦、肖明、陈纪言。分数为0至10，最多一位小数；允许空值；两个分数均为null时删除该人员对该股票的记录。返回格式沿用项目的 `WebResult`。数据库主键为股票代码和评分人，各人记录独立。

本接口仅按姓名识别，与已接受的产品方案一致；调用方仍须自行部署所需的访问限制。

## 接入步骤

1. 在团队管理的 MySQL 测试库中检查并执行 `migrations/20261103_stock_multi_rating.sql`，该脚本仅新增评分表。
2. 使用项目原有 source1 数据源配置编译运行。
3. 验证两人独立保存、空值删除、零分保留、越界拒绝及数据库重启后数据持久化。
4. 前端若迁移回 Java 服务，需要将 Sites 的 `/api/ratings` 请求格式映射到本接口；这不是本次 Git 提交自动完成的部署步骤。

验证限制：开发电脑没有 Java/Maven 或 MySQL 测试实例，因此本次 Java 接口尚未进行编译和数据库集成测试。已上线并经过读写验证的是 Sites 版本。
