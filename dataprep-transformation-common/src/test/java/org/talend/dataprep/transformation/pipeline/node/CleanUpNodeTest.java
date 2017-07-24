package org.talend.dataprep.transformation.pipeline.node;

import static java.util.stream.Stream.empty;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.actions.common.RunnableAction;
import org.talend.dataprep.transformation.actions.context.ActionContext;
import org.talend.dataprep.transformation.actions.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.Runtimes;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;

public class CleanUpNodeTest {

    @Rule
    public Runtimes runtimes = new Runtimes();

    @Test
    public void testCleanUp() {
        // Given
        final TransformationContext transformationContext = runtimes.getExecutor().getContext();
        final ActionContext actionContext = transformationContext.create(new RunnableAction(), new RowMetadata());
        final AtomicInteger wasDestroyed = new AtomicInteger(0);

        Destroyable destroyable1 = new Destroyable() {

            @Override
            public void destroy() {
                wasDestroyed.incrementAndGet();
            }
        };
        Destroyable destroyable2 = new Destroyable() {

            @Override
            public void destroy() {
                wasDestroyed.incrementAndGet();
            }
        };
        actionContext.get("test1", p -> destroyable1);
        actionContext.get("test2", p -> destroyable2);
        final Node node = NodeBuilder
                .source(empty()) //
                .to(new BasicNode()) //
                .to(new CleanUpNode()) //
                .build();

        // when
        Runtimes.execute(node);

        // then
        assertEquals(2, wasDestroyed.get());
    }

    // Equivalent for a DisposableBean (has a public destroy() method).
    public interface Destroyable { // NOSONAR

        void destroy();
    }
}
