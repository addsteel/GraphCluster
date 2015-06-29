package GetMatrix;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import SparseMatrix.SparseMatrix;

enum RelTypes implements RelationshipType {
	CO_AUTHOR
}
public class GetMatrixByNeo4j {
	private static GraphDatabaseService graphDb;
	private static String DBPATH;
	private SparseMatrix sparse = new SparseMatrix(true);
	private HashMap<String, Integer> nameToId;
	private HashMap<Integer, String> idToName;
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
	public GetMatrixByNeo4j(String neo4jFilepath, String matrixFilepath, String authorsNameFilepath) throws IOException{
		DBPATH = neo4jFilepath;
		//= "E:\\Code\\JAVA\\CommunityDetection\\acmlib2.db\\acmlib2.db"
		GetData();
		sparse.SaveMatrixByFile(matrixFilepath);
		SaveAuthorNames(authorsNameFilepath);
	}
	private void GetData(){
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DBPATH);
		registerShutdownHook(graphDb);
		nameToId = new HashMap<String, Integer>();
		idToName = new HashMap<Integer, String>();
		int edge_num  = 0;
		int author_id = 0;
		{
			Label authorlabel = DynamicLabel.label("Author");
			try(Transaction tx = graphDb.beginTx()){
				try (ResourceIterator<Node> authorIterator = graphDb.findNodes(authorlabel)) {
					while (authorIterator.hasNext()) {
						Node node = authorIterator.next();
						String nodeName = node.getProperty("name").toString();
						if(!nameToId.containsKey(nodeName)){
							nameToId.put(nodeName, author_id);
							idToName.put(author_id, nodeName);
							++author_id;
						}
					}
				}
				tx.success();
			}
		}
		System.out.println(author_id);
		{
			Label paperLabel = DynamicLabel.label("Article");
			Label authorLabel = DynamicLabel.label("Author");
			try(Transaction tx = graphDb.beginTx()){
				try(ResourceIterator<Node> paperIterator = graphDb.
						findNodes(paperLabel)){
					while(paperIterator.hasNext()){
						Node paperNode = paperIterator.next();
						Iterable<Relationship> rels = paperNode.getRelationships();
						Iterator<Relationship> relIterator = rels.iterator();
						ArrayList<Node> authors = new ArrayList<Node>();
						while(relIterator.hasNext()){
							Relationship rel = relIterator.next();
							if(rel.getOtherNode(paperNode).hasLabel(authorLabel)){
								authors.add(rel.getOtherNode(paperNode));
							//	System.out.println("Fine");
							}
						}
						for(int i = 0; i < authors.size(); ++i){
							String curName = authors.get(i).getProperty("name").toString();
							int curIndex = nameToId.get(curName);
							for(int j = i + 1; j < authors.size(); ++j){
								String otherName =  authors.get(j).getProperty("name").toString();
								int otherIndex = nameToId.get(otherName);
								//Maybe there are two copies for one author
								if(otherIndex != curIndex){
									sparse.SetValue(otherIndex, curIndex, (float) 1.0);
									sparse.SetValue(curIndex, otherIndex, (float) 1.0);
									++edge_num;
								}
							}
						}
					}
				}
				tx.success();
			}
		}
		graphDb.shutdown();
		System.out.println(edge_num);
	}
	private void SaveAuthorNames(String authorsNameFilepath) throws IOException{
		File file = new File(authorsNameFilepath);
		FileWriter wr = new FileWriter(file);
		wr.write(idToName.size() + "\n");
		for(int i = 0; i < idToName.size(); ++i){
			wr.write(idToName.get(i) + "\n");
		}
		wr.close();
	}
	private void shutdownDb() {
		graphDb.shutdown();
	}
}
