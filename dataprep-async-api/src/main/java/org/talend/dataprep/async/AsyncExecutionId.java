// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.async;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.talend.dataprep.transformation.pipeline.node.AsyncExecution;

/**
 * Annotation to indicate the {@link AsyncExecution#getId()} value for the created execution. This annotation is
 * especially useful if you want to reuse a previously created execution (for resuming pre-created executions).
 * 
 * @see AsyncOperation
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncExecutionId {

}
