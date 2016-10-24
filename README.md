# 2-Phase-Locking-Protocol
Rigorous 2 Phase Locking protocol with wound-wait method for dealing with deadlock.  

An application in JAVA that simulates wound wait 2PL concurrency control protocol to on schedule of interleaved transactions thus preventing deadlocks.  
The application accepts a schedule file and applies 2PL with deadlock prevention method and creates a log file consisting of the history of each operation as well as the final state of all the transactions in the schedule.  
The result is verified by comparing the history & final states of the transaction with manually traversing each operation sequentially in the input schedule file. 
