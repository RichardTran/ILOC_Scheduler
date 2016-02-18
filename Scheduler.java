import java.util.ArrayList;
import java.util.Scanner;
import java.util.Hashtable;

// At most two pieces of information are always on either side of the '=>'
class LineInfo{
	
	String cmd;
	String[] regReads = new String[2];
	String[] regWrites = new String[2];
	int latency;
	
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
	}
}

class Node{
	int cycleTimer; // Used as a timer from inital call to finish. Decerements as cyccle increments
	
	// for easier access, keep track of both parents and children
	ArrayList<Node> directed;
	ArrayList<Node> parents;

	public Node(){
		int cycleTimer = 0;
		String instr = null;
		directed = new ArrayList<Node>();
		parents = new ArrayList<Node>();
	}

}

public class Scheduler{
	
	// Using an ArrayList is ideal as each index would represent a line number of a code block
	static ArrayList<LineInfo> ParseFileInput(){
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

	/*
	static Node SetupTree(ArrayList<String> codeBlock){
		Hashtable<String, Integer> ht = new Hashtable<String, Integer>();
		int numOfLines = codeBlock.length() - 1;
		
		// from bottom to top, scan block of code, and create dependency graph
		while(numOfLines >= 0){
			codeBlock.get(numOfLines);
				
		}		
		return null;
	}
	*/
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
		
	
		ArrayList<LineInfo> codeBlock = ParseFileInput();
		TestLineInfo(codeBlock);
		
		// When building the dependeny graph, we do not need to track output dependencies as per assignment
		// instructions.
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
