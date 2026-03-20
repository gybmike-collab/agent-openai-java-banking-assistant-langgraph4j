// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.assistant.langgraph4j;

import dev.langchain4j.data.message.ChatMessage;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.*;

public class AgentWorkflowState extends MessagesState<ChatMessage>  {


    //Jackson 序列化所需
    public AgentWorkflowState() {
        super( Map.of() );
    }

    public AgentWorkflowState(Map<String, Object> initData ) {
        super( initData );
    }

    public Optional<String> nextAgent() {
           return value("nextAgent");
    }

}
