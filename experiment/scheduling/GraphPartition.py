from config import *

import networkx as nx
import numpy as np
from sklearn.cluster import KMeans


ETLT_data = [
(2.56,9.2,410.46, "spout1"),
(5.67,731.15,1200.42, "SenMlParseBolt"),
(5.98,862.93,1111.53, "RangeFilterBolt"),
(6.28,746.48,1200.35, "BloomFilterBolt"),
(3.68,1355.75,1025.55, "InterpolationBolt"),
(8.51, 808.38, 168.78, "JoinBolt"),
]

PREDICTT_data = [
 (5.13,10.83,710.66, "spout1"),
 (9.74,622.46,582.55, "SenMLParseBoltPRED"),
 (11.86,234.28,194.23, "DecisionTreeClassifyBolt"),
 (3.76,214.47,176.12, "LinearRegressionPredictorBolt"),
 (7.14,169.86,12.29, "BlockWindowAverageBolt"),
 (8.81,509.51,978.09, "ErrorEstimationBolt"),
 (4.23,261.12,261.02, "MQTTPublishBolt"),
 ]

PREDICT =  [[0, 1], [1, 2], [1, 3], [1, 4], [2, 6], [3, 5], [4, 5], [5, 6]]
ETL = [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5]]
edges = ETL
weights = ETLT_data

edges = PREDICT
weights = PREDICTT_data
edges = ETL
weights = ETLT_data


def partition(edges, weights):
    
    # Spectral 
    # Create a directed acyclic graph (DAG)
    G = nx.DiGraph()
    total_weight = 0
    for i in range(len(edges)):
        u, v = edges[i][0], edges[i][1]
        total_weight +=  int(weights[u][2]+weights[v][1])
 
    for i in range(len(edges)):
        u, v = edges[i][0], edges[i][1]
        G.add_edge(u, v, weight=(weights[u][2]+weights[v][1])/total_weight)
    total_cpu = 0 
    for i in range(len(weights)):
        u, v = i, i 
        total_cpu += weights[i][0] 
    for i in range(len(weights)):
        u, v = i, i 
        G.add_edge(u, v, weight=weights[i][0]/total_cpu)
    
    # Convert the DAG to its underlying undirected version
    undirected_G = G.to_undirected()
    
    # Construct the Laplacian matrix of the undirected graph
    laplacian_matrix = nx.laplacian_matrix(undirected_G).toarray()
    
    # Compute the eigenvectors and eigenvalues of the Laplacian matrix
    eigenvalues, eigenvectors = np.linalg.eig(laplacian_matrix)
    
    # Sort the eigenvectors based on their corresponding eigenvalues
    sorted_indices = np.argsort(eigenvalues)
    sorted_eigenvectors = eigenvectors[:, sorted_indices]
    
    # Select the eigenvectors corresponding to the smallest eigenvalues
    selected_eigenvectors = sorted_eigenvectors[:, 1:4]  # Selecting the second, third, and fourth eigenvectors
    
    # Apply clustering to the selected eigenvectors
    num_partitions = 2  # Number of partitions desired
    kmeans = KMeans(n_clusters=num_partitions)
    kmeans.fit(selected_eigenvectors)
    
    # Retrieve the cluster labels
    partition_labels = kmeans.labels_
    
    # Assign each node to a partition based on the clustering result
    partitions = {node: partition_labels[i] for i, node in enumerate(undirected_G.nodes())}
    
    print(partitions)
    return partitions

partition(ETL, ETLT_data)
partition(PREDICT, PREDICTT_data)
