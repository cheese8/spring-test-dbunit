package com.github.springtestdbunit.annotation;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Repeatable(Exports.class)
public @interface Export {

	String connection() default "";

	String tableName() default "";

	String fileName() default "";

	String format() default "xml";

	String query() default "";
}
