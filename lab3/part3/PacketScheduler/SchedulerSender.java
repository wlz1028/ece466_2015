package PacketScheduler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Removes and sends packets from buffers to a given address and port.
 */
public class SchedulerSender implements Runnable
{	
	/**
	 * senderActiveUntil holds the time (in ns) when next packet can be sent, i.e. time when last sending ends.
	 * NOTE: this is not the actual time it takes to send packet, 
	 * it is the time it would take to send packet at given link capacity.
	 */
	private long senderActiveUntil;
	
	// destination port
	private int destPort;
	// destination address
	private InetAddress destAddress;
	// socket used to send packets
	private DatagramSocket socket;
	// buffers from which packets are sent
	private Buffer[] buffers;
	// link capacity at which packet scheduler operates (pbs)
	private long linkCapacity;

	
	/**
	 * Constructor. Creates socket.
	 * @param buffers Buffers from which packets are sent.
	 * @param destAddress IP address to which packets are sent.
	 * @param destPort Port to which packets are sent.
	 * @param linkCapacity Link capacity at which FIFO scheduler operates (bps).
	 */
	public SchedulerSender(Buffer[] buffers, InetAddress destAddress, int destPort, long linkCapacity)
	{
		this.senderActiveUntil = 0l;
		this.buffers = buffers;
		this.destAddress = destAddress;
		this.destPort = destPort;
		this.linkCapacity = linkCapacity;
		
		try
		{
			socket = new DatagramSocket();
		}
		catch (SocketException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Send packet using socket.
	 * @param packet Packet to send.
	 * @param startTime Time when sending of this packet was started.
	 */
	public synchronized void sendPacket(DatagramPacket packet, long startTime)
	{
		try 
		{
			// change destination of packet (do forwarding)
			packet.setAddress(destAddress);
			packet.setPort(destPort);
			
			// time it would take to send packet with given link capacity
			long sendingTime = (long)((((float)packet.getLength()*8)/linkCapacity)*1000000000);
			
//			System.out.println("Send packet");
			socket.send(packet);
			
			// time before next packet can be sent (simulate link capacity)
			long timeToWait = sendingTime - (System.nanoTime() - startTime);
			// set when next packet can be sent
			senderActiveUntil = startTime+timeToWait;
		}
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	/**
	 * Remove packets form buffers and send them.
	 * This method is invoked when starting a thread for this class.
	 */
	public void run()
	{
		float[] L = {100.0f,100.0f,100.0f};
		float[] w = {3.0f, 1.0f, 1.0f};

		float[] x = new float[3];
		float Xmin = Float.MAX_VALUE;
		int[] packetNo = new int[3];

		//step 1
		for (int i=0; i<3; i++){
			x[i] = w[i]/L[i];
		}

		//step 2: find min x
		for (int i=0; i<3; i++){
			if (x[i] < Xmin) {
				Xmin = x[i];
			}
		}

		//step 2: get packet per round
		for (int i=0; i<3; i++){
			packetNo[i] = (int) Math.ceil(x[i] / Xmin);
			System.out.println("Packet to Send = "+packetNo[i]);
		}

		
		try{
			PrintStream[] pOut = {new PrintStream (new FileOutputStream("send_q_1_2.txt")),
						new PrintStream (new FileOutputStream("send_q_2_2.txt")),
						new PrintStream (new FileOutputStream("send_q_3_2.txt")),
						};

			while(true)
			{
				DatagramPacket packet = null;	
				// number of empty buffers
				int noEmpty = 0;
				
				// get time when next packet can be sent 
				long startTime = System.nanoTime();
				long nextSendOK = senderActiveUntil;
				
				// if no packet is in transmission look for next packet to send
				if (System.nanoTime() >= nextSendOK)
				{
					/*
					 * Check if there is a packet in queue.
					 * If there is send packet, remove it form queue.
					 * If there is no packet increase noEmpty that keeps track of number of empty queues 
					 */

					for (int i=0; i<3; i ++){
						if ((buffers[i].peek() != null)) {
							int counter = 0;
							while (((packet = buffers[i].peek()) != null) && (counter < packetNo[i])){
								pOut[i].println(System.nanoTime()+"\t"+packet.getLength());
								sendPacket(packet, startTime);
								buffers[i].removePacket();
								counter ++;
							}
//							System.out.println("Queue = "+i+" sent packets = "+counter);
						} else {
							noEmpty++;
						}
					}
//					if ((packet = buffers[0].peek()) != null)
//					{
//						sendPacket(packet, startTime);
//						buffers[0].removePacket();
////						System.out.println("Send q1");
//					}
//					else if ((packet = buffers[1].peek()) != null)
//					{
//						noEmpty++;
//						sendPacket(packet, startTime);
//						buffers[1].removePacket();
////						System.out.println("Send q2");
//					} else {
//						noEmpty++;
//					}
					
					/*
					 * TODO:
					 * Implement sending of a SINGLE packet from packet scheduler.
					 * Variable noEmpty must be set to total number of queues if all are empty. 
					 * 
					 * NOTE: The code you are adding sends at most one packet!
					 * 
					 * Look at the example above to find out how to check if a particular queue is empty.
					 * Once you have found from which queue to send, send a packet and remove it from that queue 
					 * (as in example above).
					 */
				}
				else
				{
					// wait until it is possible to send
					long timeToWait = nextSendOK-startTime;
					long start_time = System.nanoTime();
					while ((System.nanoTime() - start_time) < timeToWait){;}
					continue;
				}
				
				// there are no packets in buffers to send. Wait for one to arrive to buffer.
				// (busy wait)
				if (noEmpty == buffers.length)
				{	
					boolean anyNotEmpty = false;
					for (int i=0; i<buffers.length; i++)
					{
						if (buffers[i].getSize()>0)
						{
							anyNotEmpty = true;
						}
					}
					while(!anyNotEmpty)
					{
						for (int i=0; i<buffers.length; i++)
						{
							if (buffers[i].getSize()>0)
							{
								anyNotEmpty = true;
							}
						}
					}
				}	
			}
		} 
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	


	}
}
