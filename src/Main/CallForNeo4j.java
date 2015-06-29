package Main;

import java.io.IOException;
import Cluster.ClusterRN;
import DataToNeo4j.PutClusterToNeo4j;
import DataToNeo4j.PutCooperate;
import GetMatrix.GetMatrixByNeo4j;
public class CallForNeo4j {
	public static void main(String[] args) throws IOException{
		String DBPATH = "E:\\Code\\JAVA\\CommunityDetection\\acmlib2.db\\acmlib2.db";
		//GetMatrixByNeo4j gmbn = new GetMatrixByNeo4j(DBPATH, "neo4j_matrix.txt", "neo4j_authors.txt");
		//ClusterRN rn = new ClusterRN("neo4j_matrix.txt", "neo4j_cluster_result.txt", (float)1, Integer.MAX_VALUE);
		//ClusterRN rn = new ClusterRN("imported_neo4j_matrix.txt", "neo4j_cluster_result.txt", (float)1, Integer.MAX_VALUE);
		PutClusterToNeo4j pctn = new PutClusterToNeo4j(DBPATH, "imported_neo4j_matrix.txt", "neo4j_authors.txt", "neo4j_cluster_result.txt"); 	
		PutCooperate pc = new PutCooperate(DBPATH, "imported_neo4j_matrix.txt", "neo4j_authors.txt");
	}
}
