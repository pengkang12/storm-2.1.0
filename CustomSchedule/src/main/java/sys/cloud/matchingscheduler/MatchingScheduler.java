package sys.cloud.matchingscheduler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;


import org.apache.storm.generated.*;
import org.apache.storm.scheduler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("unused")
public class MatchingScheduler implements IScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(MatchingScheduler.class);

    private final String untaggedTag = "untagged";
    private final String unKnownTag = "unknown_tag";

    private Map<String, ArrayList<SupervisorDetails>> getSupervisorsByTag(
            Collection<SupervisorDetails> supervisorDetails
    ) {
        // A map of tag -> supervisors, to help with scheduling of components with specific tags
        Map<String, ArrayList<SupervisorDetails>> supervisorsByTag = new HashMap<String, ArrayList<SupervisorDetails>>();

        for (SupervisorDetails supervisor : supervisorDetails) {
            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) supervisor.getSchedulerMeta();

            String tags;

            if (metadata == null) {
                tags = untaggedTag;
            } else {
                tags = metadata.get("tags");

                if (tags == null) {
                    tags = untaggedTag;
                }
            }

            // If the supervisor has tags attached to it, handle it by populating the supervisorsByTag map.
            // Loop through each of the tags to handle individually
            for (String tag : tags.split(",")) {
                tag = tag.trim();

                if (supervisorsByTag.containsKey(tag)) {
                    // If we've already seen this tag, then just add the supervisor to the existing ArrayList.
                    supervisorsByTag.get(tag).add(supervisor);
                } else {
                    // If this tag is new, then create a new ArrayList<SupervisorDetails>,
                    // add the current supervisor, and populate the map's tag entry with it.
                    ArrayList<SupervisorDetails> newSupervisorList = new ArrayList<SupervisorDetails>();
                    newSupervisorList.add(supervisor);
                    supervisorsByTag.put(tag, newSupervisorList);
                }
            }
        }

        return supervisorsByTag;
    }

    private <T> void populateComponentsByContainer(
            Map<String, ArrayList<Container>>  componentsByContainer,
            Map<String, T> components,
            String topologyID,
            Map<String, List<ExecutorDetails>> executorsByComponent,
            Map<String, Collection<Container>> componentMapToContainer
    ) {
        ArrayList<ExecutorDetails> executorList = new ArrayList<ExecutorDetails>();


        for (Entry<String, T> componentEntry : components.entrySet()) {
            String componentID = componentEntry.getKey();

            T component = componentEntry.getValue();
            // Fetch the executors for the current component ID
            List<ExecutorDetails> executorsForComponent = executorsByComponent.get(componentID);
            if (executorsForComponent == null) {
                continue;
            }

            int i = 0;
            List<ExecutorDetails> newExecutorList = new ArrayList<>();
            for (ExecutorDetails executor : executorsForComponent){
                newExecutorList.add(executor);
                if (newExecutorList.size() == 8 || i == executorsForComponent.size()-1){
                    Container container = Container.createContainer(topologyID, newExecutorList);
                    Collection<Object> componentList = new ArrayList<>();
                    componentList.add(component);
                    container.setComponentList(componentList);
                    if (componentMapToContainer.containsKey(componentID)){
                        componentMapToContainer.get(componentID).add(container);
                    }else{
                        Collection<Container> containerCollection = new ArrayList<>();
                        containerCollection.add(container);
                        componentMapToContainer.put(componentID, containerCollection);
                    }

                    if (componentsByContainer.containsKey(topologyID)) {
                        componentsByContainer.get(topologyID).add(container);
                    } else {
                        ArrayList<Container> containersList = new ArrayList<Container>();
                        containersList.add(container);
                        componentsByContainer.put(topologyID, containersList);
                    }
                    newExecutorList = new ArrayList<>();
                }
                i += 1;
            }
        }
    }

    private <T> void updateContainerPredecessor(
            Map<String, ArrayList<Container>>  componentsByContainer,
            String topologyID,
            Map<String, Collection<Container>> componentMapToContainer
    ) {
        ArrayList<ExecutorDetails> executorList = new ArrayList<ExecutorDetails>();
        ArrayList<Container> containerCollection = componentsByContainer.get(topologyID);
        for (Container container : containerCollection){
            Collection<Container> predecessors = new ArrayList<>();
            Collection<Object> componentCollection = container.getComponentList();
            if (componentCollection == null){
                continue;
            }
            for (Object component: componentCollection) {
                //Add predecessor
                try {
                    // Get the component's conf irrespective of its type (via java reflection)
                    Method getCommonComponentMethod = component.getClass().getMethod("get_common");
                    ComponentCommon commonComponent = (ComponentCommon) getCommonComponentMethod.invoke(component);

                    Map<GlobalStreamId, Grouping> inputs = commonComponent.get_inputs();
                    if (inputs != null){
                        for (Entry<GlobalStreamId, Grouping> entry : inputs.entrySet()){
                            predecessors.addAll(componentMapToContainer.get(entry.getKey().get_componentId()));
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }
            container.setPredecessors(predecessors);
            LOG.info("PengSetPredecessors"+predecessors.toString());
        }

        Graph G = new Graph(containerCollection.size());
        HashMap<Container, Integer> containerMap = new HashMap<>();
        int i = 0;
        for (Container container : containerCollection){
            containerMap.put(container, i);
            i += 1;
        }
        for (Container container: containerCollection){
            if (container.getPredecessors() == null){
                continue;
            }
            for (Object predecessor : container.getPredecessors()) {
                G.addEdge(containerMap.get(predecessor), containerMap.get(container));
            }
        }
        ArrayList<Integer> topologyOrder = G.topologicalSort();

        ArrayList<Container> newContainerCollection = new ArrayList<Container>();
        for (Integer index: topologyOrder){
            newContainerCollection.add(containerCollection.get(index));
        }
        LOG.info("PengTopologyOrder " + topologyOrder.toString());
        componentsByContainer.put(topologyID, newContainerCollection);
    }
    private <T> void populateComponentsByContainerForInternals(
            Map<String, ArrayList<Container>>  componentsByContainer,
            String topologyID,
            Map<String, List<ExecutorDetails>> executorsByComponent,
            Map<String, Collection<Container>> componentMapToContainer

    ) {
        ArrayList<ExecutorDetails> executorList = new ArrayList<ExecutorDetails>();
        for (Entry<String, List<ExecutorDetails>> componentEntry: executorsByComponent.entrySet()) {
            String componentID = componentEntry.getKey();
            if (componentID.startsWith("__")){
                // Fetch the executors for the current component ID
                List<ExecutorDetails> executorsForComponent = executorsByComponent.get(componentID);
                if (executorsForComponent == null) {
                    continue;
                }

                // Convert the list of executors to a set
                Set<ExecutorDetails> executorsToAssignForComponent = new HashSet<ExecutorDetails>(
                        executorsForComponent
                );
                // Remove already assigned executors from the set of executors to assign, if any
                //executorsToAssignForComponent.removeAll(aliveExecutors);
                // Add the component's waiting to be assigned executors to the current container
                Container container = Container.createContainer(topologyID, executorsToAssignForComponent);

                if (componentsByContainer.containsKey(topologyID)) {
                    componentsByContainer.get(topologyID).add(container);
                } else {
                    ArrayList<Container> containersList = new ArrayList<Container>();
                    containersList.add(container);
                    componentsByContainer.put(topologyID, containersList);
                }

                if (componentMapToContainer.containsKey(componentID)){
                    componentMapToContainer.get(componentID).add(container);
                }else{
                    Collection<Container> containerCollection = new ArrayList<>();
                    containerCollection.add(container);
                    componentMapToContainer.put(componentID, containerCollection);
                }

            }
        }

    }


    private void handleUnsuccessfulScheduling(
            Cluster cluster,
            TopologyDetails topologyDetails,
            String message
    ) throws Exception {
        // This is the prefix of the message displayed on Storm's UI for any unsuccessful scheduling
        String unsuccessfulSchedulingMessage = "SCHEDULING FAILED: ";

        cluster.setStatus(topologyDetails.getId(), unsuccessfulSchedulingMessage + message);
        throw new Exception(message);
    }

    private Set<WorkerSlot> getAliveSlots(Cluster cluster, TopologyDetails topologyDetails) {
        // Get the existing assignment of the current topology as it's live in the cluster
        SchedulerAssignment existingAssignment = cluster.getAssignmentById(topologyDetails.getId());

        // Return alive slots, if any, otherwise an empty set
        if (existingAssignment != null) {
            return existingAssignment.getSlots();
        } else {
            return new HashSet<WorkerSlot>();
        }
    }



    private void MatchingSchedule(Topologies topologies, Cluster cluster) {
        Collection<SupervisorDetails> supervisorDetails = cluster.getSupervisors().values();
        // Get the lists of tagged and unreserved supervisors.
        Map<String, ArrayList<SupervisorDetails>> supervisorsByTag = getSupervisorsByTag(supervisorDetails);

        //PengAddStart
        Map<String, ArrayList<Container>> executorsByContainer = new HashMap<String, ArrayList<Container>>();
        if (cluster.needsSchedulingTopologies().size() < 2){
            return;
        }
        for (TopologyDetails topologyDetails: cluster.needsSchedulingTopologies()) {
            StormTopology stormTopology = topologyDetails.getTopology();
            String topologyID = topologyDetails.getId();
            //get components from topology
            Map<String, Bolt> bolts = stormTopology.get_bolts();
            Map<String, SpoutSpec> spouts = stormTopology.get_spouts();
            LOG.info("PengBolts" + bolts.toString());
            LOG.info("PengSpouts", spouts.toString());
            // get A map of component to executors
            Map<String, List<ExecutorDetails>> executorsByComponent = cluster.getNeedsSchedulingComponentToExecutors(topologyDetails);
            Map<String, Collection<Container>> componentMapToContainer = new HashMap<>();
            //put executor into container
            populateComponentsByContainer(executorsByContainer, spouts, topologyID, executorsByComponent, componentMapToContainer);
            populateComponentsByContainer(executorsByContainer, bolts, topologyID, executorsByComponent, componentMapToContainer);
            populateComponentsByContainerForInternals(executorsByContainer, topologyID, executorsByComponent, componentMapToContainer);
            updateContainerPredecessor(executorsByContainer, topologyID, componentMapToContainer);
        }

        // get all available slots
        // get the resource usage for each Node(supervisor)
        HashMap<String, NodeResource> nodeResourceList = new HashMap<String, NodeResource>();
        List<WorkerSlot> availableSlots = new ArrayList<WorkerSlot>();
        for (SupervisorDetails supervisor : supervisorDetails) {
            availableSlots.addAll(cluster.getAvailableSlots(supervisor));
            nodeResourceList.put(supervisor.getId(), new NodeResource(2000, 200000));
        }

        List<WorkerSlot> allocatedSlots = new ArrayList<>();
        //store the map of workerslot and executors.
        Map<WorkerSlot, Container> containerToSlotsMap = (new HashMap<WorkerSlot, Container>());


        // need to machine
        int layer_number = 0;
        while (true) {
            // get each layer's container by topologyID
            ArrayList<Container> containerForLayer = new ArrayList<Container>();
            for (Entry<String, ArrayList<Container>> entry : executorsByContainer.entrySet()) {
                // already reached the last layer.
                if (layer_number >= entry.getValue().size()){
                    continue;
                }

                String topologyID = entry.getKey();
                ArrayList<Container> containers = entry.getValue();
                containerForLayer.add(containers.get(layer_number));
            }
            // we can't find a group of executor to match node.
            if (containerForLayer.isEmpty()){
                break;
            }

            List<WorkerSlot> newAllocatedSlots = TwoSideMatching(cluster, containerToSlotsMap, containerForLayer,
                    availableSlots, allocatedSlots, nodeResourceList);
            allocatedSlots.addAll(newAllocatedSlots);
            layer_number += 1;
            LOG.info("PengAllocatedConf" + containerForLayer);
        }

        HashSet<String> topologyIDSet = new HashSet<>();

        for (Entry<WorkerSlot, Container> entry : containerToSlotsMap.entrySet()) {
            WorkerSlot slotToAssign = entry.getKey();
            Container container = entry.getValue();
            String topologyID = container.getTopologyId();
            Collection<ExecutorDetails> executorsToAssign = container.getExecutorDetailsList();
            LOG.info("PengAllocated " + topologyID + " \n executor " + executorsToAssign.toString() + "\nSlot " +slotToAssign);
            cluster.assign(slotToAssign, topologyID, executorsToAssign);
            topologyIDSet.add(topologyID);
        }

        // If we've reached this far, then scheduling must have been successful
        for(String topologyID : topologyIDSet){
            cluster.setStatus(topologyID, "SCHEDULING SUCCESSFUL");
            LOG.info("PengSchedule Successfully " + topologyID);
        }
        return ;
        ///PengAddEnd
    }


    private Map<String, ArrayList<SupervisorDetails>> getSupervisorsConfiguration(
            Collection<SupervisorDetails> supervisorDetails
    ){
        Map<String, ArrayList<SupervisorDetails>> supervisorsConf = new HashMap<String,ArrayList<SupervisorDetails>>();
        for (SupervisorDetails supervisor : supervisorDetails) {
            Map<String, String> metadata = (Map<String, String>) supervisor.getSchedulerMeta();
            String metaConf = null;
            if (metadata == null){
                metaConf = unKnownTag;
            } else {
                metaConf = metadata.get("tags");
                if (metaConf == null){
                    metaConf = unKnownTag;
                }
            }

            for (String conf : metaConf.split(",")){
                conf = conf.trim();
                if ( supervisorsConf.containsKey(conf) ){
                    supervisorsConf.get(conf).add(supervisor);
                } else {
                    ArrayList<SupervisorDetails> newSupervisorList = new ArrayList<SupervisorDetails>();
                    newSupervisorList.add(supervisor);
                    supervisorsConf.put(conf, newSupervisorList);
                }
            }
        }
        return supervisorsConf;
    }


    private Map<String, List<String>> NodePreferContainer1(List<WorkerSlotExtern> availableSlot, ArrayList<Container> containersList){
        Map<String, List<String>> girlPrefersMap = new HashMap<>();

        for (WorkerSlotExtern slot : availableSlot){

            //sort container by BandWidth
            Collections.sort(containersList, new Comparator<Container>(){
                public int compare(Container o1, Container o2){
                    return o1.getBandWidth() - o2.getBandWidth();
                }
            });
            List<String> girlPrefers = new ArrayList<>();
            for (Container container: containersList){
                girlPrefers.add(String.valueOf(container.getId()));
            }
            girlPrefersMap.put(String.valueOf(slot.getId()), girlPrefers);
        }
        return girlPrefersMap;
    }

    private Map<String, List<String>> ContainerPreferNode1(List<WorkerSlotExtern> availableSlot, ArrayList<Container> containersList){
        Map<String, List<String>> guyPrefersMap = new HashMap<String, List<String>>();
        for (Container container : containersList){
            for (WorkerSlotExtern slot : availableSlot){
                slot.calculateScore(container);
            }
            Collections.sort(availableSlot, new Comparator<WorkerSlotExtern>() {
                @Override
                public int compare(WorkerSlotExtern o1, WorkerSlotExtern o2) {
                    return o1.getScore() - o2.getScore();
                }
            });

            List<String> oneGuyPrefers = new ArrayList<>();
            for (WorkerSlotExtern slot : availableSlot){
                oneGuyPrefers.add(String.valueOf(slot.getId()));
            }
            guyPrefersMap.put(String.valueOf(container.getId()), oneGuyPrefers);

        }
        return guyPrefersMap;
    }

    private List<WorkerSlot> TwoSideMatching(Cluster cluster,
                                             Map<WorkerSlot, Container> assignments,
                                             //container for current layer
                                             ArrayList<Container> containersList,
                                             List<WorkerSlot> allAvailableSlots,
                                             List<WorkerSlot> allocatedSlots,
                                             HashMap<String, NodeResource> nodeResourceMap)
            // ExecutorByContainerForLayer for each layer.
    {
        // Microservices will prefer the cheapest resoruces. Microserivces also want to spend less money.
        // resrouce preference is the microservice request smalledst resource. Each resource want to hold as much as possible microservices.
        // using matching algorithm to find a good match.

        //remove allocated slots from available slots
        Set<WorkerSlot> allAvailableSlotsSet = new HashSet<WorkerSlot>(allAvailableSlots);

        Set<WorkerSlot> allocatedSlotsSet = new HashSet<WorkerSlot>(allocatedSlots);
        allAvailableSlotsSet.removeAll(allocatedSlotsSet);
        List<WorkerSlot> availableSlots = new ArrayList<WorkerSlot>(allAvailableSlotsSet);
        List<WorkerSlot> newAllocatedSlots = new ArrayList<WorkerSlot>();

        if (availableSlots.isEmpty()) {
            // This is bad, we have supervisors and executors to assign, but no available slots!
            String message = "No slots are available for assigning executors for (components)";
            LOG.error("Peng " + message);
        }

        // score each worker slot for matching
        List<WorkerSlotExtern> workerSlotExternList = new ArrayList<WorkerSlotExtern>();
        for (WorkerSlot slot : allAvailableSlotsSet){
            WorkerSlotExtern slotExtern = new WorkerSlotExtern();
            slotExtern.setWorkerSlot(slot);
            slotExtern.setNode(nodeResourceMap.get(slot.getNodeId()));

            workerSlotExternList.add(slotExtern);
        }

        // score each Container for matching

        // create the preference for container

        List<String> guys = new ArrayList<String>();
        for(Container container: containersList){
            guys.add(String.valueOf(container.getId()));
        }

        Map<String, List<String>> guyPrefers = ContainerPreferNode1(workerSlotExternList, containersList);
        for (Entry<String, List<String>> entry : guyPrefers.entrySet()) {

            LOG.info("PengGuy " + entry.getKey() + " " + entry.toString());
        }

        // create the preference for worker slot.
        List<String> girls = new ArrayList<String>();
        for(WorkerSlotExtern slotExtern: workerSlotExternList){
            girls.add(String.valueOf(slotExtern.getId()));
        }

        Map<String, List<String>> girlPrefers = NodePreferContainer1(workerSlotExternList, containersList);
        for (Entry<String, List<String>> entry : girlPrefers.entrySet()) {

            LOG.info("PengGirl " + entry.getKey() + " " + entry.toString());
        }
        //Start to match
        Map<String, String> matches = StableMatching.match(guys, guyPrefers, girlPrefers);

        for(Map.Entry<String, String> match:matches.entrySet()){
            LOG.info(
                    match.getKey() + " is engaged to " + match.getValue());
        }
        Set<String> slotToContainer = matches.keySet();
        for (String girl : slotToContainer) {
            LOG.info("Woman: " + girl + " is engaged with man: " + matches.get(girl));
            if (matches.get(girl) == null) {
                continue;
            }
            //find slot
            WorkerSlotExtern workerSlotExtern = null;
            for (WorkerSlotExtern slotExtern : workerSlotExternList){
                if (Objects.equals(String.valueOf(slotExtern.getId()), girl)){
                    workerSlotExtern = slotExtern;
                    break;
                }
            }
            //find
            Container container = null;
            for (Container container1 : containersList){
                if (String.valueOf(container1.getId()).equals(matches.get(girl))){
                    container = container1;
                    break;
                }
            }
            if (container != null && workerSlotExtern != null){
                container.setWorkerSlotExtern(workerSlotExtern);
                Collection<ExecutorDetails> containerExecutorList = container.getExecutorDetailsList();
                WorkerSlot workerSlot = workerSlotExtern.getWorkerSlot();
                newAllocatedSlots.add(workerSlot);
                assignments.put(workerSlot, container);
                LOG.info("PengContainer " + container.getWorkerSlotExtern().toString());
            }else{
                LOG.info("PengAssignment Can't find container or workerSlot");
            }
        }
        return newAllocatedSlots;

    }

    @Override
    @SuppressWarnings("rawtypes")
    public void prepare(Map conf) {
    }
    @Override
    public Map<String, Map<String, Double>> config() {
	return new HashMap<>();
    }

    @Override
    public void schedule(Topologies topologies, Cluster cluster) {
        MatchingSchedule(topologies, cluster);
    }
}