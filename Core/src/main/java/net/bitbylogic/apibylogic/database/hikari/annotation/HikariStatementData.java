package net.bitbylogic.apibylogic.database.hikari.annotation;

import net.bitbylogic.apibylogic.database.hikari.processor.HikariFieldProcessor;
import net.bitbylogic.apibylogic.database.hikari.processor.impl.DefaultHikariFieldProcessor;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface HikariStatementData {

    String dataType() default "";

    String columnName() default "";

    boolean allowNull() default true;

    boolean autoIncrement() default false;

    boolean primaryKey() default false;

    boolean updateOnSave() default true;

    boolean subClass() default false;

    Class<? extends HikariFieldProcessor<?>> processor() default DefaultHikariFieldProcessor.class;

    String foreignTable() default "";

    boolean foreignDelete() default false;

}
