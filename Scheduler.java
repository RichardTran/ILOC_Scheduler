import java.util.ArrayList;
import java.util.Scanner;
import java.util.Hashtable;

class Edge{
	int endLineNum; // the line number the edge is pointing to
	int dependType; // 0: no child | 1: anti dependency | 2: true dependency
	public Edge(int lineNum, int type){
		endLineNum = lineNum;
		dependType = type;
	}
}

// At most two pieces of information are always on either side of the '=>'
class LineInfo{

	int latency;
	int 	
	String cmd;
	String[] regReads;
	String[] regWrites;
	ArrayList<Edge> edges;

	public LineInfo(String cmd, String[] reads, String[] writes){
		if(cmd.equalsIgnoreCase("nop") || cmd.equalsIgnoreCase("addI") || 
			cmd.equalsIgnoreCase("add") || cmd.equalsIgnoreCase("subI") || 
			cmd.equalsIgnoreCase("sub") || cmd.equalsIgnoreCase("loadI") ||
			cmd.equalsIgnoreCase("output"))
		{
			latency = 1;
		}
		else if(cmd.equalsIgnoreCase("mult") || cmd.equalsIgnoreCase("div")) 
		{
			latency = 3;
		}
		else if(cmd.equalsIgnoreCase("load") || cmd.equalsIgnoreCase("loadAO") ||
				cmd.equalsIgnoreCase("loadAI") || cmd.equalsIgnoreCase("store") ||
				cmd.equalsIgnoreCase("storeAO") || cmd.equalsIgnoreCase("storeAI"))
		{
			latency = 5;
		}
		else{
			System.err.println("Not a real command");
		}
		this.cmd = cmd;
		regReads = reads;
		regWrites = writes;
		edges = new ArrayList<Edge>();
	}

	public void AddEdge(Edge e){
		for(int i = 0; i < edges.size(); i++){
			Edge temp = edges.get(i);
			if(temp.endLineNum == e.endLineNum){ // Edge already exists between the nodes. Must determine dependency
				if(temp.dependType >= e.dependType){
					return;
				}
				else{
					// If e is greater, then simply update current dependency to a true dependency
					temp.dependType = e.dependType;
					return;
				}
			}
		}
		edges.add(e);
	}
}

public class Scheduler{	

	// Using an ArrayList is ideal as each index would represent a line number of a code block
	static ArrayList<LineInfo> LoadLineInfo(){
		Scanner scan = new Scanner(System.in);
		ArrayList<LineInfo> codeBlock = new ArrayList<LineInfo>();
		String cmd = null;
		String[] reads;
		String[] writes;
		boolean foundArrow = false;
		int readPos;
		int writePos;

		while(scan.hasNextLine()){
			String[] temp = scan.nextLine().split(",*(\\s+)");
			cmd = temp[1];
			readPos = 0;
			writePos = 0;
			foundArrow = false;
			reads = new String[2];
			writes = new String[2];
			// Unique commands get their own special IF
			/*if(cmd.equalsIgnoreCase("nop")){
				int i = 0; // code that doesn't matter
			}*/
			// If line is nop, then temp.length should be 2.
			for(int i = 2; i < temp.length; i++){
				if(temp[i].equalsIgnoreCase("=>")){
					foundArrow = true;
				}
				else if(foundArrow == false){
					reads[readPos] = temp[i];
					readPos++;
				}
				else if(foundArrow == true){
					writes[writePos] = temp[i];
					writePos++;
				}
			}
			LineInfo data = new LineInfo(cmd,reads,writes);
			codeBlock.add(data);
		}
		return codeBlock;
	}
	
	// After finishing this method, DO NOT remove the entry from hash write. could be more anti-dependencies
	static void Update_RW_Depend(LineInfo li, String key, Hashtable<String, Integer> htr, Hashtable<String, Integer> htw){
		if(htw.get(key) != null){
			// Create anti-dependencies between LineInfo nodes
			int endLineNum = htw.get(key); // retrieve the node from hashtable with Key
			int dependType = 1;
			li.AddEdge(new Edge(endLineNum, dependType));
		}
	}
	
