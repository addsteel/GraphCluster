package GetMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import SparseMatrix.SparseMatrix;

public class GetMatrixByDBLP {
	private int authorId = 0;
	private int minDegree;
	private HashMap<String, Integer> nameToId = new HashMap<String, Integer>();
	private HashMap<Integer, String> idToName = new HashMap<Integer, String>();
	private ArrayList<Integer> authorBuf = new ArrayList<Integer>();
	private SparseMatrix sparse = new SparseMatrix(true);
	public GetMatrixByDBLP(String dataFilepath, String matrixFilepath, String authorsFilepath, int minDegree) throws IOException{
		 ReadXml(dataFilepath, matrixFilepath, authorsFilepath);
		 this.minDegree = minDegree;
	}
	private void ReadXml(String dataFilepath, String matrixFilepath, String authorsFilepath) throws IOException{
		File file=new File(dataFilepath);
	     if(!file.exists()|| file.isDirectory()){
	    	 throw new FileNotFoundException();
	     }
		 BufferedReader br = new BufferedReader(new FileReader(file));
		 String line = new String();
		 line = br.readLine().trim();
		 //The first line must be <dblp>
		 while(!line.equals("<dblp>")){
			 line = br.readLine().trim();
		 }
		 System.out.println(line);
		 String endLabel;
		 boolean endSign = true;
		 while(endSign){
			 line = br.readLine().trim();
			 if(line.equals("</dblp>")){
				//End
				 endSign = false;
				 break;
			 }
			//Here are two types of label, <proceedings> or <inproceedings>
			 //They are the label for the article published in conference
			 else if(line.length() > 12 && line.substring(0, 12).equals("<proceedings")){
				 endLabel = "</proceedings";
			 }
			 else if(line.length() > 14 && line.substring(0, 14).equals("<inproceedings")){
				 endLabel = "</inproceedings";
			 }
			 else{
				 continue;
			 }
			 line = br.readLine().trim();
			 //Run until end label, </proceedings> or </inproceedings>
			 //Get all authors who write this article and save their id in authorBuf initialed empty before 'while' run
			 //If there are some new authors, assign them a new author id
			 //and save them by nameToId and idToName.
			 authorBuf.clear();
			 while(!(line.length() > endLabel.length() && line.substring(0, endLabel.length()).equals(endLabel))){
				 if(line.length() > 7 && line.substring(0, 8).equals("<author>")){
					 //I guess that there one and only one line for an author
					 String authorName = line.substring(8, line.length() - 9).trim();
					 if(!nameToId.containsKey(authorName)){
						 //new author
						// System.out.println(authorName);
						 nameToId.put(authorName, authorId);
						 idToName.put(authorId, authorName);
						 authorBuf.add(authorId);
						 ++authorId;
						 if(authorId % 1024 == 0) System.out.println(authorId);
					 }
					 else{
						 authorBuf.add(nameToId.get(authorName));
					 }
				 }
				 line = br.readLine().trim();
			 } 
			 for(int i = 0; i < authorBuf.size(); ++i){
				 for(int j = i + 1; j < authorBuf.size(); ++j){
					 sparse.SetValue(authorBuf.get(i), authorBuf.get(j), (float)1.0);
					 sparse.SetValue(authorBuf.get(j), authorBuf.get(i), (float)1.0);
				 }
			 }
		 }
		 System.out.println("The number of all authors: " + sparse.GetRowNum());
		 if(!nameToId.containsKey("Jiawei Han")){
			 System.err.println("No Jiawei Han");
			 return ;
		 }
		 int initId = nameToId.get("Jiawei Han");
		 HashMap<Integer, Integer> saveId = new HashMap<Integer, Integer>();
		 saveId.put(initId, 0);
		 Queue<Integer> queue = new LinkedList<Integer>();
		 queue.offer(initId);
		 while(!queue.isEmpty()){
			 int curId = queue.poll();
			 Iterator<Integer> iter = sparse.GetRowElements(curId).keySet().iterator();
			 while(iter.hasNext()){
				 int adjacencyId = iter.next();
				// System.out.println("Out " + idToName.get(adjacencyId));
				 if(!saveId.containsKey(adjacencyId) && sparse.GetRowSizeByIndex(adjacencyId) > minDegree){
					// System.out.println("In " + idToName.get(adjacencyId));
					 queue.offer(adjacencyId);
					 saveId.put(adjacencyId, 1);
				 }
			 }
		 }
		System.out.println("The number of authors: " + saveId.size());
		 
		 int[] newIdToOldId = sparse.GetSubMatrix(saveId);
		 System.out.println(newIdToOldId.length);
		 System.out.println("Begin Write Matrix");
		 sparse.SaveMatrixByFile(matrixFilepath);
		 file = new File(authorsFilepath);
		 FileWriter wr = new FileWriter(file);
		 System.out.println("Begin Write Authors: newIdToOldId.length = " + newIdToOldId.length);
		 wr.write(newIdToOldId.length + "\n");
		 for(int i = 0; i < newIdToOldId.length; ++i){
			 wr.write(idToName.get(newIdToOldId[i]) + "\n");
		 }
		 wr.close();
	}
}
