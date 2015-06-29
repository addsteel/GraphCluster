package Cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import SparseMatrix.SparseMatrix;

/**
 * @author Iron
 * @date 2015��6��4��
 *
 */
public class ClusterRN {
	SparseMatrix sparse;
	//<cluster_id, <author_id in cluster_id, 1>>
	HashMap<Integer, HashMap<Integer, Integer>> clusters;
	//<author_id, author_id's cluster_id>
	int[] nodesClusterId;
	//<cluster_id, sum of nodes' degree in cluster_id>
	HashMap<Integer, Integer> clustersDegSum;
	//Agorithm's parameter which is larger than 0. 
	//The smaller of gamma, the larger of cluster's size
	//and the lower of cluster coefficient.
	float gamma;
	//Max time of MoveNode running
	int maxTime;
	Random rd1;
	//Here are two types ClusterRN, 
	//one training basing on the cluster result of resultFilepath
	//if it exists and set continueSign true while another not
	public ClusterRN(String matrixFilepath, String resultFilepath, float gamma, int maxTime) throws IOException{
		this.gamma = gamma;
		this.maxTime= maxTime;
		Run(matrixFilepath, resultFilepath, false);
	}
	public ClusterRN(String matrixFilepath, String resultFilepath, float gamma, int maxTime, boolean continueSign) throws IOException{
		this.gamma = gamma;
		this.maxTime= maxTime;
		Run(matrixFilepath, resultFilepath, continueSign);
	}
	private void Run(String matrixFilepath, String resultFilepath, boolean continueSign) throws IOException{
		sparse = new SparseMatrix(matrixFilepath);
		InitCluster(resultFilepath, false);
		RN();
		Print(resultFilepath);
		System.out.println("ClusterRN.clusters.size = " + clusters.size());
	}
	//��ʼ�������������
	//resultFilepath�����ڻ���breakpointSign == fasle,���ʼ��ʱ��ÿ���㶼��һ�����࣬ÿ�������н���һ����
	//���򣬶�ȡresultFilepath������resultFilepath���־��࣬����ʼ��clusters��clustersDegSum
	void InitCluster(String resultFilepath, boolean breakpointSign) throws IOException{
		System.out.println("ClusterRN.InitCluster: BEGIN");
		rd1 = new Random();
		nodesClusterId = new int[sparse.GetRowNum()];
		clusters = new HashMap<Integer, HashMap<Integer, Integer>>();
		clustersDegSum = new HashMap<Integer, Integer>();
		File file = new File(resultFilepath);
		if(breakpointSign == false ||!file.exists()|| file.isDirectory()){
			//All nodes as a cluster
			for(int i = 0; i < nodesClusterId.length; ++i){
				nodesClusterId[i] = i;
			}
	    }
		else{
			//Read the cluster result from resultFilepath
			//Later program will cluster basing on the inputed cluster result
			BufferedReader br = new BufferedReader(new FileReader(file));
			int length = Integer.parseInt(br.readLine());
			if(nodesClusterId.length != length){
				System.err.println("ClusterRN.InitCluster: nodesClusterId.length != length");
				System.exit(-1);
			}
			for(int i = 0; i < nodesClusterId.length; ++i){
				nodesClusterId[i] = Integer.parseInt(br.readLine());
			}
			System.out.println("Cluster.InitCluster will overwrite " + resultFilepath);
		}
		for(int i = 0; i <nodesClusterId.length; ++i){
			HashMap<Integer, Integer> tmp = null;
			if(clusters.containsKey(nodesClusterId[i])){
				tmp = clusters.get(nodesClusterId[i]);
				tmp.put(i, 1);
				int sum = clustersDegSum.get(nodesClusterId[i]);
				clustersDegSum.put(nodesClusterId[i], sum+sparse.GetRowSizeByIndex(i));
			}
			else{
				tmp = new HashMap<Integer, Integer>();
				tmp.put(i, 1);
				clusters.put(nodesClusterId[i], tmp);
				clustersDegSum.put(nodesClusterId[i], sparse.GetRowSizeByIndex(i));
			}
		}
		System.out.println("ClusterRN.InitCluster: END");
	}	
	//RN Algorithm. More details are in website: "http://arxiv.org/abs/0803.2548v4" 
	//or "Local resolution-limit-free Potts model for community detection"
	void RN(){
		int runTime = 0;
		do{
			while(MoveNode() && runTime < maxTime){
				++runTime;
				System.out.println("ClusterRN.RN.while.runTime = " + runTime);
			}
			System.out.println("ClusterRN.RN.dowhile.runTime = " + runTime);
		}while(MergeCluster() && runTime < maxTime);
	}
	//�ھ�����ƶ������Ż�Ŀ�꺯��
	//���û�е���ƶ�������false�����򷵻�true
	boolean MoveNode(){
		boolean result = false;
		int initNode = rd1.nextInt(sparse.GetRowNum());
		for(int i = 0; i < nodesClusterId.length; ++i){
			//�����ĳ���㿪ʼ�ƶ������水���±�˳�������
			//������sparse.GetRowNum()ʱ������
			int curNode= (i + initNode)%sparse.GetRowNum();
			//��curNode���ڵĵ�
			HashMap<Integer, Float> adjNodes = sparse.GetRowElements(curNode);
			//System.out.println("adjNodes.length = " + adjNodes.size());
			//<�ڲ��е��curNode���ڵľ����ID����������curNode���ڵĵ���size>
			//<cluster_id, nodes size>
			HashMap<Integer, Integer> adjClusters = new HashMap<Integer, Integer>();
			//��������ʱ����һ
			//��Ҫ��Ϊ�˺�����ʽ�ϵ�ͳһ
			adjClusters.put(nodesClusterId[curNode], 1);
			//Set adjClusters
			Iterator<Integer> nodeIter =  adjNodes.keySet().iterator();
			while(nodeIter.hasNext()){
				int adjNode = nodeIter.next();
				if(adjClusters.containsKey(nodesClusterId[adjNode])){
					int elemsNum = adjClusters.get(nodesClusterId[adjNode]);
					adjClusters.put(nodesClusterId[adjNode],elemsNum+1);
					//System.out.println("elemNum = " + elemsNum + 1);
				}
				else{
					adjClusters.put(nodesClusterId[adjNode], 1);
				}
			}
			//Calculate the energy loss of move curNode to nodesClusterId[curNode]
			//when it is a cluster along
			//or the energy gain of move it to be along
			float maxEnergy = adjClusters.get(nodesClusterId[curNode]) - 1
					- gamma * sparse.GetRowSizeByIndex(curNode) * 
					(clustersDegSum.get(nodesClusterId[curNode])- sparse.GetRowSizeByIndex(curNode))/(float)sparse.GetRowNum();
			int maxClusterId = -1;
			adjClusters.remove(nodesClusterId[curNode]);
			Iterator<Entry<Integer, Integer>> clusterIter = adjClusters.entrySet().iterator();
			//Calculate the max energy loss of moving curNode to other clusters
			while(clusterIter.hasNext()){
				Entry<Integer, Integer> en  = clusterIter.next();
				//Calculate the energy loss of moving curNode to cluster of en.key
				float curEnergy = en.getValue() - gamma * sparse.GetRowSizeByIndex(curNode) * clustersDegSum.get(en.getKey())/(float)sparse.GetRowNum(); 
				if(curEnergy > maxEnergy){
					maxClusterId = en.getKey();
					maxEnergy = curEnergy;
				}
			}
			//Here are cluster that the energy loss of move curNode to it is smaller
			//than the energy gain of stay still
			//Move curNode from current cluster to maxClusterId
			if(maxClusterId != -1){
				int degSum = clustersDegSum.get(maxClusterId);
				clustersDegSum.put(maxClusterId, degSum + sparse.GetRowSizeByIndex(curNode));
				clusters.get(maxClusterId).put(curNode, 1);
				//if the size of cur cluster is less than 2, then we remove it since it will be empty
				if(clusters.get(nodesClusterId[curNode]).size() > 1){
					degSum = clustersDegSum.get(nodesClusterId[curNode]);
					clustersDegSum.put(nodesClusterId[curNode], degSum - sparse.GetRowSizeByIndex(curNode));
					clusters.get(nodesClusterId[curNode]).remove(curNode);
				}
				else{
					clustersDegSum.remove(nodesClusterId[curNode]);
					clusters.remove(nodesClusterId[curNode]);
				}
				nodesClusterId[curNode] = maxClusterId;
				result = true;
			}
		}
		return result;
	}
	//�ϲ��������Ż�Ŀ�꺯��
	//������ִ��ھ���ϲ������Ż���������������������true
	//��������ڷ���false
	private boolean MergeCluster(){
		boolean result = false;
		//record the clusters that have been observed 
		HashMap<Integer, Integer> oldClusters = new HashMap<Integer, Integer>();
		Iterator clusterIter = clusters.entrySet().iterator();
		int curClusterId;
		while(clusterIter.hasNext()){
			Entry<Integer, HashMap<Integer, Integer>> clusterEn = (Entry<Integer, HashMap<Integer, Integer>>) clusterIter.next();
			curClusterId = clusterEn.getKey();
			//Record the cluster id that have been calculated
			//All nodes in cluster that oldClusters contain will not be count
			oldClusters.put(curClusterId, 1);
			//<cluster_id that there exist nodes connect to nodes in current cluster, 
			//the number of edges that connect cluster_id and current cluster>
			HashMap<Integer, Integer> adjClusters = new HashMap<Integer, Integer>();
			Iterator<Integer> nodeIter = clusterEn.getValue().keySet().iterator();
			//Set adjClusters
			while(nodeIter.hasNext()){
				Iterator<Integer> adjNodeIter = sparse.GetRowElements(nodeIter.next()).keySet().iterator();
				while(adjNodeIter.hasNext()){
					int adjNodeClu = nodesClusterId[adjNodeIter.next()];
					if(oldClusters.containsKey(adjNodeClu)){
						continue;
					}
					if(adjClusters.containsKey(adjNodeClu)){
						int size = adjClusters.get(adjNodeClu);
						adjClusters.put(adjNodeClu, size+1);
					}
					else{
						adjClusters.put(adjNodeClu, 1);
					}
				}
			}
			float maxEnergy = 0;
			int maxClusterId = -1;
			Iterator<Entry<Integer, Integer>> adjClusterIter = adjClusters.entrySet().iterator();  
			//If here is a cluster that merging the cluster and current cluster will
			//decrease the energy
			while(adjClusterIter.hasNext()){
				Entry<Integer, Integer> en = adjClusterIter.next();
				float curEnergy = en.getValue() - gamma * clustersDegSum.get(curClusterId) * clustersDegSum.get(en.getKey()) / (float)sparse.GetRowNum();
				if(curEnergy > maxEnergy){
					maxEnergy = curEnergy;
					maxClusterId = en.getKey();
				}
			}
			if(maxClusterId > 0){
				//updata clustersDegSum
				int degSum = clustersDegSum.get(curClusterId);
				clustersDegSum.remove(curClusterId);
				degSum += clustersDegSum.get(maxClusterId);
				clustersDegSum.put(maxClusterId, degSum);
				//updata nodesClusterId
				//updata clusters
				HashMap curNodes = clusters.get(curClusterId);
				clusters.remove(curClusterId);
				for(Object obj: curNodes.keySet()){
					nodesClusterId[(int)obj] = maxClusterId;
				}
				clusters.get(maxClusterId).putAll(curNodes);
				return true;
			}
		}
		return result;
	}
	//Save the cluster result by author id
	private void Print(String resultFilepath) throws IOException{
		File file = new File(resultFilepath);
		//FileOutputStream out = new FileOutputStream(file);
		FileWriter wr = new FileWriter(file);
		wr.write(nodesClusterId.length + "\n");
		for(int i = 0; i < nodesClusterId.length; ++i){
			wr.write(nodesClusterId[i] + "\n");
		}
		wr.close();
		//Output the information of cluster
		file = new File("log.txt");
		wr = new FileWriter(file);
		wr.write(clusters.size() + "\n");
		Iterator<Entry<Integer, HashMap<Integer, Integer>>> clusterIter = clusters.entrySet().iterator();
		while(clusterIter.hasNext()){
			int edgeNum = 0;
			Entry<Integer, HashMap<Integer, Integer>> en = clusterIter.next();
			HashMap<Integer, Integer> nodes = en.getValue();
			if(nodes.size() < 3){
				wr.write(nodes.size() + "\n");
				wr.write(0 + "\n");
				continue;
			}
			Iterator<Integer> nodeIter = nodes.keySet().iterator();
			while(nodeIter.hasNext()){
				Iterator<Integer> adjNodeIter = sparse.GetRowElements(nodeIter.next()).keySet().iterator();
				while(adjNodeIter.hasNext()){
					if(nodes.containsKey(adjNodeIter.next())){
						++edgeNum;
					}
				}
			}
			wr.write(nodes.size() + "\n");
			wr.write((float)edgeNum/(nodes.size() * (nodes.size() - 1)) + "\n");
		}
		wr.close();
	}
}
