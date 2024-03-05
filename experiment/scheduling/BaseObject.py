class Node:
    def __init__(self, nodeId: int, cpu: float, mem: float, bandwidth: float = 0):
        # keep 1 GB memory for operating system, change GB to MB
        self.cpu = (cpu / 2 ) * 0.8
        self.mem = mem * 1024 - 1024
        self.id = nodeId
        self.bandwidth = bandwidth       

    def getMem(self) -> float: 
        return self.mem
    
    def getCPU(self) -> float: 
        return self.cpu

    def getBandwidth(self) -> float:
        return self.bandwidth

    def setMem(self, mem: float): 
        self.mem = mem
    
    def setCPU(self, cpu: float): 
        self.cpu = cpu
    
    def output(self) -> None:
        print("vertic id ", self.id, "cpu: ", self.cpu, "mem: ", self.mem)


class Edge:
    def __init__(self, bandwidth: float, delay: float) -> None:
        self.bandwidth = bandwidth
        self.delay = delay 

    def getBandwidth(self) -> float: 
        return self.bandwidth

    def getDelay(self) -> float: 
        return self.delay

    def setBandwidth(self, band: float): 
        self.bandwidth = band



class MergedOperator:
    def __init__(self, cpu : float, networkIn: float, networkOut : float, opIdList: int, mem: int = 128, topology = None) -> None:
        self.mem = mem
        self.cpu = cpu 
        self.networkIn = networkIn 
        self.networkOut = networkOut
        self.idList = opIdList 
        self.topology = topology
        self.upstreamNode = []
        self.opList = []

class Operator:
    def __init__(self, cpu : float, networkIn: float, networkOut : float, operatorName: str, opID: int, mem: int = 128, topologyName: str = "") -> None:
        self.upstream = set() 
        self.downstream = set()
        self.mem = mem
        self.cpu = cpu
        self.networkIn = networkIn * 4
        self.networkOut = networkOut * 4
        self.nodeId = None
        self.id = opID 
        self.targetNode = None
        self.topologyName = topologyName
        self.operators = None
        self.operatorName = operatorName

    def getId(self) -> int:
        return self.id
    
    def getTopologyName(self) -> str:
        return self.topologyName

    def getNetworkIn(self) -> float:
        return self.networkIn

    def getNetworkOut(self) -> float:
        return self.networkOut

    def getMem(self) -> float:
        return self.mem
    
    def getCPU(self) -> float:
        return self.cpu
 
    def getOperatorList(self) -> list:
        return self.operators

    def setOperatorList(self, operators: list) -> None:
        self.operators = operators 
    
    def getNodeId(self) -> int:
        return self.nodeId

    def setNodeId(self, nodeId: int) -> None:
        self.nodeId = nodeId
 
    def getTargetNode(self) -> Node:
        return self.targetNode

    def getUpstream(self) -> set():
        return self.upstream

    def getDownstream(self) -> set():
        return self.downstream

    def getDownstreamInstance(self) -> set():
        return set([self.operators[x] for x in self.downstream])

    def setDownstream(self, op: int) -> None:
        self.downstream.add(op)

    def setUpstream(self, op: int) -> None:
        self.upstream.add(op)

    def output(self, detailed: bool = False):
        if detailed == True:
            print([a.id for a in self.upstream], [a.id for a in self.downstream], self.mem, self.cpu, self.networkIn, self.networkOut, self.nodeId, self.id)
        ret = '{}:{}'.format(self.operatorName, self.nodeId)
        print(ret)
        return self.operatorName, self.nodeId 
