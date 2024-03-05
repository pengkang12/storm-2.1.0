#!/bin/python3 
# reference paper: https://my.ece.msstate.edu/faculty/skhan/pub/K_I_2008_JPDC.pdf

import math
import random

#node degree
DEGREE = {10, 15, 20}
BANDWIDTH = [10, 20, 40, 60, 100]
# aggregation edge topology
DISTANCE = [2, 5, 10, 15, 20]
# Core edge topology
BANDWIDTH = [100, 200, 400, 600, 1000]
DISTANCE = [500, 1000, 2000, 3000, 4000]

def generateLinkInfo():
   band = BANDWIDTH[random.randint(1, 5)-1]
   distance = DISTANCE[random.randint(1, 5)-1]
   return distance, band 

def waxMan(row=2, col=3, p=0.3):
    alist = [0.1, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5]
    blist = [0.2, 0.3, 0.4, 0.5]
    # L is maximum distance bewteen two node, we use Manhattan island.
    # Manhattan Island is 22.7 square miles (59 km2) in area, 13.4 miles (21.6 km) long and 2.3 miles (3.7 km) wide
    # every 1 km we put an edge node 
    # we use grid to mimic the node location. 
    # * * * * * * * * * * * * * * *
    # * * * * * * * * * * * * * * *
    # * * * * * * * * * * * * * * *
    # * * * * * * * * * * * * * * *
    # we use Manhattan distance to calculate d(u, v)
    L = 20
    def manhattanDistance(u, v):
        urow, ucol = u/row, u%row
        vrow, vcol = v/row, v%row
        return abs(urow-vrow) + abs(ucol-vcol)

    def probability(u, v, a, b):
        exponent = -1 * manhattanDistance(u, v) / (L*a)
        p = b * math.exp(exponent)        
        return p 
    nodeNumber = row * col
    
    for a in alist:
        for b in blist:
            edge = []
            for s in range(nodeNumber):
                for e in range(s+1, nodeNumber):
                    if probability(s, e, a, b) >= p:
                        dist,band = generateLinkInfo()
                        edge.append((s, e, band, dist))
            if len(edge) > nodeNumber*2 or len(edge) < nodeNumber*1.5:
                print("not good", len(edge)) 
                #continue
            print("Generate the WaxMan model with parameter a, b:", a, b)
            print(edge)
            print()

       
waxMan()
waxMan(p=0.15) 
#
#pureRandom()


for x in DEGREE:
    print(x)
