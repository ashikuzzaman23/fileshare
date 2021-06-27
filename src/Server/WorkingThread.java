package Server;

import Tools.NetworkUtil;
import javafx.stage.FileChooser;
import sun.nio.ch.Net;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Toufik on 9/27/2017.
 */
public class WorkingThread implements Runnable{
    ServerThread serverThread;
    Thread thread;
    String ID;
    NetworkUtil netUtil;

    public WorkingThread(ServerThread serverThread, String id, NetworkUtil nu) throws IOException
    {
        this.serverThread = serverThread;
        this.ID = id;
        this.netUtil = nu;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        outer:while(true)
        {
            String str = (String )netUtil.read();
            if(str==null)
            {

            }
            else if(str.equals("receive"))
            {
                String msz = "send";
                netUtil.write(msz);
                msz = (String) netUtil.read();
                if(serverThread.studentConnectionList.containsKey(msz))
                {
                    String receiver = msz;
                    msz = "yes";
                    netUtil.write(msz);
                    long size =(long) netUtil.read();
                    if(size<=serverThread.Avialable())
                    {
                        netUtil.write(msz);
                        String fileName =(String) netUtil.read();
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        String fileID = ID+ ":"+receiver+":";
                        fileID += fileName+ ":"+timestamp.toString();
                        serverThread.controller1.log.appendText("FileID: "+fileID+"is being received\n");
                        long chunksize;
                        long receivedSize=0;
                        Random random = new Random();
                        chunksize = (random.nextInt(100)+1)*1000;
                        if(size<chunksize) chunksize=size;
                        netUtil.write(chunksize);
                        ArrayList<byte[]> chunks = new ArrayList<>();
                        serverThread.fileChunk.put(fileID,chunks);
                        while(true)
                        {
                            Object obj = netUtil.read();
                            if(obj instanceof String )
                            {
                                String rep = (String) obj;
                                if(rep.equals("time out"))
                                {
                                    serverThread.controller1.log.appendText(fileID+" Transmission timed out\n");
                                    serverThread.fileChunk.remove(fileID);
                                    serverThread.Increase(receivedSize);
                                    break;
                                }
                                else if(rep.equals("cDisconnect"))
                                {
                                    serverThread.controller1.log.appendText("File Transmisson aborted for: "+fileID+"\n");
                                    serverThread.fileChunk.remove(fileID);
                                    serverThread.Increase(receivedSize);
                                    break;
                                }
                                else if(!rep.equals("ok")||receivedSize!=size)
                                {

                                    serverThread.fileChunk.remove(fileID);
                                    serverThread.Increase(receivedSize);
                                    serverThread.controller1.log.appendText("File Transmisson aborted for: "+fileID+"\n");
                                    String ack = "File Transmisson aborted for: "+fileID+"\n";
                                    netUtil.write(ack);
                                    break;
                                }
                                else
                                {
                                    String ack = "Successfully uploaded "+fileID;
                                    serverThread.controller1.log.appendText(ack+"\n");
                                    netUtil.write(ack);
                                    NetworkUtil recepient;
                                    recepient = serverThread.studentConnectionList.get(receiver);
                                    String knock ="receive";
                                    recepient.write(knock);
                                    recepient.write(fileID);
                                    break;
                                }
                            }
                            else if(obj instanceof byte[])
                            {
                                byte[] b = (byte[]) obj;
                                chunks.add(b);
                                if((serverThread.Avialable()-b.length)>0)
                                {
                                    serverThread.Decrese(b.length);
                                    receivedSize+=b.length;
                                    String ack = "yes";
                                    netUtil.write(ack);
                                }
                                else
                                {
                                    serverThread.fileChunk.remove(fileID);
                                    serverThread.Increase(receivedSize);
                                    String ack = "nob";
                                    serverThread.controller1.log.appendText("File Transmisson aborted for: "+fileID+"\n");
                                    netUtil.write(ack);
                                    break;
                                }
                            }
                        }
                    }
                    else
                    {
                        msz = "no";
                        netUtil.write(msz);
                    }
                }
                else
                {
                    msz = "no";
                    netUtil.write(msz);
                }
            }

            else if(str.equals("send"))
            {
                String fileID =(String) netUtil.read();
                String rep = (String) netUtil.read();
                ArrayList<byte[]> arrayList = serverThread.fileChunk.get(fileID);
                long size =0;
                for(int i=0;  i<arrayList.size();i++)
                {
                    size+=arrayList.get(i).length;
                }
                if(!rep.equals("no"))
                {
                    /*for(int i=0;i<arrayList.size();i++)
                    {
                        netUtil.write(arrayList.get(i));
                    }
                    rep="complete";
                    netUtil.write(rep);*/
                    try
                    {
                        for(int i=0;i<arrayList.size();i++)
                        {
                            netUtil.ewrite(arrayList.get(i));
                        }
                        rep="complete";
                        netUtil.write(rep);
                    }
                    catch (Exception e)
                    {
                        serverThread.controller1.log.appendText("File sending Failed\n");
                    }
                }
                serverThread.fileChunk.remove(fileID);
                serverThread.Increase(size);
            }
            else if(str.equals("cDisconnect"))
            {
                netUtil.write(str);
                netUtil.closeConnection();
                serverThread.studentConnectionList.remove(ID);
                break outer;
            }
        }
    }
}
