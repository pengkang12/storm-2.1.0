#    !/bin/python3
# stores the number of vertices in the graph
import networkx as nx
from sklearn.cluster import KMeans
import numpy as np
from BaseObject import *
# Example graph
#graph = 
#    {
#       "v1" : {
#			"v2": (ReceivedData -> float, NormalizedNetworkDelay -> float),
#                        "mem": remainedMem -> float,
#                        "cpu": remainedCPU -> float,
#		}
#       "v2" : {"v1": (ReceivedData -> float, NormalizedNetworkDelay -> float)}
#    }


class ResourceGraph:
    def __init__(self):
        self.graph = {}
        self.vertices_no = -1
        self.vertices = {}
        self.edge = {}
        self.G = nx.Graph()
        self.nodeMap = {}

    def findMaxFlowPath(self, s: int, d: int) -> None:
        if s == d:
            return float('inf'), 0, []
        edgeList = []
        for key, e in self.edge.items(): 
            edgeList.append([e.getBandwidth(), e.getDelay(), key]) 
        edgeList = sorted(edgeList, key=lambda x: (x[0], -1.0*x[1]))
        #print(edgeList)
        G1 = self.G.copy()
        for edge in edgeList:
            source, dist = edge[2]
            G1.remove_edge(source, dist)
            if not nx.has_path(G1, source, dist):
                G1.add_edge(source, dist)
        path = []
        for path1 in nx.all_simple_paths(G1, source=s, target=d):
            path = path1
        
        # return value
        minBand = float('inf')
        delay = 0
        edges = []
        for i in range(len(path)-1):
            edge = (path[i], path[i+1])
            edges.append(edge)
            delay += self.G.edges[edge]['weight'].getDelay()
            minBand = min(minBand, self.G.edges[edge]['weight'].getBandwidth())
        print("find the path: ", edges, minBand, delay)
        return minBand, delay, edges

    def calMergedOpScore(self, nodeId: int, op: MergedOperator) -> float:
        node = self.vertices[nodeId]
        return (node.getCPU()- op.cpu)/node.getCPU() + (node.getMem() - op.mem)/node.getMem() + (node.getBandwidth()-op.networkIn-op.networkOut)/node.getBandwidth()

    def calculateOpScore(self, nodeId: int, op: Operator) -> float:
        node = self.vertices[nodeId]
        return (node.getCPU()- op.getCPU())/node.getCPU() + (node.getMem() - op.getMem())/node.getMem() + (node.getBandwidth()-op.getNetworkIn()-op.getNetworkOut())/node.getBandwidth()

    def getVertexNum(self) -> float:
        return self.vertices_no

    def updateMem(self, nodeId: int, val: float) -> None:
        mem = self.vertices[nodeId].getMem() 
        self.vertices[nodeId].setMem(mem - val)

    def updateCPU(self, nodeId: int, val: float) -> None: 
        cpu = self.vertices[nodeId].getCPU() 
        self.vertices[nodeId].setCPU(cpu - val)

    def updateEdge(self, u: int, v: int, val: float) -> None:
        key = (u, v) if (u, v) in self.edge else (v, u)
        band = self.edge[key].getBandwidth()
        self.edge[key].setBandwidth(band - val)

    def getEdge(self, u: int, v: int):
        return self.edge[(u, v)] if (u, v) in self.edge else self.edge[(v, u)]
    
    def getNode(self, u: int) -> Node:
        return self.vertices[u]
 
    # Add a vertex to the dictionary
    def add_vertex(self, v: int, cpu: float, mem: float):
        if v in self.graph:
            print("Vertex ", v, " already exists.")
        else:
            self.vertices_no = self.vertices_no + 1
        self.graph[v] = []
        self.vertices[v] = Node(v, cpu, mem)
    
    # Add an edge between vertex v1 and v2 with edge weight e
    def add_edge(self, v1: int, v2: int, e: Edge):
        # Check if vertex v1 is a valid vertex
        if v1 not in self.graph:
            print("Vertex ", v1, " does not exist.")
        # Check if vertex v2 is a valid vertex
        elif v2 not in self.graph:
            print("Vertex ", v2, " does not exist.")
        else:
            # Since this is an undirected graph, each node need 
            # to store the edge and we need to save edge (v1, v2) and (v2, v1)
            self.graph[v1].append((v2, e))
            self.graph[v2].append((v1, e))
            self.edge[(v1, v2)] = e
            #self.edge[(v2, v1)] = e

    # Print the graph
    def print_graph(self):
        print("\ngraph info")
        #for vertex in self.graph:
        #    for v2 in self.graph[vertex]:
        #    	vertex2, edge = v2[0], v2[1]
        #    	print(vertex,  " -> ", vertex2, " edge weight: ", edge.getBandwidth(), edge.getDelay())
        for vertex in self.vertices.values():
            vertex.output()
        print(self.G.edges.items())
        for key, value in self.G.edges.items():
            print(value['weight'].getBandwidth(), value['weight'].getDelay())
        print('\n')

    def checkResource(self, nodeIdx: int, operator: Operator):
        node = self.vertices[nodeIdx] 
        if node.getMem() <= operator.getMem():
            print("don't have enough memory")
            return False
        if node.getCPU() <= operator.getCPU():
            print("don't have enough cpu")
            return False

        for opId in operator.getUpstream():
            op = operator.getOperatorList()[opId]
            bandwidth, delay, path = self.findMaxFlowPath(op.getNodeId(), nodeIdx)
            if bandwidth < op.getNetworkOut():
                print("don't have enough bandwidth")
                return False
        return True

    def checkMergedOp(self, nodeIdx: int, mergedOp: MergedOperator):
        node = self.vertices[nodeIdx] 
        if node.getMem() <= mergedOp.mem:
            print("don't have enough memory")
            return False
        if node.getCPU() <= mergedOp.cpu:
            print("don't have enough cpu")
            return False

        for nodeId in mergedOp.upstreamNode:
            bandwidth, delay, path = self.findMaxFlowPath(nodeId, nodeIdx)
            if bandwidth < mergedOp.networkOut:
                print("don't have enough bandwidth")
                return False
        return True


    def calNodeScoreWithMergedOp(self, curNode : Node, op: MergedOperator):
    	# find the dominator resource, the maximum score as the dominator resource 
        cpuScore = op.cpu/self.getNode(curNode).getCPU() #if op.cpu < self.getNode(curNode).getCPU() else float('inf')
        memScore = op.mem/self.getNode(curNode).getMem() #if op.mem < self.getNode(curNode).getMem()  else float('inf')
        bandwidthScore = 0.0
        minBandwidth = float('inf')
        networkDelay = 0
        for upstreamNodeId in op.upstreamNode:	
            print(upstreamNodeId, curNode)
            bandwidth, delay, path = self.findMaxFlowPath(upstreamNodeId, curNode)
            bandwidthScore += op.networkOut*1.0 / bandwidth
            print("this is bandscore", bandwidthScore, op.networkIn, bandwidth)
            networkDelay += delay /100
            minBandwidth = min(minBandwidth, bandwidth)
        print("operator score at node ", curNode,  op.idList, " cpu: ", cpuScore, "mem :", memScore, "band :", bandwidthScore, "node res: ", self.getNode(curNode).getCPU(), self.getNode(curNode).getMem())
        # network delay should be considered for CPU, MEM, and bandwidth together.

        #bandwidth, delay, path = self.findMaxFlowPath(curNode, op.getOperatorList()[-1].getNodeId())
        #bandwidthScore += op.bandwidthOut*1.0 / bandwidth + delay
        # weight is an experience values
        weight = minBandwidth / self.getNode(curNode).getCPU()
        weight = 1
        return max(cpuScore , memScore ) + bandwidthScore*weight + networkDelay


    def calculateNodeScore(self, curNode : Node, op: Operator):
    	# find the dominator resource, the maximum score as the dominator resource
        cpuScore = op.cpu/self.getNode(curNode).getCPU() #if op.cpu < self.getNode(curNode).getCPU() else float('inf')
        memScore = op.mem/self.getNode(curNode).getMem() #if op.mem < self.getNode(curNode).getMem()  else float('inf')
        bandwidthScore = 0.0
        minBandwidth = float('inf')
        for upopId in op.upstream:	
            upop = op.getOperatorList()[upopId]
            prevNodeId = upop.getNodeId()
            bandwidth, delay, path = self.findMaxFlowPath(prevNodeId, curNode)
            bandwidthScore += op.networkIn*1.0 / bandwidth + delay / 1000
            minBandwidth = min(minBandwidth, bandwidth)
        print("operator score at node ", curNode, op.getTopologyName(), op.getId(), " cpu: ", cpuScore, "mem :", memScore, "band :", bandwidthScore)
        # network delay should be considered for CPU, MEM, and bandwidth together.

        #bandwidth, delay, path = self.findMaxFlowPath(curNode, op.getOperatorList()[-1].getNodeId())
        #bandwidthScore += op.bandwidthOut*1.0 / bandwidth + delay
        # weight is an experience values
        weight = minBandwidth / self.getNode(curNode).getCPU()
        weight = 1
        return cpuScore + bandwidthScore*weight  #+ networkDelay

    def updateGraphWithMergedOp(self, curNodeId: int, mergedOp: MergedOperator):
        self.updateMem(curNodeId, mergedOp.mem)
        self.updateCPU(curNodeId, mergedOp.cpu)
        for op in mergedOp.opList:
            op.setNodeId(curNodeId)
 
        for prevNodeId in mergedOp.upstreamNode:
            if not prevNodeId:
                continue
            band, delay, path = self.findMaxFlowPath(prevNodeId, curNodeId)
            for (s, d) in path:
                self.updateEdge(s, d, mergedOp.networkOut)
            print("path ", curNodeId, path)
 
    def updateGraph(self, curNodeId: int, operator: Operator):
        self.updateMem(curNodeId, operator.getMem())
        self.updateCPU(curNodeId, operator.getCPU())
        operator.setNodeId(curNodeId)
 
        for opId in operator.getUpstream():
            op = operator.getOperatorList()[opId]
            prevNodeId = op.getNodeId()
            if not prevNodeId:
                continue
            band, delay, path = self.findMaxFlowPath(prevNodeId, curNodeId)
            for (s, d) in path:
                self.updateEdge(s, d, op.getNetworkOut())
            print("path ", opId, curNodeId, path)
 
    def create_graph(self, nodeResource, networkResource):
        # driver code
        # stores the number of vertices in the graph
        self.vertices_no = 0
        for val in nodeResource.values():
            self.add_vertex(*val)
        # Add the edges between the vertices by specifying
        # the from and to vertex along with the edge weights.
        for s, e, data in networkResource:
            band, delay = data['b']*1000, data['d']
            edge = Edge(band, delay)
            self.add_edge(s, e, edge)

            self.vertices[s].bandwidth += band
            self.vertices[e].bandwidth += band
 
            self.G.add_edge(s, e)
            self.G.edges[s, e]['weight'] = edge

        self.print_graph()
        # Reminder: the second element of each list inside the dictionary
        # denotes the edge weight.
        pass 
 
