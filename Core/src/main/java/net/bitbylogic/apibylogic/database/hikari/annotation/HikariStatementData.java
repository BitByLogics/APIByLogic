package net.bitbylogic.apibylogic.database.hikari.annotation;

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

    String processorID() default "";

}
