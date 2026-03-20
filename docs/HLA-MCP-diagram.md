# HLA-MCP 架构图（Mermaid）

与 `assets/HLA-MCP.png` 一致的分层关系，图源便于在 [mermaid.live](https://mermaid.live) 或 VS Code 中预览。

**读图方式：** 上面一条横排是「用户 → Copilot → 业务 API」；虚线向下连 **Azure AI**。具体 MCP 工具名见本文末尾表格。

---

## 总览

```mermaid
%%{init: {
  'theme': 'dark',
  'flowchart': {
    'curve': 'step',
    'padding': 32,
    'nodeSpacing': 32,
    'rankSpacing': 64
  }
}}%%
flowchart TB

  subgraph L1[" "]
    direction LR

    subgraph G_USERS["Users"]
      U(["个人银行 · 交易 · 发票<br/>提问 / 任务"])
    end

    subgraph G_COP["Copilot App · Java · Langchain4j"]
      direction TB
      S(["Supervisor · gpt-4o<br/>理解意图 · 路由"])
      subgraph G_AG["Domain agents · gpt-4o"]
        direction TB
        A["Account"]
        T["Transactions"]
        P["Payments"]
      end
      N_MCP{{"MCP 通道<br/>HTTP / SSE"}}
      S -->|AccountInfo| A
      S -->|Transactions| T
      S -->|PayInvoice · Repeat| P
      A & T & P --> N_MCP
    end

    subgraph G_BIZ["Existing Business APIs"]
      direction TB
      N_IN{{"MCP Tools"}}
      acc["Account Service"]
      pay["Payments Service"]
      rpt["Reporting Service"]
      N_IN --> acc & pay & rpt
    end

  end

  subgraph G_AZ["Azure AI Services"]
    direction LR
    oai["Azure OpenAI<br/>GPT"]
    di["Document Intelligence<br/>发票实体"]
  end

  U --> S
  N_MCP <-->|HTTP / SSE| N_IN
  S & A & T & P -.->|Chat Completions| oai
  P -.->|ScanInvoice| di

  classDef cU fill:#3949ab,stroke:#9fa8da,color:#fff,stroke-width:2px
  classDef cC fill:#00695c,stroke:#80cbc4,color:#fff,stroke-width:2px
  classDef cB fill:#33691e,stroke:#c5e1a5,color:#fff,stroke-width:2px
  classDef cA fill:#4a148c,stroke:#e1bee7,color:#fff,stroke-width:2px
  classDef cN fill:#37474f,stroke:#90a4ae,color:#eceff1,stroke-width:2px

  class U cU
  class S,A,T,P cC
  class N_MCP cN
  class N_IN,acc,pay,rpt cB
  class oai,di cA
```

> **布局仍乱时：** 可在 `flowchart` 中尝试加入 `"defaultRenderer": "elk"`（需预览器支持，[mermaid.live](https://mermaid.live) 通常可用）。

---

## MCP 工具对照（与图中 API 区块一致）

| 服务 | MCP / 能力 |
|------|------------|
| **Account Service** | `getAccountByUsername`，`getAccountDetails`，`getPaymentMethods`，`getRegisteredBeneficiaries` |
| **Payments Service** | `submitPayment` |
| **Reporting Service** | `notifyTransaction`，`searchTransactions`，`getTransactionByRecipient` |

智能体侧 **API1** 一般指 Account Service；**API2** Payments；**API3** Reporting；**ScanInvoice** 走 Document Intelligence。