class Topology_Profiler:
    def __init__(self, spoutId:int = 0, sinkId:int = 0):
        self.spoutId = spoutId
        self.sinkId = sinkId
        self.currentLayer = set()
        self.name = ""
        self.sink = []
        self.spout = []
        self.operators = []
        # used for storing graph information and partition graph 
        self.G = nx.DiGraph()
        self.partitionList = []

    def createTopology(self, name: str, resource_info: map, edge: list, target: list):
        self.name = name 
        for i, info in enumerate(resource_info):
            operator= Operator(*info, opID=i, mem=128, topologyName=name)
            self.operators.append(operator)
            operator.setOperatorList(self.operators)

        for s, e in edge:
            self.operators[s].setDownstream(e)
            self.operators[e].setUpstream(s)
 
        self.currentLayer = []
        for op in self.operators:
            # make each operator can find the whole operator list
            if len(op.getUpstream()) == 0:
                # find the spout 
                op.setNodeId(target[0])
                self.spout.append(op)
                self.currentLayer.append(op)
            if len(op.getDownstream()) == 0:
                # find the sink 
                op.setNodeId(target[1])
                self.sink.append(op)

        # build graph used for partitioning graph
        for u, v in edge:
            self.G.add_edge(u, v, weight=int(resource_info[u][2]))
        for op in self.spout+self.sink:
            u = op.getId()
            if self.G.has_node(u):
                self.G.remove_node(u)

    def partition(self) -> list():
        # Convert the DAG to its underlying undirected version
        undirected_G = self.G.to_undirected()
        
        # Construct the Laplacian matrix of the undirected graph
        laplacian_matrix = nx.laplacian_matrix(undirected_G).toarray()
        
        # Compute the eigenvectors and eigenvalues of the Laplacian matrix
        eigenvalues, eigenvectors = np.linalg.eig(laplacian_matrix)
        
        # Sort the eigenvectors based on their corresponding eigenvalues
        sorted_indices = np.argsort(eigenvalues)
        sorted_eigenvectors = eigenvectors[:, sorted_indices]
        
        # Select the eigenvectors corresponding to the smallest eigenvalues
        # Selecting the second, third, and fourth eigenvectors
        selected_eigenvectors = sorted_eigenvectors[:, 1:4]  
        
        # Apply clustering to the selected eigenvectors
        num_partitions = 2  # Number of partitions desired
        kmeans = KMeans(n_clusters=num_partitions)
        kmeans.fit(selected_eigenvectors)
        
        # Retrieve the cluster labels
        partition_labels = kmeans.labels_
        
        nodes = sorted(undirected_G.nodes())
        # Assign each node to a partition based on the clustering result
        partitions = {node: partition_labels[i] for i, node in enumerate(undirected_G.nodes())}
        partition0 = [k for k, v in partitions.items() if v == 0]
        partition1 = [k for k, v in partitions.items() if v == 1]
        if sum(partition0)*1.0/len(partition0) > sum(partition1)*1.0/len(partition1):
            self.partitionList.insert(0, partition0) 
            self.partitionList.insert(0, partition1)
        else:
            self.partitionList.insert(0, partition1) 
            self.partitionList.insert(0, partition0)
        # need to store the partition with a list
        print(self.partitionList)
        return self.partitionList[0]

    def mergeOperator(self, parList: list) -> MergedOperator:
        totalCPU, totalMem = 0, 0
        totalNetIn, totalNetOut = 0, 0
        # calculate total CPU and Memory
        for opId in parList:
            totalCPU += self.operators[opId].getCPU()
            totalMem += self.operators[opId].getMem()
        # calculate upstream Node id
        upstreamNode = set()
        for opId in parList:
            for upOpId in self.operators[opId].getUpstream():
                upOp = self.operators[upOpId]
                if upOp.getNodeId():
                    upstreamNode.add(upOp.getNodeId())
 
        # calculate the network in and out
        undirected_G = self.G.to_undirected()
        for u, v in undirected_G.edges:
            if u in parList and v in parList:
                continue
            elif u not in parList and v not in parList:
                continue
            elif u in parList:
                totalNetOut += self.operators[u].getNetworkOut()
            elif v in parList:
                totalNetIn += self.operators[v].getNetworkIn()
        print(totalCPU, totalMem, totalNetIn, totalNetOut)
        op = MergedOperator(totalCPU, totalNetIn, totalNetOut, opIdList=parList, mem=totalMem, topology=self)
        op.upstreamNode = upstreamNode
        op.opList = [ self.operators[v] for v in parList] 
        return op
 
    def nextMergedOperator(self) -> list:
        if len(self.partitionList) > 0:
            self.partitionList.pop(0) 
        return self.partitionList[0] if len(self.partitionList) > 0 else [] 

    def getLayer(self) -> list():
        return self.currentLayer

    def getSink(self) -> Operator:
        return self.sink
 
    def __getPath(self, op: Operator) -> set():
        path = set([op])
        for op1 in op.getDownstream():
            path.update(self.__getPath(self.operators[op1]))
        return  path

    def getOperator(self, x):
        return self.operators[x] 

    def nextLayer(self) -> set():
       	next_layer = set()
        for node in self.currentLayer:
            next_layer.update([self.operators[x] for x in node.downstream])
        needToRemove = set()
        
        for op in next_layer:
            if op in needToRemove:
                continue
            path =  self.__getPath(op)
            for op1 in path:
                if op1 != op and op1 in next_layer:
                    needToRemove.add(op1)
        print("layer and need to remove ", [op.id for op in next_layer], [op.id for op in needToRemove])  
        next_layer -= needToRemove
        self.currentLayer = next_layer
        return self.currentLayer 

    def output(self) -> None:
        print("Topology ", self.name)
        seen = set()
        queue = []
        for op in self.spout:
            queue.append(op)
            seen.add(op.id)
        ret_list = []
        while queue:
            op = queue.pop(0)
            ret_list.append(op.output())
            for nextOpId in op.downstream:
                if nextOpId not in seen:
                    seen.add(nextOpId)
                    queue.append(self.operators[nextOpId])
        return ret_list

  
if __name__ == "__main__":
    graph = ResourceGraph()
    networkResource = [
	(0, 1, 20., 15),
	(1, 2, 20., 15)
    ]

    nodeResource = {
    # hostname: (index, CPU, Memory)
    "edge1": [0, 400., 8],
    "edge2": [1, 200., 4],
    "edge3": [2, 200., 4],
   }
    graph.create_graph(nodeResource, networkResource)
    operator1 = Operator(90, 300, 400, 0)
    operator2 = Operator(85, 350, 450, 1)
 
    operator1.setUpstream(1)
    operator2.setUpstream(0)
    operator2.setDownstream(0)
    operator1.setDownstream(1)
    operator1.setNodeId(1)
    operator2.setNodeId(0) 
    oplist = [operator1, operator2]
    operator1.setOperatorList(oplist)
    operator2.setOperatorList(oplist) 


    graph.updateGraph(1, operator1)
    graph.updateGraph(0, operator2)
    graph.print_graph() 
