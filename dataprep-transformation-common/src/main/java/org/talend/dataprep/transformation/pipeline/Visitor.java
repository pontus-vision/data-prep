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

package org.talend.dataprep.transformation.pipeline;

import org.talend.dataprep.transformation.pipeline.link.BasicLink;
import org.talend.dataprep.transformation.pipeline.link.CloneLink;
import org.talend.dataprep.transformation.pipeline.link.ZipLink;
import org.talend.dataprep.transformation.pipeline.node.*;

public abstract class Visitor<T> {

    protected T doNodeVisit(Node node) {
        if (node != null && node.getLink() != null) {
            return node.getLink().accept(this);
        }
        return null;
    }

    public T visitAction(ActionNode actionNode) {
        return doNodeVisit(actionNode);
    }

    public T visitCompile(CompileNode compileNode) {
        return doNodeVisit(compileNode);
    }

    public T visitSource(SourceNode sourceNode) {
        return doNodeVisit(sourceNode);
    }

    public T visitBasicLink(BasicLink basicLink) {
        return basicLink.getTarget().accept(this);
    }

    public T visitPipeline(Pipeline pipeline) {
        return pipeline.getNode().accept(this);
    }

    public T visitStep(StepNode stepNode) {
        return doNodeVisit(stepNode);
    }

    public T visitCleanUp(CleanUpNode cleanUpNode) {
        return doNodeVisit(cleanUpNode);
    }

    public T visitLimit(LimitNode limitNode) {
        return doNodeVisit(limitNode);
    }

    public T visitFilterNode(FilterNode filterNode) {
        return doNodeVisit(filterNode);
    }

    public T visitNode(Node node) {
        return doNodeVisit(node);
    }

    public T visitZipLink(ZipLink zipLink) {
        return zipLink.getTarget().accept(this);
    }

    public T visitStatistics(StatisticsNode statisticsNode) {
        return doNodeVisit(statisticsNode);
    }

    public T visitInvalidDetection(InvalidDetectionNode invalidDetectionNode) {
        return doNodeVisit(invalidDetectionNode);
    }

    public T visitCloneLink(CloneLink cloneLink) {
        final Node[] nodes = cloneLink.getNodes();
        T lastNode = null;
        for (Node node : nodes) {
            lastNode = node.accept(this);
        }
        return lastNode;
    }

    public T visitTypeDetection(TypeDetectionNode typeDetectionNode) {
        return doNodeVisit(typeDetectionNode);
    }

    public T visitNToOne(NToOneNode nToOneNode) {
        return doNodeVisit(nToOneNode);
    }

    public T visitLocalSource(LocalSourceNode localSourceNode) {
        return doNodeVisit(localSourceNode);
    }

    public T visitCollector(CollectorNode collectorNode) {
        return doNodeVisit(collectorNode);
    }

    public T visitConsumerNode(ConsumerNode consumerNode) {
        return doNodeVisit(consumerNode);
    }

    public T visitWriterNode(WriterNode writerNode) {
        return doNodeVisit(writerNode);
    }

    public T visitCacheMetadata(CacheMetadataNode cacheMetadataNode) {
        return doNodeVisit(cacheMetadataNode);
    }

    public T visitMetadataEnforcer(MetadataEnforcerNode metadataEnforcerNode) {
        return doNodeVisit(metadataEnforcerNode);
    }

    public T visitDomainAndTypeEnforcer(DomainAndTypeEnforcerNode domainAndTypeEnforcerNode) {
        return doNodeVisit(domainAndTypeEnforcerNode);
    }

    public T visitSort(SortNode sortNode) {
        return doNodeVisit(sortNode);
    }
}
