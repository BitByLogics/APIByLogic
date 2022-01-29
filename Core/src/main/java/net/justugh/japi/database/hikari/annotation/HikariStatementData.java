package net.justugh.japi.database.hikari.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface HikariStatementData {

    String dataType();

    boolean allowNull() default false;
    boolean autoIncrement() default false;

}
