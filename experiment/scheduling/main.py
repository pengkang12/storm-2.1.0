#    !/bin/python3
# stores the number of vertices in the graph
from implement import *
from config import *

resourceGraph = ResourceGraph()
nodeMap = { val[0]: key for key, val in nodeResource.items()}

def processing(topologyRes):
    # initialze the topology 
    topologies = []
    for name, val in topologyRes.items():
        tprofiler = Topology_Profiler()
        tprofiler.createTopology(name, val, appTopology[name], target[name])
        topologies.append(tprofiler)
        tprofiler.output()
    # upforward optimization 
    schedule(topologies, resourceGraph) 
    # backforward optimization 
    ret = {}
    for topology in topologies:
        result = topology.output()
        result = [ "{}:{}".format(a, nodeMap[b]) for a, b in result]
        ret[topology.name] = ",".join(result)
    for key, val in ret.items():
        print(key, val)

def main():
    # initialize the physical network resource
    resourceGraph.create_graph(nodeResource, networkResource)
    
    # first allocate 
    processing(resource) 
    # scaling up
    #processing(resource1)
    #processing(resource2)
    resourceGraph.findMaxFlowPath(0, 3)
 
if __name__ == '__main__':
    main()
