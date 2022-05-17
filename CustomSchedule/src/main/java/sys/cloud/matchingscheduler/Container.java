package sys.cloud.matchingscheduler;

import org.apache.storm.scheduler.ExecutorDetails;
import org.apache.storm.scheduler.WorkerSlot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;


public class Container {

    private static final AtomicInteger idGen = new AtomicInteger(1);
    private final Integer id;
    // cpu is 100 milicores
    Integer cpu = 100;
    Integer bandwidth = 10000;
    // bandwidth is 10000M.
    // memory is 1000M
    Integer memory = 1000;

    Collection<Container> previous = null;
    ArrayList<ExecutorDetails> executorDetailsList = null;

    WorkerSlot workerSlot = null;
    String topologyId = null;

    private Container(){
        this.id = idGen.getAndIncrement();
    }

    private Container(String topologyId, ArrayList<ExecutorDetails> executorDetailsList){
        this.topologyId = topologyId;
        this.executorDetailsList = executorDetailsList;
        this.id = idGen.getAndIncrement();
    }
    private Container(int cpu, int bandwidth){
        this.cpu = cpu;
        this.bandwidth = bandwidth;
        this.id = idGen.getAndIncrement();
    }

    static Container createContainer(int cpu, int bandwidth) {
        return new Container(cpu, bandwidth);
    }

    static Container createContainer(String topologyId, ArrayList<ExecutorDetails> executorDetailsList) {
        return new Container(topologyId, executorDetailsList);
    }

    static Container createContainer() {
        return new Container();
    }

    public int getId(){
        return this.id;
    }
    public  void setWorkerSlot(WorkerSlot workerSlot){
        this.workerSlot = workerSlot;
    }

    public WorkerSlot getWorkerSlot() {
        return this.workerSlot;
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
    public ArrayList<ExecutorDetails> getExecutorDetailsList(){
        return this.executorDetailsList;
    }
    public void setCpu(int cpu){
        this.cpu = cpu;
    }
    public void setBandwidth(int bandwidth){
        this.bandwidth = bandwidth;
    }
    public int getScore(){
        int score = 0;
        score = cpu/ 1000 + bandwidth/10000;
        return score;
    }
}