	// If a true dependency occurs, remove the key entry and it's values from the reads hash table as.
	// Steps: Create the dependencies
	// 		  Remove register (example r1) from the reads, and its arraylist
	//  
	static void Update_WR_Depend(LineInfo li, String key, Hashtable<String, ArrayList<Integer>> htr, Hashtable<String, Integer> htw){
		if(htr.get(key) != null){
			ArrayList<Integer> endLineNums = htr.get(key); // retrieve all lineNums that read from this register
			int dependType = 2;
			for(int i = 0; i < endLineNums.size(); i++){
				endLineNum = endLineNums.get(i)
				li.AddEdge(new Edge(endLineNum, dependType));
			}
			htr.remove(key);
		}

		// The "else" case is that we have not have not read from the register before,
		// and therefore we do not need to do anything else.
	}

// Used to add the registers we just read from the current line
// searching for any depdendencies
	static void UpdateReadHash(Hashtable htr, String key, int lineNum){
		if(key == null){
			return;
		}
		ArrayList oldEntry = htr.get(key);
		if(oldEntry != null){
			oldEntry.add(lineNum);
		}
		else{
			ArrayList<Integer> newEntry = new ArrayList<Integer>();
			newEntry.add(lineNum); 
			htr.add(key, newEntry);
		}
	}
// Used to add the registers we just wrote to, from the current lime.
// Only need to keep track of one write.
	static void UpdateWriteHash(Hashtable htw, String key, int lineNum){
		if(key == null){
			return;
		}
		if(htw.get(key) != null){
			htw.remove(key);
		}
		htw.add(key, lineNum);
	}


/* 
 * If there exists any dependency, have to put the instruction earlier in the code no matter
 * if it is a true or anti dependency. Anti-dependeny only indicates that it can immediately run the next command
 * 
 * True dependency has priority over anti-dependency and makes it require the cmd's latency 
 *		| Value of 2 in Edge.dependType
 * Anti dependency indicates that it'll take one cycle, and then we can use the next line 
 *		| Value of 1 in Edge.dependType
 */	
	static Node SetupTree(ArrayList<LineInfo> codeBlock){
		Hashtable<String, Integer> htr = new Hashtable<String, ArrayList<Integer>>(); // hash table of regs read from
		Hashtable<String, Integer> htw = new Hashtable<String, Integer>(); // hash table of regs written to
	
		int lineNumber = codeBlock.size() - 1;
		ArrayList<Integer> = new ArrayList<Integer>();
		
		// from bottom to top, scan block of code, and create dependency graph
		while(lineNumber >= 0){
			LineInfo li = codeBlock.get(lineNumber); 
			// Need 4 separate keys to add to hash table all at once per line
			String key1 = null; // read[0]
			String key2 = null; // read[1]
			String key3 = null; // write[0]
			String key4 = null; // write[1]
			String[] reads = li.regReads;
			String[] writes = li.regWrites;

			if(reads[1]!=null && reads[0].equalsIgnoreCase("r0")){
				int addr = 1024 + Integer.parseInt(reads[1]); //assumption that r0 always is 1024.
				key1 = String.valueOf(addr);
				Update_RW_Depend(li, key1, htr, htw);

			}
			else {
				// Checks for anti-dependency. htw must have written, then read above it.
				// Only keep key in hashtable if it does contain as others could've read as well
				if(reads[0]!=null){
					key1 = reads[0];
					Update_RW_Depend(li, key1, htr, htw);
				}
				if(reads[1]!=null){
					key2 = reads[1];
					Update_RW_Depend(li, key2, htr, htw);
				}
			}
			if(writes[1] != null && writes[0].equalsIgnoreCase("r0")){
				int addr = 1024 + Integer.parseInt(writes[1]); //assumption that r0 always is 1024.
				key3 = String.valueOf(addr);
				Update_WR_Depend(li, key3, htr, htw);
			}
			else{
				if(writes[0]!=null){
					key3 = writes[0];
					Update_WR_Depend(li, key3, htr, htw);
				}
				// This is an impossible case that will be only reached by storeAO.
				// ****** Have to change this *****
				if(writes[1]!=null){
					key4 = writes[1];
					Update_WR_Depend(li, key4, htr, htw);
				}
			}
			UpdateReadHash(htr, key1, lineNumber);
			UpdateReadHash(htr, key2, lineNumber);
			UpdateWriteHash(htr, key3, lineNumber);
			UpdateWriteHash(htr, key4, lineNumber);

			lineNumber = lineNumber - 1;
		}
		return null; // Can return an array of with two indicies; The tail, and the head
	}
	
	// Longest Latency Path
	static void ScheduleA(){
		
	}

	static void ScheduleB(){
		
	}

	static void ScheduleC(){
		
	}

	static void Rearrange(){
		
	}
	
	static void TestLineInfo(ArrayList<LineInfo> block){
		for(int i = 0; i < block.size(); i++){
			System.out.println("Command is: " + block.get(i).cmd);
			for(int j = 0; j < block.get(i).regReads.length; j++)
				System.out.println("Reads is: " + block.get(i).regReads[j]);
			for(int j = 0; j < block.get(i).regWrites.length; j++)
				System.out.println("Writes is: " + block.get(i).regWrites[j]);
		}
	}
	

	public static void main(String[] args){		
	
		ArrayList<LineInfo> codeBlock = LoadLineInfo();
		TestLineInfo(codeBlock);
		
		if(args.length == 1){

			if(args[0].equalsIgnoreCase("-a")){
				
			}
			else if(args[0].equalsIgnoreCase("-b")){
				// implement highest path cost
			}
			else if(args[0].equalsIgnoreCase("-c")){
				// implement custom
			}
			else{
				System.err.println("Incorrect argument");
				return;
			}
		}
		else{
			System.out.println("Please enter -a, -b, or -c as an argument");
		}
	}
}
/*
class Node{	
	int lineNum;
		// for easier access, keep track of both parents and children
	ArrayList<Edge> edges; // Assign node edge if lineNum1 < lineNum2 then lineNum1 -> lineNum2

	public Node(int num){
		lineNum = num;
		edges = new ArrayList<Edge>();
	}
	public void AddEdge(Edge e){
		edges.add(e);
	}
	public ArrayList<Edge> GetEdges(){
		return edges;
	}
}
*/