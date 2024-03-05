#    !/bin/python3
# stores the number of vertices in the graph
from resource_graph import ResourceGraph, Topology_Profiler, Operator, MergedOperator


def operator_rank(op: Operator, resourceGraph: ResourceGraph) -> int:
    res = [resourceGraph.calculateNodeScore(i, op) for i in range(resourceGraph.getVertexNum())] 
    print("operator side ranking: ", res)
    print("the choice is ", res.index(min(res)), "the score is ", min(res))
    return res.index(min(res))

def node_ranking(operator_side, resourceGraph) -> map:
    nodes = set(operator_side.values())
    node_side = {}
    for node in nodes:
        node_side[node] = []
        for op, val in operator_side.items():
            if val == node:
                score = resourceGraph.calculateOpScore(node, op)
                node_side[node].append((score, op))
        node_side[node] = sorted(node_side[node], key=lambda x: -x[0])
    print("operator_side and node side: ", operator_side, node_side)
    return node_side

# coda based method
def schedule2(topologies: list, resourceGraph: ResourceGraph) -> None:
    # get the longest topology 
    # directly put Spout into the target Node and update node information. 
    result = {}
    for topology in topologies:
        for operator in topology.getLayer():
            resourceGraph.updateGraph(operator.getNodeId(), operator)
        topology.nextLayer()
    # directly allocate Sink into the target node. We assume only has one sink in each topology. 
    for topology in topologies:
        for sink in topology.getSink():
            resourceGraph.updateGraph(sink.getNodeId(), sink)
    # get the operator from each topology need to allocate
    curOperators = set()
    for topology in topologies:
        curLayer = topology.getLayer()
        nextLayer = topology.nextLayer()
        if len(nextLayer):
        	curOperators.update(curLayer)
        	print("next Layer ", nextLayer)
    print("current Operator", [op.id for op in curOperators])

    OperatorParentCount = {}
    while curOperators: 
        print("current operator list", curOperators)
        # Operator Ranking.
        operator_side = {}
        for operator in curOperators:
            # for each node, calculate the dominator resource.
            # CPU, memory, network-in
            node = operator_rank(operator, resourceGraph)
            # resourceGraph.update_graph(node, operator)
            operator_side[operator] = node
        # Resource Ranking. 
        resource_side = node_ranking(operator_side, resourceGraph)
       
        failed_opList = []
        # Matching and update resource graph
        for node, opList in resource_side.items():
            for _, op in opList:
                if resourceGraph.checkResource(node, op):
                    resourceGraph.updateGraph(node, op)
                    # todo: check whether has enough resource.
                    # only choose the first op. then matching again.  
                    curOperators.remove(op)
                    for childOp in op.getDownstreamInstance():
                        if len(childOp.getDownstream()) > 0:
                            # Don't need to allocate Sink.
                            # If child operator has multiple parents, this child must wait all parent is allocated.
                            if childOp not in OperatorParentCount:
                                OperatorParentCount[childOp] = len(childOp.getUpstream())
                          
                            # We need to remove the upstream relation from there. 
                            if OperatorParentCount[childOp] <= 1: 
                                curOperators.add(childOp)
                            else:
                                OperatorParentCount[childOp] -= 1
                    break
                else:
                    print("can't find resource")
                    failed_opList.append(op)
                    return

    print(OperatorParentCount)
    resourceGraph.print_graph()
    # directly put spout into the target Node and update node information. 


def mergedOp_ranking_node(op: MergedOperator, resourceGraph: ResourceGraph) -> int:
    res = [resourceGraph.calNodeScoreWithMergedOp(i, op) for i in range(resourceGraph.getVertexNum())] 
    print("operator side ranking: ", res)
    print("the choice is ", res.index(min(res)), "the score is ", min(res))
    return res.index(min(res))

def node_ranking_mergedOp(operator_side, resourceGraph) -> map:
    nodes = set(operator_side.values())
    node_side = {}
    for node in nodes:
        node_side[node] = []
        for mergedOp, val in operator_side.items():
            if val == node:
                score = resourceGraph.calMergedOpScore(node, mergedOp)
                node_side[node].append((score, mergedOp))
        node_side[node] = sorted(node_side[node], key=lambda x: -x[0])
    print("operator_side and node side: ", operator_side, node_side)
    return node_side

def schedule(topologies: list, resourceGraph: ResourceGraph) -> None:
    # get the longest topology 
    # directly put Spout into the target Node and update node information. 
    result = {}
    for topology in topologies:
        for operator in topology.getLayer():
            resourceGraph.updateGraph(operator.getNodeId(), operator)
        topology.nextLayer()
    # directly allocate Sink into the target node. We assume only has one sink in each topology. 
    for topology in topologies:
        for sink in topology.getSink():
            resourceGraph.updateGraph(sink.getNodeId(), sink)
   
    mergedOpList = []
    # split topology
    for topology in topologies:
        firstPartition = topology.partition()
        mergedOperator = topology.mergeOperator(firstPartition) 
        mergedOpList.append(mergedOperator)
    
    # start matching
    while mergedOpList:
        # Operator Ranking.
        operator_side = {}
        for mergedOp in mergedOpList:
            # for each node, calculate the dominator resource.
            # CPU, memory, network-in
            node = mergedOp_ranking_node(mergedOp, resourceGraph)
            # resourceGraph.update_graph(node, operator)
            operator_side[mergedOp] = node
        # Resource Ranking. 
        nodePreference = node_ranking_mergedOp(operator_side, resourceGraph)

        print(nodePreference)
 
        failed_opList = []
        # Matching and update resource graph
        for node, opList in nodePreference.items():
            for _, mergedOp in opList:
                if resourceGraph.checkMergedOp(node, mergedOp):
                    resourceGraph.updateGraphWithMergedOp(node, mergedOp)
                    # todo: check whether has enough resource.
                    # only choose the first op. then matching again.  
                    mergedOpList.remove(mergedOp)
                    nextPartition = mergedOp.topology.nextMergedOperator()
                    if nextPartition:
                        mergedOpList.append(mergedOp.topology.mergeOperator(nextPartition))
                    break
                else:
                    print("can't find resource")
                    failed_opList.append(mergedOp)
                    return
    resourceGraph.print_graph()
    #
