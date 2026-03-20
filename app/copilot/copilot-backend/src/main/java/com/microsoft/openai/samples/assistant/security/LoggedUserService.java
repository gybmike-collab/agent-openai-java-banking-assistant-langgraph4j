package com.microsoft.openai.samples.assistant.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class LoggedUserService {

    public LoggedUser getLoggedUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //在 PoC 代码中这始终为 true
        if(authentication == null) {
           return getDefaultUser();
        }
        //该代码在 PoC 中不会被执行，用作未来与认证提供方集成后的扩展钩子。
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUserName = authentication.getName();

            Object details = authentication.getDetails();
            //需要根据认证提供方将对象转换为对应的具体类型。
            return new LoggedUser(currentUserName, "changeme@contoso.com", "changeme", "changeme");
        }
        return getDefaultUser();
    }

    private LoggedUser getDefaultUser() {
        return new LoggedUser("bob.user@contoso.com", "bob.user@contoso.com", "generic", "Bob The User");
    }
}
