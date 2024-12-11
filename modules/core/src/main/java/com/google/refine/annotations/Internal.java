
package com.google.refine.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation signifying that the given class or method is only provided for internal use (inside OpenRefine itself) and
 * should not be used by extensions, as it could change or be removed without notice.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PACKAGE })
public @interface Internal {

    // the date or version number since this element has been marked as internal
    public String since();
}
