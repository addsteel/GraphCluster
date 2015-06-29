package SparseMatrix;

import java.io.IOException;
import java.util.HashMap;

import GetMatrix.GetMatrixByDBLP;
import GetMatrix.GetMatrixByNeo4j;

public class Call {
	public static void main(String[] args) throws IOException{
		GetMatrixByDBLP gmb = new GetMatrixByDBLP("demo.xml", "demo_matrix.txt", "demo_author.txt", 0);
		//GetMatrixByDBLP gmb = new GetMatrixByDBLP("dblp.xml", "dblp_matrix.txt", "dblp_author.txt", 3);
		//GetMatrixByNeo4j gmbn = new GetMatrixByNeo4j("new4j_matrix.txt", "new4j_authors.txt");
		SparseMatrix sm = new SparseMatrix("demo_matrix.txt");
		//System.out.println(sm.GetRowNum());
	}
}
