import java.util.ArrayList;
import java.util.Scanner;
import java.util.Hashtable;
import java.util.Collections;

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
	int llp;
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
		llp = 0;
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
	// Returns true if it creates a dependency. False otherwise
	static void Update_RW_Depend(LineInfo li, String key, Hashtable<String, Integer> htw, ArrayList<Integer> ready){
		if(htw.get(key) != null){
			// Create anti-dependencies between LineInfo nodes
			int endLineNum = htw.get(key); // retrieve the node from hashtable with Key
			int dependType = 1;
			li.AddEdge(new Edge(endLineNum, dependType));
			if(ready.contains(endLineNum)){
				Integer x = endLineNum;
				ready.remove((Integer)x);
			}
		}

	}
	
	// If a true dependency occurs, remove the key entry and it's values from the reads hash table as.
	// Steps: Create the dependencies
	// 		  Remove register (example r1) from the reads, and its arraylist
	//  
	// Returns true if it creates a dependency. False otherwise
	static void Update_WR_Depend(LineInfo li, String key, Hashtable<String, ArrayList<Integer>> htr, ArrayList<Integer> ready){
		if(htr.get(key) != null){
			ArrayList<Integer> endLineNums = htr.get(key); // retrieve all lineNums that read from this register
			int dependType = 2;
			for(int i = 0; i < endLineNums.size(); i++){
				int endLineNum = endLineNums.get(i);
				if(ready.contains(endLineNum)){
					Integer x = endLineNum;
					ready.remove((Integer)x);
				}
				li.AddEdge(new Edge(endLineNum, dependType));
			}
			htr.remove(key);
		}
		// The "else" case is that we have not have not read from the register before,
		// and therefore we do not need to do anything else.
	}

// Used to add the registers we just read from the current line
// searching for any depdendencies
	static void UpdateReadHash(Hashtable<String, ArrayList<Integer>> htr, String key, int lineNum){
		if(key == null){
			return;
		}
		ArrayList<Integer> oldEntry = htr.get(key);
		if(oldEntry != null){
			oldEntry.add(lineNum); // (Register as key, lineNum as value)
		}

		else{
			ArrayList<Integer> newEntry = new ArrayList<Integer>();
			newEntry.add(lineNum); 
			htr.put(key, newEntry);
		}
	}
// Used to add the registers we just wrote to, from the current lime.
// Only need to keep track of one write.
	static void UpdateWriteHash(Hashtable<String, Integer> htw, String key, int lineNum){
		if(key == null){
			return;
		}
		if(htw.get(key) != null){
			htw.remove(key);
		}
		htw.put(key, lineNum);
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
	static ArrayList<Integer> SetupTree(ArrayList<LineInfo> codeBlock){
		Hashtable<String, ArrayList<Integer>> htr = new Hashtable<String, ArrayList<Integer>>(); // hash table of regs read from
		Hashtable<String, Integer> htw = new Hashtable<String, Integer>(); // hash table of regs written to
	
		int lineNumber = codeBlock.size() - 1;
		ArrayList<Integer> ready = new ArrayList<Integer>();
		
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
			ready.add(lineNumber);
			if(li.cmd.equalsIgnoreCase("loadI") && 
				writes[0].equalsIgnoreCase("r0") && writes[1] == null ){  // lame hardcoded replacement for init
				if(!ready.contains(lineNumber))
					ready.add(0,lineNumber);
			}
			else{
				if(reads[1]!=null && reads[0].equalsIgnoreCase("r0")){
					int addr = 1024 + Integer.parseInt(reads[1]); //assumption that r0 always is 1024.
					key1 = String.valueOf(addr);
					Update_RW_Depend(li, key1, htw, ready);
				}
				else {
					// Checks for anti-dependency. htw must have written, then read above it.
					// Only keep key in hashtable if it does contain as others could've read as well
					if(reads[0]!=null){
						key1 = reads[0];
						Update_RW_Depend(li, key1, htw, ready);
					}
					if(reads[1]!=null){
						key2 = reads[1];
						Update_RW_Depend(li, key2, htw, ready);
					}
				}
				if(writes[1] != null && writes[0].equalsIgnoreCase("r0")){
					int addr = 1024 + Integer.parseInt(writes[1]); //assumption that r0 always is 1024.
					key3 = String.valueOf(addr);
					Update_WR_Depend(li, key3, htr, ready);
				}
				else{
					// if writes == size 2, it's a READ, then WRITE
					if(writes[0]!=null){
						key3 = writes[0];
						Update_WR_Depend(li, key3, htr, ready);
					}
					// This is an impossible case that will be only reached by storeAO.
					// ****** Have to change this *****
					if(writes[1]!=null){
						key4 = writes[1];
						Update_WR_Depend(li, key4, htr, ready);
					}
				}
			}
			UpdateReadHash(htr, key1, lineNumber);
			UpdateReadHash(htr, key2, lineNumber);
			UpdateWriteHash(htw, key3, lineNumber);
			UpdateWriteHash(htw, key4, lineNumber);

			lineNumber = lineNumber - 1;
		}
		return ready;
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

	static void AssignCost(ArrayList<Integer> ready, ArrayList<LineInfo> codeBlock){
		ArrayList<Integer> copy = new ArrayList<Integer>();
		//Collections.copy(copy, ready);
		llp(ready, codeBlock);
	}

	static void llp(ArrayList<Integer> copy, ArrayList<LineInfo> codeBlock){
		for(int i = 0; i < copy.size(); i++){
			LineInfo temp = codeBlock.get(copy.get(i));
			recursion(temp, codeBlock);
		}
	}

	static int recursion(LineInfo li, ArrayList<LineInfo> codeBlock){
		if(li.llp != 0){
			return li.llp;
		}
		else if(li.edges.size() == 0){
			li.llp = li.latency;
			return li.latency;
		}
		else{
			int max = 0;
			Edge maxEdge = null;
			for(int i = 0; i < li.edges.size(); i++){
				int a = recursion(codeBlock.get(li.edges.get(i).endLineNum), codeBlock);
				if(a > max){
					max = a;
					maxEdge = li.edges.get(i);
				}
			}
			li.llp = max;
			if(maxEdge.dependType == 1){
				li.llp+=1;
			}
			else if(maxEdge.dependType == 2){
				li.llp+=li.latency;
			}
			// add max of dependencies
			return li.llp;
		}
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

	static void TestDependencies(ArrayList<LineInfo> block){
		for(int i = 0; i < block.size(); i++){
			ArrayList<Edge> edges = block.get(i).edges;
			System.out.println("Node: "+ (i+1));
			System.out.println("Cmd: " + block.get(i).cmd);
			System.out.println("LLP: " + block.get(i).llp);
			for(int j = 0; j < edges.size(); j++){
				System.out.println("To node: " + (edges.get(j).endLineNum+1));
				System.out.println("DependType: " + edges.get(j).dependType);
			}
			System.out.println();
		}
	}
	

	public static void main(String[] args){		
	
		ArrayList<LineInfo> codeBlock = LoadLineInfo();
	//	TestLineInfo(codeBlock);
		ArrayList<Integer> readySet = SetupTree(codeBlock); // codeBlock now has edges in each object
		
		AssignCost(readySet, codeBlock);
		TestDependencies(codeBlock);
		System.out.println("Ready set: ");
		for(int i = 0; i < readySet.size(); i++){
			System.out.println(readySet.get(i)+1);
		}
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
