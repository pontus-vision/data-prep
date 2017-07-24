The purpose of this module is too provide a common model for transformation independent of runtime execution.

The main unit for pipeline model element is `Node` and there is a node per type of actions:
* `ActionNode`: this represents the Data Preparation application of an action (upper case, split...).
* `CompileNode`: this represents the Data Preparation initial application of an action. Compilation can include checks of parameters and eventually cancel application of the action.
* `CleanUpNode`: this is a node that represents any clean up of resources of the end of the transformation.
* `CacheMetadataNode`: this node represents a step where the received metadata should be cached.
* `ConsumerNode`: this node allows custom actions on rows using lambdas (see note about lambdas).
* `FilterNode`: this node allows to filters rows based on predicates (see note about lambdas).
* `InvalidDetectionNode`: this node should detect invalid values in rows.
* `LimitNode`: this node limits the number of rows (e.g. the first 50 rows).
* `NToOneNode`: this node reduces the _n_ nodes it has as input into a single row.
* `Pipeline`: this node represents a root node in a pipeline.
* `ProgressNode`: a node to notify progress (number of rows, rows/sec...).
* `StatisticsNode`: a node to represents statistics computation.
* `StepNode`: a node that groups together nodes dedicated to processing a single step in a preparation. 
* `TypeDetectionNode`: a node that indicates type detection should be performed on received rows.

To clarify (1)
* `DomainAndTypeEnforcerNode`: this node allows to override row's metadata.
* `MetadataEnforcerNode`: this node allows to override row's metadata.


Terminal nodes
* `WriterNode`: This is a node that represents the output of the transformation pipeline.
* `CollectorNode`: This is a node that can be used in tests to collect all rows that went through the pipeline.

Source nodes
* `SourceNode`: represents the input of a pipeline (a connection to a data set service)
* `LocalSourceNode`: represents a `Stream<DataSetRow>` to be used as input. Useful for unit tests.

Utility nodes
* `BasicNode` A placeholder and a basic node implementation to be specialized.
* `CollectorNode` A node to use to collect results in memory (not be used outside of unit tests).
* `NoOpNode` A node to indicate no action on row (should pass row as is to the next step, if any).

A pipeline in a unit test looks like:
```
LocalSource -> ... -> Collector
```

### About Lambdas
When used lambdas **must** be serializable (in `java.io.Serializable` way).