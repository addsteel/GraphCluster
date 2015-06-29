package SparseMatrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SparseMatrix {
	//I swear row = matrix.size() - 1;
	private int row;
	private int col;
	private boolean zeroOneSign = false;
	//All index are begin from 0
	private ArrayList<HashMap<Integer, Float>> matrix;
	//Initialed to be empty
	public SparseMatrix(boolean zeroOneSign){
		this.zeroOneSign = zeroOneSign;
		matrix = new ArrayList<HashMap<Integer, Float>>();
		row = 0;
		col = 0;
	}
	public SparseMatrix(ArrayList<HashMap<Integer, Float>> matrix){
		this.matrix = matrix;
		//set row and col
		//row most euqal ti matrix.size() 
		row = matrix.size();
		//Maybe I should find the maximum index of col in all rows of matrix
		col = row;
	}
	//set sparse matrix by inputted file
	public SparseMatrix(String filepath) throws IOException{
		File data = new File(filepath);
		if(!data.exists()|| data.isDirectory()){
	    	 throw new FileNotFoundException();
	    }
		matrix = new ArrayList<HashMap<Integer, Float>>();
		BufferedReader br = new BufferedReader(new FileReader(data));
		row = Integer.parseInt(br.readLine());
		col = Integer.parseInt(br.readLine());
		int colIndex = Integer.parseInt(br.readLine());
		float entryValue = 0;
		int colLengthInFile = 0;
		if(colIndex == 0){
			//the matrix is 0-1 matrix
			zeroOneSign = true;
			entryValue = (float)1.0;
		}
		for(int i = 0; i < row; ++i){
			HashMap<Integer, Float> cols = new HashMap<Integer, Float>();
			colLengthInFile = Integer.parseInt(br.readLine());
			for(int j = 0; j < colLengthInFile; ++j){
				colIndex = Integer.parseInt(br.readLine());
				if(colIndex >= col){
					matrix = null;
					col = 0;
					row = 0;
					System.out.println("SparseMatrix.SparseMatrix: Data Error");
					break;
				}
				if(zeroOneSign){
					cols.put(colIndex, entryValue);
				}
				else{
					entryValue = Float.parseFloat(br.readLine());
					cols.put(colIndex, entryValue);
				}
			}
			matrix.add(cols);
		}
		br.close();
	}
	//Wait to be implemented
	public void CopyMatrix(ArrayList<HashMap<Integer, Float>> matrix){
		
	}
	public void SetValue(int rowIndex, int colIndex, Float value){
		if(rowIndex >= row){
			while(row <= rowIndex){
				HashMap<Integer, Float> tmp = new HashMap<Integer, Float>();
				matrix.add(tmp);
				++row;
			}
		}
		if(colIndex >= col){
			col = colIndex + 1;
		}
		HashMap<Integer, Float> rowMap = matrix.get(rowIndex);
		if(!zeroOneSign) rowMap.put(colIndex, value);
		else rowMap.put(colIndex, (float)1.0);
	}
	public float GetValue(int rowIndex, int colIndex){
		if(rowIndex >= row){
			System.out.println("SparseMatrix.GetValue: row >= this.row");
			return -1;
		}
		if(!matrix.get(rowIndex).containsKey(colIndex)) return 0;
		return matrix.get(rowIndex).get(colIndex);
	}
	public HashMap<Integer, Float> GetRowElements(int rowIndex){
		if(rowIndex > row){
			System.out.println("SparseMatrix.SetValue: row > this.row");
			return null;
		}
		return matrix.get(rowIndex);
	}
	//Save matrix by file
	public void SaveMatrixByFile(String filepath) throws IOException{
		File file  = new File(filepath);
		FileWriter wr = new FileWriter(file);
		BufferedWriter wr_buf = new BufferedWriter(wr); 
		String data = null;
		if(zeroOneSign) wr.write(row + "\n" + col + "\n" + 0 + "\n");
		else wr.write(row + "\n" + col + "\n" + 1 + "\n");
		Iterator<Entry<Integer, Float>> iter = null;
		for(int i = 0; i < row; ++i){
			wr.write(matrix.get(i).size() + "\n");
			iter = matrix.get(i).entrySet().iterator();
			while(iter.hasNext()){
				Entry<Integer, Float> en = iter.next();
				wr.write(en.getKey() + "\n");
				if(!zeroOneSign){
					wr.write(en.getValue() + "\n");
				}
			}
		}
		wr.close();
	}
	/**
	 * @param rowIndexs: the indexes of row and col that will be saved
	 * @return the map from new matrix's id to old matrix's id
	 */
	//The function are only for matrix such that col == row
	public int[] GetSubMatrix(HashMap<Integer, Integer> rowIndexes){
		if( zeroOneSign == false){
			System.err.println("Have not been implemented for col != row or not zero-one matrix");
			return null;
		}
		//new id is the id in sub matrix
		//old id is the id in matrix
		int[] newIdToOldId = new int[rowIndexes.size()];
		int[] oldIdToNewId = new int[row];
		ArrayList<HashMap<Integer, Float>> subMatrix = new ArrayList<HashMap<Integer, Float>>();
		//
		int curNewId = 0;
		for(int i = 0 ; i < row; ++i){
			if(!rowIndexes.containsKey(i)){
				oldIdToNewId[i] = -1;
				continue;
			}
			subMatrix.add(matrix.get(i));
			newIdToOldId[curNewId] = i;
			oldIdToNewId[i] = curNewId;
			++curNewId;
		}
		matrix = null;
		System.gc();
		for(int i = 0; i < subMatrix.size(); ++i){
			HashMap<Integer, Float> tmp = new HashMap<Integer, Float>();
			Iterator<Map.Entry<Integer, Float>> iter = subMatrix.get(i).entrySet().iterator();
			while(iter.hasNext()){
				Map.Entry<Integer, Float> me = iter.next();
				//me.getKey() is in rowIndexs
				if(oldIdToNewId[me.getKey()] >= 0){
					tmp.put(oldIdToNewId[me.getKey()], me.getValue());
				}
			}
			subMatrix.set(i, tmp);
			if(i % 4096 == 0){
				System.out.println("GetSubMatrix.i = " + i);
				System.gc();
			}
		}
		matrix = subMatrix;
		row = col = matrix.size();
		int[] result;
		//If here are elements in rowIndexs out of [0, row), 
		//newIdToOldId is larger than row, which may take some bugs.
		if(newIdToOldId.length != row){
			result = new int[row];
			for(int i = 0; i < result.length; ++i){
				result[i] = newIdToOldId[i];
			}
		}
		else{
			result = newIdToOldId;
		}
		return result;
	}
	public int GetRowNum(){
		return row;
	}
	public int GetRowSizeByIndex(int rowIndex){
		if(rowIndex >= row) return -1;
		return matrix.get(rowIndex).size();
	}
}
