package com.microsoft.openai.samples.assistant.langgraph4j;

import java.util.Arrays;
import java.util.List;

public enum Intent {
    TransactionHistoryAgent,
    AccountAgent,
    PaymentAgent;

    /**
     * 返回所有 {@code Intent} 枚举常量可用的名称列表。
     *
     * @return 包含 {@code Intent} 枚举常量名称的不可修改列表
     */
    public static List<String> names() {
        return Arrays.stream(Intent.values())
                .map(Enum::name)
                .toList();
    }
}
