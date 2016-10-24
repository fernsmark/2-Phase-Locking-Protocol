
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TwoPhaseLocking {
	static HashMap<Integer, TransactionDetails> TransactionTable = new HashMap<Integer, TransactionDetails>();
	static HashMap<String, LockDetails> LockTable = new HashMap<String, LockDetails>();
	static final String activeStatus = "Active";
	static final String blockStatus = "Blocked";
	static final String abortStatus = "Aborted";
	static final String commitStatus = "Committed";
	static final String readLock = "RL";
	static final String writeLock = "WL";
	static final String Begin = "b";
	static final String Read = "r";
	static final String Write = "w";
	static final String Commit = "e";
	static final String Commit2 = "c";
	static final String delimeter = "-";
	static int transTimeStamp = 0;
	static Boolean finalTrans = Boolean.FALSE;
	static Boolean finalTransLockSuccess = Boolean.FALSE;
	static String outputFile = "Output.txt"; // Also Change inside TransactionDetails and LockDetails Class
	static Log log = new Log(outputFile);
	
    public static void main(String[] args) {
        //Read from file stored at the below location
//        String fileName= "schedule.txt";
    	String fileName= "IP2.txt";
    	
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            String recordLine; //used to read the entire file line by line
            String dataItem = "";
            String operation = "";
            int TID = 0;
            
            File file = new File(outputFile);
        	if(file.exists()){
            	file.delete();
            	file.createNewFile();
            }
            else{
            	file.createNewFile();
            }
            
            while ((recordLine = in.readLine()) != null) {
            	
            	if (recordLine.isEmpty()){
            		continue;
            	}
            
                System.out.println("");
                log.writeToFile("");
                
                //Identify the operation, transaction ids, data items operated on
                String[] operationFetch= recordLine.split("[0-9;]+");
                System.out.println(operationFetch[0]);
                log.writeToFile(operationFetch[0]);

                String[] idFetch= recordLine.split("[ecbwr()A-Z;]");
                System.out.println(idFetch[1]);
                log.writeToFile(idFetch[1]);

                String[] dataItemFetch= recordLine.split("[();]");  //ignores (), data_item[0] contains operation + transaction_id


                if (dataItemFetch.length>1){
                	dataItem = dataItemFetch[1].trim();
                    System.out.println(dataItemFetch[1]);
                    log.writeToFile(dataItemFetch[1]);
                }   
                else{
                	dataItem = "";
                }
                
                try{
                	operation = operationFetch[0].trim();
                    TID = Integer.parseInt(idFetch[1].trim());
                }catch(Exception e){
                	System.out.println("Error Reading Line :" + recordLine);
                	log.writeToFile("Error Reading Line :" + recordLine);
                }
                   
                
                performOperation(operation, TID, dataItem);
             
                printTransactionTable();
                printLockTable();
            }
            performFinalCommit();
//            printTransactionTable();
//            printLockTable();


        }
        catch(FileNotFoundException ex) {
            System.out.println("Unable to open file at '" + fileName + "'");
            log.writeToFile("Unable to open file at '" + fileName + "'");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    static void performOperation(String operation, int TID, String dataItem){
    	
    	switch(operation){
    	case Begin:
    		// Begin Operation
    		beginOperation(TID);
    		break;
    		
    	case Read:
    		// Read Operation
    		readWriteOperation(TID, dataItem, Read);
    		break;
    		
    	case Write:
    		// Write Operation
    		readWriteOperation(TID, dataItem, Write);
    		break;
    		
    	case Commit:
    	case Commit2:
    		//Commit Operation
    		commitOperation(TID);
    		break;
    		
    	default:
    		String printMsg = "Invalid Operation Detected : "+ operation;
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    	}
    }
    
    static void beginOperation(int TID){
    	if (! TransactionTable.containsKey(TID)){
    		transTimeStamp++;
    		TransactionDetails transaction = new TransactionDetails(TID, activeStatus, transTimeStamp);
    		TransactionTable.put(TID, transaction);
    	}
    	else{
    		String printMsg = "Error: Transaction Already Started !";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    	}
    }
    
    static void readWriteOperation(int TID, String dataItem, String operation){
    	String printMsg = "";
    	if(!TransactionTable.containsKey(TID)){
    		printMsg = "Error: Transaction T"+TID+" not Started(Begin Operation Not Found) !";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		return;
    	}
    	
    	TransactionDetails transaction = TransactionTable.get(TID);
    	String LockType;
    	if(operation.equals(Read)){
    		LockType = readLock;
    	}
    	else{
    		LockType = writeLock;
    	}
    	
    	
    	switch (transaction.getStatus()){
    	case activeStatus:
    		//Transaction in Active Status hence check Lock Table
    		checkLockTable(TID, dataItem, LockType);
    		break;
    		
    	case blockStatus:
    		if(! finalTrans){
    			//Transaction in Block Status hence add operation to Operation List
    			printMsg = "\nTransaction T"+TID+" in Block Status hence add operation "
						 + operation+delimeter+dataItem +" to Operation List";
        		System.out.println(printMsg);
        		log.writeToFile(printMsg);
        		transaction.operationItemList.add(operation+delimeter+dataItem);
        		TransactionTable.put(TID, transaction);
    		}		

    		break;
    		
    	case abortStatus:
    		printMsg = "Transaction T"+TID+ " Already Aborted...Skipping Operation";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		break;
    		
    	case commitStatus:
    		printMsg = "Transaction T"+TID+ " Already Commited...Error in Operation Sequence";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    	}
    }
    
    static void checkLockTable(int TID, String dataItem, String operation){
    	LockDetails lockDetails = new LockDetails(dataItem, operation);
    	TransactionDetails transaction = TransactionTable.get(TID);
    	
    	//Data Item Currently not Locked by any Transaction
    	if(! LockTable.containsKey(dataItem)){
//    		lockDetails.setOperation(operation);
    		lockDetails.addHoldingTID(TID);
    		if(! finalTrans){
    			transaction.addOperationItem(delimeter+dataItem);
    		}
    		else{
    			finalTransLockSuccess = Boolean.TRUE;
    		}
    	}
    	//Data Item Currently Locked by some other Transaction
    	else{
    		lockDetails = LockTable.get(dataItem);	
    		switch(operation){
    		case readLock:
    				//If Data Item has Read Lock in Lock Table then append TID to Holding TID List
    				if(lockDetails.getOperation().equals(readLock)){
    					lockDetails.addHoldingTID(TID);
    					if(! finalTrans){
							transaction.addOperationItem(delimeter+dataItem);
						}
						else{
			    			finalTransLockSuccess = Boolean.TRUE;
			    		}
    				}
    				//If Data Item has Write Lock in Lock Table
    				else{
    					if(checkWoundWait(TID, Read+delimeter+dataItem,lockDetails.getHoldingTID())){
    						//Transaction Wounded the holding transaction 
    						lockDetails.setOperation(readLock);
    						lockDetails.holdingTID.clear();
    						lockDetails.addHoldingTID(TID);
    						if(! finalTrans){
    							transaction.addOperationItem(delimeter+dataItem);
    						}
    						else{
    			    			finalTransLockSuccess = Boolean.TRUE;
    			    		}
    					}
    					else{
    						//Transaction younger hence added to Waiting List
    						lockDetails.addWaitingTID(TID);
    					}
    				}
    			break;		
    		case writeLock:
	    			if(checkWoundWait(TID, Write+delimeter+dataItem,lockDetails.getHoldingTID())){
						//Transaction Wounded the holding transaction 
	    				lockDetails.setOperation(writeLock);
	    				lockDetails.holdingTID.clear();
						lockDetails.addHoldingTID(TID);
						if(! finalTrans){
							transaction.addOperationItem(delimeter+dataItem);	
						}
						else{
			    			finalTransLockSuccess = Boolean.TRUE;
			    		}
					}
					else{
						//Transaction younger hence added to Waiting List
						lockDetails.addWaitingTID(TID);
					}
    			break;
    		}
    		
    	}
    	// Updating the Lock Table
    	LockTable.put(dataItem, lockDetails);
    	// Updating the Transaction Table
    	TransactionTable.put(TID, transaction);
    }
    
    static Boolean checkWoundWait(int reqTID, String reqOperation, List<Integer> holdingTID){
    	String printMsg = "";
    	Boolean result = Boolean.TRUE;
    	TransactionDetails reqTrans = TransactionTable.get(reqTID);
    	
    	for(Integer holdTID: holdingTID){
    		TransactionDetails holdTrans = TransactionTable.get(holdTID);
    		
    		// Growing Phase i.e Same Transaction RL to WL
    		if(reqTrans.getTimestamp() == holdTrans.getTimestamp()){
    			return result;
    		}
    		
    		//Requesting TID is older than holding TID Wound(abort) the Transaction
    		if(reqTrans.getTimestamp() < holdTrans.getTimestamp()){
    			printMsg = "Requesting T"+reqTID+" is older than holding T"+holdTID
						+": Aborting (Wounding) the Transaction";
    			System.out.println(printMsg);
    			log.writeToFile(printMsg);
    			holdTrans.setStatus(abortStatus);
    			TransactionTable.put(holdTID, holdTrans);
    		}
    		//Requesting TID is younger than holding TID than Wait
    		else{
    			if(result){
	    			result = Boolean.FALSE;
	    			reqTrans.setStatus(blockStatus);
	    			if(! finalTrans){
	    				reqTrans.addOperationItem(reqOperation);
	    			}
	    			TransactionTable.put(reqTID, reqTrans);
	    			printMsg = "Transaction T"+reqTID+ " Waits as lock is placed by older transaction  T"+holdTID;
	    			System.out.println(printMsg);    
	    			log.writeToFile(printMsg);
    			}
    		}
    	} 	
    	return result;
    }
    
    static void commitOperation(int TID){
    	String printMsg = "";
    	if(!TransactionTable.containsKey(TID)){
    		printMsg = "Error: Transaction T"+TID+" not Started(Begin Operation Not Found) !";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		return;
    	}
    	
    	TransactionDetails transaction = TransactionTable.get(TID);
    	
    	switch(transaction.getStatus()){
    	case activeStatus:
    		//Perform any Pending Operations First
    		for(int i =0; i < transaction.operationItemList.size(); i++){
    			String operation = transaction.operationItemList.get(i);
    			String optSplit [] = operation.split(delimeter);
        		
        		if(! optSplit[0].isEmpty()){
	        		//This check is required during Final Commit Transaction for handling case of Commit
	        		if (optSplit.length>1){
	        			Boolean holdFlag = finalTrans;
	        			Boolean holdTransLockSuccess = finalTransLockSuccess;
	        			finalTransLockSuccess = Boolean.FALSE;
	            		finalTrans = Boolean.TRUE;
	        			performOperation(optSplit[0],TID, optSplit[1]);
	        			
	        			if(finalTransLockSuccess){
							transaction.operationItemList.set(i, delimeter+optSplit[1]);
    						TransactionTable.put(TID, transaction);
						}
	        			printTransactionTable();
	        			printLockTable();
	        			
	        			finalTransLockSuccess = holdTransLockSuccess;
	        			finalTrans = holdFlag;
	        		}
        		}
        	}
    		
    		//If Transaction get Blocked or Aborted no need to proceed
//    		transaction = TransactionTable.get(TID);
    		if(transaction.getStatus().contains(blockStatus) | transaction.getStatus().contains(abortStatus)){
    			return;
    		}
    		
    		//Release Locks
    		for(String operation : transaction.operationItemList){
        		String optSplit [] = operation.split(delimeter);
        		
        		//This check is required during Final Commit Transaction for handling case of Commit
        		if (optSplit.length>1){
        			releaseLock(TID, optSplit[1]);
        		}
        		
        	}
    		transaction.setStatus(commitStatus);
    		printMsg = "Transaction T"+TID+" Commited Successfully";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		break;
    		
    	case blockStatus:
    		printMsg = "Transaction T"+TID+" is in Blocked Status hence cannot Commit";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		
    		if(! finalTrans){
    			// If its not Final Commit then add operation to Operation List
    			transaction.addOperationItem(Commit+delimeter);
    		}
    		break;
    		
    	case abortStatus:
    		printMsg = "Transation T"+TID+" is already Aborted !";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		break;
    		
    	case commitStatus:
    		printMsg = "Transation T"+TID+" is already Commited !";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		break;
    	}
    	
    }
    
    static void releaseLock(int TID, String dataItem){
    	String printMsg = "";
    	
    	if(! LockTable.containsKey(dataItem)){
    		return;
    	}
    	LockDetails lockDetails = LockTable.get(dataItem);
    	
    	//If Transaction has already Released the Lock for the data Item (to handle Read and then Write Operation on same Data Item)
    	if(! lockDetails.holdingTID.contains(TID)){
    		return;
    	}
    	
    	switch(lockDetails.getOperation()){
    	case readLock:		
    		for(int i = 0; i < lockDetails.holdingTID.size(); i++){
    			if(lockDetails.holdingTID.get(i) == TID){
    				lockDetails.holdingTID.remove(i);
    				
    				printMsg = "\nFor Data Item "+dataItem+" "+readLock+" is released by T"+TID;
    				System.out.println(printMsg);
    				log.writeToFile(printMsg);
    				
    				break;
    			}
    		}
    		if(lockDetails.holdingTID.isEmpty()){
    			for(int i = 0 ; i < lockDetails.waitingTID.size(); i++){
    				int nextTID  = lockDetails.waitingTID.get(i);
    				lockDetails.waitingTID.remove(i);
    				String lockType = getNextValidTransactionOperation(nextTID, dataItem);
    				if (! lockType.isEmpty()){
    					
    					printMsg = "Transaction T"+nextTID+ " getting "+lockType+" on "+dataItem;
    					System.out.println(printMsg);
    					log.writeToFile(printMsg);
    					
    	    			lockDetails.addHoldingTID(nextTID);
    	    			lockDetails.setOperation(lockType);
    	    			break;
    				}
    			}
    		}
    		
    		break;
    		
    	case writeLock:
  	
    		lockDetails.holdingTID.clear();
    		
    		printMsg = "\nFor Data Item "+dataItem+" "+writeLock+" is released by T"+TID;
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		
			for(int i = 0 ; i < lockDetails.waitingTID.size(); i++){
				int nextTID  = lockDetails.waitingTID.get(i);
				lockDetails.waitingTID.remove(i);
				String lockType = getNextValidTransactionOperation(nextTID, dataItem);
				if (! lockType.isEmpty()){
					
					printMsg = "Transaction T"+nextTID+ " getting "+lockType+" on "+dataItem;
					System.out.println(printMsg);
					log.writeToFile(printMsg);
	    			
					lockDetails.addHoldingTID(nextTID);
	    			lockDetails.setOperation(lockType);
	    			break;
				}
			}
    	
    		break;
    	}
    	if(lockDetails.holdingTID.isEmpty() && lockDetails.waitingTID.isEmpty()){
    		LockTable.remove(dataItem);
    	}
    	else{
    		LockTable.put(dataItem, lockDetails);
    	}
    	
    }
    
    static String getNextValidTransactionOperation(int TID, String dataItem){
    	String printMsg = "";
    	String lockType = "";
    	TransactionDetails transaction = TransactionTable.get(TID);	
    	
    	switch(transaction.getStatus()){
    	case blockStatus:
    		// Transaction Status changed from Blocked to Active
    		transaction.setStatus(activeStatus);
    		
    		// Getting operation from Operation List of the transaction
    		for(int i = 0; i < transaction.operationItemList.size(); i++){
    			String [] operation = transaction.operationItemList.get(i).split(delimeter);
    				
    			//If operation is not Blank then read the operation and dataItem
    			if(!operation[0].isEmpty() && operation[1].equals(dataItem)){
    				if(operation[0].equals(Read)){
    					lockType = readLock;
    				}
    				else{
    					lockType = writeLock;
    				}
    					
    				// Since Operation will now get the Lock, removing the operation from Operation List
    				transaction.operationItemList.set(i, delimeter+dataItem);
    				break;
    			}
    		}
    		break;
    	
    	case abortStatus:
    		// Transaction already Aborted
    		printMsg = "Transaction T"+TID+ 
					   " Already Aborted...hence removing from Waiting List of Data Item "+dataItem;
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    		
    		break;
    	}		
    	TransactionTable.put(TID, transaction); 	
    	return lockType;
    }
    
    static void performFinalCommit(){
    	String printMsg = "";
    	Boolean Executed = Boolean.FALSE;
    	Boolean [] doneTrans = new Boolean[TransactionTable.keySet().size()];
    	finalTrans = Boolean.TRUE;
    	
    	for(int i =0; i< doneTrans.length; i++){
			doneTrans[i] = Boolean.FALSE;
		}  
    	
    	while(!Executed){
    		//Looping through each of the Transaction
    		for(int TID : TransactionTable.keySet()){
    			
    			TransactionDetails transaction = TransactionTable.get(TID);
    			
    			if(transaction.status.contains(activeStatus) | transaction.status.contains(blockStatus)){
    				for(int i = 0; i < transaction.operationItemList.size(); i++){
    					String opt = transaction.operationItemList.get(i);
    					String [] operation = opt.split(delimeter);
    					if(! operation[0].isEmpty()){
    						String dataItem = "";
    						if(operation.length >1){
    							dataItem = operation[1];
    						}
    						
    						printMsg = "Performing T"+TID+" :   "+ operation[0]+ "   "+ dataItem;
    						System.out.println(printMsg);
    						log.writeToFile(printMsg);
    						
    						finalTransLockSuccess = Boolean.FALSE;
    						
    						performOperation(operation[0], TID, dataItem);
    						
    						if(! dataItem.isEmpty() && finalTransLockSuccess){
    							transaction.operationItemList.set(i, delimeter+dataItem);
        						TransactionTable.put(TID, transaction);
    						}				
    						
    						printTransactionTable();
    						printLockTable();
    					}
    				}
    			}
    			else{
    				//Transaction in Commit or Abort status
    				doneTrans[TID-1] = Boolean.TRUE;
    			}
    		}
    		
    		//Checking whether all Transaction is in Commit or Abort Status
    		for(int i =0; i< doneTrans.length; i++){
    			if(doneTrans[i]){
    				Executed = Boolean.TRUE;
    			}
    			else{
    				Executed = Boolean.FALSE;
    				break;
    			}
    		}    		
    	}
    	
    	printMsg = "\n\n~~~~~~~END~~~~~~~~~~";
    	System.out.println(printMsg);
    	log.writeToFile(printMsg);
    	
    }
    
    
    static void printTransactionTable(){
    	String printMsg = "";
    	printMsg = "\nTransaction Table:\n";
    	System.out.println(printMsg);
    	log.writeToFile(printMsg);
    	
    	printMsg = "| Trans ID \t\t"+
				   "| Status \t\t"+
				   "| Time Stamp \t\t"+
				   "| Operation List \t\t\t\t\t\t|";
    	System.out.println(printMsg);
    	log.writeToFile(printMsg);
    	
    	printMsg = "-------------------------------------------------------------------"
				 + "----------------------------------------------------------------------";
    	System.out.println(printMsg);
    	log.writeToFile(printMsg);
    	
    	for(int TID : TransactionTable.keySet()){
    		TransactionDetails transaction = TransactionTable.get(TID);
    		
    		printMsg = "| "+TID+ " \t\t\t"+
					   "| "+transaction.getStatus()+ " \t\t" +
					   "| "+transaction.timestamp+ " \t\t\t" +
					   "| "+transaction.operationItemList.toString()+ " \t\t\t\t\t\t|";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    	}
    }
    
    static void printLockTable(){
    	String printMsg = "";
    	
    	printMsg = "\nLock Table:\n";
    	System.out.println(printMsg);
    	log.writeToFile(printMsg);
    	
    	printMsg = "| Data Item \t\t"+
				   "| Lock State \t\t"+
				   "| Lock Holding Transaction \t\t"+
				   "| Waiting Transaction \t\t\t|";
    	System.out.println(printMsg);
    	log.writeToFile(printMsg);
    	
    	printMsg = "-------------------------------------------------------------------"
				 + "--------------------------------------------------------------";
    	System.out.println(printMsg);
    	log.writeToFile(printMsg);
    	
    	for(String dataItem : LockTable.keySet()){
    		LockDetails lockDetails = LockTable.get(dataItem);
    		
    		printMsg = "| "+dataItem+ " \t\t\t" +
					   "| "+lockDetails.getOperation()+" \t\t\t" +
					   "| "+lockDetails.holdingTID.toString()+" \t\t\t\t\t" +
					   "| "+lockDetails.waitingTID.toString()+ " \t\t\t\t\t|";
    		System.out.println(printMsg);
    		log.writeToFile(printMsg);
    
    	}
    }
}

class TransactionDetails {
	String status;
	int TID;
	int timestamp;
	List<String> operationItemList = new ArrayList<String>();
	Log logfile = new Log("Output.txt");
	
	TransactionDetails(int TID,String status, int timestamp){
		this.TID = TID;
		this.status = status;
		this.timestamp = timestamp;
		System.out.println(	"\nTransaction T"+this.TID+
							" with Status '"+this.status+"'"+
							" and Timestamp "+this.timestamp + " has Begun Successfully"
						  );
		logfile.writeToFile("\nTransaction T"+this.TID+
							" with Status '"+this.status+"'"+
							" and Timestamp "+this.timestamp + " has Begun Successfully");
	}
	
	void setStatus(String status){
		System.out.println(	"\nStatus for Transaction T"+this.TID+ 
						 	" changed from '"+this.status+"' to '"+status+"'");
		logfile.writeToFile("\nStatus for Transaction T"+this.TID+ 
						 	" changed from '"+this.status+"' to '"+status+"'");
		this.status = status;
	}
	
	void setTimestamp(int timestamp){
		this.timestamp = timestamp;
	}
	
	void addOperationItem(String operationItem){
		this.operationItemList.add(operationItem);
		System.out.println("For T"+this.TID+" : Operation "+operationItem+" added to operation list");
		logfile.writeToFile("For T"+this.TID+" : Operation "+operationItem+" added to operation list");
	}
	
	
	String getStatus(){
		return this.status;
	}
	
	int getTimestamp(){
		return this.timestamp;
	}
	
	List<String> getItemsLocked(){
		return this.operationItemList;
	}
}

class LockDetails {
	String Item;
	String operation;
	List<Integer> holdingTID = new ArrayList<Integer>();
	List<Integer> waitingTID = new ArrayList<Integer>();
	Log logFile = new Log("Output.txt");
	
	LockDetails(String dataItem, String operation) {
		this.Item = dataItem;
		this.operation = operation;
	}
	
	void setOperation(String operation){
		System.out.println(	"\nFor Item "+this.Item+
							" in Lock Table: State change from '"+this.operation+"'"+
							" to '"+operation+"'");
		logFile.writeToFile("\nFor Item "+this.Item+
							" in Lock Table: State change from '"+this.operation+"'"+
							" to '"+operation+"'");
		this.operation = operation;
	}
	
	void setHoldingTID(int TID){
		this.holdingTID.clear();
		this.holdingTID.add(TID);
		System.out.println("\nFor Item "+this.Item+" in Lock Table: Setting T"+TID+" to Holding TIDs");
		logFile.writeToFile("\nFor Item "+this.Item+" in Lock Table: Setting T"+TID+" to Holding TIDs");
	}
	
	void addHoldingTID(int TID){
		this.holdingTID.add(TID);
		System.out.println("\nFor Item "+this.Item+" in Lock Table: Adding T"+TID+" to Holding TIDs");
		logFile.writeToFile("\nFor Item "+this.Item+" in Lock Table: Adding T"+TID+" to Holding TIDs");
	}
	
	void setWaitingTID(int TID){
		this.waitingTID.clear();
		this.waitingTID.add(TID);
		System.out.println("\nFor Item "+this.Item+" in Lock Table: Setting T"+TID+" to Waiting TIDs");
		logFile.writeToFile("\nFor Item "+this.Item+" in Lock Table: Setting T"+TID+" to Waiting TIDs");
	}
	
	void addWaitingTID(int TID){
		this.waitingTID.add(TID);
		System.out.println("\nFor Item "+this.Item+" in Lock Table: Adding T"+TID+" to Waiting TIDs");
		logFile.writeToFile("\nFor Item "+this.Item+" in Lock Table: Adding T"+TID+" to Waiting TIDs");
	}
	
	String getOperation(){
		return this.operation;
	}
	
	List<Integer> getHoldingTID(){
		return this.holdingTID;
	}
	
	List<Integer> getWaitingTID(){
		return this.waitingTID;
	}
}

class Log{
	String fileName ="";
	
	Log(String fileName){
		this.fileName = fileName;
	}
	
	void writeToFile(String data){
		BufferedWriter outFile = null;
		try {
            File file = new File(this.fileName);
            
            outFile = new BufferedWriter(new FileWriter(file, Boolean.TRUE));
            outFile.write(data);
            outFile.newLine();
            outFile.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
	}
}