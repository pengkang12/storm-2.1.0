#!/bin/python3
import random
import math

def pureRandom(nodeNumber=5):
    possible = [0.4, 0.5, 0.6, 0.7, 0.8]
    
    for p in possible:
        edge = []
        for s in range(nodeNumber):
            for e in range(s+1, nodeNumber):
                if random.random() >= p:
                    edge.append((s, e))
        print("Generate the possibility of ", p)
        print(edge)
        print()

def waxMan(row=2, col=5, p=0.3):
    alist = [0.2, 0.25, 0.3, 0.35, 0.4]
    blist = [0.2, 0.3, 0.4]
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
                        edge.append((s, e))
            if len(edge) > 20 or len(edge) < 15:
                print("not good", len(edge)) 
                continue
            print("Generate the WaxMan model with parameter a, b:", a, b)
            print(edge)
            print()

       
waxMan() 
#pureRandom()

 
