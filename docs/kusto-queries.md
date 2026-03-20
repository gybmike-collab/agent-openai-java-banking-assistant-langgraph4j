# 用于检查工具调用的实用 Kusto 查询
默认情况下，所有 Azure OpenAI 的请求与响应都会记录在 Java 应用日志中。
可使用下列 Kusto 查询检查工具调用的请求与响应。


### 检查所有 Azure OpenAI 响应
```kusto
traces
| where cloud_RoleName == "copilot-api"
| where message contains "openai.azure.com/openai/deployments"
| extend chatmessage=parse_json(message)
| where chatmessage["az.sdk.message"] == "HTTP response"
| extend chatbody=parse_json(tostring(chatmessage.body)).choices[0]
```
### 检查所有 Azure OpenAI 请求
```kusto
traces 
| where cloud_RoleName == "copilot-api"
| where message contains "openai.azure.com/openai/deployments"
| extend chatrequest=parse_json(message)
| where chatrequest.method == "POST" 
| extend chatbody=parse_json(tostring(parse_json(message).body))
```

### 检查所有工具调用请求
```kusto
traces
| where cloud_RoleName == "copilot-api"
| where message contains "openai.azure.com/openai/deployments"
| extend chatrequest=parse_json(message)
| where chatrequest["az.sdk.message"] == "HTTP response"
| extend response=parse_json(tostring(parse_json(tostring(chatrequest.body)))).choices[0]
| where response.finish_reason=="tool_calls"
| extend tool_calls=parse_json(tostring(parse_json(tostring(response.message)).tool_calls))
```

### 检查 TransactionHistoryPlugin 函数的工具调用请求
```kusto
traces 
| where cloud_RoleName == "copilot-api"
| where message contains "openai.azure.com/openai/deployments"
| extend chatrequest=parse_json(message)
| where chatrequest["az.sdk.message"] == "HTTP response" 
| extend response=parse_json(tostring(parse_json(tostring(chatrequest.body)))).choices[0]
| where response.finish_reason=="tool_calls" //and response.message contains "TransactionHistoryPlugin"
| extend tool_calls=parse_json(tostring(parse_json(tostring(response.message)).tool_calls))
```