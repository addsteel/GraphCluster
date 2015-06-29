package DataToNeo4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import SparseMatrix.SparseMatrix;

/**
 * @author Iron
 * @date 2015Äê6ÔÂ5ÈÕ
 * Put the relations of cooperate between authors into Neo4j database
 */
public class PutCooperate {
	private static GraphDatabaseService graphDb;
	private static String DBPATH;
	enum RelTypes implements RelationshipType {
		Cooperate
	}
	SparseMatrix sparse;
	HashMap<String, Integer> nameToId;
	HashMap<Integer, String> idToName;
	public PutCooperate(String neo4jFilepath, String matrixFilepath,String authorsFilepath) throws IOException{
		DBPATH = neo4jFilepath;
		sparse = new SparseMatrix(matrixFilepath);
		GetAuthorsName(authorsFilepath);
		ImportEdge();
	}
	public void ImportEdge() throws IOException{
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DBPATH);
		int coauthorCount = 0;
		registerShutdownHook(graphDb);
		{	
			try (Transaction deleteEdge = graphDb.beginTx();
					Result result = graphDb.execute( "match (n:Author)-[r: Cooperate]->(m:Author) delete r")){
				    //Delete all community nodes
				deleteEdge.success();
			}
			Label authorLabel = DynamicLabel.label("Author");
			try(Transaction tx = graphDb.beginTx()){
				try(ResourceIterator<Node> authorIterator = graphDb
						.findNodes(authorLabel)){
					while(authorIterator.hasNext()){
						Node node = authorIterator.next();
						String name = node.getProperty("name").toString();
						//if(!authors.containsKey(name)) continue;
						int authorId = nameToId.get(name);
						//System.out.println(authorId);
						Iterator iter = sparse.GetRowElements(authorId).keySet().iterator();
						while(iter.hasNext()){
							String coAuthorName = idToName.get((int)iter.next());
							//System.out.println(coAuthorName);
							if(name.equals(authorIterator)) continue;
							try(ResourceIterator<Node> coAuthorIterator = graphDb.findNodes(authorLabel, "name", coAuthorName)){
								while(coAuthorIterator.hasNext()){
									Node coNode = coAuthorIterator.next();
									node.createRelationshipTo(coNode, RelTypes.Cooperate);
									++coauthorCount;
								}
							}
						}
					}
				}
				tx.success();
				System.out.println("ImportEdge.Coauthor Number: " + coauthorCount);
			}
		}
		graphDb.shutdown();
	}
	private void GetAuthorsName(String authorsFilepath) throws IOException{
		File data = new File(authorsFilepath);
		if(!data.exists()|| data.isDirectory()){
	    	 throw new FileNotFoundException();
	     }
		 BufferedReader br = new BufferedReader(new FileReader(data));
		String line = "";
		nameToId = new HashMap<String, Integer>();
		idToName = new HashMap<Integer, String>();
		int author_id = 0;
		int length = Integer.parseInt(br.readLine());
		if(length != sparse.GetRowNum()){
			System.err.println("ImportEdge.GetAuthorsName: length != sparse.GetRowNum()");
			System.exit(-1);
		}
		while((line = br.readLine()) != null){
			nameToId.put(line, author_id);
			idToName.put(author_id, line);
			++author_id;
		}
		System.out.println("PutClusterToNeo4j.GetAuthorsName" + author_id);
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
