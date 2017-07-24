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

package org.talend.dataprep.transformation.pipeline.builder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Test;
import org.talend.daikon.exception.TalendRuntimeException;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.pipeline.Node;
import org.talend.dataprep.transformation.pipeline.TestLink;
import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.node.BasicNode;
import org.talend.dataprep.transformation.pipeline.node.FilterNode;
import org.talend.dataprep.transformation.pipeline.node.LocalSourceNode;
import org.talend.dataprep.transformation.pipeline.node.NToOneNode;

public class NodeBuilderTest {

    @Test
    public void default_first_node_should_be_SourceNode() {
        // when
        final Node node = NodeBuilder.source(Stream.empty()).build();

        // then
        assertThat(node, instanceOf(LocalSourceNode.class));
    }

    @Test
    public void should_take_provided_node_as_source() {
        // given
        final Node source = new BasicNode();

        // when
        final Node node = NodeBuilder.from(source).build();

        // then
        assertThat(node, is(source));
    }

    @Test
    public void should_create_filtered_node_as_source() {
        // given
        final Predicate<DataSetRow> predicate = (dataSetRow) -> true;

        // when
        final Node node = NodeBuilder.filteredSource(predicate, Stream.empty()).build();

        // then
        assertThat(node, instanceOf(LocalSourceNode.class));
        assertThat(node.getLink().getTarget(), instanceOf(FilterNode.class));
    }

    @Test
    public void should_append_node_with_basic_link() {
        // given
        final Node nextNode = new BasicNode();

        // when
        final Node node = NodeBuilder.source(Stream.empty()).to(nextNode).build();

        // then
        assertThat(node.getLink(), instanceOf(BasicLink.class));
        assertThat(node.getLink().getTarget(), is(nextNode));
    }

    @Test
    public void should_append_node_with_provided_link() {
        // given
        final Node nextNode = new BasicNode();

        // when
        final Node node = NodeBuilder.source(Stream.empty()).to(TestLink::new, nextNode).build();

        // then
        assertThat(node.getLink(), instanceOf(TestLink.class));
        assertThat(node.getLink().getTarget(), is(nextNode));
    }

    @Test
    public void should_append_a_pipeline() {
        // given
        final Node firstNode = new BasicNode();
        final Node secondNode = new BasicNode();
        final Node nodeToAppend = new BasicNode();

        final Node pipeline = NodeBuilder.from(firstNode).to(secondNode).build();

        // when
        final Node node = NodeBuilder.from(pipeline).to(nodeToAppend).build();

        // then
        assertTrue(secondNode.getLink() != null);
        assertTrue(secondNode.getLink().getTarget() == nodeToAppend);
        assertTrue(pipeline == node);
    }

    @Test
    public void should_append_clone_link_then_zip_to_a_common_node() {
        // given
        final Node firstNode = new BasicNode();
        final Node branch1 = new BasicNode();
        final Node branch2 = new BasicNode();

        // when
        final Node node = NodeBuilder.from(firstNode).dispatch(branch1, branch2).zip(rows -> rows[0]).build();

        // then
        assertThat(node, is(firstNode));
        assertThat(node.getLink(), instanceOf(CloneLink.class));
        assertThat(((CloneLink) node.getLink()).getNodes(), arrayContaining(branch1, branch2));
        assertThat(branch1.getLink(), instanceOf(BasicLink.class));
        assertThat(branch2.getLink(), instanceOf(BasicLink.class));
        assertEquals(NToOneNode.class, branch1.getLink().getTarget().getClass());
        assertEquals(NToOneNode.class, branch2.getLink().getTarget().getClass());
        assertTrue(branch1.getLink().getTarget() == branch2.getLink().getTarget());
    }

    @Test(expected = TalendRuntimeException.class)
    public void shouldNotDispatchToNoNode() {
        // given
        final Node firstNode = new BasicNode();

        // when
        NodeBuilder.from(firstNode).dispatch();
    }
}
