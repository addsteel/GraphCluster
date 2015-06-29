package DataToNeo4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.helpers.collection.IteratorUtil;

import SparseMatrix.SparseMatrix;

/**
 * @author Iron
 * @date 2015Äê6ÔÂ5ÈÕ
 * Put the cluster node to Neo4j database and connect author node to correspond cluster node.
 */
public class PutClusterToNeo4j {
	private static GraphDatabaseService graphDb;
	private static String DBPATH;
	enum RelTypes implements RelationshipType {
		BELONG_TO
	}
	SparseMatrix sparse;
	HashMap<String, Integer> nameToId;
	int[] authorsClusterId;
	//<cluster_id, 1>
	HashMap<Integer, Integer> clusterIdSet;
	public PutClusterToNeo4j(String neo4jFilepath, String filepath, String authorsFilepath, String clusterFilepath) throws IOException{
		DBPATH = neo4jFilepath;
		sparse = new SparseMatrix(filepath);
		GetAuthorsName(authorsFilepath);
		GetCluster(clusterFilepath);
		PutCluster();
	}
	private void GetAuthorsName(String authorsFilepath) throws IOException{
		File data = new File(authorsFilepath);
		if(!data.exists()|| data.isDirectory()){
	    	 throw new FileNotFoundException();
	     }
		 BufferedReader br = new BufferedReader(new FileReader(data));
		String line = "";
		nameToId = new HashMap<String, Integer>();
		int author_id = 0;
		int length = Integer.parseInt(br.readLine());
		if(length != sparse.GetRowNum()){
			System.err.println("PutClusterToNeo4j.GetAuthorsName: length != sparse.GetRowNum()");
			System.exit(-1);
		}
		while((line = br.readLine()) != null){
			nameToId.put(line, author_id);
			++author_id;
		}
		System.out.println("PutClusterToNeo4j.GetAuthorsName" + author_id);
	}
	private void GetCluster(String clusterFilepath) throws IOException{
		File data = new File(clusterFilepath);
		if(!data.exists()|| data.isDirectory()){
	    	 throw new FileNotFoundException();
	     }
		 BufferedReader br = new BufferedReader(new FileReader(data));
		String line = "";
		int length = Integer.parseInt(br.readLine());
		if(length != sparse.GetRowNum()){
			System.err.println("PutClusterToNeo4j.GetCluster: length != sparse.GetRowNum()");
			System.exit(-1);
		}
		authorsClusterId = new int[length];
		clusterIdSet = new HashMap<Integer, Integer>();
		int authorId = 0;
		while((line = br.readLine()) != null){
			//System.out.println(line);
			authorsClusterId[authorId] = Integer.parseInt(line);
			if(!clusterIdSet.containsKey(authorsClusterId[authorId])){
				clusterIdSet.put(authorsClusterId[authorId], 1);
			}
			++authorId;
		}
	}
	private void PutCluster(){
		System.out.println("PutCluster");
		int clusterSize = sparse.GetRowNum();
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DBPATH);
		Label clusterLabel = DynamicLabel.label("Community");
		Label authorLabel = DynamicLabel.label("Author");
		registerShutdownHook(graphDb);
		{
			//
			try (Transaction deleteNode = graphDb.beginTx();
					Result result = graphDb.execute( "match (n:Community)-[r]-() delete n,r")){
				    //Delete all community nodes
				deleteNode.success();
			}
			Node[] nodes = new Node[clusterSize];
			HashMap<Integer, Integer> idToIndex = new HashMap<Integer, Integer>();
			try(Transaction tx = graphDb.beginTx()){
				Iterator<Integer> iter = clusterIdSet.keySet().iterator();
				int index = 0;
				while(iter.hasNext()){
					int id = iter.next();
					nodes[index] = graphDb.createNode(clusterLabel);
					nodes[index].setProperty("id", id);
					idToIndex.put(id, index);
					++index;
				}
				try(ResourceIterator<Node> authorIterator = graphDb
						.findNodes(authorLabel)){
					while(authorIterator.hasNext()){
						Node node = authorIterator.next();
						String name = node.getProperty("name").toString();
						if(!nameToId.containsKey(name)){
							System.out.println(name);
							continue;
						}
						int author_id = nameToId.get(name);
						index = idToIndex.get(authorsClusterId[author_id]);
						//System.out.println(index);
						Relationship rel = node.createRelationshipTo(nodes[index], RelTypes.BELONG_TO);
					}
				}
				tx.success();
			}
		}
		graphDb.shutdown();
	}
	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
	private void shutdownDb() {
		graphDb.shutdown();
	}
}

