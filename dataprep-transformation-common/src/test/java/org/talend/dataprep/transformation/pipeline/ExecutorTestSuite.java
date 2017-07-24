package org.talend.dataprep.transformation.pipeline;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.talend.dataprep.transformation.pipeline.node.*;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ExecutorTest.class, //
        // Node tests
        BasicNodeTest.class, //
        CleanUpNodeTest.class, //
        ConsumerNodeTest.class, //
        FilterNodeTest.class, //
        InvalidDetectionNodeTest.class, //
        LimitNodeTest.class, //
        NToOneNodeTest.class, //
        PipelineTest.class })
public abstract class ExecutorTestSuite {

    @Test
    public void init() throws Exception {
    }
}
