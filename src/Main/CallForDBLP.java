package Main;

import java.io.IOException;

import Cluster.ClusterRN;

public class CallForDBLP {
	public static void main(String[] args) throws IOException{
		ClusterRN rn = new ClusterRN("demo_matrix.txt", "demo_cluster_result.txt", (float)1, 1000);
	}
}
