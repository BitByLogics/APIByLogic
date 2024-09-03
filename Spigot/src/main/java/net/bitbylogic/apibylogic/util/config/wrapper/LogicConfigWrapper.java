package net.bitbylogic.apibylogic.util.config.wrapper;

public interface LogicConfigWrapper {

    <T> String wrap(T object);

    <T, W> T unwrap(W wrappedObject);

}
