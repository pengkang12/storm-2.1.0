package sys.cloud.matchingscheduler;

public class NodeResource {
    // cpu is 100 milicores
    int cpu = 2000;
    int bandwidth = 10000;
    // bandwidth is 10000M.
    // memory is 1000M
    int memory = 8000;
    // latency is 20ms.
    int latency = 20;
    NodeResource(){}
    NodeResource(int cpu, int bandwidth){
        this.cpu = cpu;
        this.bandwidth = bandwidth;
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