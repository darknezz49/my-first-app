package com.example.myfirstapp.ui

enum class SortType(val title: String) {
    DEFAULT("默认顺序 (最新添加)"),
    PRICE_ASC("价格 (由低到高)"),
    PRICE_DESC("价格 (由高到低)"),
    TIME_ASC("陪伴时长 (由短到长)"),
    TIME_DESC("陪伴时长 (由长到短)")
}
