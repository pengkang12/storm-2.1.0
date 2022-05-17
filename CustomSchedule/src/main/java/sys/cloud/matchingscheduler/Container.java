package sys.cloud.matchingscheduler;

import org.apache.storm.scheduler.Component;
import org.apache.storm.scheduler.ExecutorDetails;
import org.apache.storm.scheduler.WorkerSlot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

public class Container <T> {

    private static final AtomicInteger idGen = new AtomicInteger(1);
    private final Integer id;
    // cpu is 100 milicores
    Integer cpu = 100;
    Integer bandwidth = 10000;
    // bandwidth is 10000M.
    // memory is 1000M
    Integer memory = 1000;

    Collection<Container> predecessors = null;
    Collection<ExecutorDetails> executorDetailsList = null;
    Collection<T> componentList = null;

    WorkerSlotExtern workerSlotExtern = null;
    String topologyId = null;

    private Container(){
        this.id = idGen.getAndIncrement();
    }

    private Container(String topologyId, Collection<ExecutorDetails> executorDetailsList){
        this.topologyId = topologyId;
        this.executorDetailsList = executorDetailsList;
        this.id = idGen.getAndIncrement();
        Random rand = new Random();
        this.bandwidth = this.id * 1000 * rand.nextInt(10);
    }
    private Container(int cpu, int bandwidth){
        this.cpu = cpu;
        this.bandwidth = bandwidth;
        this.id = idGen.getAndIncrement();
    }

    static Container createContainer(int cpu, int bandwidth) {
        return new Container(cpu, bandwidth);
    }

    static Container createContainer(String topologyId, Collection<ExecutorDetails> executorDetailsList) {
        return new Container(topologyId, executorDetailsList);
    }

    static Container createContainer() {
        return new Container();
    }

    public int getId(){
        return this.id;
    }
    public  void setWorkerSlotExtern(WorkerSlotExtern workerSlot){
        this.workerSlotExtern = workerSlot;
    }

    public WorkerSlotExtern getWorkerSlotExtern() {
        return this.workerSlotExtern;
    }

    public void setTopologyId(String topologyId){
        this.topologyId = topologyId;
    }

    public String getTopologyId() {
        return this.topologyId;
    }

    public void setExecutorDetailsList(ArrayList<ExecutorDetails> executorDetailsList){
        this.executorDetailsList = executorDetailsList;
    }
    public Collection<ExecutorDetails> getExecutorDetailsList(){
        return this.executorDetailsList;
    }
    public void setCpu(int cpu){
        this.cpu = cpu;
    }
    public int getCpu(){
        return this.cpu;
    }
    public void setBandwidth(int bandwidth){
        this.bandwidth = bandwidth;
    }
    public int getBandWidth(){
        return this.bandwidth;
    }
    public int getScore(){
        int score = 0;
        score = cpu/ 1000 + bandwidth/10000;
        return score;
    }

    public void setPredecessors(Collection<Container> predecessors) {
        this.predecessors = predecessors;
    }
    public void setComponentList(Collection<T> componentList){
        this.componentList = componentList;
    }
    public Collection<T> getComponentList(){
        return this.componentList;
    }
    public Collection<Container> getPredecessors() {
        return this.predecessors;
    }
}