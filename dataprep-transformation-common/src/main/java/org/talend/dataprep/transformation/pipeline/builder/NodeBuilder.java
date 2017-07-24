// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.pipeline.builder;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.dataprep.BaseErrorCodes;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Link;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public class NodeBuilder {

    private final Node sourceNode;

    private State state;

    /**
     * Constructor from an initial node as source
     */
    private NodeBuilder(Node sourceNode) {
        this.sourceNode = sourceNode;
        state = new NodeState(sourceNode);
    }

    /**
     * Set a SourceNode as initial node.
     * This SourceNode is a BasicNode but logged as source
     */
    public static NodeBuilder source() {
        return new NodeBuilder(new NoOpNode());
    }

    /**
     * Set a SourceNode as initial node using a url. This source is compatible with serialization based executors (as
     * opposed to {@link #source(Stream)}.
     *
     * @param url The url that returns a content compliant with {@link org.talend.dataprep.api.dataset.DataSet}.
     */
    public static NodeBuilder source(String url) {
        return new NodeBuilder(new SourceNode(url));
    }

    /**
     * Set a SourceNode as initial node using local object instances. Using this makes pipeline <b>not</b> serializable,
     * so
     * should be used in tests or when you're sure execution code will run locally.
     *
     * @param localSource A {@link Stream} of records.
     */
    public static NodeBuilder source(Stream<DataSetRow> localSource) {
        return new NodeBuilder(new LocalSourceNode(localSource));
    }

    /**
     * Create a new builder from a provided source node
     */
    public static NodeBuilder from(Node node) {
        return new NodeBuilder(node);
    }

    /**
     * Create a new builder from a filter node as a source
     */
    public static NodeBuilder filteredSource(Predicate<DataSetRow> filter, String url) {
        return new NodeBuilder(new SourceNode(url)).to(new FilterNode(new FilterWrapper(filter)));
    }

    /**
     * Create a new builder from a filter node as a source
     */
    public static NodeBuilder filteredSource(Predicate<DataSetRow> filter, Stream<DataSetRow> localSource) {
        return new NodeBuilder(new LocalSourceNode(localSource)).to(new FilterNode(new FilterWrapper(filter)));
    }

    /**
     * Create a new builder from a filter node as a source
     */
    public static NodeBuilder filteredSource(Predicate<DataSetRow> filter, FilterNode.Behavior behavior, Stream<DataSetRow> localSource) {
        return new NodeBuilder(new LocalSourceNode(localSource)).to(new FilterNode(behavior, new FilterWrapper(filter)));
    }

    /**
     * Append a new node at the end, with a basic link
     */
    public NodeBuilder to(final Node node) {
        final Function<Node, Link> linkFunction = BasicLink::new;
        return this.to(linkFunction, node);
    }

    /**
     * Append a new node at the end, with a custom link
     *
     * @param linkFunction A function that create the link from the previous nodes
     * @param node The new node to append
     */
    public NodeBuilder to(final Function<Node, Link> linkFunction, final Node node) {
        state = state.next(linkFunction);
        state = state.next(node);
        return this;
    }

    public NodeBuilder zip(final Function<DataSetRow[], DataSetRow> rowFunction) {
        final NToOneNode target = new NToOneNode(state.getSourceCount(), rowFunction);
        state = state.next(target);

        return this;
    }

    /**
     * Create a multi pipeline.
     * The previous node will dispatch its input to all the provided nodes via a CloneLink
     */
    public NodeBuilder dispatch(final Node... nodes) {
        if (nodes == null || nodes.length == 0) {
            throw new TalendRuntimeException(BaseErrorCodes.UNEXPECTED_EXCEPTION,
                    new IllegalArgumentException("Each dispatch() must be followed by nodes()."));
        }
        state = state.next(CloneLink::new);
        state = state.next(nodes);
        return this;
    }

    /**
     * Build the node pipeline
     *
     * @return The first node
     */
    public Node build() {
        return sourceNode;
    }

    private interface State {

        State next(Function<Node, Link> link);

        State next(Node node);

        State next(Node... nodes);

        int getSourceCount();

        Node getNode();
    }

    private static class LinkState implements State {

        private final Node previousNode;

        private final Function<Node, Link> linkFunction;

        private LinkState(Node previousNode, Function<Node, Link> linkFunction) {
            this.previousNode = previousNode;
            this.linkFunction = linkFunction;
        }

        @Override
        public State next(Function<Node, Link> link) {
            throw new IllegalStateException();
        }

        @Override
        public State next(Node node) {
            previousNode.setLink(linkFunction.apply(node));
            return new NodeState(node);
        }

        @Override
        public Node getNode() {
            return previousNode;
        }

        @Override
        public State next(Node... nodes) {
            previousNode.setLink(new CloneLink(nodes));

            Node[] zipTargets = new Node[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                Node current = nodes[i];
                while (current.getLink() != null) {
                    current = current.getLink().getTarget();
                }
                zipTargets[i] = current;
            }

            return new ZipLinkState(zipTargets);
        }

        @Override
        public int getSourceCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "Waiting for next node...";
        }
    }

    private static class ZipLinkState implements State {

        private Node[] sources;

        private ZipLinkState(Node[] sources) {
            this.sources = sources;
        }

        @Override
        public State next(Function<Node, Link> link) {
            throw new IllegalStateException();
        }

        @Override
        public State next(Node node) {
            for (Node source : sources) {
                source.setLink(new BasicLink(node));
            }
            return new NodeState(node);
        }

        @Override
        public Node getNode() {
            return sources[0];
        }

        @Override
        public State next(Node... nodes) {
            throw new IllegalStateException();
        }

        @Override
        public int getSourceCount() {
            return sources.length;
        }

        @Override
        public String toString() {
            return "Waiting for a zip target...";
        }
    }

    private static class NodeState implements State {

        private final Node node;

        private NodeState(Node node) {
            this.node = node;
        }

        @Override
        public State next(Function<Node, Link> link) {
            Node current = node;
            while (current.getLink() != null) {
                current = current.getLink().getTarget();
            }
            return new LinkState(current, link);
        }

        @Override
        public State next(Node node) {
            throw new IllegalStateException();
        }

        @Override
        public Node getNode() {
            return node;
        }

        @Override
        public State next(Node... nodes) {
            throw new IllegalStateException();
        }

        @Override
        public int getSourceCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "Waiting for next link...";
        }

    }

    private static class FilterWrapper implements Predicate<DataSetRow>, Serializable {

        private final Predicate<DataSetRow> filter;

        private FilterWrapper(Predicate<DataSetRow> filter) {
            this.filter = filter;
        }

        @Override
        public boolean test(DataSetRow dataSetRow) {
            return filter.test(dataSetRow);
        }
    }
}
