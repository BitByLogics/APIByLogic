package net.bitbylogic.apibylogic.util.request;

import lombok.Getter;

import java.util.function.Consumer;

@Getter
public class LogicRequest {

    private final RequestType type;
    private final Consumer<Object> callback;

    public LogicRequest(RequestType type, Consumer<Object> callback) {
        this.type = type;
        this.callback = callback;
    }

    public enum RequestType {
        REDIS,
        HIKARI;
    }

}
