package DataToNeo4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import SparseMatrix.SparseMatrix;

/**
 * @author Iron
 * @date 2015Äê6ÔÂ5ÈÕ
 * Generate cooperate edges by DBLP database and import it into Neo4j database
 * to make the author cooperation graph more dense. 
 */
public class GenerateEdge {
	SparseMatrix subSparse;
	SparseMatrix sparse;
	HashMap<String, Integer> subLabel = new HashMap<String, Integer>();
	HashMap<String, Integer> label = new HashMap<String, Integer>();
	HashMap<Integer, Integer> idToSubId;
	public GenerateEdge(String subMatrixFilepath, String matrixFilepath, String subLabelFilepath, String labelFilepath) throws IOException{
		GetLabel(subLabel, subLabelFilepath);
		GetLabel(label, labelFilepath);
		MapId();
		sparse = new SparseMatrix(matrixFilepath);
		int[] newIdToOldId = sparse.GetSubMatrix(idToSubId);
		subSparse = new SparseMatrix(subMatrixFilepath);
		ImportEdge(newIdToOldId);
		subSparse.SaveMatrixByFile("imported_" + subMatrixFilepath);
	}
	public static void main(String[] args) throws IOException{
		GenerateEdge ge = new GenerateEdge("neo4j_matrix.txt", "dblp_matrix.txt", "neo4j_authors.txt", "dblp_author.txt");
	}
	private void GetLabel(HashMap<String, Integer> label, String filepath) throws NumberFormatException, IOException{
		File file = new File(filepath);
		if(!file.exists()|| file.isDirectory() || label == null){
	    	 throw new FileNotFoundException();
	    }
		BufferedReader br = new BufferedReader(new FileReader(file));
		int length = Integer.parseInt(br.readLine());
		for(int i = 0; i < length; ++i){
			String str = br.readLine();
			label.put(str, i);
		}
		br.close();
	}
	private void MapId(){
		Iterator<String> iter = subLabel.keySet().iterator();
		idToSubId = new HashMap<Integer, Integer>();
		while(iter.hasNext()){
			String str = iter.next();
			if(label.containsKey(str)){
				idToSubId.put(label.get(str), subLabel.get(str));
			}
		}
	}
	void ImportEdge(int[] newIdToOld){
		int importedEdgeNum = 0;
		for(int i = 0; i <sparse.GetRowNum(); ++i){
			int curSubMatrixId = idToSubId.get(newIdToOld[i]);
			HashMap<Integer, Float> row = sparse.GetRowElements(i);
			HashMap<Integer, Float> subRow = subSparse.GetRowElements(curSubMatrixId);
			Iterator<Integer> iter = row.keySet().iterator();
			while(iter.hasNext()){
				int curId = iter.next();
				if(!subRow.containsKey(idToSubId.get(newIdToOld[curId]))){
					subRow.put(idToSubId.get(newIdToOld[curId]), (float) 1.0);
					++importedEdgeNum;
				}
			}
		}
		System.out.println(importedEdgeNum);
	}
}
