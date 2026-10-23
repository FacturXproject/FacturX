package com.facturx.app.websocket;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket //ce insemna
public class WebSocketConfig implements WebSocketConfigurer{
    private final DocumentWebSocketHandler handler;
    public WebSocketConfig(DocumentWebSocketHandler handler)
    {
        this.handler = handler;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registery)
    {
        registery.addHandler(handler, "/ws")
            .setAllowedOrigins("https://localhost:8443");
    }
}
