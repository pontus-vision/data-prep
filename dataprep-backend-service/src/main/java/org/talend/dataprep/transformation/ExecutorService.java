package org.talend.dataprep.transformation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.talend.dataprep.transformation.pipeline.runtime.BeamRuntime;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorVisitor;
import org.talend.dataprep.transformation.pipeline.runtime.JavaRuntime;

@Component
public class ExecutorService {

    @Value("${executor.service:java}")
    // @Value("{executor.service:java}")
    private String type;

    /**
     * @return The {@link ExecutorVisitor} instance to transform a
     * {@link org.talend.dataprep.transformation.pipeline.node.Pipeline pipeline} (or a
     * {@link org.talend.dataprep.transformation.pipeline.Node node} into a
     * {@link org.talend.dataprep.transformation.pipeline.runtime.ExecutorRunnable runnable}.
     *
     * @see org.talend.dataprep.transformation.pipeline.Node
     * @see org.talend.dataprep.transformation.pipeline.runtime.ExecutorRunnable
     */
    public ExecutorVisitor<?> getExecutor() {
        switch (type.toUpperCase()) {
        case "BEAM":
            return new BeamRuntime();
        case "JAVA":
            return new JavaRuntime();
        default:
            throw new UnsupportedOperationException("Unsupported executor type: '" + type + "'.");
        }
    }
}
