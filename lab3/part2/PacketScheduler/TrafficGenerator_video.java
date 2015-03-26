import java.io.*; 
import java.util.*; 

/* 
 *  The program reads an input file "data.txt"  that has entries of the form 
 *  0	0.000000	I	536	98.190	92.170	92.170
 *  4	133.333330	P	152	98.190	92.170	92.170
 * 	1	33.333330	B	136	98.190	92.170	92.170
 *
 * The file is read line-by-line, values are parsed and assigned to variables,
 * values are  displayed, and then written to a file with name "output.txt"  
 */

class TrafficGenerator_video {  
	public static void main (String[] args) { 
		
		
		BufferedReader bis = null; 
		String currentLine = null; 
		PrintStream pout = null;

                int I_frame_size = 0;
                int I_frame_no = 0;
                int P_frame_size = 0;
                int P_frame_no = 0;
                int B_frame_size = 0;
                int B_frame_no = 0;

                long last_frame_time = 0;

		
		try {  
			
			/*
			 * Open input file as a BufferedReader
			 */ 
			File fin = new File("short_movie.data"); 
			FileReader fis = new FileReader(fin);  
			bis = new BufferedReader(fis);  
                        Sender mySender = new Sender("127.0.0.1");
			byte tag = 0x02;
			
			/*
			 * Open file for output 
			 */
			FileOutputStream fout =  new FileOutputStream("generator.txt");
			pout = new PrintStream (fout);
                        ArrayList<String> input_content = new ArrayList<String>();
			while ( (currentLine = bis.readLine()) != null) { 
                            input_content.add(currentLine);

                        }
			
                        System.out.println("finshied reading");
			/*
			 *  Read file line-by-line until the end of the file 
			 */
                        for (String _currentLine: input_content) {
				
				/*
				 *  Parse line and break up into elements 
				 */
				StringTokenizer st = new StringTokenizer(_currentLine); 
				String col1 = st.nextToken(); 
				String col2 = st.nextToken(); 
				String col3  = st.nextToken(); 
				String col4  = st.nextToken(); 

				Float __Ftime = Float.parseFloat(col2);
				int _Ftime = Math.round(__Ftime);
				long Ftime = (long) _Ftime;
				int Fsize 	= Integer.parseInt(col4);

				/*
				 *  Write line to output file 
				 */
				long time_delta = 33l*1000000l;
				long start_time = System.nanoTime();
				while ((System.nanoTime() - start_time) < time_delta){;}
//				System.out.println("tool: "+ (System.nanoTime()-start_time)/1000000);
				last_frame_time = Ftime;
				
				mySender.send(Fsize, tag);
			}
		} catch (IOException e) {  
			// catch io errors from FileInputStream or readLine()  
			System.out.println("IOException: " + e.getMessage());  
                        e.printStackTrace();
		} finally {  
			// Close files   
			if (bis != null) { 
				try { 
					bis.close(); 
					pout.close();
				} catch (IOException e) { 
					System.out.println("IOException: " +  e.getMessage());  
				} 
			} 
		} 
	}
}
