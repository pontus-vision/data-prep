package org.talend.dataprep.transformation.pipeline;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.node.CollectorNode;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorRunnable;
import org.talend.dataprep.transformation.pipeline.runtime.ExecutorVisitor;

public class Runtimes implements TestRule, Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runtimes.class);

    private static ExecutorVisitor<Object> executorVisitor;

    private final AvailableRuntimes[] runtimes;

    public Runtimes() {
        runtimes = new AvailableRuntimes[] { AvailableRuntimes.BEAM, AvailableRuntimes.JAVA };
    }

    public Runtimes(AvailableRuntimes... runtimes) {
        this.runtimes = runtimes;
    }

    public static List<DataSetRow> execute(Node node) {
        // Use executor
        final Node resultPipeline = node.accept(executorVisitor);

        final Runnable runnable = executorVisitor.toRunnable();
        runnable.run();

        final List<DataSetRow> collected = new ArrayList<>();
        resultPipeline.accept(new Visitor<List<DataSetRow>>() {

            @Override
            public List<DataSetRow> visitCollector(CollectorNode collectorNode) {
                collected.addAll(collectorNode.collect());
                return super.visitCollector(collectorNode);
            }
        });
        return collected;
    }

    public static ExecutorVisitor<Object> getCurrent() {
        return executorVisitor;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                ServiceLoader<ExecutorVisitor> visitors = ServiceLoader.load(ExecutorVisitor.class);
                Iterator<ExecutorVisitor> iterator = visitors.iterator();

                if (!iterator.hasNext()) {
                    LOGGER.warn("No executor found in current classpath. Consider defining a file " + //
                    "'META-INF/services/org.talend.dataprep.transformation.pipeline.runtime.ExecutorVisitor'" + //
                    " with implementation you wish to test.");

                    executorVisitor = new ExecutorVisitor<Object>() {

                        @Override
                        public Object getResult() {
                            return new Object();
                        }

                        @Override
                        public ExecutorRunnable toRunnable() {
                            return new ExecutorRunnable() {

                                @Override
                                public void signal(Signal signal) {
                                }

                                @Override
                                public void run() {
                                }
                            };
                        }
                    };
                    base.evaluate();
                    return;
                }

                while (iterator.hasNext()) {
                    executorVisitor = iterator.next();
                    final String className = executorVisitor.getClass().getName();
                    final Optional<AvailableRuntimes> match = Stream
                            .of(runtimes) //
                            .filter(r -> Objects.equals(className, r.className)) //
                            .findFirst();
                    if (match.isPresent()) {
                        if (runtimes.length > 1) {
                            System.out.println("-> (" + match.get() + ") " + description.getDisplayName());
                        }
                        base.evaluate();
                        if (runtimes.length > 1) {
                            System.out.println("<- (" + match.get() + ") " + description.getDisplayName());
                        }
                    } else {
                        LOGGER.debug("Runtime '{}' is not enabled.", className);
                    }
                }
            }
        };
    }

    public ExecutorVisitor<Object> getExecutor() {
        return executorVisitor;
    }

    public static List<DataSetRow> execute(Node node, AvailableRuntimes runtime) {
        if (matches(runtime)) {
            // Use executor
            final Node resultPipeline = node.accept(executorVisitor);

            final Runnable runnable = executorVisitor.toRunnable();
            runnable.run();

            final List<DataSetRow> collected = new ArrayList<>();
            resultPipeline.accept(new Visitor<List<DataSetRow>>() {

                @Override
                public List<DataSetRow> visitCollector(CollectorNode collectorNode) {
                    collected.addAll(collectorNode.collect());
                    return super.visitCollector(collectorNode);
                }
            });
            return collected;
        } else {
            return Collections.emptyList();
        }
    }

    public static boolean matches(AvailableRuntimes runtime) {
        return executorVisitor.getClass().getName().equals(runtime.className);
    }

    public enum AvailableRuntimes {
        BEAM("org.talend.dataprep.transformation.pipeline.runtime.BeamRuntime"),
        JAVA("org.talend.dataprep.transformation.pipeline.runtime.JavaRuntime"),
        UNKNOWN("");

        private String className;

        AvailableRuntimes(String className) {
            this.className = className;
        }
    }
}
