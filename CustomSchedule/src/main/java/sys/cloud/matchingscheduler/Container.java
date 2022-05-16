package sys.cloud.matchingscheduler;

import org.apache.storm.scheduler.ExecutorDetails;
import org.apache.storm.scheduler.WorkerSlot;

import java.util.ArrayList;
import java.util.Collection;

public class Container {
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


    Container(String topologyId, ArrayList<ExecutorDetails> executorDetailsList){
        this.topologyId = topologyId;
        this.executorDetailsList = executorDetailsList;
    }
    Container(int cpu, int bandwidth){
        this.cpu = cpu;
        this.bandwidth = bandwidth;
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