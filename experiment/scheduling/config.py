#!/usr/bin/env python3

# application resource usage for given input
resource = {
    # application name
    'PREDICT': [
        #cpu, received, transfer, operator's name
        (5.15,9.38,673.54, "spout1"),
        (9.55,681.99,877.63, "SenMLParseBoltPRED"),
        (2.67,307.6,221.16, "DecisionTreeClassifyBolt"),
        (3.26,304.56,224.38, "LinearRegressionPredictorBolt"),
        (4.74,293.87,10.55, "BlockWindowAverageBolt"),
        (9.57,438.29,875.47, "ErrorEstimationBolt"),
        (2.63,227.14,221.97, "MQTTPublishBolt"),
        (3.68, 397.08, 30.37, "sink")
    ],
    'PREDICTT': [
        (5.13,10.83,710.66, "spout1"),
        (9.74,622.46,582.55, "SenMLParseBoltPRED"),
        (11.86,234.28,194.23, "DecisionTreeClassifyBolt"),
        (3.76,214.47,176.12, "LinearRegressionPredictorBolt"),
        (7.14,169.86,12.29, "BlockWindowAverageBolt"),
        (8.81,509.51,978.09, "ErrorEstimationBolt"),
        (4.23,261.12,261.02, "MQTTPublishBolt"),
        (3.59,295.46,117.54, "sink")
    ],
    'ETL': [
        (4.52, 8.3, 682.33, "spout1"),
        (8.25,696.05,1158.27, "SenMlParseBolt"),
        (5.67,1140.97,1153.38, "RangeFilterBolt"),
        (5.82,1145.21,1180.6, "BloomFilterBolt"),
        (10.33,1149.68,1174.15, "InterpolationBolt"),
        (5.32,1137.72,250.15, "JoinBolt"),
        (4.81,253.68,282.44, "AnnotationBolt"),
        (8.85,283.99,876.74, "CsvToSenMLBolt"),
        (7.57,896.37,1701.91, "PublishBolt"),
        (3.74,862.01,18.21, "sink"),
    ],
     'ETLT': [
        (2.56,9.2,410.46, "spout1"),
        (5.67,731.15,1200.42, "SenMlParseBolt"),
        (5.98,862.93,1111.53, "RangeFilterBolt"),
        (6.28,746.48,1200.35, "BloomFilterBolt"),
        (3.68,1355.75,1025.55, "InterpolationBolt"),
        (8.51, 808.38, 168.78, "JoinBolt"),
        (5.88,283.68,304.68, "AnnotationBolt"),
        (10.1,232.83,796.86, "CsvToSenMLBolt"),
        (5.24,789.33,1435.76, "PublishBolt"),
        (3.61,892.21,12.4, "sink"),
        ],
}

# list all application's name
appName = {
    "ETL",
    "PREDICT",
}
#Application topology, each number means the operator's sequence in Variable resource
appTopology = {
    'PREDICT': [[0, 1], [1, 2], [1, 3], [1, 4], [2, 6], [3, 5], [4, 5], [5, 6], [6, 7]],
    'PREDICTT': [[0, 1], [1, 2], [1, 3], [1, 4], [2, 6], [3, 5], [4, 5], [5, 6], [6, 7]],
    'ETL': [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 7], [7, 8], [8, 9]],
    'ETLT': [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 7], [7, 8], [8, 9]],
    'ETL2': [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [6, 7], [7, 8], [8, 9]],
}

# Variable target is a map.
target = {
    # application's name : [start edge device, end edge device]
    'ETL': [7, 0],
    'PREDICT': [3, 5],
    'PREDICTT': [4, 0],
    'ETLT': [6, 0],
    'ETL2': [3, 0],
}

# Edge node resource 
nodeResource = {
    # hostname: (index, CPU, Memory)
    "core1": [0, 800., 14],
    "worker1": [1, 400., 8],
    "worker2": [2, 400., 8],
    "edge1": [3, 200., 4],
    "edge2": [4, 200., 4],
    "edge3": [5, 200., 4],
    "edge4": [6, 200., 4],
    "edge5": [7, 200., 4],
}

# Edge node topology
networkResource = [
    # (node1, node2, {'b': network bandwidth between node1 and node2, 'd': network delay between node1 and node2})
    (0, 1, {'b': 50, 'd': 50}), 
    (0, 2, {'b': 50, 'd': 50}),
    (1, 3, {'b': 10, 'd': 15}), 
    (1, 4, {'b': 10, 'd': 15}), 
    (1, 5, {'b': 10, 'd': 15}), 
    (2, 6, {'b': 10, 'd': 15}),
    (2, 7, {'b': 10, 'd': 15})
]
