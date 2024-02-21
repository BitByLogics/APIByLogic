package net.justugh.japi.util.request;

import lombok.Getter;
import net.justugh.japi.util.Callback;

@Getter
public class JustAPIRequest {

    private final RequestType type;
    private final Callback<Object> callback;

    public JustAPIRequest(RequestType type, Callback<Object> callback) {
        this.type = type;
        this.callback = callback;
    }

    public enum RequestType {

        REDIS,
        HIKARI;

    }

}
