package net.bitbylogic.apibylogic.config;

public interface LogicConfigWrapper {

    <T> String wrap(T object);

    <T, W> T unwrap(W wrappedObject);

}
