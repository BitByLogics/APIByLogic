package net.bitbylogic.apibylogic.util.request;

import lombok.Getter;
import net.bitbylogic.apibylogic.util.Callback;

@Getter
public class LogicRequest {

    private final RequestType type;
    private final Callback<Object> callback;

    public LogicRequest(RequestType type, Callback<Object> callback) {
        this.type = type;
        this.callback = callback;
    }

    public enum RequestType {

        REDIS,
        HIKARI;

    }

}
