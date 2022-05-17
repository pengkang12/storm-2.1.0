package sys.cloud.matchingscheduler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import org.apache.storm.generated.Bolt;
import org.apache.storm.generated.ComponentCommon;
import org.apache.storm.generated.SpoutSpec;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.scheduler.Cluster;
import org.apache.storm.scheduler.ExecutorDetails;
import org.apache.storm.scheduler.IScheduler;
import org.apache.storm.scheduler.SchedulerAssignment;
import org.apache.storm.scheduler.SupervisorDetails;
import org.apache.storm.scheduler.Topologies;
import org.apache.storm.scheduler.TopologyDetails;
import org.apache.storm.scheduler.WorkerSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


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
    private <T> void populateComponentsByTag(
            Map<String, ArrayList<String>> componentsByTag,
            Map<String, T> components
    ) {
        // Type T can be either Bolt or SpoutSpec, so that this logic can be reused for both component types
        JSONParser parser = new JSONParser();

        for (Entry<String, T> componentEntry : components.entrySet()) {
            JSONObject conf = null;

            String componentID = componentEntry.getKey();
            T component = componentEntry.getValue();

            try {
                // Get the component's conf irrespective of its type (via java reflection)
                Method getCommonComponentMethod = component.getClass().getMethod("get_common");
                ComponentCommon commonComponent = (ComponentCommon) getCommonComponentMethod.invoke(component);
                conf = (JSONObject) parser.parse(commonComponent.get_json_conf());
            } catch (ParseException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }

            String tags;

            // If there's no config, use a fake tag to group all untagged components
            if (conf == null) {
                tags = untaggedTag;
            } else {
                tags = (String) conf.get("tags");

                // If there are no tags, use a fake tag to group all untagged components
                if (tags == null) {
                    tags = untaggedTag;
                }
            }

            // If the component has tags attached to it, handle it by populating the componentsByTag map.
            // Loop through each of the tags to handle individually
            for (String tag : tags.split(",")) {
                tag = tag.trim();

                if (componentsByTag.containsKey(tag)) {
                    // If we've already seen this tag, then just add the component to the existing ArrayList.
                    componentsByTag.get(tag).add(componentID);
                } else {
                    // If this tag is new, then create a new ArrayList,
                    // add the current component, and populate the map's tag entry with it.
                    ArrayList<String> newComponentList = new ArrayList<String>();
                    newComponentList.add(componentID);
                    componentsByTag.put(tag, newComponentList);
                }
            }
        }
    }
    private <T> void populateComponentsByContainer(
            Map<String, ArrayList<Container>>  componentsByContainer,
            Map<String, T> components,
            String topologyID,
            Map<String, List<ExecutorDetails>> executorsByComponent
    ) {
        ArrayList<ExecutorDetails> executorList = new ArrayList<ExecutorDetails>();

        for (Entry<String, T> componentEntry : components.entrySet()) {
            String componentID = componentEntry.getKey();

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
            executorList.addAll(executorsToAssignForComponent);
        }
        ArrayList<String> executorPerContainerList = new ArrayList<String>();
        int count = 0;
        ArrayList<ExecutorDetails> newExecutorList = new ArrayList<ExecutorDetails>();

        for (ExecutorDetails executorDetail: executorList){
            count += 1;
            newExecutorList.add(executorDetail);
            if (count %4 == 0){
                Container container = Container.createContainer(topologyID, newExecutorList);
                if (componentsByContainer.containsKey(topologyID)) {
                    componentsByContainer.get(topologyID).add(container);
                } else {
                    ArrayList<Container> containersList = new ArrayList<Container>();
                    containersList.add(container);
                    componentsByContainer.put(topologyID, containersList);
                }
                newExecutorList = new ArrayList<ExecutorDetails>();
            }
        }
        if (!newExecutorList.isEmpty()){
            Container container = Container.createContainer(topologyID, newExecutorList);
            componentsByContainer.get(topologyID).add(container);
        }
    }

    private <T> void populateComponentsByContainerForInternals(
            Map<String, ArrayList<Container>>  componentsByContainer,
            String topologyID,
            Map<String, List<ExecutorDetails>> executorsByComponent

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
                executorList.addAll(executorsToAssignForComponent);
            }
        }
        ArrayList<String> executorPerContainerList = new ArrayList<String>();
        int count = 0;
        ArrayList<ExecutorDetails> newExecutorList = new ArrayList<ExecutorDetails>();

        for (ExecutorDetails executorDetail: executorList){
            count += 1;
            newExecutorList.add(executorDetail);
            if (count %4 == 0){
                Container container = Container.createContainer(topologyID, newExecutorList);
                if (componentsByContainer.containsKey(topologyID)) {
                    componentsByContainer.get(topologyID).add(container);
                } else {
                    ArrayList<Container> containersList = new ArrayList<Container>();
                    containersList.add(container);
                    componentsByContainer.put(topologyID, containersList);
                }
                newExecutorList = new ArrayList<ExecutorDetails>();
            }
        }
    }
    private void populateComponentsByTagWithStormInternals(
            Map<String, ArrayList<String>> componentsByTag,
            Set<String> components
    ) {
        // Storm uses some internal components, like __acker.
        // These components are topology-agnostic and are therefore not accessible through a StormTopology object.
        // While a bit hacky, this is a way to make sure that we schedule those components along with our topology ones:
        // we treat these internal components as regular untagged components and add them to the componentsByTag map.

        for (String componentID : components) {
            if (componentID.startsWith("__")) {
                if (componentsByTag.containsKey(untaggedTag)) {
                    // If we've already seen untagged components, then just add the component to the existing ArrayList.
                    componentsByTag.get(untaggedTag).add(componentID);
                } else {
                    // If this is the first untagged component we see, then create a new ArrayList,
                    // add the current component, and populate the map's untagged entry with it.
                    ArrayList<String> newComponentList = new ArrayList<String>();
                    newComponentList.add(componentID);
                    componentsByTag.put(untaggedTag, newComponentList);
                }
            }
        }
    }

    private Set<ExecutorDetails> getAliveExecutors(Cluster cluster, TopologyDetails topologyDetails) {
        // Get the existing assignment of the current topology as it's live in the cluster
        SchedulerAssignment existingAssignment = cluster.getAssignmentById(topologyDetails.getId());

        // Return alive executors, if any, otherwise an empty set
        if (existingAssignment != null) {
            return existingAssignment.getExecutors();
        } else {
            return new HashSet<ExecutorDetails>();
        }
    }

    private Map<String, ArrayList<ExecutorDetails>> getExecutorsToBeScheduledByTag(
            Cluster cluster,
            TopologyDetails topologyDetails,
            Map<String, ArrayList<String>> componentsPerTag
    ) {
        // Initialise the return value
        Map<String, ArrayList<ExecutorDetails>> executorsByTag = new HashMap<String, ArrayList<ExecutorDetails>>();

        // Find which topology executors are already assigned
        Set<ExecutorDetails> aliveExecutors = getAliveExecutors(cluster, topologyDetails);

        // Get a map of component to executors for the topology that need scheduling
        Map<String, List<ExecutorDetails>> executorsByComponent = cluster.getNeedsSchedulingComponentToExecutors(
                topologyDetails
        );

        // Loop through componentsPerTag to populate the map
        for (Entry<String, ArrayList<String>> entry : componentsPerTag.entrySet()) {
            String tag = entry.getKey();
            ArrayList<String> componentIDs = entry.getValue();

            // Initialise the map entry for the current tag
            ArrayList<ExecutorDetails> executorsForTag = new ArrayList<ExecutorDetails>();

            // Loop through this tag's component IDs
            for (String componentID : componentIDs) {
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
                executorsToAssignForComponent.removeAll(aliveExecutors);

                // Add the component's waiting to be assigned executors to the current tag executors
                executorsForTag.addAll(executorsToAssignForComponent);
            }

            // Populate the map of executors by tag after looping through all of the tag's components,
            // if there are any executors to be scheduled
            if (!executorsForTag.isEmpty()) {
                executorsByTag.put(tag, executorsForTag);
            }
        }

        return executorsByTag;
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

    private List<WorkerSlot> getSlotsToAssign(
            Cluster cluster,
            TopologyDetails topologyDetails,
            List<SupervisorDetails> supervisors,
            List<String> componentsForTag,
            String tag
    ) throws Exception {
        String topologyID = topologyDetails.getId();

        // Collect the available slots of each of the supervisors we were given in a list
        List<WorkerSlot> availableSlots = new ArrayList<WorkerSlot>();
        for (SupervisorDetails supervisor : supervisors) {
            availableSlots.addAll(cluster.getAvailableSlots(supervisor));
        }

        if (availableSlots.isEmpty()) {
            // This is bad, we have supervisors and executors to assign, but no available slots!
            String message = String.format(
                    "No slots are available for assigning executors for tag %s (components: %s)",
                    tag, componentsForTag
            );
            handleUnsuccessfulScheduling(cluster, topologyDetails, message);
        }

        Set<WorkerSlot> aliveSlots = getAliveSlots(cluster, topologyDetails);

        int numAvailableSlots = availableSlots.size();
        int numSlotsNeeded = topologyDetails.getNumWorkers() - aliveSlots.size();

        // We want to check that we have enough available slots
        // based on the topology's number of workers and already assigned slots.
        if (numAvailableSlots < numSlotsNeeded) {
            // This is bad, we don't have enough slots to assign to!
            String message = String.format(
                    "Not enough slots available for assigning executors for tag %s (components: %s). "
                            + "Need %s slots to schedule but found only %s",
                    tag, componentsForTag, numSlotsNeeded, numAvailableSlots
            );
            handleUnsuccessfulScheduling(cluster, topologyDetails, message);
        }

        // Now we can use only as many slots as are required.
        return availableSlots.subList(0, numSlotsNeeded);
    }

    private Map<WorkerSlot, ArrayList<ExecutorDetails>> getExecutorsBySlot(
            List<WorkerSlot> slots,
            List<ExecutorDetails> executors
    ) {
        // todo: use two matching algorithm to reschedule it.
        // todo: need to sort slot by resources.
//        Initialize all men and women to free
//        while there exist a free man m who still has a woman w to propose to
//        {
//            w = m's highest ranked such woman to whom he has not yet proposed
//            if w is free
//                (m, w) become engaged
//            else some pair (m', w) already exists
//                if w prefers m to m'
//                      (m, w) become engaged
//                      m' becomes free
//                else
//                      (m', w) remain engaged
//        }
        // men is executor
        // women is slot.
        // 1. merge executor to a group

        Map<WorkerSlot, ArrayList<ExecutorDetails>> assignments = new HashMap<WorkerSlot, ArrayList<ExecutorDetails>>();

        int numberOfSlots = slots.size();

        // We want to split the executors as evenly as possible, across each slot available,
        // so we assign each executor to a slot via round robin
        for (int i = 0; i < executors.size(); i++) {
            WorkerSlot slotToAssign = slots.get(i % numberOfSlots);
            ExecutorDetails executorToAssign = executors.get(i);

            if (assignments.containsKey(slotToAssign)) {
                // If we've already seen this slot, then just add the executor to the existing ArrayList.
                assignments.get(slotToAssign).add(executorToAssign);
            } else {
                // If this slot is new, then create a new ArrayList,
                // add the current executor, and populate the map's slot entry with it.
                ArrayList<ExecutorDetails> newExecutorList = new ArrayList<ExecutorDetails>();
                newExecutorList.add(executorToAssign);
                assignments.put(slotToAssign, newExecutorList);
            }
        }

        return assignments;
    }

    private void populateComponentExecutorsToSlotsMap(
            Map<WorkerSlot, ArrayList<ExecutorDetails>> componentExecutorsToSlotsMap,
            Cluster cluster,
            TopologyDetails topologyDetails,
            List<SupervisorDetails> supervisors,
            List<ExecutorDetails> executors,
            List<String> componentsForTag,
            String tag
    ) throws Exception {
        String topologyID = topologyDetails.getId();

        if (supervisors == null) {
            // This is bad, we don't have any supervisors but have executors to assign!
            String message = String.format(
                    "No supervisors given for executors %s of topology %s and tag %s (components: %s)",
                    executors, topologyID, tag, componentsForTag
            );
            handleUnsuccessfulScheduling(cluster, topologyDetails, message);
        }

        List<WorkerSlot> slotsToAssign = getSlotsToAssign(
                cluster, topologyDetails, supervisors, componentsForTag, tag
        );

        // Divide the executors evenly across the slots and get a map of slot to executors
        // using two side matching algorithm


        Map<WorkerSlot, ArrayList<ExecutorDetails>> executorsBySlot = getExecutorsBySlot(
                slotsToAssign, executors
        );

        for (Entry<WorkerSlot, ArrayList<ExecutorDetails>> entry : executorsBySlot.entrySet()) {
            WorkerSlot slotToAssign = entry.getKey();
            ArrayList<ExecutorDetails> executorsToAssign = entry.getValue();

            // Assign the topology's executors to slots in the cluster's supervisors
            //componentExecutorsToSlotsMap.put(slotToAssign, executorsToAssign);
            if (!componentExecutorsToSlotsMap.containsKey(slotToAssign))
                componentExecutorsToSlotsMap.put(slotToAssign, executorsToAssign);
            else
                componentExecutorsToSlotsMap.get(slotToAssign).addAll(executorsToAssign);
        }
    }

    private void MatchingSchedule(Topologies topologies, Cluster cluster) {
        Collection<SupervisorDetails> supervisorDetails = cluster.getSupervisors().values();
        // Get the lists of tagged and unreserved supervisors.
        Map<String, ArrayList<SupervisorDetails>> supervisorsByTag = getSupervisorsByTag(supervisorDetails);

        //PengAddStart
        Map<String, ArrayList<Container>> executorsByContainer = new HashMap<String, ArrayList<Container>>();

        for (TopologyDetails topologyDetails: cluster.needsSchedulingTopologies()) {
            StormTopology stormTopology = topologyDetails.getTopology();
            String topologyID = topologyDetails.getId();
            //get components from topology
            Map<String, Bolt> bolts = stormTopology.get_bolts();
            Map<String, SpoutSpec> spouts = stormTopology.get_spouts();
            LOG.info(bolts.toString());
            LOG.info(spouts.toString());
            // get A map of component to executors
            Map<String, List<ExecutorDetails>> executorsByComponent = cluster.getNeedsSchedulingComponentToExecutors(topologyDetails);
            populateComponentsByContainer(executorsByContainer, spouts, topologyID, executorsByComponent);
            LOG.info("PengSpouts " + executorsByContainer);
            populateComponentsByContainer(executorsByContainer, bolts, topologyID, executorsByComponent);
            LOG.info("PengBolts " + executorsByContainer);
            populateComponentsByContainerForInternals(executorsByContainer, topologyID, executorsByComponent);
            //Todo: we ignore internal components, like __acker, etc. need to do in the future.
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
            ArrayList<ExecutorDetails> executorsToAssign = container.getExecutorDetailsList();
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

//        for (TopologyDetails topologyDetails : cluster.needsSchedulingTopologies()) {
//            StormTopology stormTopology = topologyDetails.getTopology();
//            String topologyID = topologyDetails.getId();
//
//            // Get components from topology
//            Map<String, Bolt> bolts = stormTopology.get_bolts();
//            Map<String, SpoutSpec> spouts = stormTopology.get_spouts();
//            // Get a map of component to executors
//            Map<String, List<ExecutorDetails>> executorsByComponent = cluster.getNeedsSchedulingComponentToExecutors(
//                    topologyDetails
//            );
//            // Get a map of tag to
//            Map<String, ArrayList<String>> componentsByTag = new HashMap<String, ArrayList<String>>();
//            populateComponentsByTag(componentsByTag, bolts);
//            populateComponentsByTag(componentsByTag, spouts);
//            populateComponentsByTagWithStormInternals(componentsByTag, executorsByComponent.keySet());
//
//            // Get a map of tag to executors
//            Map<String, ArrayList<ExecutorDetails>> executorsToBeScheduledByTag = getExecutorsToBeScheduledByTag(
//                    cluster, topologyDetails, componentsByTag
//            );
//
//            // Initialise a map of slot -> executors
//            Map<WorkerSlot, ArrayList<ExecutorDetails>> componentExecutorsToSlotsMap = (
//                    new HashMap<WorkerSlot, ArrayList<ExecutorDetails>>()
//            );
//
//            // Time to match everything up!
//            for (Entry<String, ArrayList<ExecutorDetails>> entry : executorsToBeScheduledByTag.entrySet()) {
//                String tag = entry.getKey();
//
//                ArrayList<ExecutorDetails> executorsForTag = entry.getValue();
//                ArrayList<SupervisorDetails> supervisorsForTag = supervisorsByTag.get(tag);
//                ArrayList<String> componentsForTag = componentsByTag.get(tag);
//
//                try {
//                    populateComponentExecutorsToSlotsMap(
//                            componentExecutorsToSlotsMap,
//                            cluster, topologyDetails, supervisorsForTag, executorsForTag, componentsForTag, tag
//                    );
//                } catch (Exception e) {
//                    e.printStackTrace();
//
//                    // Cut this scheduling short to avoid partial scheduling.
//                    return;
//                }
//            }
//
//            // Do the actual assigning
//            // We do this as a separate step to only perform any assigning if there have been no issues so far.
//            // That's aimed at avoiding partial scheduling from occurring, with some components already scheduled
//            // and alive, while others cannot be scheduled.
//            for (Entry<WorkerSlot, ArrayList<ExecutorDetails>> entry : componentExecutorsToSlotsMap.entrySet()) {
//                WorkerSlot slotToAssign = entry.getKey();
//                ArrayList<ExecutorDetails> executorsToAssign = entry.getValue();
//
//                cluster.assign(slotToAssign, topologyID, executorsToAssign);
//            }
//
//            // If we've reached this far, then scheduling must have been successful
//            cluster.setStatus(topologyID, "SCHEDULING SUCCESSFUL");
//        }
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

    private void NodePreferContainer(List<WorkerSlotExtern> availableSlot, ArrayList<Container> containersList, int[][] w){
        int i = 0;
        for (WorkerSlotExtern slot : availableSlot){
            int j = 0;

            //sort container by BandWidth
            Collections.sort(containersList, new Comparator<Container>(){
                public int compare(Container o1, Container o2){
                    return o1.getBandWidth() - o2.getBandWidth();
                }
            });

            for (Container container: containersList){
                w[i][j] = container.getId();
                j++;
            }
            i++;
        }
    }
    private void ContainerPreferNode(List<WorkerSlotExtern> availableSlot, ArrayList<Container> containersList, int[][] m){
        int i = 0;
        for (Container container : containersList){
            int j = 0;
            for (WorkerSlotExtern slot : availableSlot){
                slot.calculateScore(container);
            }
            Collections.sort(availableSlot, new Comparator<WorkerSlotExtern>() {
                @Override
                public int compare(WorkerSlotExtern o1, WorkerSlotExtern o2) {
                    return o1.getScore() - o2.getScore();
                }
            });
            for (WorkerSlotExtern slot : availableSlot){
                m[i][j] = slot.getId();
            }
            i++;
        }
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
        int[][] men1 = new int[containersList.size()][workerSlotExternList.size()];
        ContainerPreferNode(workerSlotExternList, containersList, men1);
        for (int[] ints : men1) {
            LOG.info("PengMen " + Arrays.toString(ints));
        }
        // create the preference for worker slot.
        int [][] women1 = new int[workerSlotExternList.size()][containersList.size()];
        NodePreferContainer(workerSlotExternList, containersList, women1);
        for (int[] ints : women1) {
            LOG.info("PengWomen " + Arrays.toString(ints));
        }

        //Start to match
        StableMarriage sm = new StableMarriage();
        HashMap<Integer, Integer> couples = sm.findCouples(men1, women1);

        LOG.info("\n------------------Final Matching----------------------------");
        Set<Integer> set = couples.keySet();
        for (int key : set) {
            LOG.info("Woman: " + key + " is engaged with man: " + couples.get(key));
        }

        LOG.info("\n-----------------Assign workerSlot to Container.----------------");
        Set<Integer> slotToContainer = couples.keySet();
        for (int key : slotToContainer) {
            LOG.info("Woman: " + key + " is engaged with man: " + couples.get(key));
            if (couples.get(key) == null) {
                continue;
            }
            //find slot
            WorkerSlot workerSlot = null;
            for (WorkerSlotExtern slotExtern : workerSlotExternList){
                if (slotExtern.getId() == key){
                    workerSlot = slotExtern.getWorkerSlot();
                    break;
                }
            }
            //find
            Container container = null;
            for (Container container1 : containersList){
                if (container1.getId() == couples.get(key)){
                    container = container1;
                    break;
                }
            }
            if (container != null && workerSlot != null){
                ArrayList<ExecutorDetails> containerExecutorList = container.getExecutorDetailsList();
                newAllocatedSlots.add(workerSlot);
                assignments.put(workerSlot, container);
            }
        }
        LOG.info("PengAssignment" + assignments.toString());

        //todo: update NodeResource.


        // Divide the executors evenly across the slots and get a map of slot to executors
        // using two side matching algorithm
        //Map<WorkerSlot, ArrayList<ExecutorDetails>> assignments = new HashMap<WorkerSlot, ArrayList<ExecutorDetails>>();

//        int currentSlotIndex = 0;
//        for (Container container : containersList) {
//            String topologyID = container.getTopologyId();
//
//            ArrayList<ExecutorDetails> containerExecutorList = container.getExecutorDetailsList();
//
//            WorkerSlot slotToAssign = availableSlots.get(currentSlotIndex);
//            newAllocatedSlots.add(slotToAssign);
//            assignments.put(slotToAssign, container);
//
//            currentSlotIndex += 1;
//        }
//        LOG.info("PengAssignment" + assignments.toString());

        // We want to split the executors as evenly as possible, across each slot available,
        // so we assign each executor to a slot via round robin
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
    public void schedule(Topologies topologies, Cluster cluster) {MatchingSchedule(topologies, cluster);
    }
}