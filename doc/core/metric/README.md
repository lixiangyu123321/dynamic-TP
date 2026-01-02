# Metric 模块文档

`core/metric` 目录包含了框架的指标计算相关功能，包括计量器、摘要、计数器等。

## 目录结构

```
metric/
├── Meter.java                      # 计量器接口
├── Summary.java                     # 摘要接口
├── MMACounter.java                  # 移动平均计数器
├── MMAPCounter.java                 # 移动平均百分比计数器
└── LimitedUniformReservoir.java     # 有限均匀采样器
```

## 模块说明

### 核心接口

- **[Meter](Meter.md)**: 计量器接口，提供重置功能
- **[Summary](Summary.md)**: 摘要接口，提供值添加功能

### 实现类

- **[MMACounter](MMACounter.md)**: 移动平均计数器
- **[MMAPCounter](MMAPCounter.md)**: 移动平均百分比计数器
- **[LimitedUniformReservoir](LimitedUniformReservoir.md)**: 有限均匀采样器

## 快速导航

- 想了解计量器？查看 [Meter](Meter.md)
- 想了解摘要？查看 [Summary](Summary.md)
- 想了解移动平均？查看 [MMACounter](MMACounter.md)

